"use client";

import { useEffect, ReactNode, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "@/context/AuthContext";


const ALWAYS_PUBLIC = ["/", "/catalog", "/products", "/product", "/cart"];
const GUEST_ONLY = ["/login", "/register"];
const ADMIN_PREFIXES = ["/admin"];

function isAlwaysPublic(p: string): boolean {
  if (ALWAYS_PUBLIC.includes(p)) return true;
  if (p.startsWith("/product/") || p.startsWith("/catalog")) return true;
  if (p.startsWith("/products")) return true;
  if (p === "/cart" || p.startsWith("/cart/")) return true;
  return false;
}

function isGuestOnly(p: string): boolean {
  return GUEST_ONLY.some((x) => p === x || p.startsWith(x + "/"));
}

function isPublicPath(p: string): boolean {
  return isAlwaysPublic(p) || isGuestOnly(p);
}

function isAdminPath(p: string): boolean {
  return ADMIN_PREFIXES.some((x) => p === x || p.startsWith(x + "/"));
}

function isProtectedPath(p: string): boolean {
  return !isPublicPath(p) && !isAdminPath(p);
}

export default function AuthGate({ children }: { children: ReactNode }) {
  const { loading, isAuthenticated, isAdmin } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  useEffect(() => {
    if (loading || !pathname) return;

    const nextParam = searchParams.get("next") || searchParams.get("redirect");

    if (isGuestOnly(pathname) && isAuthenticated) {
      if (nextParam && nextParam.startsWith("/") && !nextParam.startsWith("//")) {
        if (isAdminPath(nextParam) && !isAdmin) {
          router.replace("/");
          return;
        }
        if (!isAdminPath(nextParam) && isAdmin) {
          router.replace(nextParam);
          return;
        }
        router.replace(nextParam);
      } else {
        router.replace(isAdmin ? "/admin" : "/");
      }
      return;
    }

    if (isAdminPath(pathname)) {
      if (!isAuthenticated) {
        router.replace(`/login?next=${encodeURIComponent(pathname)}`);
        return;
      }
      if (!isAdmin) {
        router.replace("/?error=forbidden");
        return;
      }
      return;
    }

    if (isProtectedPath(pathname) && !isAuthenticated) {
      const target = pathname + (searchParams.toString() ? `?${searchParams.toString()}` : "");
      router.replace(`/login?next=${encodeURIComponent(target)}`);
    }

  }, [loading, isAuthenticated, isAdmin, pathname, router, searchParams]);

  if (!mounted) {
    if (pathname && isPublicPath(pathname) && !isAdminPath(pathname)) {
      return <>{children}</>;
    }
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#fafaf9] gap-4">
        <div className="w-9 h-9 rounded-full border-[2.5px] border-[#d6d3d1] border-t-[#b45309] animate-spin" />
        <p className="text-[11px] tracking-[0.14em] font-semibold text-[#a8a29e] uppercase">ApexCommerce</p>
      </div>
    );
  }

  if (loading) {
    const needsBlock = pathname ? isProtectedPath(pathname) || isAdminPath(pathname) : true;
    if (needsBlock) {
      return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-[#fafaf9] gap-4">
          <div className="w-9 h-9 rounded-full border-[2.5px] border-[#d6d3d1] border-t-[#b45309] animate-spin" />
          <p className="text-[11px] tracking-[0.14em] font-semibold text-[#a8a29e] uppercase">ApexCommerce</p>
        </div>
      );
    }
  }

  return <>{children}</>;
}

export { isAlwaysPublic, isGuestOnly, isAdminPath, isProtectedPath, isPublicPath };
