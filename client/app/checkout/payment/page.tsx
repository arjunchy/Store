"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ProtectedRoute } from "@/components/AuthGuard";
import { CheckoutStepper } from "@/components/CheckoutStepper";
import { useCart } from "@/context/CartContext";
import { getSelectedAddress, getDelivery, getPayment, setPayment } from "@/lib/checkout";
import type { PaymentMethod } from "@/lib/types";

const formatUSD = formatNPR;
const WALLET_OPTIONS: {
  id: PaymentMethod;
  label: string;
  sub: string;
  badge?: string;
  brandColor: string;
  brandBg: string;
  logo: string;
}[] = [
  {
    id: "esewa",
    label: "eSewa",
    sub: "Pay with eSewa wallet",
    badge: "Popular in Nepal",
    brandColor: "#1AA16B",
    brandBg: "#e6f6ee",
    logo: "account_balance_wallet",
  },
  {
    id: "khalti",
    label: "Khalti",
    sub: "Pay with Khalti wallet",
    badge: "Fast & Secure",
    brandColor: "#5C2D91",
    brandBg: "#f3e8ff",
    logo: "wallet",
  },
];

export default function PaymentMethodPage() {
  return (
    <ProtectedRoute>
      <PaymentInner />
    </ProtectedRoute>
  );
}

