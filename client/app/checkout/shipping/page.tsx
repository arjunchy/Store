"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getShipping, setShipping, addAddress } from "@/lib/checkout";
import { useCart } from "@/context/CartContext";
import type { ShippingAddress } from "@/lib/types";
import { ProtectedRoute } from "@/components/AuthGuard";
import { CheckoutStepper } from "@/components/CheckoutStepper";

export default function ShippingAddressPage() {
  return (
    <ProtectedRoute>
      <ShippingInner />
    </ProtectedRoute>
  );
}

function ShippingInner() {
  const router = useRouter();
  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [showAdd, setShowAdd] = useState(false);
  const [loading, setLoading] = useState(true);
  const [triedContinue, setTriedContinue] = useState(false);
  const [newAddr, setNewAddr] = useState({ label: "", street: "", city: "", state: "", postalCode: "", country: "" });
  const [addrError, setAddrError] = useState("");
  const { subtotal } = useCart();

  const canProceed = !!selected && addresses.length > 0;

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getShipping()
      .then((s) => {
        if (cancelled) return;
        setAddresses(s.addresses);
        if (typeof window !== "undefined") {
          const stored = localStorage.getItem("apexcommerce_selected_address");
          const exists = stored ? s.addresses.find((a) => a.id === stored) : null;
          if (exists) {
            setSelected(stored);
          } else {
            setSelected(s.selectedId);
          }
        } else {
          setSelected(s.selectedId);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setAddresses([]);
          setSelected(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSelect = async (id: string) => {
    setSelected(id);
    setTriedContinue(false);
    const s = await getShipping();
    await setShipping({ ...s, selectedId: id });
  };

  const handleAdd = async () => {
    if (!newAddr.label || !newAddr.street || !newAddr.city || !newAddr.postalCode || !newAddr.country) {
      setAddrError("Label, street, city, postal code and country are required.");
      return;
    }
    setAddrError("");
    const addr = await addAddress({
      label: newAddr.label,
      street: newAddr.street,
      city: newAddr.city,
      state: newAddr.state,
      postal_code: newAddr.postalCode,
      zip: newAddr.postalCode,
      country: newAddr.country,
      is_default: false,
      isDefault: false,
    } as unknown as Omit<ShippingAddress, "id">);
    const s = await getShipping();
    setAddresses(s.addresses);
    setSelected(addr.id);
    await setShipping({ ...s, selectedId: addr.id });
    setTriedContinue(false);
    setShowAdd(false);
    setNewAddr({ label: "", street: "", city: "", state: "", postalCode: "", country: "" });
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <header className="bg-[#fafaf9] shadow-sm sticky top-0 z-50">
        <div className="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop py-3 max-w-[1440px] mx-auto">
          <Link href="/" className="font-headline-md text-headline-md font-bold text-[#b45309]">ApexCommerce</Link>
          <div className="font-label-md text-label-md text-[#57534e] flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px]">lock</span> Secure Checkout
          </div>
        </div>
      </header>

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-8 xl:py-xl grid grid-cols-1 lg:grid-cols-12 gap-xl">
        <div className="lg:col-span-8 flex flex-col gap-8">
          <CheckoutStepper currentStep={1} />

          <div className="pt-6">
            <h1 className="font-bold text-[32px] md:text-[42px] leading-tight text-[#1c1917] mb-2">Shipping Address</h1>
            <p className="font-body-lg text-body-lg text-[#57534e]">Select a saved address or enter a new one.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {addresses.map((addr) => {
              const isSelected = selected === addr.id;
              return (
                <div key={addr.id} onClick={() => handleSelect(addr.id)} className={`relative rounded-[16px] p-4 flex flex-col gap-3 cursor-pointer group transition-all ${isSelected ? "bg-white border-2 border-[#b45309] shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.1)] hover:-translate-y-[2px]" : "bg-white border border-[#d6d3d1] shadow-sm hover:-translate-y-[2px]"}`}>
                  <div className={`absolute top-4 right-4 ${isSelected ? "text-[#b45309]" : "text-[#a8a29e]-variant group-hover:text-[#b45309]"}`}>
                    <span className="material-symbols-outlined text-[22px]" style={{ fontVariationSettings: `'FILL' ${isSelected ? 1 : 0}` }}>{isSelected ? "check_circle" : "radio_button_unchecked"}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-1 rounded text-[11px] font-semibold uppercase ${isSelected ? "bg-[#fef3c7] text-white-container" : "bg-[#fafaf9] text-[#57534e]"}`}>{addr.label}</span>
                    {addr.isDefault && <span className="text-[11px] font-semibold text-[#1c1917]">Default</span>}
                  </div>
                  <div className="flex flex-col gap-0.5 mt-1">
                    <p className="font-semibold text-[15px] text-[#1c1917]">{addr.label}</p>
                    <p className="text-[14px] text-[#57534e] leading-snug">{addr.street}</p>
                    <p className="text-[14px] text-[#57534e] leading-snug">{addr.city}, {addr.state} {addr.postal_code || addr.zip}</p>
                    <p className="text-[14px] text-[#57534e] leading-snug">{addr.country}</p>
                  </div>
                  <div className="flex gap-4 mt-auto pt-3 border-t border-surface-variant">
                    {isSelected ? (
                      <>
                        <Link onClick={(e)=>e.stopPropagation()} href="/addresses" className="text-[13px] font-semibold text-[#b45309] hover:underline">Edit</Link>
                        <Link onClick={(e)=>e.stopPropagation()} href="/addresses" className="text-[13px] font-semibold text-[#b91c1c] hover:underline">Delete</Link>
                      </>
                    ) : (
                      <>
                        <button onClick={(e) => { e.stopPropagation(); handleSelect(addr.id); }} className="text-[13px] font-semibold text-[#b45309]">Select</button>
                        <Link onClick={(e)=>e.stopPropagation()} href="/addresses" className="text-[13px] font-semibold text-[#b45309] hover:underline">Edit</Link>
                        <Link onClick={(e)=>e.stopPropagation()} href="/addresses" className="text-[13px] font-semibold text-[#b91c1c] hover:underline">Delete</Link>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {!showAdd ? (
            <button onClick={() => setShowAdd(true)} className="w-full flex items-center justify-center gap-2 py-4 border-2 border-dashed border-[#d6d3d1] rounded-[16px] text-[#57534e] hover:text-[#b45309] hover:border-[#b45309] hover:bg-[#fafaf9] transition-all font-semibold text-[14px]">
              <span className="material-symbols-outlined text-[20px]">add</span> Add New Address
            </button>
          ) : (
            <div className="bg-white border border-[#d6d3d1] rounded-[16px] p-6 shadow-sm">
              <h3 className="font-semibold text-[16px] text-[#1c1917] mb-4">Add New Address</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <input placeholder="Label (Home, Office) *" value={newAddr.label} onChange={(e) => setNewAddr({ ...newAddr, label: e.target.value })} className="rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white focus:ring-1 focus:ring-[#b45309] outline-none px-4 py-3 text-[14px]" />
                <input placeholder="Country *" value={newAddr.country} onChange={(e) => setNewAddr({ ...newAddr, country: e.target.value })} className="rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white focus:ring-1 focus:ring-[#b45309] outline-none px-4 py-3 text-[14px]" />
                <input placeholder="Street Address *" value={newAddr.street} onChange={(e) => setNewAddr({ ...newAddr, street: e.target.value })} className="col-span-2 rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white focus:ring-1 focus:ring-[#b45309] outline-none px-4 py-3 text-[14px]" />
                <input placeholder="City *" value={newAddr.city} onChange={(e) => setNewAddr({ ...newAddr, city: e.target.value })} className="rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white focus:ring-1 focus:ring-[#b45309] outline-none px-4 py-3 text-[14px]" />
                <input placeholder="State" value={newAddr.state} onChange={(e) => setNewAddr({ ...newAddr, state: e.target.value })} className="rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white outline-none px-4 py-3 text-[14px]" />
                <input placeholder="Postal Code *" value={newAddr.postalCode} onChange={(e) => setNewAddr({ ...newAddr, postalCode: e.target.value })} className="rounded-xl bg-[#f3f4f6] border border-transparent focus:border-[#b45309] focus:bg-white focus:ring-1 focus:ring-[#b45309] outline-none px-4 py-3 text-[14px]" />
              </div>
              {addrError && <p className="text-[12px] text-[#b91c1c] mt-2">{addrError}</p>}
              <div className="flex gap-3 mt-4">
                <button onClick={() => { setShowAdd(false); setAddrError(""); }} className="px-5 py-2.5 border border-[#d6d3d1] rounded-lg text-[13px] font-semibold hover:bg-[#fafaf9]">Cancel</button>
                <button onClick={handleAdd} className="px-5 py-2.5 bg-[#b45309] text-white rounded-lg text-[13px] font-semibold hover:bg-[#92400e]">Save Address</button>
              </div>
            </div>
          )}

          <div className="mt-2 flex flex-col items-end gap-2">
            {!loading && !canProceed && (
              <p
                role="alert"
                className={`text-[13px] flex items-center gap-1.5 px-3 py-2 rounded-lg border ${
                  triedContinue
                    ? "bg-[#fee2e2]/40 border-[#fecaca] text-[#b91c1c]"
                    : "bg-[#fafaf9] border-[#d6d3d1] text-[#57534e]"
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">{triedContinue ? "error" : "info"}</span>
                {addresses.length === 0
                  ? "No address yet. Please add and save an address to continue."
                  : "Please select a shipping address to continue."}
              </p>
            )}
            <button
              onClick={() => {
                if (!canProceed) {
                  setTriedContinue(true);
                  return;
                }
                router.push("/checkout/delivery");
              }}
              disabled={!canProceed || loading}
              aria-disabled={!canProceed || loading}
              title={!canProceed ? "Please select a shipping address to continue" : undefined}
              className={`font-semibold text-[14px] px-6 py-3 rounded-lg transition-colors shadow-sm flex items-center gap-1.5 ${
                canProceed && !loading
                  ? "bg-[#b45309] text-white hover:bg-[#92400e]"
                  : "bg-outline-variant text-[#57534e] cursor-not-allowed opacity-60"
              }`}
            >
              {loading ? "Loading..." : "Continue to Delivery"}
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
            {triedContinue && !canProceed && !loading && (
              <span className="text-[11px] text-[#b91c1c]">You must select or add an address before proceeding.</span>
            )}
          </div>
        </div>

        <div className="lg:col-span-4">
          <div className="bg-white border border-[#d6d3d1] rounded-[16px] p-6 sticky top-24 shadow-sm flex flex-col gap-4">
            <h2 className="font-semibold text-[18px] text-[#1c1917] border-b border-surface-variant pb-3">Order Summary</h2>
            <div className="flex flex-col gap-3">
              <div className="flex justify-between items-center"><span className="text-[14px] text-[#57534e]">Subtotal</span><span className="text-[14px] text-[#1c1917] font-semibold">{formatNPR(subtotal)}</span></div>
              <div className="flex justify-between items-center"><span className="text-[14px] text-[#57534e]">Shipping</span><span className="text-[13px] text-[#1c1917]">Calculated at next step</span></div>
              <div className="flex justify-between items-center"><span className="text-[14px] text-[#57534e]">Taxes</span><span className="text-[13px] text-[#1c1917]">Calculated at next step</span></div>
            </div>
            <div className="border-t border-surface-variant pt-4 flex justify-between items-end"><span className="font-semibold text-[16px] text-[#1c1917]">Total</span><span className="font-bold text-[20px] text-[#1c1917]">{formatNPR(subtotal)}</span></div>
            <div className="mt-1 flex items-start gap-2 bg-[#fafaf9] p-3 rounded-lg"><span className="material-symbols-outlined text-[#b45309] text-[20px] shrink-0">info</span><p className="text-[13px] leading-snug text-[#57534e]">Taxes and shipping are calculated based on your final shipping address.</p></div>
          </div>
        </div>
      </main>

      <footer className="bg-white border-t border-[#d6d3d1] w-full mt-auto">
        <div className="w-full px-margin-desktop py-4 max-w-[1440px] mx-auto flex flex-col md:flex-row justify-between items-center gap-3">
          <p className="text-[13px] text-[#57534e]">© 2024 ApexCommerce. All rights reserved.</p>
          <div className="flex gap-4"><Link href="/catalog" className="text-[13px] text-[#57534e] hover:text-[#b45309]">Privacy Policy</Link><Link href="/catalog" className="text-[13px] text-[#57534e] hover:text-[#b45309]">Terms of Service</Link></div>
        </div>
      </footer>
    </div>
  );
}
