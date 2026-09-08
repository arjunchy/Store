"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Fraunces } from "next/font/google";
import { useAuth } from "@/context/AuthContext";
import { GuestOnlyRoute } from "@/components/AuthGuard";

const fraunces = Fraunces({ subsets: ["latin"], weight: ["600", "700"], display: "swap" });

function getNextParam(): string {
  if (typeof window === "undefined") return "/";
  const p = new URLSearchParams(window.location.search);
  const n = p.get("next") || p.get("redirect");
  if (n && n.startsWith("/") && !n.startsWith("//")) return n;
  return "/";
}

function isAdminRole(r?: string | null) {
  if (!r) return false;
  return r.replace(/^ROLE_/, "").toUpperCase() === "ADMIN";
}

export default function LoginPage() {
  return (
    <GuestOnlyRoute>
      <LoginInner />
    </GuestOnlyRoute>
  );
}

function LoginInner() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);
  const [focused, setFocused] = useState<string | null>(null);
  const [nextQ, setNextQ] = useState<string | null>(null);
  const { login } = useAuth();
  const router = useRouter();

  useEffect(() => {
    const q = new URLSearchParams(window.location.search);
    if (q.get("error") === "forbidden") setError("That area is admin-only. Please sign in with an admin account.");
    else if (q.get("next")) setInfo(`Sign in to continue`);
    setNextQ(q.get("next") || q.get("redirect"));
  }, []);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const profile = await login(email.trim(), password);
      const admin = isAdminRole(profile.userRole);
      const next = getNextParam();
      if (admin) {
        if (next && next !== "/" && (next === "/admin" || next.startsWith("/admin/"))) router.replace(next);
        else router.replace("/admin");
      } else {
        if (next && (next === "/admin" || next.startsWith("/admin/"))) router.replace("/");
        else router.replace(next);
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  const registerHref = nextQ ? `/register?next=${encodeURIComponent(nextQ)}` : "/register";

  return (
    <div className="min-h-screen flex bg-[#fafaf9]">
      <div className="hidden lg:flex lg:w-[50%] xl:w-[52%] relative overflow-hidden bg-[#0c0a09]">
        <img
          src="https://images.unsplash.com/photo-1441986300917-64674bd600d8?q=80&w=1200&auto=format&fit=crop"
          alt=""
          className="absolute inset-0 w-full h-full object-cover opacity-[0.58]"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0f0e0d] via-[#0f0e0d]/40 to-transparent" />
        <div className="absolute inset-0 opacity-[0.07] mix-blend-overlay" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.4'/%3E%3C/svg%3E")` }} />
        <div className="absolute top-8 left-8 flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-white text-[#0c0a09] flex items-center justify-center">
            <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>diamond</span>
          </div>
          <span className="text-white font-semibold tracking-tight text-[13px] uppercase">ApexCommerce</span>
          <span className="text-white/40 text-[11px] tracking-widest uppercase ml-1">est. 2024</span>
        </div>

        <div className="relative z-10 flex flex-col justify-end p-10 xl:p-12 w-full">
          <div className="inline-flex items-center gap-2 self-start px-2.5 py-1 rounded-full bg-white/10 backdrop-blur border border-white/10 text-white/80 text-[11px] tracking-widest uppercase font-medium mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-[#ff6b42] animate-pulse" /> Trusted by 40k+ shoppers
          </div>
          <h2 className={`${fraunces.className} text-white text-[38px] xl:text-[44px] leading-[0.95] tracking-[-0.03em] font-semibold`}>
            The store <span className="font-light italic tracking-tight">that knows</span> what you love.
          </h2>
          <p className="text-white/70 text-[14px] leading-relaxed mt-3 max-w-[42ch]">
            Curated essentials, fair prices, and a shopping experience that feels like your favourite neighbourhood boutique — now online.
          </p>

          <div className="mt-7 bg-white rounded-2xl p-4 flex gap-3.5 items-start shadow-[0_12px_40px_rgba(0,0,0,0.25)] max-w-[420px] rotate-[-0.4deg]">
            <img src="https://i.pravatar.cc/100?img=32" alt="" className="w-10 h-10 rounded-full object-cover shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-1 text-[#ff6b42]">
                {[...Array(5)].map((_, i) => (
                  <span key={i} className="material-symbols-outlined text-[13px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                ))}
                <span className="text-[#0c0a09] font-semibold text-[12px] ml-1">4.9</span>
              </div>
              <p className="text-[13px] leading-snug text-[#2b2b2b] mt-1">“Feels like it was made for me. Delivery was next-day and the quality is just *chef’s kiss*.”</p>
              <p className="text-[11px] text-[#a8a29e] mt-1.5 font-medium">Maya R. — verified buyer · 2 days ago</p>
            </div>
          </div>

          <div className="flex items-center gap-6 mt-6 text-white/55 text-[11px] tracking-wide">
            <span className="flex items-center gap-1.5"><span className="material-symbols-outlined text-[14px]">verified</span> Secure checkout</span>
            <span className="flex items-center gap-1.5"><span className="material-symbols-outlined text-[14px]">local_shipping</span> Free returns 30 days</span>
          </div>
        </div>
      </div>

      <div className="flex-1 flex flex-col min-h-screen">
        <div className="lg:hidden flex items-center justify-between px-5 py-4 border-b border-[#d6d3d1] bg-white/70 backdrop-blur sticky top-0 z-10">
          <Link href="/login" className="flex items-center gap-2">
            <span className="w-7 h-7 rounded-lg bg-[#0c0a09] text-white flex items-center justify-center">
              <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>diamond</span>
            </span>
            <span className="font-semibold tracking-tight text-[15px] text-[#0c0a09]">ApexCommerce</span>
          </Link>
          <span className="text-[11px] tracking-widest uppercase text-[#a8a29e] font-medium">Sign in</span>
        </div>

        <div className="flex-1 flex items-center justify-center px-5 sm:px-8 lg:px-10 xl:px-16 py-8 lg:py-10">
          <div className="w-full max-w-[420px]">
            <div className="hidden lg:flex items-center gap-2 mb-8">
              <span className="w-8 h-8 rounded-lg bg-[#0c0a09] text-white flex items-center justify-center">
                <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>diamond</span>
              </span>
              <span className="font-semibold text-[#0c0a09] text-[14px] tracking-tight">ApexCommerce</span>
            </div>

            <div className="mb-7">
              <h1 className={`${fraunces.className} text-[30px] leading-none tracking-[-0.025em] text-[#0c0a09]`}>Welcome back</h1>
              <p className="text-[13.5px] text-[#57534e] mt-2 leading-relaxed">Sign in to your account. Admins will be taken to the dashboard.</p>
            </div>

            {info && !error && (
              <div className="mb-5 rounded-xl bg-[#fff4e6] border border-[#ffedd5] px-3.5 py-2.5 flex gap-2.5 items-start">
                <span className="material-symbols-outlined text-[#b45309] text-[18px] mt-0.5">info</span>
                <p className="text-[12.5px] leading-snug text-[#7c4d12] font-medium">{info}</p>
              </div>
            )}
            {error && (
              <div className="mb-5 rounded-xl bg-[#fff1f0] border border-[#ffc9c5] px-3.5 py-2.5 flex gap-2.5 items-start">
                <span className="material-symbols-outlined text-[#b42318] text-[18px] mt-0.5">error</span>
                <p className="text-[12.5px] leading-snug text-[#7a1e16] font-medium">{error}</p>
              </div>
            )}

            <form onSubmit={onSubmit} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Email</label>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "email" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">mail</span>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onFocus={() => setFocused("email")}
                    onBlur={() => setFocused(null)}
                    required
                    autoComplete="email"
                    placeholder="you@example.com"
                    className="w-full bg-transparent pl-10 pr-4 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none"
                  />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Password</label>
                  <button type="button" className="text-[12px] font-medium text-[#57534e] hover:text-[#0c0a09] underline underline-offset-4 decoration-[#d6d3d1]">Forgot?</button>
                </div>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "password" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">lock</span>
                  <input
                    type={show ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setFocused("password")}
                    onBlur={() => setFocused(null)}
                    required
                    placeholder="••••••••"
                    className="w-full bg-transparent pl-10 pr-11 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none"
                  />
                  <button type="button" onClick={() => setShow(!show)} className="absolute right-2.5 top-1/2 -translate-y-1/2 w-8 h-8 rounded-lg hover:bg-[#fafaf9] flex items-center justify-center text-[#a8a29e]">
                    <span className="material-symbols-outlined text-[18px]">{show ? "visibility_off" : "visibility"}</span>
                  </button>
                </div>
                <p className="text-[11px] text-[#a8a29e]">Use your account password. Admin demo below.</p>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="mt-1 w-full bg-[#0c0a09] text-white font-medium text-[14px] py-3.5 rounded-full hover:bg-black transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-[0_8px_20px_rgba(18,17,16,0.18)] hover:shadow-[0_10px_24px_rgba(18,17,16,0.22)] hover:-translate-y-[1px] active:translate-y-0"
              >
                {loading ? <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" /> : <>Sign in <span className="material-symbols-outlined text-[16px]">arrow_forward</span></>}
              </button>
            </form>

            <div className="flex items-center gap-3 my-6">
              <div className="flex-1 h-px bg-[#d6d3d1]" />
              <span className="text-[11px] tracking-widest uppercase font-semibold text-[#a99f9b]">or</span>
              <div className="flex-1 h-px bg-[#d6d3d1]" />
            </div>

            <div className="grid grid-cols-2 gap-2.5">
              <button type="button" className="flex items-center justify-center gap-2 py-2.5 rounded-full border border-[#d6d3d1] bg-white hover:bg-[#ffffff] text-[13px] font-medium text-[#2b2b2b] transition-colors">
                <svg width="16" height="16" viewBox="0 0 24 24"><path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 01-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/><path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/><path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/><path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/></svg>
                Google
              </button>
              <button type="button" className="flex items-center justify-center gap-2 py-2.5 rounded-full border border-[#d6d3d1] bg-white hover:bg-[#ffffff] text-[13px] font-medium text-[#2b2b2b] transition-colors">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.702"/></svg>
                Apple
              </button>
            </div>

            <p className="text-[13px] text-[#57534e] text-center mt-6">
              New here? <Link href={registerHref} className="font-semibold text-[#0c0a09] underline underline-offset-4 decoration-[#d6d3d1] hover:decoration-[#0c0a09]">Create an account</Link>
            </p>

            <div className="mt-6 rounded-2xl border border-dashed border-[#d6d3d1] bg-white p-4 rotate-[0.15deg]">
              <div className="flex items-center gap-2 mb-1.5">
                <span className="w-6 h-6 rounded-full bg-[#fff4e6] border border-[#ffedd5] flex items-center justify-center">
                  <span className="material-symbols-outlined text-[14px] text-[#b45309]">key</span>
                </span>
                <p className="text-[11px] font-bold tracking-widest uppercase text-[#a8a29e]">Demo access</p>
                <span className="ml-auto text-[10px] px-1.5 py-0.5 rounded-full bg-[#0c0a09] text-white font-semibold">ADMIN</span>
              </div>
              <div className="grid grid-cols-1 gap-1.5 font-mono text-[12px]">
                <div className="flex justify-between bg-[#fafaf9] rounded-lg px-2.5 py-2 border border-[#e7e5e4]">
                  <span className="text-[#a8a29e]">email</span><span className="text-[#0c0a09] font-medium select-all">ecommerce@gmail.com</span>
                </div>
                <div className="flex justify-between bg-[#fafaf9] rounded-lg px-2.5 py-2 border border-[#e7e5e4]">
                  <span className="text-[#a8a29e]">password</span><span className="text-[#0c0a09] font-medium select-all">ecommerce@gmail</span>
                </div>
              </div>
              <p className="text-[11px] text-[#a8a29e] mt-2 leading-snug">Admins land on <span className="font-semibold text-[#0c0a09]">/admin</span> — users go to the storefront.</p>
            </div>

            <p className="text-[11px] text-[#a99f9b] text-center mt-5 leading-relaxed">By continuing you agree to our Terms & Privacy. We never share your email.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
