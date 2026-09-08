"use client";

import { ReactNode, useEffect, useRef } from "react";
import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";

function LoadingScreen({ message = "Checking authentication..." }: { message?: string }) {
  return (
    <div className="min-h-[50vh] flex flex-col items-center justify-center gap-3">
      <div className="w-8 h-8 rounded-full border-[2.5px] border-[#d6d3d1] border-t-[#b45309] animate-spin" />
      <p className="text-[12px] tracking-wide text-[#a8a29e] font-medium">{message}</p>
    </div>
  );
}

export function ProtectedRoute({ children, adminOnly = false }: { children: ReactNode; adminOnly?: boolean }) {
  const { user, loading, isAuthenticated, isAdmin } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const ref = useRef(false);

  useEffect(() => {
    if (loading || ref.current) return;
    if (!isAuthenticated || !user) {
      ref.current = true;
      const cur = pathname || "/";
      router.replace(cur === "/" ? "/login" : `/login?next=${encodeURIComponent(cur)}`);
      return;
    }
    if (adminOnly && !isAdmin) {
      ref.current = true;
      router.replace("/?error=forbidden");
    }
  }, [loading, isAuthenticated, user, isAdmin, adminOnly, pathname, router]);

  if (loading) return <LoadingScreen />;
  if (!isAuthenticated || !user) return <LoadingScreen message="Redirecting to login..." />;
  if (adminOnly && !isAdmin) return <LoadingScreen message="Checking permissions..." />;
  return <>{children}</>;
}

export function AdminRoute({ children }: { children: ReactNode }) {
  return <ProtectedRoute adminOnly>{children}</ProtectedRoute>;
}

export function GuestOnlyRoute({ children }: { children: ReactNode }) {
  const { loading, isAuthenticated, isAdmin } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (loading || !isAuthenticated) return;
    const params = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : null;
    const next = params?.get("next") || params?.get("redirect");
    if (next && next.startsWith("/") && !next.startsWith("//")) {
      const isAdminNext = next === "/admin" || next.startsWith("/admin/");
      if (isAdminNext && !isAdmin) return;
      if (!isAdminNext && isAdmin) {
        router.replace("/admin");
        return;
      }
      router.replace(next);
    } else {
      router.replace(isAdmin ? "/admin" : "/");
    }
  }, [loading, isAuthenticated, isAdmin, router]);

  if (loading) return <LoadingScreen message="Loading..." />;
  if (isAuthenticated) return null;
  return <>{children}</>;
}


