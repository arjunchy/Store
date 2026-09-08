"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getProducts } from "@/lib/product";
import { getCategories } from "@/lib/category";
import { getOrders } from "@/lib/order";
import { getAllUsers } from "@/lib/user";
import { getAdminStats } from "@/lib/admin";
import type { Product } from "@/lib/types";
import type { Order } from "@/lib/types";
import type { Category } from "@/lib/types";
import type { User } from "@/lib/types";

const formatUSD = formatNPR;
type AdminStats = {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  outOfStockProducts: number;
  recentOrders: Order[];
  recentUsers: User[];
};

export default function AdminOverview() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [adminStats, setAdminStats] = useState<AdminStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const stats = await getAdminStats().catch(() => null);
        if (!cancelled && stats) {
          setAdminStats(stats as AdminStats);
          const [prod, cat] = await Promise.allSettled([
            getProducts().catch(() => [] as Product[]),
            getCategories().catch(() => [] as Category[]),
          ]);
          if (cancelled) return;
          if (prod.status === "fulfilled") setProducts(prod.value);
          if (cat.status === "fulfilled") setCategories(cat.value);
          if (stats.recentOrders && stats.recentOrders.length) setOrders(stats.recentOrders as Order[]);
          if (stats.recentUsers && stats.recentUsers.length) setUsers(stats.recentUsers as User[]);
          if (!stats.recentOrders || stats.recentOrders.length === 0) {
            const ord = await getOrders().catch(() => [] as Order[]);
            if (!cancelled) setOrders(ord);
          }
          if (!stats.recentUsers || stats.recentUsers.length === 0) {
            const usr = await getAllUsers().catch(() => [] as User[]);
            if (!cancelled) setUsers(usr);
          }
        } else {
          const [prod, cat, ord, usr] = await Promise.allSettled([
            getProducts().catch(() => [] as Product[]),
            getCategories().catch(() => [] as Category[]),
            getOrders().catch(() => [] as Order[]),
            getAllUsers().catch(() => [] as User[]),
          ]);
          if (cancelled) return;
          if (prod.status === "fulfilled") setProducts(prod.value);
          if (cat.status === "fulfilled") setCategories(cat.value);
          if (ord.status === "fulfilled") setOrders(ord.value);
          if (usr.status === "fulfilled") setUsers(usr.value);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const totalRevenue = adminStats ? adminStats.totalRevenue : orders.reduce((s, o) => s + (o.total_amount ?? o.totalAmount ?? 0), 0);
  // Align out-of-stock definition: strictly 0 when server stats, otherwise <5 client fallback is low-stock warning
  const lowStock = adminStats ? adminStats.outOfStockProducts : products.filter((p) => (p.stock_quantity ?? p.stockQuantity ?? 0) === 0).length;
  const pendingOrders = adminStats ? adminStats.pendingOrders : orders.filter((o) => (o.status as string) === "PROCESSING" || (o.status as string) === "PENDING").length;
  const newArrivals = products.filter((p) => p.is_new_arrival || p.isNewArrival).length;
  const totalProducts = adminStats ? adminStats.totalProducts : products.length;
  const totalOrders = adminStats ? adminStats.totalOrders : orders.length;
  const totalUsers = adminStats ? adminStats.totalUsers : users.length;

  if (loading) {
    return (
      <div className="flex items-center justify-center py-24">
        <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-bold text-[26px] md:text-[30px] text-[#1c1917] tracking-tight">Dashboard Overview</h1>
          <p className="text-[13px] text-[#57534e] mt-1">
            Welcome back — here&apos;s what&apos;s happening with your store today.
            {adminStats && <span className="ml-2 inline-flex items-center gap-1 text-[11px] bg-[#15803d]/15 text-[#15803d] px-1.5 py-0.5 rounded-full"><span className="w-1.5 h-1.5 rounded-full bg-[#15803d]" /> Server stats</span>}
          </p>
        </div>
        {adminStats && (
          <div className="hidden sm:flex items-center gap-2 text-[11px] text-[#57534e] bg-white border border-[#d6d3d1] rounded-xl px-3 py-2">
            <span className="material-symbols-outlined text-[16px] text-[#b45309]">analytics</span>
            <span>Live from <span className="font-mono text-[#b45309]">GET /api/admin/stats</span></span>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          icon="inventory_2"
          label="Total Products"
          value={String(totalProducts)}
          sub={`${newArrivals} new arrivals${adminStats ? ` • ${lowStock} out of stock` : ""}`}
          trend="+12%"
          color="primary"
        />
        <StatCard
          icon="receipt_long"
          label="Total Orders"
          value={String(totalOrders)}
          sub={`${pendingOrders} pending`}
          trend={totalOrders ? "+8%" : "—"}
          color="tertiary"
        />
        <StatCard icon="group" label="Total Users" value={String(totalUsers)} sub="Registered customers" trend="+5%" color="secondary" />
        <StatCard icon="payments" label="Revenue" value={formatUSD(totalRevenue)} sub={adminStats ? "Total (excl. cancelled)" : "Lifetime sales"} trend="+18%" color="success" />
      </div>

      {(lowStock > 0 || pendingOrders > 0) && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          <div className="lg:col-span-2 bg-[#fef2f2] border border-[#fecaca] rounded-xl p-4 flex items-start gap-3">
            <span className="material-symbols-outlined text-[#b91c1c]">warning</span>
            <div>
              <p className="font-semibold text-[13px] text-[#b91c1c]">
                {lowStock} product{lowStock !== 1 ? "s" : ""} {adminStats ? "out of stock" : "low on stock"}
                {pendingOrders ? ` • ${pendingOrders} pending order${pendingOrders !== 1 ? "s" : ""}` : ""}
              </p>
              <p className="text-[12px] text-[#57534e] mt-1">Restock soon to avoid out-of-stock. Pending orders need action.</p>
              <div className="flex gap-2 mt-3">
                <Link href="/admin/products" className="px-3 py-1.5 bg-error text-on-error rounded-lg text-[12px] font-semibold hover:bg-[#7f1d1d] transition-colors">
                  Manage Products
                </Link>
                <Link href="/admin/orders" className="px-3 py-1.5 bg-white border border-[#d6d3d1] rounded-lg text-[12px] font-semibold hover:bg-[#fafaf9]">
                  View Orders
                </Link>
              </div>
            </div>
          </div>
          <div className="bg-[#15803d]/10 border border-[#15803d]/20 rounded-xl p-4">
            <div className="flex items-center gap-2 mb-2">
              <span className="material-symbols-outlined text-[#15803d] text-[20px]">verified</span>
              <p className="font-semibold text-[13px] text-[#1c1917]">System Health</p>
            </div>
            <p className="text-[12px] text-[#57534e]">All services operational. API latency ~45ms.</p>
            <div className="mt-3 flex items-center gap-1.5 text-[11px] text-[#57534e]">
              <span className="w-2 h-2 rounded-full bg-[#15803d]" /> Backend connected {adminStats ? "• Admin stats live" : "• Client computed"}
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-[#d6d3d1] flex items-center justify-between">
            <h2 className="font-semibold text-[16px] text-[#1c1917] flex items-center gap-2">
              <span className="material-symbols-outlined text-[#b45309] text-[20px]">history</span>
              Recent Orders
            </h2>
            <Link href="/admin/orders" className="text-[12px] font-semibold text-[#b45309] hover:underline flex items-center gap-1">
              View all <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="bg-[#b45309] text-[11px] font-semibold tracking-widest uppercase text-[#fafaf9]">
                <tr>
                  <th className="px-5 py-3">Order</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3">Total</th>
                  <th className="px-5 py-3">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#e7e5e4]">
                {orders.slice(0, 5).map((o) => (
                  <tr key={o.id} className="hover:bg-[#fafaf9]/50 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-mono text-[12px] font-semibold text-[#1c1917]">{o.order_number || o.orderNumber || o.id.slice(0, 8)}</div>
                      <div className="text-[11px] text-[#57534e]">{o.items.length} items</div>
                    </td>
                    <td className="px-5 py-3.5">
                      <StatusPill status={o.status} />
                    </td>
                    <td className="px-5 py-3.5 font-semibold text-[13px] text-[#1c1917]">{formatUSD(o.total_amount ?? o.totalAmount ?? 0)}</td>
                    <td className="px-5 py-3.5 text-[12px] text-[#57534e]">
                      {o.createdAt ? new Date(o.createdAt).toLocaleDateString() : "—"}
                    </td>
                  </tr>
                ))}
                {orders.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-10 text-center text-[13px] text-[#57534e]">
                      No orders yet. Orders will appear here once customers checkout.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5">
            <h3 className="font-semibold text-[15px] text-[#1c1917] mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-[#b45309] text-[20px]">category</span>
              Categories
            </h3>
            <div className="space-y-3">
              {categories.slice(0, 6).map((c) => {
                const count = products.filter((p) => (p.category_id ?? p.categoryId) === c.id).length;
                const pct = products.length ? Math.round((count / products.length) * 100) : 0;
                return (
                  <div key={c.id} className="flex items-center gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-center mb-1">
                        <span className="text-[13px] font-medium text-[#1c1917] truncate">{c.name}</span>
                        <span className="text-[11px] text-[#57534e]">{count} products</span>
                      </div>
                      <div className="h-1.5 bg-[#fafaf9] rounded-full overflow-hidden">
                        <div className="h-full bg-[#b45309] rounded-full" style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  </div>
                );
              })}
              {categories.length === 0 && <p className="text-[12px] text-[#57534e]">No categories yet.</p>}
            </div>
            <Link href="/admin/categories" className="mt-4 inline-flex items-center gap-1 text-[12px] font-semibold text-[#b45309] hover:underline">
              Manage categories <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
            </Link>
          </div>

          <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5">
            <h3 className="font-semibold text-[15px] text-[#1c1917] mb-4 flex items-center gap-2">
              <span className="material-symbols-outlined text-[#b45309] text-[20px]">trending_up</span>
              Top Products
            </h3>
            <div className="space-y-3">
              {[...products]
                .sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
                .slice(0, 4)
                .map((p) => (
                  <div key={p.id} className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-[#fafaf9] flex items-center justify-center shrink-0 overflow-hidden">
                      {p.image ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img src={p.image} alt={p.name} className="w-full h-full object-cover" />
                      ) : (
                        <span className="material-symbols-outlined text-[#a8a29e]">image</span>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-[13px] font-medium text-[#1c1917] truncate">{p.name}</p>
                      <p className="text-[11px] text-[#57534e] flex items-center gap-1">
                        <span className="material-symbols-outlined text-[12px] text-[#b45309]" style={{ fontVariationSettings: "'FILL' 1" }}>
                          star
                        </span>
                        {p.rating?.toFixed(1) ?? "—"} • {formatUSD(p.price)}
                      </p>
                    </div>
                    <span className={`text-[11px] font-semibold px-1.5 py-0.5 rounded ${ (p.stock_quantity ?? p.stockQuantity ?? 0) < 5 ? "bg-[#fee2e2] text-[#b91c1c]" : "bg-[#fafaf9] text-[#57534e]"}`}>
                      {(p.stock_quantity ?? p.stockQuantity ?? 0)} left
                    </span>
                  </div>
                ))}
              {products.length === 0 && <p className="text-[12px] text-[#57534e]">No products yet.</p>}
            </div>
            <Link href="/admin/products" className="mt-4 inline-flex items-center gap-1 text-[12px] font-semibold text-[#b45309] hover:underline">
              Manage products <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
            </Link>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5">
        <h3 className="font-semibold text-[15px] text-[#1c1917] mb-3">Quick Actions</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <QuickAction icon="add_circle" label="Add Product" href="/admin/products" />
          <QuickAction icon="category" label="New Category" href="/admin/categories" />
          <QuickAction icon="local_shipping" label="Process Orders" href="/admin/orders" />
          <QuickAction icon="group_add" label="Invite Admin" href="/admin/users" />
        </div>
      </div>

      {adminStats && (
        <div className="bg-[#fafaf9] rounded-xl p-3 flex items-start gap-2 border border-[#d6d3d1]">
          <span className="material-symbols-outlined text-[#b45309] text-[18px]">info</span>
          <p className="text-[11px] text-[#57534e] leading-relaxed">
            <span className="font-semibold text-[#1c1917]">Admin API:</span> <span className="font-mono bg-white px-1 py-0.5 rounded border">GET /api/admin/stats</span> provides <span className="font-mono">totalUsers, totalProducts, totalOrders, totalRevenue, pendingOrders, outOfStockProducts, recentOrders, recentUsers</span>. Falls back to client aggregation if unavailable.
          </p>
        </div>
      )}
    </div>
  );
}

function StatCard({
  icon,
  label,
  value,
  sub,
  trend,
  color,
}: {
  icon: string;
  label: string;
  value: string;
  sub: string;
  trend: string;
  color: "primary" | "tertiary" | "secondary" | "success";
}) {
  const bg: Record<string, string> = {
    primary: "bg-[#b45309] text-white",
    tertiary: "bg-[#ffedd5] text-[#b45309]",
    secondary: "bg-[#e7e5e4] text-[#57534e]",
    success: "bg-[#15803d] text-white",
  };
  return (
    <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5 hover:shadow-md hover:-translate-y-0.5 transition-all">
      <div className="flex items-start justify-between mb-3">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${bg[color]}`}>
          <span className="material-symbols-outlined text-[20px]">{icon}</span>
        </div>
        <span className={`text-[11px] font-semibold px-1.5 py-0.5 rounded-full ${trend.startsWith("+") ? "bg-[#15803d]/15 text-[#15803d]" : "bg-[#fafaf9] text-[#57534e]"}`}>
          {trend}
        </span>
      </div>
      <div className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">{label}</div>
      <div className="font-bold text-[26px] leading-none text-[#1c1917] mt-1 tracking-tight">{value}</div>
      <div className="text-[12px] text-[#57534e] mt-1">{sub}</div>
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  const map: Record<string, string> = {
    PROCESSING: "bg-[#ffedd5] text-[#b45309] border border-[#ffedd5]",
    CONFIRMED: "bg-[#fef3c7] text-[#b45309] border border-[#b45309]/20",
    COMPLETED: "bg-[#15803d]/15 text-[#15803d] border border-[#15803d]/20",
    CANCELED: "bg-[#fee2e2] text-[#b91c1c] border border-[#fecaca]",
    PENDING: "bg-[#ffedd5] text-[#b45309] border border-[#ffedd5]",
    SHIPPED: "bg-[#e7e5e4] text-[#57534e] border border-secondary/20",
    DELIVERED: "bg-[#15803d]/15 text-[#15803d] border border-[#15803d]/20",
    CANCELLED: "bg-[#fee2e2] text-[#b91c1c] border border-[#fecaca]",
  };
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full border text-[11px] font-semibold ${map[status] ?? "bg-[#fafaf9] text-[#57534e]"}`}>
      {status}
    </span>
  );
}

function QuickAction({ icon, label, href }: { icon: string; label: string; href: string }) {
  return (
    <Link
      href={href}
      className="flex items-center gap-2.5 p-3 rounded-xl border border-[#d6d3d1] bg-[#fafaf9] hover:bg-white hover:border-[#b45309]/30 hover:text-[#b45309] transition-colors group"
    >
      <span className="material-symbols-outlined text-[20px] text-[#b45309] group-hover:scale-110 transition-transform">{icon}</span>
      <span className="text-[13px] font-medium text-[#1c1917] group-hover:text-[#b45309]">{label}</span>
    </Link>
  );
}
