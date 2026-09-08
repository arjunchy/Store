"use client";

import { formatNPR } from "@/lib/format";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ProtectedRoute } from "@/components/AuthGuard";
import { CheckoutStepper } from "@/components/CheckoutStepper";
import { useCart } from "@/context/CartContext";
import { createOrder } from "@/lib/order";
import {
  getSelectedAddress,
  getDelivery,
  getPayment,
  clearCheckout,
} from "@/lib/checkout";
import type { ShippingAddress, DeliveryMethod, PaymentMethod } from "@/lib/types";

const formatUSD = formatNPR;
export default function OrderReviewPage() {
  return (
    <ProtectedRoute>
      <ReviewInner />
    </ProtectedRoute>
  );
}

function ReviewInner() {
  const router = useRouter();
  const { cart, subtotal, clearCart, refresh } = useCart();
  const [placing, setPlacing] = useState(false);
  const [error, setError] = useState("");
  const [address, setAddress] = useState<ShippingAddress | null>(null);
  const [delivery, setDelivery] = useState<DeliveryMethod>("standard");
  const [payment, setPayment] = useState<PaymentMethod>("esewa");
  const [checking, setChecking] = useState(true);

  const deliveryCost = delivery === "express" ? 15 : 0;
  const tax = Math.round(subtotal * 0.08 * 100) / 100;
  const total = subtotal + deliveryCost + tax;
  const itemCount = cart.reduce((n, i) => n + (i.qty ?? i.quantity ?? 1), 0);

  useEffect(() => {
    let cancelled = false;
    async function loadCheckoutState() {
      try {
        const [addr, del, pay] = await Promise.all([getSelectedAddress(), getDelivery(), getPayment()]);
        if (!cancelled) {
          setAddress(addr);
          if (del.method === "standard" || del.method === "express") setDelivery(del.method);
          if (pay.method === "esewa" || pay.method === "khalti") setPayment(pay.method);
          setChecking(false);
        }
      } catch {
        if (!cancelled) setChecking(false);
      }
    }
    loadCheckoutState();
    return () => {
      cancelled = true;
    };
  }, []);

  const handlePlaceOrder = async () => {
    setError("");
    if (cart.length === 0) {
      setError("Your cart is empty — add items before placing an order.");
      return;
    }
    if (!address) {
      setError("No shipping address selected. Please select an address at the shipping step.");
      return;
    }
    setPlacing(true);
    try {
      const order = await createOrder({
        addressId: address.id,
        paymentMethod: payment,
        shipping: deliveryCost,
        tax,
      });

      // Persist snapshot for order-confirmed page (expects apexcommerce_last_order_snapshot/id)
      try {
        const snapshot = {
          address,
          delivery,
          payment,
          cart: [...cart],
          subtotal,
          deliveryCost,
          tax,
          total,
          itemCount,
          placedAt: new Date().toISOString(),
          orderId: order.id,
          orderNumber: (order as any).orderNumber || order.id,
        };
        localStorage.setItem("apexcommerce_last_order_snapshot", JSON.stringify(snapshot));
        localStorage.setItem("apexcommerce_last_order_id", (order as any).orderNumber || order.id);
      } catch {}

      await clearCheckout().catch(() => {});

      // Ensure frontend cart reflects server clear – remove ordered items (entire cart for clothing branch)
      try {
        await clearCart();
      } catch {
        try { await refresh(); } catch {}
      }

      // Direct success – payment is handled separately (wallet/cod). No gateway redirect required for clothing branch.
      router.push("/order-confirmed");
    } catch (e: unknown) {
      const msg = (e as any)?.data?.message || (e as Error)?.message || "Failed to place order. Please try again.";
      setError(msg);
      setPlacing(false);
      console.warn("handlePlaceOrder failed", e);
    }
  };

  if (checking) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9] antialiased">
        <header className="bg-[#fafaf9] shadow-sm sticky top-0 z-50">
          <div className="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop py-3 max-w-[1440px] mx-auto">
            <Link href="/" className="font-headline-md text-headline-md font-bold text-[#b45309]">
              ApexCommerce
            </Link>
            <div className="font-label-md text-label-md text-[#57534e] flex items-center gap-2">
              <span className="material-symbols-outlined text-[20px]">lock</span> Secure Checkout
            </div>
          </div>
        </header>
        <div className="flex-grow flex flex-col items-center justify-center gap-3 px-4 py-16">
          <div className="w-8 h-8 rounded-full border-[2.5px] border-surface-variant border-t-primary animate-spin" />
          <p className="text-[13px] text-[#57534e] font-medium">Loading your order summary...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9] antialiased">
      <header className="bg-[#fafaf9] shadow-sm sticky top-0 z-50">
        <div className="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop py-3 max-w-[1440px] mx-auto">
          <Link href="/" className="font-headline-md text-headline-md font-bold text-[#b45309]">
            ApexCommerce
          </Link>
          <div className="font-label-md text-label-md text-[#57534e] flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px]">lock</span> Secure Checkout
          </div>
        </div>
      </header>

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 xl:py-xl">
        <div className="w-full max-w-3xl mx-auto mb-10">
          <CheckoutStepper currentStep={4} />
        </div>

        <div className="text-center mb-8 max-w-3xl mx-auto">
          <h1 className="font-bold text-[32px] md:text-[42px] leading-tight text-[#1c1917]">Review Your Order</h1>
          <p className="font-body-lg text-body-lg text-[#57534e] mt-2">Please review your details before placing the order. You can edit any section below.</p>
        </div>

        {error && (
          <div className="max-w-3xl mx-auto mb-6 bg-[#fee2e2]/40 border border-[#fecaca] rounded-xl px-4 py-3 flex items-start gap-2 text-[#b91c1c] text-[13px]">
            <span className="material-symbols-outlined text-[18px] shrink-0 mt-0.5">error</span>
            <span>{error}</span>
          </div>
        )}

        {(!address || cart.length === 0) && (
          <div className="max-w-3xl mx-auto mb-6 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 flex items-start gap-2 text-amber-800 text-[13px]">
            <span className="material-symbols-outlined text-[18px] shrink-0">warning</span>
            <div>
              {!address && <p>• No shipping address selected — <Link href="/checkout/shipping" className="underline font-semibold">choose address</Link></p>}
              {cart.length === 0 && <p>• Your cart is empty — <Link href="/catalog" className="underline font-semibold">browse products</Link></p>}
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-xl">
          <div className="lg:col-span-8 flex flex-col gap-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-white p-5 rounded-[16px] border border-[#d6d3d1] shadow-sm flex flex-col h-full relative overflow-hidden group hover:shadow-md hover:-translate-y-[2px] transition-all duration-300">
                <div className="absolute top-0 right-0 w-16 h-16 bg-[#fafaf9] rounded-bl-full -z-10 group-hover:bg-[#b45309]/5 transition-colors" />
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-semibold text-[14px] text-[#1c1917] flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[#b45309] text-[18px]">location_on</span> Shipping
                  </h3>
                  <Link href="/checkout/shipping" className="text-[#b45309] font-semibold text-[11px] px-2 py-1 rounded-full bg-[#fef3c7] hover:bg-[#b45309]-fixed transition-colors">
                    Edit
                  </Link>
                </div>
                {address ? (
                  <div className="flex flex-col gap-1 text-[13px] leading-snug">
                    <p className="font-bold text-[#1c1917]">{address.label || "Address"}</p>
                    <p className="text-[#57534e]">{address.street}</p>
                    <p className="text-[#57534e]">
                      {address.city}
                      {address.state ? `, ${address.state}` : ""} {address.postal_code || address.zip}
                    </p>
                    <p className="text-[#57534e]">{address.country}</p>
                    {address.isDefault && <span className="mt-1 inline-flex w-fit px-1.5 py-0.5 rounded text-[10px] font-semibold bg-[#fafaf9] text-[#57534e]">Default</span>}
                  </div>
                ) : (
                  <div className="flex flex-col gap-2">
                    <p className="text-[13px] text-[#b91c1c] font-medium">No address selected</p>
                    <Link href="/checkout/shipping" className="text-[12px] text-[#b45309] font-semibold hover:underline">
                      Add / Select address →
                    </Link>
                  </div>
                )}
              </div>

              <div className="bg-white p-5 rounded-[16px] border border-[#d6d3d1] shadow-sm flex flex-col h-full relative overflow-hidden group hover:shadow-md hover:-translate-y-[2px] transition-all duration-300">
                <div className="absolute top-0 right-0 w-16 h-16 bg-[#fafaf9] rounded-bl-full -z-10 group-hover:bg-[#b45309]/5 transition-colors" />
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-semibold text-[14px] text-[#1c1917] flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[#b45309] text-[18px]">local_shipping</span> Delivery
                  </h3>
                  <Link href="/checkout/delivery" className="text-[#b45309] font-semibold text-[11px] px-2 py-1 rounded-full bg-[#fef3c7] hover:bg-[#b45309]-fixed transition-colors">
                    Edit
                  </Link>
                </div>
                <div className="flex flex-col gap-1.5">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${delivery === "express" ? "bg-tertiary-fixed text-on-tertiary-fixed" : "bg-[#fafaf9] text-[#57534e]"}`}>
                      {delivery === "express" ? "Express" : "Standard"}
                    </span>
                    <span className="text-[11px] text-[#b45309] font-medium">{deliveryCost === 0 ? "Free" : formatUSD(deliveryCost)}</span>
                  </div>
                  <p className="font-bold text-[13px] text-[#1c1917]">{delivery === "express" ? "1-2 business days" : "3-5 business days"}</p>
                  <p className="text-[12px] text-[#57534e] leading-snug">{delivery === "express" ? "Priority handling & fastest delivery." : "Carbon neutral — economical & reliable."}</p>
                  <Link href="/checkout/delivery" className="text-[#b45309] font-medium text-[11px] mt-1 hover:underline">
                    Change method →
                  </Link>
                </div>
              </div>

              <div className="bg-white p-5 rounded-[16px] border border-[#d6d3d1] shadow-sm flex flex-col h-full relative overflow-hidden group hover:shadow-md hover:-translate-y-[2px] transition-all duration-300">
                <div className="absolute top-0 right-0 w-16 h-16 bg-[#fafaf9] rounded-bl-full -z-10 group-hover:bg-[#b45309]/5 transition-colors" />
                <div className="flex justify-between items-start mb-3">
                  <h3 className="font-semibold text-[14px] text-[#1c1917] flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-[#b45309] text-[18px]">wallet</span> Payment
                  </h3>
                  <Link href="/checkout/payment" className="text-[#b45309] font-semibold text-[11px] px-2 py-1 rounded-full bg-[#fef3c7] hover:bg-[#b45309]-fixed transition-colors">
                    Edit
                  </Link>
                </div>
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-2">
                    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${payment === "esewa" ? "bg-[#e6f6ee] text-[#1AA16B]" : "bg-[#f3e8ff] text-[#5C2D91]"}`}>
                      <span className="material-symbols-outlined text-[18px]">{payment === "esewa" ? "account_balance_wallet" : "wallet"}</span>
                    </div>
                    <div>
                      <p className="font-bold text-[13px] text-[#1c1917] capitalize">{payment}</p>
                      <p className="text-[11px] text-[#57534e]">{payment === "esewa" ? "Wallet • eSewa ID / QR" : "Wallet • Khalti ID & MPIN"}</p>
                    </div>
                  </div>
                  <p className="text-[12px] text-[#57534e] leading-snug">You will be redirected to {payment === "esewa" ? "eSewa" : "Khalti"} to authorize securely. No card stored.</p>
                  <span className="inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-1 rounded-full bg-[#fafaf9] text-[#57534e] w-fit">
                    <span className="material-symbols-outlined text-[12px]">verified</span> NRB licensed • Escrow
                  </span>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-[16px] border border-[#d6d3d1] shadow-sm overflow-hidden">
              <div className="p-4 border-b border-[#d6d3d1] bg-[#fafaf9]-bright flex items-center justify-between">
                <h2 className="font-semibold text-[16px] text-[#1c1917]">Order Items ({itemCount})</h2>
                <Link href="/cart" className="text-[#b45309] text-[11px] font-semibold px-2 py-1 rounded-full bg-[#fef3c7] hover:bg-[#b45309]-fixed transition-colors">
                  Edit cart
                </Link>
              </div>
              {cart.length === 0 ? (
                <div className="p-10 text-center flex flex-col items-center gap-3">
                  <div className="w-16 h-16 rounded-full bg-[#fafaf9] flex items-center justify-center">
                    <span className="material-symbols-outlined text-[#a8a29e] text-[32px]">shopping_cart</span>
                  </div>
                  <p className="text-[14px] font-medium text-[#1c1917]">Your cart is empty</p>
                  <p className="text-[12px] text-[#57534e]">Add products to place an order.</p>
                  <Link href="/catalog" className="mt-1 inline-flex bg-[#b45309] text-white px-5 py-2.5 rounded-full text-[13px] font-semibold hover:bg-[#92400e]">
                    Browse products
                  </Link>
                </div>
              ) : (
                <div className="divide-y divide-[#d6d3d1]/50">
                  {cart.map((item) => (
                    <div key={item.id} className="flex gap-4 p-4 hover:bg-[#fafaf9] transition-colors">
                      <Link href={`/product/${item.slug}`} className="w-24 h-24 rounded-xl overflow-hidden bg-[#fafaf9] shrink-0 relative flex items-center justify-center border border-[#e7e5e4]">
                        {item.image ? (
                          <Image src={item.image} alt={item.name} fill unoptimized className="object-cover" sizes="96px" onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }} />
                        ) : (
                          <span className="material-symbols-outlined text-[#a8a29e] text-[28px]">image</span>
                        )}
                      </Link>
                      <div className="flex-grow flex flex-col sm:flex-row justify-between gap-2 min-w-0">
                        <div className="min-w-0">
                          <Link href={`/product/${item.slug}`} className="font-semibold text-[14px] text-[#1c1917] leading-tight hover:text-[#b45309] hover:underline line-clamp-2">
                            {item.name}
                          </Link>
                          <p className="font-body-sm text-body-sm text-[#57534e] mt-1 line-clamp-1">
                            Qty: {item.qty ?? item.quantity ?? 1} • {formatUSD(item.price)} each
                          </p>
                          <span className="inline-flex mt-1.5 items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full bg-[#fafaf9] text-[#57534e]">
                            <span className="material-symbols-outlined text-[12px]">inventory_2</span> In stock
                          </span>
                        </div>
                        <div className="text-left sm:text-right shrink-0">
                          <p className="font-bold text-[16px] text-[#1c1917]">{formatUSD(item.price * (item.qty ?? item.quantity ?? 1))}</p>
                          <p className="text-[11px] text-[#57534e]">Line total</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="lg:col-span-4">
            <div className="bg-white p-6 rounded-[16px] border border-[#d6d3d1] shadow-sm sticky top-24 flex flex-col gap-4">
              <h2 className="font-semibold text-[16px] text-[#1c1917] border-b border-surface-variant pb-3">Order Summary</h2>
              <div className="space-y-2.5 text-[13px] border-b border-[#d6d3d1] pb-4">
                <div className="flex justify-between text-[#57534e]">
                  <span>Subtotal ({itemCount} items)</span>
                  <span className="text-[#1c1917] font-semibold">{formatUSD(subtotal)}</span>
                </div>
                <div className="flex justify-between text-[#57534e]">
                  <span>Shipping ({delivery})</span>
                  <span className={`font-semibold ${deliveryCost === 0 ? "text-[#b45309]" : "text-[#1c1917]"}`}>{deliveryCost === 0 ? "Free" : formatUSD(deliveryCost)}</span>
                </div>
                <div className="flex justify-between text-[#57534e]">
                  <span>Estimated Tax (8%)</span>
                  <span className="text-[#1c1917] font-semibold">{formatUSD(tax)}</span>
                </div>
              </div>

              <div className="flex justify-between items-center">
                <span className="font-bold text-[16px] text-[#1c1917]">Total</span>
                <span className="font-bold text-[24px] text-[#b45309]">{formatUSD(total)}</span>
              </div>

              <div className={`rounded-xl p-3 flex items-center gap-2.5 border ${payment === "esewa" ? "bg-[#e6f6ee] border-[#a7e3c5] text-[#116a43]" : "bg-[#f3e8ff] border-[#d8b4fe] text-[#5C2D91]"}`}>
                <span className="material-symbols-outlined text-[20px]">{payment === "esewa" ? "account_balance_wallet" : "wallet"}</span>
                <div className="text-[12px] leading-tight">
                  <p className="font-semibold capitalize">Pay with {payment}</p>
                  <p className="opacity-80">Redirect to {payment === "esewa" ? "eSewa" : "Khalti"} after placing order</p>
                </div>
              </div>

              <button
                onClick={handlePlaceOrder}
                disabled={placing || cart.length === 0 || !address}
                className="w-full bg-[#b45309] hover:bg-[#92400e] disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-[15px] py-4 px-6 rounded-full shadow-sm hover:shadow-md transition-all flex items-center justify-center gap-2 group"
              >
                {placing ? (
                  <>
                    <span className="w-4 h-4 rounded-full border-2 border-on-primary/30 border-t-on-primary animate-spin" /> Redirecting to {payment === "esewa" ? "eSewa" : "Khalti"}...
                  </>
                ) : (
                  <>
                    Place Order • {formatUSD(total)}
                    <span className="material-symbols-outlined text-[18px] group-hover:translate-x-1 transition-transform">arrow_forward</span>
                  </>
                )}
              </button>

              <div className="flex flex-col gap-2">
                <p className="text-[11px] text-[#57534e] text-center flex items-center justify-center gap-1">
                  <span className="material-symbols-outlined text-[14px]">lock</span> Secure SSL Checkout • NRB licensed wallets
                </p>
                <div className="flex items-center justify-center gap-2 text-[10px] font-medium text-[#57534e]">
                  <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-[#fafaf9]">
                    <span className="material-symbols-outlined text-[12px]">shield</span> Escrow
                  </span>
                  <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-[#fafaf9]">
                    <span className="material-symbols-outlined text-[12px]">replay</span> 7-day returns
                  </span>
                </div>
                <p className="text-[11px] text-[#57534e] text-center leading-relaxed">
                  By placing your order, you agree to our Terms and Privacy Policy.
                </p>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="bg-white border-t border-[#d6d3d1] w-full mt-auto">
        <div className="w-full px-margin-desktop py-4 max-w-[1440px] mx-auto flex flex-col md:flex-row justify-between items-center gap-3">
          <p className="text-[13px] text-[#57534e]">© 2024 ApexCommerce. All rights reserved.</p>
          <div className="flex gap-4">
            <Link href="/catalog" className="text-[13px] text-[#57534e] hover:text-[#b45309]">
              Privacy Policy
            </Link>
            <Link href="/catalog" className="text-[13px] text-[#57534e] hover:text-[#b45309]">
              Terms of Service
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
}