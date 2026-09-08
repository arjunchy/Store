"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useAdminAlerts } from "@/context/AdminAlertsContext";

type NavItem = { label: string; icon: string; href: string; badge?: string };

const NAV: NavItem[] = [
  { label: "Dashboard", icon: "dashboard", href: "/admin" },
  { label: "Products", icon: "inventory_2", href: "/admin/products" },
  { label: "Categories", icon: "category", href: "/admin/categories" },
  { label: "Orders", icon: "receipt_long", href: "/admin/orders" },
  { label: "Users", icon: "group", href: "/admin/users" },
  { label: "Reviews", icon: "rate_review", href: "/admin/reviews" },
];

export default function AdminSidebar({
  mobileOpen,
  onClose,
}: {
  mobileOpen: boolean;
  onClose: () => void;
}) {
  const pathname = usePathname();
  const { user } = useAuth();
  const { pending, outOfStock } = useAdminAlerts();
  const lowStock = outOfStock;

  return (
    <>
      {mobileOpen && (
        <div className="fixed inset-0 bg-black/40 z-40 lg:hidden" onClick={onClose} aria-hidden />
      )}

      <aside
        className={`fixed lg:static inset-y-0 left-0 z-50 w-72 bg-[#0c0a09] border-r border-[#292524] flex flex-col shrink-0 transition-transform duration-300 lg:translate-x-0 ${
          mobileOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="h-16 flex items-center gap-3 px-6 border-b border-[#292524] shrink-0">
          <div className="w-9 h-9 rounded-xl bg-[#b45309] flex items-center justify-center">
            <span className="material-symbols-outlined text-[#fafaf9] text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>
              storefront
            </span>
          </div>
          <div>
            <div className="font-bold text-[#fafaf9] text-[16px] tracking-tight leading-none">ApexCommerce</div>
            <div className="text-[11px] font-semibold tracking-widest text-[#b45309] uppercase">Admin</div>
          </div>
          <button onClick={onClose} className="ml-auto lg:hidden p-2 rounded-full hover:bg-[#1c1917] text-[#a8a29e]">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {NAV.map((item) => {
            const active = pathname === item.href || (item.href !== "/admin" && pathname.startsWith(item.href));
            let badge: string | null = item.badge ?? null;
            if (item.href === "/admin/orders" && pending > 0) badge = String(pending);
            if (item.href === "/admin/products" && lowStock > 0) badge = String(lowStock);
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={onClose}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-[13px] font-medium transition-colors ${
                  active
                    ? "bg-[#b45309] text-[#fafaf9] shadow-sm"
                    : "text-[#a8a29e] hover:bg-[#1c1917] hover:text-[#fafaf9]"
                }`}
              >
                <span className={`material-symbols-outlined text-[20px] ${active ? "text-[#fafaf9]" : "text-[#b45309]"}`}>{item.icon}</span>
                {item.label}
                {badge && (
                  <span className={`ml-auto text-[11px] px-1.5 py-0.5 rounded-full font-semibold ${item.href === "/admin/orders" ? "bg-[#b45309] text-white" : "bg-[#b91c1c] text-white"}`}>{badge}</span>
                )}
              </Link>
            );
          })}

          <div className="pt-4 mt-4 border-t border-[#292524]">
            <p className="px-3 text-[11px] font-semibold tracking-widest text-[#a8a29e] uppercase mb-2">Storefront</p>
            <Link
              href="/"
              onClick={onClose}
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-[13px] font-medium text-[#a8a29e] hover:bg-[#1c1917] hover:text-[#fafaf9] transition-colors"
            >
              <span className="material-symbols-outlined text-[20px] text-[#b45309]">storefront</span>
              Back to Shop
              <span className="material-symbols-outlined text-[16px] ml-auto">open_in_new</span>
            </Link>
          </div>
        </nav>

        <div className="p-3 border-t border-[#292524] shrink-0">
          <div className="bg-[#1c1917] rounded-xl p-3 flex items-center gap-3 border border-[#292524]">
            <div className="w-9 h-9 rounded-full bg-[#b45309]/20 flex items-center justify-center shrink-0 border border-[#b45309]/30">
              <span className="font-semibold text-[#b45309] text-[13px]">{user?.username?.slice(0, 2).toUpperCase() ?? "AD"}</span>
            </div>
            <div className="min-w-0 flex-1">
              <p className="font-semibold text-[13px] text-[#fafaf9] truncate leading-none">{user?.username ?? "Admin"}</p>
              <p className="text-[11px] text-[#a8a29e] truncate">{user?.email}</p>
            </div>
            <span className="w-2 h-2 rounded-full bg-[#15803d] shrink-0" title="Online" />
          </div>
        </div>
      </aside>
    </>
  );
}
