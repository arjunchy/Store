"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ProtectedRoute } from "@/components/AuthGuard";
import { CheckoutStepper } from "@/components/CheckoutStepper";
import { getSelectedAddress, getDelivery, setDelivery } from "@/lib/checkout";
import { useCart } from "@/context/CartContext";

type DeliveryMethod = "standard" | "express";

const formatUSD = formatNPR;
export default function CheckoutDeliveryPage() {
  return (
    <ProtectedRoute>
      <DeliveryInner />
    </ProtectedRoute>
  );
}

function DeliveryInner() {
  const router = useRouter();
  const { cart, subtotal } = useCart();
  const [method, setMethod] = useState<DeliveryMethod>("standard");
  const [checkingAddress, setCheckingAddress] = useState(true);
  const [addressMissing, setAddressMissing] = useState(false);

  const deliveryCost = method === "express" ? 15.0 : 0;
  const tax = Math.round(subtotal * 0.08 * 100) / 100;
  const total = subtotal + tax + deliveryCost;
  const itemCount = cart.reduce((n, i) => n + (i.qty ?? i.quantity ?? 1), 0);

  useEffect(() => {
    getDelivery().then((d) => {
      if (d.method === "standard" || d.method === "express") setMethod(d.method);
    });
  }, []);

  const handleMethodChange = async (m: DeliveryMethod) => {
    setMethod(m);
    await setDelivery({ method: m });
  };

  const handleContinue = async () => {
    await setDelivery({ method });
    router.push("/checkout/payment");
  };

  useEffect(() => {
    let cancelled = false;
    async function verifyAddress() {
      try {
        const addr = await getSelectedAddress();
        if (!addr) {
          if (!cancelled) {
            setAddressMissing(true);
            setCheckingAddress(false);
          }
          return;
        }
        if (!cancelled) setCheckingAddress(false);
      } catch {
        if (!cancelled) {
          setAddressMissing(true);
          setCheckingAddress(false);
        }
      }
    }
    verifyAddress();
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (checkingAddress) {
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
          <p className="text-[13px] text-[#57534e] font-medium">Verifying shipping address...</p>
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
          <p className="text-[14px] text-[#57534e] max-w-md">You need to select or add a shipping address before choosing a delivery method.</p>
          <Link href="/checkout/shipping" className="mt-2 px-6 py-3 rounded-lg bg-[#b45309] text-white font-semibold text-[14px] hover:bg-[#92400e] flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[18px]">arrow_back</span> Go to Shipping
          </Link>
          <p className="text-[12px] text-[#57534e]">Redirecting to shipping...</p>
        </div>
      </div>
    );
  }

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

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 xl:py-xl grid grid-cols-1 lg:grid-cols-12 gap-xl">
        <div className="lg:col-span-8 flex flex-col gap-8">
          <CheckoutStepper currentStep={2} />

          <div className="pt-2">
            <h1 className="font-bold text-[32px] md:text-[42px] leading-tight text-[#1c1917] mb-2">Delivery Method</h1>
            <p className="font-body-lg text-body-lg text-[#57534e]">Choose how you want your order delivered.</p>
          </div>

          <section className="flex flex-col gap-3">
            <label className="block cursor-pointer relative group">
              <input
                type="radio"
                name="delivery_method"
                value="standard"
                checked={method === "standard"}
                onChange={() => handleMethodChange("standard")}
                className="absolute opacity-0 w-0 h-0"
              />
              <div
                className={`flex items-start p-4 rounded-[16px] border bg-white hover:border-[#b45309] transition-all ${
                  method === "standard" ? "border-2 border-[#b45309] shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.1)]" : "border border-[#d6d3d1] shadow-sm hover:-translate-y-[1px]"
                }`}
              >
                <div className="flex-shrink-0 mt-0.5 mr-4">
                  <div
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
                      method === "standard" ? "border-[#b45309]" : "border-[#d6d3d1] group-hover:border-[#b45309]"
                    }`}
                  >
                    <div className={`w-2.5 h-2.5 rounded-full transition-transform ${method === "standard" ? "bg-[#b45309] scale-100" : "bg-transparent scale-0"}`} />
                  </div>
                </div>
                <div className="flex-grow flex justify-between items-start gap-4">
                  <div>
                    <div className="font-label-md text-label-md text-[#1c1917] font-semibold mb-1">Standard Delivery</div>
                    <div className="font-body-sm text-body-sm text-[#57534e]">Estimated delivery in 3-5 business days</div>
                    <div className="text-[12px] text-[#57534e] mt-1 flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">eco</span> Carbon neutral shipping
                    </div>
                  </div>
                  <div className="font-label-md text-label-md text-[#b45309] font-semibold ml-4 shrink-0">Free</div>
                </div>
              </div>
            </label>

            <label className="block cursor-pointer relative group">
              <input
                type="radio"
                name="delivery_method"
                value="express"
                checked={method === "express"}
                onChange={() => handleMethodChange("express")}
                className="absolute opacity-0 w-0 h-0"
              />
              <div
                className={`flex items-start p-4 rounded-[16px] border bg-white hover:border-[#b45309] transition-all ${
                  method === "express" ? "border-2 border-[#b45309] shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.1)]" : "border border-[#d6d3d1] shadow-sm hover:-translate-y-[1px]"
                }`}
              >
                <div className="flex-shrink-0 mt-0.5 mr-4">
                  <div
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
                      method === "express" ? "border-[#b45309]" : "border-[#d6d3d1] group-hover:border-[#b45309]"
                    }`}
                  >
                    <div className={`w-2.5 h-2.5 rounded-full transition-transform ${method === "express" ? "bg-[#b45309] scale-100" : "bg-transparent scale-0"}`} />
                  </div>
                </div>
                <div className="flex-grow flex justify-between items-start gap-4">
                  <div>
                    <div className="font-label-md text-label-md text-[#1c1917] font-semibold mb-1 flex items-center gap-2 flex-wrap">
                      Express Delivery
                      <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-tertiary-fixed text-on-tertiary-fixed text-[10px] font-medium tracking-wide">Fastest</span>
                    </div>
                    <div className="font-body-sm text-body-sm text-[#57534e]">Estimated delivery in 1-2 business days</div>
                    <div className="text-[12px] text-[#57534e] mt-1 flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">bolt</span> Priority handling
                    </div>
                  </div>
                  <div className="font-label-md text-label-md text-[#1c1917] font-semibold ml-4 shrink-0">{formatNPR(15)}</div>
                </div>
              </div>
            </label>
          </section>

          <div className="flex flex-col-reverse sm:flex-row items-center justify-between gap-4 pt-2">
            <Link
              href="/checkout/shipping"
              className="w-full sm:w-auto px-6 py-3 rounded-lg border border-[#d6d3d1] bg-white text-[#1c1917] hover:bg-[#fafaf9] transition-colors font-label-md text-label-md flex items-center justify-center gap-1.5 font-medium"
            >
              <span className="material-symbols-outlined text-[18px]">arrow_back</span>
              Back to Shipping
            </Link>
            <button
              onClick={handleContinue}
              disabled={cart.length === 0}
              className={`w-full sm:w-auto px-6 py-3 rounded-lg font-label-md text-label-md flex items-center justify-center gap-1.5 shadow-sm font-medium transition-colors ${
                cart.length === 0 ? "bg-outline-variant text-[#57534e] cursor-not-allowed opacity-60" : "bg-[#b45309] text-white hover:bg-[#92400e]"
              }`}
            >
              Continue to Payment
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
          </div>
          {cart.length === 0 && <p className="text-[12px] text-[#b91c1c] text-right">Your cart is empty. Add items before continuing.</p>}
        </div>

        <div className="lg:col-span-4">
          <div className="bg-white border border-[#d6d3d1] rounded-[16px] p-6 sticky top-24 shadow-sm flex flex-col gap-4">
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
                <div className="space-y-4 max-h-[320px] overflow-y-auto pr-1">
                  {cart.map((item) => (
                    <div key={item.id} className="flex items-start gap-3">
                      <Link href={`/product/${item.slug}`} className="w-16 h-16 rounded-lg bg-[#fafaf9] overflow-hidden shrink-0 relative flex items-center justify-center">
                        {item.image ? (
                          <Image src={item.image} alt={item.name} fill unoptimized className="object-cover" sizes="64px" onError={(e) => {(e.target as HTMLImageElement).style.display = "none";}} />
                        ) : (
                          <span className="material-symbols-outlined text-[#a8a29e] text-[24px]">image</span>
                        )}
                      </Link>
                      <div className="flex-grow min-w-0">
                        <Link href={`/product/${item.slug}`} className="font-label-md text-label-md text-[#1c1917] font-medium line-clamp-1 leading-tight hover:text-[#b45309] hover:underline">
                          {item.name}
                        </Link>
                        <div className="font-body-sm text-body-sm text-[#57534e]">Qty: {item.qty ?? item.quantity ?? 1}</div>
                        <div className="text-[12px] text-[#57534e]">{formatUSD(item.price)} each</div>
                      </div>
                      <div className="font-label-md text-label-md text-[#1c1917] font-medium shrink-0">{formatUSD(item.price * (item.qty ?? item.quantity ?? 1))}</div>
                    </div>
                  ))}
                </div>

                <div className="flex flex-col gap-3 pt-2">
                  <div className="flex justify-between items-center">
                    <span className="text-[14px] text-[#57534e]">Subtotal ({itemCount} items)</span>
                    <span className="text-[14px] text-[#1c1917] font-semibold">{formatUSD(subtotal)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[14px] text-[#57534e]">Shipping</span>
                    <span className={`text-[13px] font-medium ${method === "standard" ? "text-[#b45309]" : "text-[#1c1917]"}`}>{method === "standard" ? "Free" : formatUSD(15)}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[14px] text-[#57534e]">Taxes (8%)</span>
                    <span className="text-[14px] text-[#1c1917] font-medium">{formatUSD(tax)}</span>
                  </div>
                </div>
                <div className="border-t border-surface-variant pt-4 flex justify-between items-end">
                  <span className="font-semibold text-[16px] text-[#1c1917]">Total</span>
                  <span className="font-bold text-[20px] text-[#b45309]">{formatUSD(total)}</span>
                </div>
                <div className="flex items-start gap-2 bg-[#fafaf9] p-3 rounded-lg">
                  <span className="material-symbols-outlined text-[#b45309] text-[20px] shrink-0">info</span>
                  <p className="text-[13px] leading-snug text-[#57534e]">Taxes and shipping calculated based on your shipping address and delivery method.</p>
                </div>
              </>
            )}
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