function PaymentInner() {
  const router = useRouter();
  const { cart, subtotal } = useCart();
  const [method, setMethod] = useState<PaymentMethod>("esewa");
  const [checking, setChecking] = useState(true);
  const [addressMissing, setAddressMissing] = useState(false);
  const [deliveryMethod, setDeliveryMethod] = useState<"standard" | "express">("standard");

  const deliveryCost = deliveryMethod === "express" ? 15 : 0;
  const tax = Math.round(subtotal * 0.08 * 100) / 100;
  const total = subtotal + tax + deliveryCost;
  const itemCount = cart.reduce((n, i) => n + (i.qty ?? i.quantity ?? 1), 0);

  useEffect(() => {
    let cancelled = false;
    async function init() {
      try {
        const [addr, savedPayment, savedDelivery] = await Promise.all([
          getSelectedAddress(),
          getPayment(),
          getDelivery(),
        ]);
        if (!addr) {
          if (!cancelled) {
            setAddressMissing(true);
            setChecking(false);
          }
          return;
        }
        if (!cancelled) {
          if (savedPayment.method === "esewa" || savedPayment.method === "khalti") setMethod(savedPayment.method);
          if (savedDelivery.method === "standard" || savedDelivery.method === "express") setDeliveryMethod(savedDelivery.method);
          setChecking(false);
        }
      } catch {
        if (!cancelled) {
          setAddressMissing(true);
          setChecking(false);
        }
      }
    }
    init();
    return () => {
      cancelled = true;
    };
  }, [router]);

  const handleSelect = async (m: PaymentMethod) => {
    setMethod(m);
    await setPayment({ method: m, sameBilling: true });
  };

  const handleContinue = async () => {
    await setPayment({ method, sameBilling: true });
    router.push("/checkout/review");
  };

  if (checking) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
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
          <p className="text-[13px] text-[#57534e] font-medium">Verifying checkout...</p>
        </div>
      </div>
    );
  }

  if (addressMissing) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
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
        <div className="flex-grow flex flex-col items-center justify-center gap-4 px-4 py-16 text-center">
          <div className="w-14 h-14 rounded-full bg-[#fee2e2] flex items-center justify-center">
            <span className="material-symbols-outlined text-[#b91c1c] text-[28px]">location_off</span>
          </div>
          <h2 className="font-semibold text-[18px] text-[#1c1917]">Shipping address required</h2>
          <p className="text-[14px] text-[#57534e] max-w-md">Please select a shipping address before choosing payment.</p>
          <Link href="/checkout/shipping" className="mt-2 px-6 py-3 rounded-lg bg-[#b45309] text-white font-semibold text-[14px] hover:bg-[#92400e] flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[18px]">arrow_back</span> Go to Shipping
          </Link>
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

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 xl:py-xl grid grid-cols-1 lg:grid-cols-12 gap-xl">
        <div className="lg:col-span-8 flex flex-col gap-8">
          <CheckoutStepper currentStep={3} />

          <div className="pt-2">
            <h1 className="font-bold text-[32px] md:text-[42px] leading-tight text-[#1c1917] mb-2">Payment Method</h1>
            <p className="font-body-lg text-body-lg text-[#57534e]">All transactions are secure and encrypted. Choose eSewa or Khalti.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {WALLET_OPTIONS.map((w) => {
              const active = method === w.id;
              return (
                <button
                  key={w.id}
                  onClick={() => handleSelect(w.id)}
                  aria-pressed={active}
                  className={`relative text-left rounded-[16px] p-5 flex flex-col gap-3 border-2 transition-all group ${
                    active
                      ? "border-[#b45309] bg-[#fafaf9] shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.1)]"
                      : "border-[#d6d3d1] bg-white hover:border-[#b45309]/50 hover:shadow-sm hover:-translate-y-[1px]"
                  }`}
                >
                  <div className="absolute top-4 right-4">
                    <div
                      className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${active ? "border-[#b45309]" : "border-[#d6d3d1] group-hover:border-[#b45309]"}`}
                    >
                      <div className={`w-2.5 h-2.5 rounded-full transition-transform ${active ? "bg-[#b45309] scale-100" : "scale-0"}`} />
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
                      style={{ background: w.brandBg, color: w.brandColor }}
                    >
                      <span className="material-symbols-outlined text-[28px]">{w.logo}</span>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-[16px] text-[#1c1917]">{w.label}</span>
                        {w.badge && (
                          <span
                            className="px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wide"
                            style={{ background: w.brandBg, color: w.brandColor }}
                          >
                            {w.badge}
                          </span>
                        )}
                      </div>
                      <p className="text-[12px] text-[#57534e]">{w.sub}</p>
                    </div>
                  </div>

                  <p className="text-[12px] leading-snug text-[#57534e] mt-1">
                    {w.id === "esewa" ? "Wallet, eSewa ID or QR — instant settlement via fonepay." : "Wallet, Khalti ID & MPIN — instant settlement via Khalti API."}
                  </p>

                  <div className="flex items-center gap-1.5 text-[11px] font-medium mt-auto" style={{ color: w.brandColor }}>
                    <span className="material-symbols-outlined text-[14px]">verified</span> Nepal Rastra Bank licensed
                  </div>
                </button>
              );
            })}
          </div>

          <div className="bg-white border border-[#d6d3d1] rounded-[16px] p-5 shadow-sm">
            <div className="flex justify-between items-start gap-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-[#fafaf9] flex items-center justify-center">
                  <span className="material-symbols-outlined text-[#b45309] text-[22px]">lock</span>
                </div>
                <div>
                  <h3 className="font-semibold text-[15px] text-[#1c1917]">
                    {method === "esewa" ? "You will pay with eSewa" : "You will pay with Khalti"}
                  </h3>
                  <p className="text-[12px] text-[#57534e]">You will be redirected to {method === "esewa" ? "eSewa" : "Khalti"} to authorize payment securely.</p>
                </div>
              </div>
              <span className="hidden sm:inline-flex items-center gap-1 text-[11px] font-medium px-2 py-1 rounded-full bg-[#fafaf9] text-[#57534e]">
                <span className="material-symbols-outlined text-[14px]">shield</span> Escrow protected
              </span>
            </div>

            <div className="mt-4 rounded-xl bg-[#fafaf9] p-4 border border-dashed border-[#d6d3d1]">
              <p className="text-[12px] font-semibold text-[#1c1917] mb-1">Payment flow (SIMULATED)</p>
              <p className="text-[12px] leading-relaxed text-[#57534e]">
                On <span className="font-medium text-[#1c1917]">Place Order</span>, ApexCommerce creates the order then automatically calls
                <code className="mx-1 px-1.5 py-0.5 rounded bg-[#fafaf9] text-[11px]">POST /api/payments</code>
                with <code className="px-1 py-0.5 rounded bg-[#fafaf9] text-[11px]">{"{ orderId, method, amount }"}</code>.
                The simulated gateway approves instantly and returns a transactionId, setting the order `payment_status=PAID`
                (auto-CONFIRMED). Payment history is available via
                <code className="mx-1 px-1 py-0.5 rounded bg-[#fafaf9] text-[11px]">GET /api/payments/order/{"{orderId}"}</code>.
                No redirect is required — the gateway is stubbed until real eSewa/Khalti keys are configured.
              </p>
              <div className="mt-2 flex flex-wrap gap-2">
                <span className="inline-flex items-center gap-1 px-2 py-1 rounded-full bg-[#fafaf9] text-[11px] font-medium text-[#57534e]">
                  <span className="w-2 h-2 rounded-full bg-[#b45309] animate-pulse" /> TEST mode
                </span>
                <span className="text-[11px] text-[#57534e]">No card data collected — wallets handle PCI.</span>
              </div>
            </div>

            <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
              {[
                { icon: "bolt", title: "Instant", desc: "Settlement in seconds" },
                { icon: "shield", title: "Secure", desc: "OTP + MPIN + escrow" },
                { icon: "receipt_long", title: "Refundable", desc: "7-day easy refunds" },
              ].map((f) => (
                <div key={f.title} className="flex items-center gap-2 p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
                  <span className="material-symbols-outlined text-[#b45309] text-[20px]">{f.icon}</span>
                  <div>
                    <p className="text-[12px] font-semibold text-[#1c1917]">{f.title}</p>
                    <p className="text-[11px] text-[#57534e]">{f.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex flex-col sm:flex-row justify-between gap-3">
            <Link
              href="/checkout/delivery"
              className="px-6 py-3 border border-[#d6d3d1] rounded-lg text-[13px] font-semibold text-[#1c1917] hover:bg-[#fafaf9] flex items-center justify-center gap-1"
            >
              <span className="material-symbols-outlined text-[16px]">arrow_back</span> Back to Delivery
            </Link>
            <button
              onClick={handleContinue}
              className="px-8 py-3 rounded-lg text-[13px] font-semibold text-white bg-[#b45309] hover:bg-[#92400e] flex items-center justify-center gap-2 shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Continue to Review <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>
        </div>

        <div className="lg:col-span-4">
          <div className="bg-white rounded-[16px] shadow-sm border border-[#d6d3d1] p-6 sticky top-24 flex flex-col gap-4">
            <h2 className="font-semibold text-[18px] text-[#1c1917] border-b border-surface-variant pb-3">Order Summary</h2>

            {cart.length === 0 ? (
              <div className="py-8 text-center flex flex-col items-center gap-3">
                <span className="material-symbols-outlined text-[#a8a29e] text-[40px]">shopping_cart</span>
                <p className="text-[14px] text-[#57534e]">Your cart is empty.</p>
                <Link href="/catalog" className="text-[#b45309] text-[13px] font-semibold hover:underline">
                  Browse products
                </Link>
              </div>
            ) : (
              <>
                <div className="space-y-4 max-h-[280px] overflow-y-auto pr-1">
                  {cart.map((item) => (
                    <div key={item.id} className="flex gap-4">
                      <div className="w-16 h-16 bg-[#fafaf9] rounded-lg overflow-hidden shrink-0 relative flex items-center justify-center">
                        {item.image ? (
                          <Image src={item.image} alt={item.name} fill unoptimized className="object-cover" sizes="64px" onError={(e) => {(e.target as HTMLImageElement).style.display = "none";}} />
                        ) : (
                          <span className="material-symbols-outlined text-[#a8a29e] text-[24px]">image</span>
                        )}
                      </div>
                      <div className="flex-grow min-w-0">
                        <h3 className="font-semibold text-[13px] text-[#1c1917] line-clamp-1">{item.name}</h3>
                        <p className="text-[12px] text-[#57534e]">Qty: {item.qty ?? item.quantity ?? 1} • {formatUSD(item.price)} each</p>
                        <p className="font-semibold text-[13px] text-[#b45309] mt-1">{formatUSD(item.price * (item.qty ?? item.quantity ?? 1))}</p>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="border-t border-surface-variant pt-4 space-y-2">
                  <div className="flex justify-between text-[14px] text-[#57534e]">
                    <span>Subtotal ({itemCount} items)</span>
                    <span className="text-[#1c1917] font-medium">{formatUSD(subtotal)}</span>
                  </div>
                  <div className="flex justify-between text-[14px] text-[#57534e]">
                    <span>Shipping ({deliveryMethod})</span>
                    <span className="text-[#1c1917] font-medium">{deliveryCost === 0 ? "Free" : formatUSD(deliveryCost)}</span>
                  </div>
                  <div className="flex justify-between text-[14px] text-[#57534e]">
                    <span>Tax (8%)</span>
                    <span className="text-[#1c1917] font-medium">{formatUSD(tax)}</span>
                  </div>
                </div>

                <div className="border-t border-surface-variant pt-4 flex justify-between items-center">
                  <span className="font-semibold text-[16px] text-[#1c1917]">Total</span>
                  <span className="font-bold text-[24px] text-[#b45309]">{formatUSD(total)}</span>
                </div>

                <div className="flex items-center gap-2 text-[12px] text-[#57534e] bg-[#fafaf9] p-3 rounded-lg">
                  <span className="material-symbols-outlined text-[#b45309] text-[16px]">info</span>
                  <span>
                    Paying with <strong className="text-[#1c1917] capitalize">{method}</strong>. You will be redirected securely.
                  </span>
                </div>

                <p className="text-center text-[11px] text-[#57534e] leading-relaxed">By continuing, you agree to our Terms & Privacy Policy. Wallets are NRB-licensed.</p>
              </>
            )}
          </div>
        </div>
      </main>

      <footer className="w-full px-margin-desktop py-6 mt-8 max-w-[1440px] mx-auto flex flex-col md:flex-row justify-between gap-4 bg-white border-t border-[#d6d3d1]">
        <div className="font-bold text-[18px] text-[#b45309] flex items-center gap-2">
          <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>
            shopping_bag
          </span>{" "}
          ApexCommerce
        </div>
        <div className="flex flex-wrap gap-4 text-[13px] text-[#57534e]">
          <Link href="/catalog" className="hover:text-[#b45309]">
            About Us
          </Link>
          <Link href="/catalog" className="hover:text-[#b45309]">
            Terms
          </Link>
          <Link href="/catalog" className="hover:text-[#b45309]">
            Privacy
          </Link>
          <Link href="/catalog" className="hover:text-[#b45309]">
            Customer Service
          </Link>
          <Link href="/catalog" className="hover:text-[#b45309]">
            Contact
          </Link>
          <Link href="/catalog" className="hover:text-[#b45309]">
            Shipping Info
          </Link>
        </div>
        <div className="text-[13px] text-[#57534e]">© 2024 ApexCommerce. All rights reserved.</div>
      </footer>
    </div>
  );
}
