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
  return !!r && r.replace(/^ROLE_/, "").toUpperCase() === "ADMIN";
}

export default function RegisterPage() {
  return (
    <GuestOnlyRoute>
      <RegisterInner />
    </GuestOnlyRoute>
  );
}

function RegisterInner() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [focused, setFocused] = useState<string | null>(null);
  const [nextQ, setNextQ] = useState<string | null>(null);
  const { register } = useAuth();
  const router = useRouter();

  useEffect(() => {
    setNextQ(new URLSearchParams(window.location.search).get("next") || null);
  }, []);

  const strength = (() => {
    let s = 0;
    if (password.length >= 8) s++;
    if (password.length >= 12) s++;
    if (/[A-Z]/.test(password)) s++;
    if (/[0-9]/.test(password)) s++;
    if (/[^A-Za-z0-9]/.test(password)) s++;
    return s;
  })();
  const colors = ["bg-[#991b1b]", "bg-[#dc2626]", "bg-[#f59e0b]", "bg-[#0c0a09]", "bg-[#15803d]", "bg-[#15803d]"];
  const labels = ["Very weak", "Weak", "Fair", "Good", "Strong", "Very strong"];

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (password !== confirm) return setError("Passwords don’t match");
    if (password.length < 8) return setError("Password must be at least 8 characters");
    setLoading(true);
    try {
      const profile = await register(username.trim(), email.trim(), password);
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
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  const loginHref = nextQ ? `/login?next=${encodeURIComponent(nextQ)}` : "/login";

  return (
    <div className="min-h-screen flex bg-[#fafaf9]">
      <div className="hidden lg:flex lg:w-[50%] xl:w-[52%] relative overflow-hidden bg-[#0c0a09]">
        <img
          src="https://images.unsplash.com/photo-1498049794561-7780e7231661?q=80&w=1200&auto=format&fit=crop"
          alt=""
          className="absolute inset-0 w-full h-full object-cover opacity-[0.5]"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#0f0e0d] via-[#0f0e0d]/35 to-transparent" />
        <div className="absolute inset-0 opacity-[0.07] mix-blend-overlay" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.4'/%3E%3C/svg%3E")` }} />

        <div className="absolute top-8 left-8 flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-white text-[#0c0a09] flex items-center justify-center">
            <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>diamond</span>
          </div>
          <span className="text-white font-semibold tracking-tight text-[13px] uppercase">ApexCommerce</span>
        </div>

        <div className="relative z-10 flex flex-col justify-end p-10 xl:p-12 w-full">
          <div className="inline-flex items-center gap-2 self-start px-2.5 py-1 rounded-full bg-white/10 backdrop-blur border border-white/10 text-white/80 text-[11px] tracking-widest uppercase font-medium mb-4">
            <span className="w-1.5 h-1.5 rounded-full bg-[#8ef0a8]" /> Free shipping on first order
          </div>
          <h2 className={`${fraunces.className} text-white text-[38px] xl:text-[44px] leading-[0.95] tracking-[-0.03em] font-semibold`}>
            Create your <span className="italic font-light">own corner</span> of the store.
          </h2>
          <p className="text-white/70 text-[14px] leading-relaxed mt-3 max-w-[44ch]">
            Join thousands who check out in seconds, track orders effortlessly, and get picks tailored to what you actually like.
          </p>

          <div className="mt-6 grid grid-cols-3 gap-3 max-w-[420px]">
            {[
              { k: "4.8", label: "avg rating" },
              { k: "2-day", label: "avg delivery" },
              { k: "30-day", label: "free returns" },
            ].map((s) => (
              <div key={s.k} className="bg-white/10 backdrop-blur border border-white/15 rounded-2xl px-3 py-3">
                <div className="text-white font-semibold text-[16px] leading-none">{s.k}</div>
                <div className="text-white/60 text-[11px] tracking-wide uppercase mt-1">{s.label}</div>
              </div>
            ))}
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
          <span className="text-[11px] tracking-widest uppercase text-[#a8a29e] font-medium">Sign up</span>
        </div>

        <div className="flex-1 flex items-center justify-center px-5 sm:px-8 lg:px-10 xl:px-16 py-8">
          <div className="w-full max-w-[440px]">
            <div className="hidden lg:flex items-center gap-2 mb-7">
              <span className="w-8 h-8 rounded-lg bg-[#0c0a09] text-white flex items-center justify-center">
                <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>diamond</span>
              </span>
              <span className="font-semibold text-[#0c0a09] text-[14px] tracking-tight">ApexCommerce</span>
            </div>

            <div className="mb-6">
              <h1 className={`${fraunces.className} text-[28px] leading-none tracking-[-0.025em] text-[#0c0a09]`}>Create your account</h1>
              <p className="text-[13.5px] text-[#57534e] mt-2">Start shopping in under a minute. No spam, ever.</p>
            </div>

            {error && (
              <div className="mb-5 rounded-xl bg-[#fff1f0] border border-[#ffc9c5] px-3.5 py-2.5 flex gap-2.5 items-start">
                <span className="material-symbols-outlined text-[#b42318] text-[18px] mt-0.5">error</span>
                <p className="text-[12.5px] leading-snug text-[#7a1e16] font-medium">{error}</p>
              </div>
            )}

            <form onSubmit={onSubmit} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Username</label>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "username" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">person</span>
                  <input value={username} onChange={(e) => setUsername(e.target.value)} onFocus={() => setFocused("username")} onBlur={() => setFocused(null)} required minLength={3} maxLength={50} placeholder="johndoe123" className="w-full bg-transparent pl-10 pr-4 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none" />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Email</label>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "email" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">mail</span>
                  <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} onFocus={() => setFocused("email")} onBlur={() => setFocused(null)} required placeholder="you@example.com" className="w-full bg-transparent pl-10 pr-4 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none" />
                </div>
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Password</label>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "password" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">lock</span>
                  <input type={show ? "text" : "password"} value={password} onChange={(e) => setPassword(e.target.value)} onFocus={() => setFocused("password")} onBlur={() => setFocused(null)} required placeholder="Min. 8 characters" className="w-full bg-transparent pl-10 pr-11 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none" />
                  <button type="button" onClick={() => setShow(!show)} className="absolute right-2.5 top-1/2 -translate-y-1/2 w-8 h-8 rounded-lg hover:bg-[#fafaf9] flex items-center justify-center text-[#a8a29e]">
                    <span className="material-symbols-outlined text-[18px]">{show ? "visibility_off" : "visibility"}</span>
                  </button>
                </div>
                {password.length > 0 && (
                  <div className="flex items-center gap-2">
                    <div className="flex gap-1 flex-1">
                      {[0, 1, 2, 3, 4].map((i) => (
                        <div key={i} className={`h-1 flex-1 rounded-full transition-all ${i < strength ? colors[strength] : "bg-[#d6d3d1]"}`} />
                      ))}
                    </div>
                    <span className={`text-[11px] font-medium ${strength <= 1 ? "text-[#991b1b]" : strength <= 2 ? "text-[#b45309]" : "text-[#15803d]"}`}>{labels[strength]}</span>
                  </div>
                )}
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-[12px] font-semibold tracking-wide text-[#2b2b2b]">Confirm password</label>
                <div className={`relative rounded-xl bg-white border transition-all ${focused === "confirm" ? "border-[#0c0a09] ring-4 ring-[#0c0a09]/10" : confirm && password === confirm ? "border-[#15803d]/30 ring-4 ring-[#15803d]/10" : "border-[#d6d3d1] hover:border-[#d9cbc1]"}`}>
                  <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a8a29e]">lock_reset</span>
                  <input type={show ? "text" : "password"} value={confirm} onChange={(e) => setConfirm(e.target.value)} onFocus={() => setFocused("confirm")} onBlur={() => setFocused(null)} required placeholder="Repeat password" className="w-full bg-transparent pl-10 pr-10 py-3.5 text-[14px] text-[#0c0a09] placeholder:text-[#a99f9b] outline-none" />
                  {confirm && <span className="absolute right-3 top-1/2 -translate-y-1/2">{password === confirm ? <span className="material-symbols-outlined text-[18px] text-[#15803d]" style={{ fontVariationSettings: "'FILL' 1" }}>check_circle</span> : <span className="material-symbols-outlined text-[18px] text-[#dc2626]">cancel</span>}</span>}
                </div>
              </div>

              <p className="text-[11.5px] leading-relaxed text-[#a8a29e]">
                By creating an account you agree to our <span className="font-semibold text-[#0c0a09] underline decoration-[#d6d3d1]">Terms</span> and <span className="font-semibold text-[#0c0a09] underline decoration-[#d6d3d1]">Privacy</span>.
              </p>

              <button type="submit" disabled={loading} className="mt-1 w-full bg-[#0c0a09] text-white font-medium text-[14px] py-3.5 rounded-full hover:bg-black transition-all disabled:opacity-50 flex items-center justify-center gap-2 shadow-[0_8px_20px_rgba(18,17,16,0.18)] hover:shadow-[0_10px_24px_rgba(18,17,16,0.22)] hover:-translate-y-[1px] active:translate-y-0">
                {loading ? <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" /> : <>Create account <span className="material-symbols-outlined text-[16px]">arrow_forward</span></>}
              </button>
            </form>

            <p className="text-[13px] text-[#57534e] text-center mt-6">
              Already have an account? <Link href={loginHref} className="font-semibold text-[#0c0a09] underline underline-offset-4 decoration-[#d6d3d1] hover:decoration-[#0c0a09]">Sign in</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
