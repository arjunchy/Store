"use client";

import { useState, useEffect } from "react";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { ProtectedRoute } from "@/components/AuthGuard";
import { getShipping, addAddress, updateAddress, deleteAddress, setDefaultAddress } from "@/lib/checkout";
import type { ShippingAddress } from "@/lib/types";

export default function AddressesPage() {
  return (
    <ProtectedRoute>
      <AddressesInner />
    </ProtectedRoute>
  );
}

function AddressesInner() {
  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [editing, setEditing] = useState<ShippingAddress | null>(null);
  const [form, setForm] = useState({ label: "", street: "", city: "", state: "", postalCode: "", country: "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const load = () => {
    setLoading(true);
    getShipping()
      .then((s) => setAddresses(s.addresses))
      .catch(() => setAddresses([]))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const resetForm = () => {
    setForm({ label: "", street: "", city: "", state: "", postalCode: "", country: "" });
    setEditing(null);
    setError("");
    setShowAdd(false);
  };

  const openAdd = () => {
    setEditing(null);
    setForm({ label: "", street: "", city: "", state: "", postalCode: "", country: "" });
    setError("");
    setShowAdd(true);
  };

  const openEdit = (addr: ShippingAddress) => {
    setEditing(addr);
    setForm({
      label: addr.label || "",
      street: addr.street,
      city: addr.city,
      state: addr.state || "",
      postalCode: addr.postal_code || addr.zip || "",
      country: addr.country,
    });
    setError("");
    setShowAdd(true);
  };

  const validate = () => {
    if (!form.label.trim()) return "Label is required (Home, Office)";
    if (!form.street.trim()) return "Street is required";
    if (!form.city.trim()) return "City is required";
    if (!form.postalCode.trim()) return "Postal code is required";
    if (!form.country.trim()) return "Country is required";
    return "";
  };

  const handleSave = async () => {
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    setSaving(true);
    setError("");
    try {
      const payload = {
        label: form.label.trim(),
        street: form.street.trim(),
        city: form.city.trim(),
        state: form.state.trim(),
        postal_code: form.postalCode.trim(),
        zip: form.postalCode.trim(),
        country: form.country.trim(),
        is_default: false,
        isDefault: false,
      } as unknown as Omit<ShippingAddress, "id">;
      if (editing) {
        await updateAddress(editing.id, payload as any);
        setSuccess("Address updated");
      } else {
        await addAddress(payload);
        setSuccess("Address added");
      }
      resetForm();
      load();
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save address");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this address?")) return;
    try {
      await deleteAddress(id);
      setSuccess("Address deleted");
      load();
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  const handleSetDefault = async (id: string) => {
    try {
      await setDefaultAddress(id);
      setSuccess("Default address updated");
      load();
      setTimeout(() => setSuccess(""), 3000);
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Failed to set default");
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1100px] mx-auto py-xl md:py-xxl">
        <div className="flex items-center justify-between mb-lg">
          <div>
            <h1 className="font-headline-lg text-headline-lg text-[#1c1917] font-semibold tracking-tight">Addresses</h1>
            <p className="font-body-md text-body-md text-[#57534e]">Manage your shipping addresses. Set a default for faster checkout.</p>
          </div>
          {!showAdd && (
            <button
              onClick={openAdd}
              className="bg-[#b45309] text-white font-label-md text-label-md px-lg py-3 rounded-xl hover:bg-[#92400e] transition-colors flex items-center gap-2 shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px]">add</span> Add Address
            </button>
          )}
        </div>

        {success && (
          <div className="mb-4 bg-[#15803d]/15 border border-[#15803d]/20 rounded-xl px-4 py-3 text-[13px] text-[#15803d] flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">check_circle</span> {success}
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-xl">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
          </div>
        ) : showAdd ? (
          <div className="bg-white border border-[#d6d3d1] rounded-xl p-6 max-w-[640px] shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h3 className="font-semibold text-[16px] text-[#1c1917]">{editing ? "Edit Address" : "Add New Address"}</h3>
              <button onClick={resetForm} className="p-1.5 rounded-full hover:bg-[#fafaf9]">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            {error && (
              <div className="mb-4 bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 text-[#b91c1c] text-[13px] flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">error</span> {error}
              </div>
            )}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <input placeholder="Label (Home, Office) *" value={form.label} onChange={(e) => setForm({ ...form, label: e.target.value })} className="rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
              <input placeholder="Country *" value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} className="rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
              <input placeholder="Street Address *" value={form.street} onChange={(e) => setForm({ ...form, street: e.target.value })} className="col-span-2 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
              <input placeholder="City *" value={form.city} onChange={(e) => setForm({ ...form, city: e.target.value })} className="rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
              <input placeholder="State" value={form.state} onChange={(e) => setForm({ ...form, state: e.target.value })} className="rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
              <input placeholder="Postal Code *" value={form.postalCode} onChange={(e) => setForm({ ...form, postalCode: e.target.value })} className="col-span-2 md:col-span-1 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]/80 px-4 py-3 text-[14px] text-[#1c1917] focus:border-[#b45309] outline-none" />
            </div>
            <p className="text-[11px] text-[#57534e] mt-2">* Required fields.</p>
            <div className="flex gap-3 mt-4">
              <button onClick={resetForm} className="px-5 py-2.5 border border-[#d6d3d1] rounded-lg text-[13px] font-semibold hover:bg-[#fafaf9]">
                Cancel
              </button>
              <button onClick={handleSave} disabled={saving} className="px-5 py-2.5 bg-[#b45309] text-white rounded-lg text-[13px] font-semibold hover:bg-[#92400e] disabled:opacity-50 flex items-center gap-1.5">
                {saving && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
                {editing ? "Update Address" : "Save Address"}
              </button>
            </div>
          </div>
        ) : addresses.length === 0 ? (
          <div className="bg-white rounded-xl border border-[#d6d3d1] p-xl text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px] mb-3">location_on</span>
            <h3 className="font-headline-md text-headline-md text-[#1c1917] font-semibold mb-1">No addresses saved</h3>
            <p className="text-[#57534e] text-[14px]">Add an address to speed up checkout.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {addresses.map((addr) => (
              <div key={addr.id} className="relative rounded-[16px] p-4 flex flex-col gap-3 bg-white border border-[#d6d3d1] shadow-sm hover:shadow-md transition-shadow">
                <div className="absolute top-4 right-4 flex items-center gap-1.5">
                  {addr.is_default || addr.isDefault ? (
                    <span className="px-2 py-1 rounded-full text-[11px] font-semibold bg-[#fef3c7] text-white-container border border-[#b45309]/20">Default</span>
                  ) : (
                    <button onClick={() => handleSetDefault(addr.id)} className="text-[11px] font-semibold text-[#b45309] hover:underline px-2 py-1 rounded-full border border-[#d6d3d1] hover:bg-[#fafaf9]">
                      Set default
                    </button>
                  )}
                </div>
                <div className="flex items-center gap-2">
                  <span className="px-2 py-1 rounded text-[11px] font-semibold uppercase bg-[#fafaf9] text-[#57534e]">{addr.label}</span>
                </div>
                <div className="flex flex-col gap-0.5 mt-1 min-h-[80px]">
                  <p className="font-semibold text-[15px] text-[#1c1917]">{addr.label}</p>
                  <p className="text-[14px] text-[#57534e] leading-snug">{addr.street}</p>
                  <p className="text-[14px] text-[#57534e] leading-snug">
                    {addr.city}, {addr.state} {addr.postal_code || addr.zip}
                  </p>
                  <p className="text-[14px] text-[#57534e] leading-snug">{addr.country}</p>
                </div>
                <div className="flex gap-2 pt-3 border-t border-[#e7e5e4] mt-auto">
                  <button onClick={() => openEdit(addr)} className="flex-1 py-2 rounded-xl border border-[#d6d3d1] hover:bg-[#fafaf9] text-[12px] font-semibold text-[#1c1917] flex items-center justify-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">edit</span> Edit
                  </button>
                  <button onClick={() => handleDelete(addr.id)} className="flex-1 py-2 rounded-xl border border-[#fecaca] text-[#b91c1c] hover:bg-[#fef2f2] text-[12px] font-semibold flex items-center justify-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">delete</span> Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
