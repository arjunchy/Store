"use client";

import { formatNPR } from "@/lib/format";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ProtectedRoute } from "@/components/AuthGuard";
import { CheckoutStepper } from "@/components/CheckoutStepper";
import { useAuth } from "@/context/AuthContext";
import type { ShippingAddress, DeliveryMethod, PaymentMethod } from "@/lib/types";
import type { CartItem } from "@/lib/types";

type Snapshot = {
  address: ShippingAddress | null;
  delivery: DeliveryMethod;
  payment: PaymentMethod;
  cart: CartItem[];
  subtotal: number;
  deliveryCost: number;
  tax: number;
  total: number;
  itemCount: number;
  placedAt: string;
};

const formatUSD = formatNPR;
function formatDate(iso: string) {
  try {
    return new Intl.DateTimeFormat("en-US", { day: "numeric", month: "short", year: "numeric" }).format(new Date(iso));
  } catch {
    return iso;
  }
}

function addDays(date: Date, days: number) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

export default function OrderConfirmedPage() {
  return (
    <ProtectedRoute>
      <OrderConfirmedInner />
    </ProtectedRoute>
  );
}

function OrderConfirmedInner() {
  const { user } = useAuth();
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [orderNumber, setOrderNumber] = useState<string>("");
  const [livePaid, setLivePaid] = useState<boolean | null>(null);

  useEffect(() => {
    try {
      const raw = localStorage.getItem("apexcommerce_last_order_snapshot");
      if (raw) {
        const parsed = JSON.parse(raw) as Snapshot;
        setSnapshot(parsed);
      }
      const oid = localStorage.getItem("apexcommerce_last_order_id");
      if (oid) setOrderNumber(oid.startsWith("#") ? oid : `#${oid.slice(0, 8).toUpperCase()}`);
    } catch {}
    try {
      const oid = localStorage.getItem("apexcommerce_last_order_id")?.replace(/^#/, "") || "";
      const snapRaw = localStorage.getItem("apexcommerce_last_order_snapshot");
      let sId = "";
      if (snapRaw) { try { sId = JSON.parse(snapRaw).orderId || ""; } catch {} }
      const targetId = sId || oid;
      if (targetId) {
        import("@/lib/api-client").then(({ apiClient }) => {
          apiClient.get<any>(`/orders/${targetId}`, { auth: true }).then((o) => {
            const ps = (o.paymentStatus || o.payment_status || "").toString().toUpperCase();
            setLivePaid(ps === "PAID");
          }).catch(() => setLivePaid(null));
        });
      }
    } catch {}
  }, []);

  const delivery = snapshot?.delivery ?? "standard";
  const payment = snapshot?.payment ?? "esewa";
  const address = snapshot?.address ?? null;
  const cart = snapshot?.cart ?? [];
  const subtotal = snapshot?.subtotal ?? 0;
  const deliveryCost = snapshot?.deliveryCost ?? 0;
  const tax = snapshot?.tax ?? 0;
  const total = snapshot?.total ?? snapshot?.subtotal ?? 0;
  const placedAt = snapshot?.placedAt ?? new Date().toISOString();
  const estimatedFrom = addDays(new Date(placedAt), delivery === "express" ? 1 : 3);
  const estimatedTo = addDays(new Date(placedAt), delivery === "express" ? 2 : 5);

  const hasSnapshot = !!snapshot && cart.length > 0;

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9] antialiased">
      <header className="bg-[#fafaf9] shadow-sm sticky top-0 z-50">
        <div className="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop py-3 max-w-[1440px] mx-auto">
          <Link href="/" className="font-headline-md text-headline-md font-bold text-[#b45309]">
            ApexCommerce
          </Link>
          <div className="font-label-md text-label-md text-[#57534e] flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px]">verified</span> Order Confirmed
          </div>
        </div>
      </header>

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 xl:py-xl">
        <div className="w-full max-w-3xl mx-auto mb-10">
          <CheckoutStepper currentStep={4} completed />
        </div>

          <div className="max-w-3xl mx-auto text-center mb-8">
            <div className="relative inline-flex items-center justify-center mb-5">
              <div className="absolute w-28 h-28 rounded-full bg-[#b45309]/10 animate-pulse" />
              <div className="relative w-24 h-24 rounded-full bg-[#b45309] text-white flex items-center justify-center shadow-[0_8px_24px_rgba(53,37,205,0.25)]">
                <span className="material-symbols-outlined text-[48px]">check_circle</span>
              </div>
              <div className="absolute -top-1 -right-1 w-8 h-8 rounded-full bg-white border border-[#d6d3d1] shadow-sm flex items-center justify-center">
                <span className="material-symbols-outlined text-[#b45309] text-[18px]">celebration</span>
              </div>
            </div>
            <h1 className="font-bold text-[32px] md:text-[44px] leading-tight text-[#1c1917]">{hasSnapshot ? "Order Placed Successfully!" : "Order Confirmed"}</h1>
            <p className="font-body-lg text-body-lg text-[#57534e] max-w-xl mx-auto mt-3">
              {hasSnapshot ? <>Thank you{user?.username ? `, ${user.username.split(" ")[0]}` : ""} — we&apos;ve received your order and are getting it ready to ship. A confirmation email will arrive shortly.</> : "Your order is confirmed. Check Orders for live status."}
            </p>
            <div className={`mt-4 inline-flex items-center gap-2 px-3 py-1.5 rounded-full border text-[12px] ${livePaid === false ? "bg-amber-50 border-amber-200 text-amber-800" : "bg-[#fafaf9] border-[#d6d3d1] text-[#57534e]"}`}>
              <span className={`w-2 h-2 rounded-full animate-pulse ${livePaid === false ? "bg-amber-500" : "bg-emerald-500"}`} /> {livePaid === false ? "Payment pending — complete payment in Orders" : "Order is being processed"}
              <span className="hidden sm:inline">• Track anytime in Orders</span>
            </div>
            {livePaid === false && (
              <div className="mt-3 max-w-xl mx-auto bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-[13px] text-amber-900 text-left">
                <p className="font-semibold">Payment not yet confirmed</p>
                <p className="text-[12px] text-amber-800 mt-1">Your order is saved as UNPAID. Your cart was preserved. Go to Orders to retry payment or restore cart if needed.</p>
                <Link href="/orders" className="inline-flex mt-2 px-4 py-2 rounded-full bg-[#b45309] text-white text-[12px] font-semibold">Go to Orders — Pay Now</Link>
              </div>
            )}
          </div>

        <div className="max-w-3xl mx-auto">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
            <div className="bg-white p-6 rounded-[16px] border border-[#d6d3d1] shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-semibold text-[16px] text-[#1c1917]">Order Details</h2>
                <span className={`px-2 py-1 rounded-full text-[10px] font-bold tracking-wide border ${livePaid === false ? "bg-amber-50 text-amber-700 border-amber-200" : livePaid === true ? "bg-emerald-50 text-emerald-700 border-emerald-200" : "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]"}`}>{livePaid === false ? "UNPAID" : livePaid === true ? "PAID" : "PENDING"}</span>
              </div>
              <div className="space-y-2.5 text-[13px]">
                <div className="flex justify-between items-center py-2 border-b border-surface-variant">
                  <span className="text-[#57534e]">Order Number</span>
                  <span className="font-mono font-semibold text-[#1c1917]">{orderNumber}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-surface-variant">
                  <span className="text-[#57534e]">Order Date</span>
                  <span className="font-semibold text-[#1c1917]">{formatDate(placedAt)}</span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-surface-variant">
                  <span className="text-[#57534e]">Payment</span>
                  <span className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-[11px] font-semibold border ${payment === "esewa" ? "bg-[#e6f6ee] border-[#a7e3c5] text-[#116a43]" : "bg-[#f3e8ff] border-[#d8b4fe] text-[#5C2D91]"}`}>
                    <span className="material-symbols-outlined text-[14px]">{payment === "esewa" ? "account_balance_wallet" : "wallet"}</span> {payment === "esewa" ? "eSewa" : "Khalti"} • {livePaid === false ? "Unpaid" : livePaid === true ? "Paid" : "Pending"}
                  </span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-surface-variant">
                  <span className="text-[#57534e]">Delivery</span>
                  <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${delivery === "express" ? "bg-tertiary-fixed text-on-tertiary-fixed" : "bg-[#fafaf9] text-[#57534e]"}`}>
                    {delivery === "express" ? "Express" : "Standard"} • {deliveryCost === 0 ? "Free" : formatUSD(deliveryCost)}
                  </span>
                </div>
                <div className="flex justify-between items-center py-2 border-b border-surface-variant">
                  <span className="text-[#57534e]">Estimated Delivery</span>
                  <span className="font-semibold text-[#b45309]">
                    {formatDate(estimatedFrom.toISOString())} – {formatDate(estimatedTo.toISOString())}
                  </span>
                </div>
                <div className="flex justify-between items-center pt-2">
                  <span className="font-bold text-[14px] text-[#1c1917]">Total Amount</span>
                  <span className="font-bold text-[18px] text-[#b45309]">{formatUSD(hasSnapshot ? total : 0)}</span>
                </div>
              </div>
            </div>

            <div className="bg-white p-6 rounded-[16px] border border-[#d6d3d1] shadow-sm">
              <div className="flex items-center gap-2 mb-4">
                <span className="w-8 h-8 rounded-lg bg-[#fafaf9] flex items-center justify-center">
                  <span className="material-symbols-outlined text-[#b45309] text-[18px]">local_shipping</span>
                </span>
                <h2 className="font-semibold text-[16px] text-[#1c1917]">Shipping Information</h2>
              </div>
              {address ? (
                <div className="space-y-1 text-[13px]">
                  <p className="font-bold text-[#1c1917]">{address.label || "Shipping Address"}</p>
                  <p className="text-[#57534e]">{address.street}</p>
                  <p className="text-[#57534e]">
                    {address.city}
                    {address.state ? `, ${address.state}` : ""} {address.postal_code || address.zip}
                  </p>
                  <p className="text-[#57534e]">{address.country}</p>
                  <div className="mt-3 pt-3 border-t border-surface-variant space-y-1">
                    <p className="text-[#57534e] flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[16px]">mail</span> {user?.email ?? "email on file"}
                    </p>
                    <p className="text-[#57534e] flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[16px]">call</span> Delivery updates via SMS
                    </p>
                  </div>
                </div>
              ) : (
                <div className="space-y-1 text-[13px]">
                  <p className="font-semibold text-[#1c1917]">{user?.username ?? "Valued Customer"}</p>
                  <p className="text-[#57534e]">No address on file</p>
                  <p className="text-[11px] text-[#57534e] mt-2 bg-amber-50 border border-amber-200 rounded-lg px-2 py-1">
                    Shipping address will be shown here after order creation.
                  </p>
                </div>
              )}
              <div className="mt-4 p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] flex items-start gap-2">
                <span className="material-symbols-outlined text-[#b45309] text-[18px]">info</span>
                <p className="text-[11px] leading-snug text-[#57534e]">You’ll receive tracking details once shipped. Est. dispatch within 24h.</p>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-[16px] border border-[#d6d3d1] shadow-sm mb-8 overflow-hidden">
            <div className="px-6 py-4 bg-[#fafaf9]-bright border-b border-[#d6d3d1] flex items-center justify-between">
              <h2 className="font-semibold text-[16px] text-[#1c1917]">Order Summary • {hasSnapshot ? `${cart.length} items` : "2 items"}</h2>
              <span className="text-[11px] font-medium px-2 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">Confirmed</span>
            </div>

            {hasSnapshot ? (
              <>
                <div className="divide-y divide-[#d6d3d1]/50">
                  {cart.map((item) => (
                    <div key={item.id} className="flex gap-4 p-4 hover:bg-[#fafaf9] transition-colors">
                      <div className="w-20 h-20 rounded-xl bg-[#fafaf9] overflow-hidden shrink-0 relative flex items-center justify-center border border-[#d6d3d1]/20">
                        {item.image ? (
                          <Image src={item.image} alt={item.name} fill unoptimized className="object-cover" sizes="80px" onError={(e) => {(e.target as HTMLImageElement).style.display = "none";}} />
                        ) : (
                          <span className="material-symbols-outlined text-[#a8a29e] text-[24px]">image</span>
                        )}
                      </div>
                      <div className="flex-grow min-w-0">
                        <h3 className="font-semibold text-[13px] text-[#1c1917] line-clamp-1">{item.name}</h3>
                        <p className="text-[11px] text-[#57534e] mt-1">Qty: {item.qty ?? item.quantity ?? 1} • {formatUSD(item.price)} each</p>
                        <div className="flex justify-between items-center mt-2">
                          <span className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-[#fafaf9] text-[#57534e]">
                            <span className="material-symbols-outlined text-[12px]">verified</span> Premium
                          </span>
                          <span className="font-bold text-[13px] text-[#1c1917]">{formatUSD(item.price * (item.qty ?? item.quantity ?? 1))}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="px-6 py-4 bg-[#fafaf9]/50 border-t border-[#d6d3d1] space-y-2 text-[13px]">
                  <div className="flex justify-between text-[#57534e]">
                    <span>Subtotal</span>
                    <span className="font-medium text-[#1c1917]">{formatUSD(subtotal)}</span>
                  </div>
                  <div className="flex justify-between text-[#57534e]">
                    <span>Shipping ({delivery})</span>
                    <span className="font-medium text-[#1c1917]">{deliveryCost === 0 ? "Free" : formatUSD(deliveryCost)}</span>
                  </div>
                  <div className="flex justify-between text-[#57534e]">
                    <span>Tax (8%)</span>
                    <span className="font-medium text-[#1c1917]">{formatUSD(tax)}</span>
                  </div>
                  <div className="flex justify-between items-center pt-3 border-t border-[#d6d3d1]">
                    <span className="font-bold text-[#1c1917]">Total Paid</span>
                    <span className="font-bold text-[18px] text-[#b45309]">{formatUSD(total)}</span>
                  </div>
                </div>
              </>
            ) : (
              <div className="p-10 text-center flex flex-col items-center gap-3">
                <span className="material-symbols-outlined text-[#a8a29e] text-[40px]">receipt</span>
                <p className="text-[13px] text-[#57534e]">No order snapshot found. Place an order to see summary here, or <Link href="/orders" className="text-[#b45309] underline">view your orders</Link>.</p>
              </div>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-8">
            {[
              { icon: "shield", title: "Buyer Protection", desc: "Escrow until delivered" },
              { icon: "replay", title: "7-Day Returns", desc: "No questions asked" },
              { icon: "support_agent", title: "24/7 Support", desc: "Chat & phone help" },
            ].map((f) => (
              <div key={f.title} className="flex items-center gap-3 p-4 rounded-[16px] bg-white border border-[#d6d3d1]">
                <span className="w-9 h-9 rounded-full bg-[#b45309]/10 text-[#b45309] flex items-center justify-center">
                  <span className="material-symbols-outlined text-[18px]">{f.icon}</span>
                </span>
                <div>
                  <p className="text-[12px] font-bold text-[#1c1917]">{f.title}</p>
                  <p className="text-[11px] text-[#57534e]">{f.desc}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="flex flex-col sm:flex-row justify-center items-center gap-3">
            <Link href="/orders" className="w-full sm:w-auto px-6 py-3 bg-[#b45309] text-white font-semibold text-[13px] rounded-full hover:bg-[#92400e] transition-colors shadow-sm flex items-center justify-center gap-2">
              <span className="material-symbols-outlined text-[18px]">local_shipping</span> Track Order
            </Link>
            <Link href="/orders" className="w-full sm:w-auto px-6 py-3 bg-white text-[#1c1917] font-semibold text-[13px] rounded-full border border-[#d6d3d1] hover:bg-[#fafaf9] transition-colors shadow-sm flex items-center justify-center gap-2">
              <span className="material-symbols-outlined text-[18px]">history</span> View Order History
            </Link>
            <Link href="/catalog" className="w-full sm:w-auto px-6 py-3 text-[#b45309] font-semibold text-[13px] rounded-full hover:bg-[#fafaf9] transition-colors flex items-center justify-center gap-2">
              Continue Shopping <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </Link>
          </div>
        </div>
      </main>

      <footer className="bg-white border-t border-[#d6d3d1] mt-auto">
        <div className="w-full px-margin-desktop py-6 max-w-[1440px] mx-auto flex flex-col md:flex-row justify-between items-center gap-3">
          <div className="font-bold text-[18px] text-[#b45309]">ApexCommerce</div>
          <div className="flex gap-4 text-[13px] text-[#57534e]">
            <Link href="/catalog" className="hover:text-[#b45309]">
              Help
            </Link>
            <Link href="/catalog" className="hover:text-[#b45309]">
              Returns
            </Link>
            <Link href="/catalog" className="hover:text-[#b45309]">
              Contact
            </Link>
          </div>
          <div className="text-[13px] text-[#57534e]">© 2024 ApexCommerce. All rights reserved.</div>
        </div>
      </footer>
    </div>
  );
}
