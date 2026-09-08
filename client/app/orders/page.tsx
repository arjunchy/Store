"use client";

import { formatNPR } from "@/lib/format";
import { useCallback, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { ProtectedRoute } from "@/components/AuthGuard";
import { orderApi } from "@/lib/order-api";
import { usePaginatedData } from "@/hooks/usePaginatedData";
import { useUrlState } from "@/hooks/useUrlState";
import { getOrderStatusConfig, getPaymentStatusConfig, getDeliveryStatusConfig } from "@/lib/statusConfig";
import type { Order } from "@/lib/types";

const formatUSD = formatNPR;

export default function OrdersPage() {
  return (
    <ProtectedRoute>
      <OrdersInner />
    </ProtectedRoute>
  );
}

function OrdersInner() {
  const [urlState, setUrlState] = useUrlState({ q: "", page: "0" });
  const search = urlState.q;
  const pageFromUrl = parseInt(urlState.page || "0", 10);

  const fetcher = useCallback((params: any) => orderApi.getMyOrders({
    page: params.page,
    size: params.size,
    search: search || undefined,
  }), [search]);

  const { data: orders, page, setPage, totalPages, totalElements, loading, error, refresh } = usePaginatedData(fetcher, { page: pageFromUrl, size: 10 });

  const [cancelling, setCancelling] = useState<string | null>(null);

  const handleCancel = async (id: string) => {
    if (!confirm("Cancel this order?")) return;
    setCancelling(id);
    try {
      await orderApi.cancelOrder(id);
      await refresh();
    } catch (e: any) {
      alert(e?.message || "Cancel failed");
    } finally {
      setCancelling(null);
    }
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    setUrlState({ q: search, page: String(newPage) });
  };

  const handleSearchChange = (value: string) => {
    setUrlState({ q: value, page: "0" });
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1100px] mx-auto py-xl md:py-xxl">
        <div className="mb-lg flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 className="font-headline-lg text-headline-lg text-[#1c1917] font-semibold tracking-tight">My Orders</h1>
            <p className="font-body-md text-body-md text-[#57534e]">Track and review your past purchases.</p>
          </div>
          <span className="text-[11px] font-semibold bg-[#fafaf9] border border-[#d6d3d1] px-2 py-1 rounded-full text-[#57534e] self-start sm:self-auto">{totalElements} orders</span>
        </div>

        <div className="mb-4 flex gap-2">
          <div className="relative flex-1 max-w-md">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
            <input
              type="search"
              placeholder="Search by order number..."
              value={search}
              onChange={(e) => handleSearchChange(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 border border-[#d6d3d1] rounded-xl bg-white text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
            />
          </div>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-xl">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
          </div>
        ) : error ? (
          <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-4 py-3 text-[#b91c1c] text-[13px]">{error}</div>
        ) : orders.length === 0 ? (
          <div className="bg-white rounded-xl border border-[#d6d3d1] p-xl text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px] mb-3">receipt_long</span>
            <h3 className="font-headline-md text-headline-md text-[#1c1917] font-semibold mb-1">No orders found</h3>
            <p className="text-[#57534e] text-[14px] mb-4">You haven&apos;t placed any orders yet.</p>
            <Link href="/products" className="inline-flex bg-[#b45309] text-white font-label-md text-label-md px-lg py-3 rounded-xl hover:bg-[#92400e] transition-colors">
              Browse Products
            </Link>
          </div>
        ) : (
          <div className="flex flex-col gap-4">
            {orders.map((order) => {
              const oc = getOrderStatusConfig(order.status);
              const pc = getPaymentStatusConfig(order.payment_status ?? order.paymentStatus ?? "");
              const dc = getDeliveryStatusConfig(order.delivery_status ?? order.deliveryStatus ?? "");
              const canCancel = order.status === "PROCESSING" || order.status === "CONFIRMED";
              const isDelivered = order.status === "DELIVERED" || order.status === "COMPLETED";
              return (
                <div key={order.id} className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-md hover:shadow-md transition-all block">
                  <div className="flex flex-wrap items-center justify-between gap-3 mb-4 pb-3 border-b border-[#d6d3d1]">
                    <div>
                      <p className="font-semibold text-[14px] text-[#1c1917] flex items-center gap-2">
                        Order #{order.order_number || order.orderNumber || order.id.slice(0, 8)}
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${oc.color}`}>{oc.label}</span>
                      </p>
                      <p className="text-[12px] text-[#57534e]">{new Date(order.createdAt).toLocaleDateString()} • {order.items.length} Items • Total: {formatUSD(order.total_amount ?? order.totalAmount ?? 0)}</p>
                    </div>
                    <div className="flex gap-1.5">
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${pc.color}`}>{pc.label}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${dc.color}`}>{dc.label}</span>
                    </div>
                  </div>
                  <div className="flex flex-col gap-3">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-lg bg-[#fafaf9] overflow-hidden relative shrink-0 flex items-center justify-center">
                          {item.image ? (
                            <Image src={item.image} alt={item.name || "item"} fill unoptimized className="object-cover" sizes="48px" onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }} />
                          ) : (
                            <span className="material-symbols-outlined text-[#a8a29e] text-[20px]">image</span>
                          )}
                        </div>
                        <div className="flex-grow min-w-0">
                          <Link href={`/product/${item.product_id || item.productId}`} className="font-medium text-[13px] text-[#1c1917] truncate hover:text-[#b45309] hover:underline block">
                            {item.name || item.product_id}
                          </Link>
                          <p className="text-[12px] text-[#57534e]">Qty: {item.quantity} • {formatUSD(item.price)} each</p>
                        </div>
                        <p className="font-semibold text-[13px] text-[#1c1917]">{formatUSD(item.subtotal)}</p>
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-between items-center mt-4 pt-3 border-t border-[#d6d3d1]">
                    <span className="text-[13px] text-[#57534e]">Total</span>
                    <span className="font-bold text-[16px] text-[#b45309]">{formatUSD(order.total_amount ?? order.totalAmount ?? 0)}</span>
                  </div>
                  <div className="flex gap-2 mt-3">
                    <Link href={`/orders/${order.id}`} className="flex-1 py-2 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] text-center text-[13px] font-semibold hover:bg-[#fafaf9]">View Details</Link>
                    {canCancel && (
                      <button onClick={() => handleCancel(order.id)} disabled={cancelling === order.id} className="flex-1 py-2 rounded-xl bg-[#fee2e2] text-[#b91c1c] border border-[#fecaca] text-[13px] font-semibold hover:bg-error hover:text-on-error disabled:opacity-50 flex items-center justify-center gap-1">
                        {cancelling === order.id && <span className="material-symbols-outlined animate-spin text-[14px]">progress_activity</span>} Cancel
                      </button>
                    )}
                    {isDelivered && (
                      <Link href={`/products`} className="flex-1 py-2 rounded-xl bg-[#b45309] text-white text-center text-[13px] font-semibold hover:bg-[#92400e]">Reorder</Link>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-between mt-6">
            <button onClick={() => handlePageChange(page - 1)} disabled={page <= 0} className="px-4 py-2 rounded-xl border border-[#d6d3d1] bg-white text-[13px] font-semibold disabled:opacity-40">Prev</button>
            <span className="text-[13px] text-[#57534e]">Page {page + 1} of {totalPages} • {totalElements} orders</span>
            <button onClick={() => handlePageChange(page + 1)} disabled={page + 1 >= totalPages} className="px-4 py-2 rounded-xl border border-[#d6d3d1] bg-white text-[13px] font-semibold disabled:opacity-40">Next</button>
          </div>
        )}
      </main>
      <Footer />
    </div>
  );
}
