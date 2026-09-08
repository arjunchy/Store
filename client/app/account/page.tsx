"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { useAuth } from "@/context/AuthContext";
import { ProtectedRoute } from "@/components/AuthGuard";
import { getCurrentUser, updateProfile, deleteMyAccount } from "@/lib/user";
import type { User } from "@/lib/types";

export default function AccountPage() {
  return (
    <ProtectedRoute>
      <AccountInner />
    </ProtectedRoute>
  );
}

function AccountInner() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [profile, setProfile] = useState<User | null>(null);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    getCurrentUser().then((u) => {
      if (u) {
        setProfile(u);
        setUsername(u.username);
        setEmail(u.email);
      }
    });
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage("");
    setErrorMsg("");
    try {
      const payload: { username: string; email: string; password?: string } = { username, email };
      if (password) {
        if (password.length < 8) {
          setErrorMsg("Password must be at least 8 characters");
          setSaving(false);
          return;
        }
        payload.password = password;
      }
      const updated = await updateProfile(payload);
      if (updated) {
        setProfile(updated);
        setMessage("Profile updated successfully." + (password ? " Password changed." : ""));
        setPassword("");
      } else {
        setErrorMsg("Could not update profile. Please try again.");
      }
    } catch (e: unknown) {
      setErrorMsg(e instanceof Error ? e.message : "Could not update profile. Please try again.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("Delete your account? This cannot be undone. All your data will be removed.")) return;
    if (!confirm("Confirm again: permanently delete account?")) return;
    try {
      await deleteMyAccount();
      await logout();
      router.push("/");
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1100px] mx-auto py-xl md:py-xxl">
        <div className="mb-lg">
          <h1 className="font-headline-lg text-headline-lg text-[#1c1917] font-semibold tracking-tight">My Account</h1>
          <p className="font-body-md text-body-md text-[#57534e]">Manage your profile and account settings.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-gutter">
          <div className="lg:col-span-2 bg-white rounded-xl p-xl border border-[#d6d3d1] shadow-sm">
            <h2 className="font-headline-md text-headline-md text-[#1c1917] font-semibold mb-md">Profile</h2>
            {message && (
              <div className="mb-md bg-[#fef3c7]/30 border border-[#b45309]/20 rounded-xl px-4 py-3 text-[13px] text-[#b45309] font-medium flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">check_circle</span> {message}
              </div>
            )}
            {errorMsg && (
              <div className="mb-md bg-[#fef2f2] border border-[#fecaca] rounded-xl px-4 py-3 text-[13px] text-[#b91c1c] font-medium flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">error</span> {errorMsg}
              </div>
            )}
            <form onSubmit={handleSave} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-[#1c1917]">Username</label>
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  minLength={3}
                  maxLength={50}
                  placeholder="username"
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/80 rounded-xl px-4 py-3 font-body-md text-body-md text-[#1c1917] focus:border-[#b45309] outline-none"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-[#1c1917]">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/80 rounded-xl px-4 py-3 font-body-md text-body-md text-[#1c1917] focus:border-[#b45309] outline-none"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-label-md text-label-md text-[#1c1917]">New password <span className="text-[#57534e] font-normal text-[12px]">(leave blank to keep)</span></label>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="At least 8 characters"
                  minLength={8}
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/80 rounded-xl px-4 py-3 font-body-md text-body-md text-[#1c1917] focus:border-[#b45309] outline-none placeholder:text-[#57534e]/50"
                />
              </div>
              <div>
                <button
                  type="submit"
                  disabled={saving}
                  className="bg-[#b45309] text-white font-label-md text-label-md py-3 px-6 rounded-xl hover:bg-[#92400e] transition-colors disabled:opacity-50 flex items-center gap-2"
                >
                  {saving && <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>}
                  {saving ? "Saving..." : "Save changes"}
                </button>
              </div>
            </form>

            <div className="mt-8 pt-6 border-t border-[#d6d3d1]">
              <h3 className="font-semibold text-[14px] text-[#b91c1c] mb-2 flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">warning</span> Danger Zone
              </h3>
              <p className="text-[12px] text-[#57534e] mb-3 leading-relaxed">Permanently delete your account and all associated data. This action cannot be undone.</p>
              <button onClick={handleDelete} className="px-4 py-2 rounded-xl border border-error/30 text-[#b91c1c] hover:bg-[#fee2e2]/40 text-[13px] font-semibold flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px]">delete_forever</span> Delete Account
              </button>
            </div>
          </div>

          <div className="flex flex-col gap-4">
            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
              <Link href="/orders" className="flex items-center gap-3 px-4 py-4 hover:bg-[#fafaf9] transition-colors border-b border-[#d6d3d1]/40">
                <span className="material-symbols-outlined text-[#b45309]">receipt_long</span>
                <span className="font-medium text-[14px] text-[#1c1917]">My Orders</span>
                <span className="material-symbols-outlined text-[#a8a29e] ml-auto text-[18px]">chevron_right</span>
              </Link>
              <Link href="/wishlist" className="flex items-center gap-3 px-4 py-4 hover:bg-[#fafaf9] transition-colors border-b border-[#d6d3d1]/40">
                <span className="material-symbols-outlined text-[#b45309]">favorite</span>
                <span className="font-medium text-[14px] text-[#1c1917]">Wishlist</span>
                <span className="material-symbols-outlined text-[#a8a29e] ml-auto text-[18px]">chevron_right</span>
              </Link>
              <Link href="/addresses" className="flex items-center gap-3 px-4 py-4 hover:bg-[#fafaf9] transition-colors">
                <span className="material-symbols-outlined text-[#b45309]">location_on</span>
                <span className="font-medium text-[14px] text-[#1c1917]">Addresses</span>
                <span className="material-symbols-outlined text-[#a8a29e] ml-auto text-[18px]">chevron_right</span>
              </Link>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center justify-center gap-2 bg-[#fee2e2]/20 text-[#b91c1c] font-label-md text-label-md py-3 px-6 rounded-xl hover:bg-[#fee2e2]/40 transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span> Sign Out
            </button>
            {profile && (
              <p className="text-[12px] text-[#57534e] text-center">Signed in as {profile.email}</p>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
