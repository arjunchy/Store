"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { AdminRoute } from "@/components/AuthGuard";
import AdminSidebar from "@/components/admin/AdminSidebar";
import { useAuth } from "@/context/AuthContext";
import { AdminAlertsProvider, useAdminAlerts } from "@/context/AdminAlertsContext";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AdminRoute>
      <AdminAlertsProvider>
        <AdminShell>{children}</AdminShell>
      </AdminAlertsProvider>
    </AdminRoute>
  );
}

function AdminShell({ children }: { children: React.ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { logout } = useAuth();
  const router = useRouter();
  const { total } = useAdminAlerts();
  const alertCount = total;

  const handleLogout = async () => {
    await logout();
    router.push("/login");
  };

  return (
    <div className="min-h-screen flex bg-[#fafaf9]">
      <AdminSidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />

      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-[#0c0a09] border-b border-[#292524] sticky top-0 z-30 flex items-center gap-3 px-4 lg:px-6 shrink-0">
          <button
            onClick={() => setMobileOpen(true)}
            className="lg:hidden p-2 rounded-xl hover:bg-[#1c1917] text-[#fafaf9]"
            aria-label="Open menu"
          >
            <span className="material-symbols-outlined">menu</span>
          </button>

          <div className="hidden sm:flex items-center gap-2 text-[13px] text-[#a8a29e]">
            <span className="material-symbols-outlined text-[18px] text-[#b45309]">shield</span>
            <span className="text-[#fafaf9]">Admin Dashboard</span>
            <span className="w-1 h-1 rounded-full bg-[#44403c]" />
            <span className="text-[#b45309] font-medium">Secure Area</span>
          </div>

          <div className="flex-1" />

          <div className="flex items-center gap-2">
            <Link href="/admin" className="hidden sm:flex items-center gap-2 px-3 py-2 rounded-xl bg-[#1c1917] hover:bg-[#292524] text-[#fafaf9] transition-colors text-[13px] font-medium border border-[#292524]">
              <span className="material-symbols-outlined text-[18px] text-[#b45309]">notifications</span>
              <span className="hidden lg:inline">Alerts</span>
              {alertCount > 0 ? (
                <span className="bg-[#b91c1c] text-white text-[11px] px-1.5 py-0.5 rounded-full font-semibold">{alertCount}</span>
              ) : (
                <span className="w-2 h-2 rounded-full bg-[#15803d]" title="No alerts" />
              )}
            </Link>

            <Link
              href="/"
              className="hidden md:flex items-center gap-1.5 px-3 py-2 rounded-xl border border-[#b45309]/30 hover:bg-[#b45309] text-[13px] font-medium text-[#fafaf9] hover:text-white transition-colors"
            >
              <span className="material-symbols-outlined text-[18px]">visibility</span>
              View Store
            </Link>

            <button
              onClick={handleLogout}
              className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-[#b45309] hover:bg-[#92400e] text-[#fafaf9] transition-colors text-[13px] font-semibold"
            >
              <span className="material-symbols-outlined text-[18px]">logout</span>
              <span className="hidden sm:inline">Sign Out</span>
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 lg:p-6 bg-[#fafaf9] overflow-y-auto">{children}</main>

        <footer className="px-4 lg:px-6 py-3 border-t border-[#d6d3d1] bg-white text-[12px] text-[#57534e] flex flex-col sm:flex-row justify-between gap-2">
          <span>© 2024 ApexCommerce Admin • Role-restricted. All actions are logged.</span>
          <span className="flex items-center gap-1">
            <span className="w-1.5 h-1.5 rounded-full bg-[#15803d] animate-pulse" /> System operational
          </span>
        </footer>
      </div>
    </div>
  );
}
