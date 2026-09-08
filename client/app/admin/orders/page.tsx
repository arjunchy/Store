"use client";

import { formatNPR } from "@/lib/format";
import { useEffect, useState, useCallback, useRef } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Image from "next/image";
import { apiClient } from "@/lib/api-client";
import { orderAdminApi } from "@/lib/order-api";
import { getOrderFullDetails } from "@/lib/admin";
import { getStatusHistory } from "@/lib/order";
import type { Order, OrderStatus, OrderStatusHistory, PaymentStatus, DeliveryStatus } from "@/lib/types";
import { StatusControl } from "@/components/admin/OrderStatusControl";
import OrderDrawer from "@/components/admin/orders/OrderDrawer";
import { ORDER_STATUSES, DELIVERY_STATUSES, PAYMENT_STATUSES, ORDER_ALLOWED, DELIVERY_ALLOWED, PAYMENT_ALLOWED, getOrderStatusConfig, getPaymentStatusConfig, getDeliveryStatusConfig, isValidOrderTransition, isValidDeliveryTransition, isValidPaymentTransition, getDisabledReason } from "@/lib/statusConfig";

const formatUSD = formatNPR;

export default function AdminOrdersPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [orders, setOrders] = useState<Order[]>([]);
  const [totalElements, setTotalElements] = useState<number | null>(null);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [loading, setLoading] = useState(true);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [deliveryFilter, setDeliveryFilter] = useState<string>("ALL");
  const [paymentFilter, setPaymentFilter] = useState<string>("ALL");
  const [updating, setUpdating] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detail, setDetail] = useState<Order | null>(null);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const [fulfillment, setFulfillment] = useState<{ trackingNumber: string; carrier: string; estimatedDelivery: string }>({
    trackingNumber: "",
    carrier: "",
    estimatedDelivery: "",
  });
  const [fulfillmentSaving, setFulfillmentSaving] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<Order | null>(null);
  const [verifyData, setVerifyData] = useState<any>(null);
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [confirmInput, setConfirmInput] = useState("");
  const [confirmChecked, setConfirmChecked] = useState(false);
  const [forceChecked, setForceChecked] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const pageRef = useRef(0);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchInput.trim()), 400);
    return () => clearTimeout(t);
  }, [searchInput]);

  const load = useCallback(async (opts?: { pageOverride?: number }) => {
    const p = opts?.pageOverride ?? pageRef.current;
    setLoading(true);
    setError("");
    try {
      const res: any = await orderAdminApi.getOrders({
        page: p,
        size: PAGE_SIZE,
        status: statusFilter !== "ALL" ? statusFilter : undefined,
        deliveryStatus: deliveryFilter !== "ALL" ? deliveryFilter : undefined,
        paymentStatus: paymentFilter !== "ALL" ? paymentFilter : undefined,
        search: debouncedSearch || undefined,
      });
      const list = res.content ?? [];
      setOrders(Array.isArray(list) ? list : []);
      setTotalElements(res.totalElements ?? null);
      setTotalPages(res.totalPages ?? Math.ceil((res.totalElements ?? 0) / PAGE_SIZE));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load orders");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, deliveryFilter, paymentFilter, debouncedSearch]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    pageRef.current = 0;
    setPage(0);
  }, [statusFilter, deliveryFilter, paymentFilter, debouncedSearch]);

  useEffect(() => {
    const oid = searchParams.get("orderId");
    if (oid && oid !== selectedId) setSelectedId(oid);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const handleStatusChange = async (orderId: string, newStatus: OrderStatus) => {
    const order = orders.find((o) => o.id === orderId);
    if (!order) return;
    const allowed = (ORDER_ALLOWED as Record<string, string[]>)[order.status as string] ?? [];
    if (!(allowed as string[]).includes(newStatus as unknown as string) && (order.status as string) !== (newStatus as string)) {
      showToast(`Invalid: ${order.status} → ${newStatus}. Allowed: ${allowed.join(", ") || "terminal"}`, "error");
      return;
    }
    if ((order.status as string) === (newStatus as string)) return;
    if (["CANCELLED", "CANCELED", "COMPLETED"].includes(order.status as string)) {
      showToast(`Order is ${order.status} — terminal`, "error");
      return;
    }
    setUpdating(orderId);
    try {
      const updated = await orderAdminApi.updateStatus(orderId, newStatus, `Admin updated ${order.status} → ${newStatus}`);
      if (updated) {
        setOrders((prev) => prev.map((o) => (o.id === orderId ? (updated as Order) : o)));
        showToast(`Order ${updated.order_number || updated.id.slice(0, 8)} → ${newStatus}`, "success");
      } else await load();
      if (selectedId === orderId) {
        const fresh = await getOrderFullDetails(orderId).catch(() => null);
        if (fresh) {
          setDetail(fresh as Order);
          setFulfillment({
            trackingNumber: (fresh as any).trackingNumber ?? (fresh as any).tracking_number ?? "",
            carrier: (fresh as any).carrier ?? "",
            estimatedDelivery: (fresh as any).estimatedDelivery ?? (fresh as any).estimated_delivery ?? "",
          });
        }
        const h = await getStatusHistory(orderId).catch(() => [] as OrderStatusHistory[]);
        setHistory(h);
      }
    } catch (e: unknown) {
      showToast(e instanceof Error ? e.message : "Failed to update status", "error");
    } finally {
      setUpdating(null);
    }
  };

  const handlePaymentChange = async (orderId: string, newPs: PaymentStatus) => {
    const order = orders.find(o=>o.id===orderId);
    const cur = (order?.payment_status ?? (order as any)?.paymentStatus ?? "UNPAID") as string;
    const allowedP = (PAYMENT_ALLOWED as Record<string,string[]>)[cur] ?? [];
    if (cur !== newPs && !allowedP.includes(newPs as unknown as string)) {
      showToast(`Invalid payment: ${cur} → ${newPs}. Allowed: ${allowedP.join(", ")||"terminal"}`, "error");
      return;
    }
    setUpdating(orderId);
    try {
      const updated = await orderAdminApi.updatePayment(orderId, newPs);
      if (updated) {
        setOrders((prev) => prev.map((o) => (o.id === orderId ? (updated as Order) : o)));
      } else await load();
      showToast(`Payment ${orderId.slice(0, 8)} → ${newPs}`, "success");
      if (selectedId === orderId) {
        const fresh = await getOrderFullDetails(orderId).catch(() => null);
        if (fresh) {
          setDetail(fresh as Order);
          setFulfillment({
            trackingNumber: (fresh as any).trackingNumber ?? (fresh as any).tracking_number ?? "",
            carrier: (fresh as any).carrier ?? "",
            estimatedDelivery: (fresh as any).estimatedDelivery ?? (fresh as any).estimated_delivery ?? "",
          });
        }
        const h = await getStatusHistory(orderId).catch(() => [] as OrderStatusHistory[]);
        setHistory(h);
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Payment status update failed", "error");
    } finally {
      setUpdating(null);
    }
  };

  const handleDeliveryChange = async (orderId: string, newDs: DeliveryStatus) => {
    const order = orders.find((o) => o.id === orderId);
    const currentDelivery = (order?.delivery_status ?? (order as unknown as { deliveryStatus?: string })?.deliveryStatus ?? "PLACED") as string;
    const allowed = (DELIVERY_ALLOWED as Record<string, string[]>)[currentDelivery] ?? [];
    if (!(allowed as string[]).includes(newDs as unknown as string) && currentDelivery !== (newDs as string)) {
      showToast(`Invalid delivery: ${currentDelivery} → ${newDs}. Allowed: ${allowed.join(", ") || "terminal"}`, "error");
      return;
    }
    setUpdating(orderId);
    try {
      const updated = await orderAdminApi.updateDelivery(orderId, newDs);
      if (updated) {
        setOrders((prev) => prev.map((o) => (o.id === orderId ? (updated as Order) : o)));
      } else await load();
      showToast(`Delivery ${orderId.slice(0, 8)} → ${newDs}`, "success");
      if (selectedId === orderId) {
        const fresh = await getOrderFullDetails(orderId).catch(() => null);
        if (fresh) {
          setDetail(fresh as Order);
          setFulfillment({
            trackingNumber: (fresh as any).trackingNumber ?? (fresh as any).tracking_number ?? "",
            carrier: (fresh as any).carrier ?? "",
            estimatedDelivery: (fresh as any).estimatedDelivery ?? (fresh as any).estimated_delivery ?? "",
          });
        }
        const h = await getStatusHistory(orderId).catch(() => [] as OrderStatusHistory[]);
        setHistory(h);
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Delivery status update failed", "error");
    } finally {
      setUpdating(null);
    }
  };

  const handleFulfillmentSave = async () => {
    if (!detail) return;
    setFulfillmentSaving(true);
    try {
      const body: Record<string, string> = {};
      if (fulfillment.trackingNumber.trim()) body.trackingNumber = fulfillment.trackingNumber.trim();
      if (fulfillment.carrier.trim()) body.carrier = fulfillment.carrier.trim();
      if (fulfillment.estimatedDelivery) body.estimatedDelivery = fulfillment.estimatedDelivery;
      if (Object.keys(body).length === 0) {
        showToast("Enter at least one fulfillment field", "error");
        setFulfillmentSaving(false);
        return;
      }
      await apiClient.patch(`/orders/${detail.id}/fulfillment`, body, { auth: true });
      showToast("Fulfillment updated", "success");
      const fresh = await getOrderFullDetails(detail.id).catch(() => null);
      if (fresh) {
        setDetail(fresh as Order);
        setOrders((prev) => prev.map((o) => (o.id === detail.id ? (fresh as Order) : o)));
        setFulfillment({
          trackingNumber: (fresh as any).trackingNumber ?? (fresh as any).tracking_number ?? "",
          carrier: (fresh as any).carrier ?? "",
          estimatedDelivery: (fresh as any).estimatedDelivery ?? (fresh as any).estimated_delivery ?? "",
        });
      }
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Fulfillment update failed", "error");
    } finally {
      setFulfillmentSaving(false);
    }
  };

  const openDetail = async (orderId: string) => {
    setSelectedId(orderId);
    try {
      const params = new URLSearchParams(searchParams.toString());
      params.set("orderId", orderId);
      router.replace(`?${params.toString()}` as any);
    } catch { router.replace(`?orderId=${orderId}` as any); }
    setDetailLoading(true);
    try {
      const [full, h] = await Promise.all([getOrderFullDetails(orderId).catch(() => null), getStatusHistory(orderId).catch(() => [] as OrderStatusHistory[])]);
      const fallback = orders.find((o) => o.id === orderId) ?? null;
      const d = (full as Order) ?? fallback;
      setDetail(d);
      if (d) {
        setFulfillment({
          trackingNumber: (d as any).trackingNumber ?? (d as any).tracking_number ?? "",
          carrier: (d as any).carrier ?? "",
          estimatedDelivery: (d as any).estimatedDelivery ?? (d as any).estimated_delivery ?? "",
        });
      }
      setHistory(h);
    } finally {
      setDetailLoading(false);
    }
  };
  const closeDetail = () => {
    setSelectedId(null);
    setDetail(null);
    try {
      const params = new URLSearchParams(searchParams.toString());
      params.delete("orderId");
      const qs = params.toString();
      router.replace(qs ? `?${qs}` as any : "?" as any);
    } catch { router.replace("?" as any); }
  };

  const startDeleteVerification = async (order: Order) => {
    setDeleteTarget(order);
    setVerifyData(null);
    setConfirmInput("");
    setConfirmChecked(false);
    setForceChecked(false);
    setDeleteError("");
    setVerifyLoading(true);
    try {
      const v = await orderAdminApi.verifyDelete(order.id).catch(() => null);
      if (v) setVerifyData(v);
      else {
        setVerifyData({
          id: order.id,
          orderNumber: (order as any).orderNumber ?? (order as any).order_number ?? order.id,
          userId: (order as any).userId,
          userEmail: (order as any).userEmail ?? (order as any).userName ?? "—",
          totalAmount: (order as any).totalAmount ?? (order as any).total_amount ?? 0,
          status: order.status,
          paymentStatus: (order as any).paymentStatus ?? (order as any).payment_status ?? "UNPAID",
          deliveryStatus: (order as any).deliveryStatus ?? (order as any).delivery_status ?? "PLACED",
          items: order.items,
          createdAt: order.createdAt,
        });
      }
      const isTerminal = ["CANCELED","COMPLETED","DELIVERED"].includes(order.status) || (order as any).deliveryStatus === "RETURNED" || (order as any).deliveryStatus === "COLLECTED";
      if (!isTerminal) setDeleteError("Order is not in terminal state — you must check 'Force delete' to proceed (use with caution).");
      else setDeleteError("");
    } catch (e: any) {
      setDeleteError(e?.message || "Verification failed");
    } finally {
      setVerifyLoading(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget || !verifyData) return;
    const expected = verifyData.orderNumber ?? verifyData.order_number ?? deleteTarget.orderNumber ?? deleteTarget.id;
    if (confirmInput.trim() !== expected) {
      setDeleteError(`Type "${expected}" to confirm`);
      return;
    }
    if (!confirmChecked) {
      setDeleteError("Check the confirmation box");
      return;
    }
    const isTerminal = ["CANCELED","COMPLETED","DELIVERED"].includes(deleteTarget.status) || (deleteTarget as any).deliveryStatus === "RETURNED";
    if (!isTerminal && !forceChecked) {
      setDeleteError("Force delete required for non-terminal order — check the force box");
      return;
    }
    setDeleting(true);
    setDeleteError("");
    try {
      const needsForce = !["CANCELED","COMPLETED","DELIVERED"].includes(deleteTarget.status) && (deleteTarget as any).deliveryStatus !== "RETURNED";
      await orderAdminApi.deleteOrder(deleteTarget.id, needsForce ? forceChecked : false);
      showToast(`Order ${expected} deleted`, "success");
      setOrders((prev) => prev.filter((o) => o.id !== deleteTarget.id));
      setTotalElements((prev) => (prev !== null ? prev - 1 : prev));
      if (selectedId === deleteTarget.id) closeDetail();
      setDeleteTarget(null);
      setVerifyData(null);
      setConfirmInput("");
      setConfirmChecked(false);
      setForceChecked(false);
    } catch (e: any) {
      const msg = e?.data?.message ?? e?.message ?? "Delete failed";
      setDeleteError(msg);
      if (msg.includes("force=true") || msg.includes("terminal")) {
        setDeleteError(msg + " — check 'Force delete' and retry.");
      }
    } finally {
      setDeleting(false);
    }
  };

  const goPage = (newPage: number) => {
    if (newPage < 0 || (totalPages > 0 && newPage >= totalPages)) return;
    pageRef.current = newPage;
    setPage(newPage);
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div>
          <h1 className="font-bold text-[24px] text-[#1c1917] tracking-tight flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">receipt_long</span>
            Orders
            {totalElements !== null && <span className="text-[11px] font-semibold bg-[#fafaf9] border border-[#d6d3d1] px-2 py-1 rounded-full text-[#57534e]">{totalElements} total</span>}
            {totalPages > 1 && <span className="text-[11px] font-semibold bg-[#fef3c7] text-[#b45309] px-2 py-1 rounded-full">Page {page + 1}/{totalPages}</span>}
          </h1>
          <p className="text-[13px] text-[#57534e]">ADMIN — view <span className="font-semibold text-[#1c1917]">all users&apos; orders</span> via <span className="font-mono text-[#b45309]">GET /orders/all</span> • Enforces valid lifecycle.</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="hidden sm:inline-flex items-center gap-1.5 text-[11px] bg-[#15803d]/10 border border-[#15803d]/20 text-[#15803d] px-2 py-1 rounded-full font-medium">
            <span className="w-2 h-2 rounded-full bg-[#15803d] animate-pulse" /> Live
          </span>
          <button onClick={() => load({ pageOverride: page })} className="px-3 py-2 rounded-xl border border-[#d6d3d1] bg-white hover:bg-[#fafaf9] text-[12px] font-semibold flex items-center gap-1">
            <span className="material-symbols-outlined text-[16px]">refresh</span> Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-center gap-2 text-[#b91c1c] text-[13px]">
          <span className="material-symbols-outlined text-[18px]">error</span> {error}
        </div>
      )}

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-4 flex flex-col gap-3">
        <div className="flex flex-col lg:flex-row gap-3">
          <div className="relative flex-1">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
            <input
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by order ID, number or customer..."
              className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-xl pl-10 pr-4 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
            />
          </div>
          <button
            onClick={() => { setSearchInput(""); setStatusFilter("ALL"); setDeliveryFilter("ALL"); setPaymentFilter("ALL"); }}
            className="px-4 py-2 rounded-xl border border-[#d6d3d1] bg-[#fafaf9] text-[12px] font-semibold hover:bg-white"
          >Clear</button>
        </div>
        <div className="flex flex-wrap gap-2">
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="px-3 py-2 rounded-xl border border-[#d6d3d1] bg-[#fafaf9] text-[12px] font-semibold focus:border-[#b45309] outline-none">
            <option value="ALL">All Statuses</option>
            {ORDER_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <select value={deliveryFilter} onChange={(e) => setDeliveryFilter(e.target.value)} className="px-3 py-2 rounded-xl border border-[#d6d3d1] bg-[#fafaf9] text-[12px] font-semibold focus:border-[#b45309] outline-none">
            <option value="ALL">All Delivery</option>
            {DELIVERY_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <select value={paymentFilter} onChange={(e) => setPaymentFilter(e.target.value)} className="px-3 py-2 rounded-xl border border-[#d6d3d1] bg-[#fafaf9] text-[12px] font-semibold focus:border-[#b45309] outline-none">
            <option value="ALL">All Payment</option>
            {PAYMENT_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <div className="hidden sm:flex gap-1 overflow-x-auto ml-auto">
            {["ALL", ...ORDER_STATUSES].map((s) => (
              <button key={s} onClick={() => setStatusFilter(s)} className={`px-2.5 py-1.5 rounded-lg text-[11px] font-semibold border whitespace-nowrap ${statusFilter===s?"bg-[#b45309] text-white border-[#b45309]":"bg-[#fafaf9] border-[#d6d3d1] text-[#57534e] hover:text-[#1c1917]"}`}>{s === "ALL" ? "All" : s}</button>
            ))}
          </div>
        </div>
        <div className="flex flex-wrap gap-2 mt-2">
          <span className="text-[11px] font-semibold text-[#57534e]">Payment:</span>
          {["ALL", ...PAYMENT_STATUSES].map((s) => (
            <button key={s} onClick={() => setPaymentFilter(s)} className={`px-2.5 py-1.5 rounded-lg text-[11px] font-semibold border whitespace-nowrap ${paymentFilter===s?"bg-[#b45309] text-white border-[#b45309]":"bg-[#fafaf9] border-[#d6d3d1] text-[#57534e] hover:text-[#1c1917]"}`}>{s === "ALL" ? "All" : getPaymentStatusConfig(s).label}</button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span>
            <span className="text-[12px] text-[#57534e]">Loading all orders…</span>
          </div>
        ) : orders.length === 0 ? (
          <div className="py-16 text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">receipt</span>
            <p className="text-[14px] text-[#57534e] mt-2">No orders match your filters.</p>
            <p className="text-[11px] text-[#57534e] mt-1">Try “ALL” or clear search. Orders appear after customers checkout.</p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead className="bg-[#b45309] text-[11px] font-semibold tracking-widest uppercase text-[#fafaf9]">
                  <tr>
                    <th className="px-4 py-3">Order / Customer</th>
                    <th className="px-4 py-3">Order Status</th>
                    <th className="px-4 py-3">Delivery</th>
                    <th className="px-4 py-3">Payment</th>
                    <th className="px-4 py-3">Items</th>
                    <th className="px-4 py-3">Total</th>
                    <th className="px-4 py-3">Date</th>
                    <th className="px-4 py-3 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#e7e5e4]">
                  {orders.map((o) => {
                    const allowed = ORDER_ALLOWED[o.status as string] ?? [];
                    const delivery = (o.delivery_status ?? (o as unknown as { deliveryStatus?: string }).deliveryStatus ?? "PLACED") as string;
                    const allowedDelivery = DELIVERY_ALLOWED[delivery] ?? [];
                    const payment = (o.payment_status ?? (o as unknown as { paymentStatus?: string }).paymentStatus ?? "UNPAID") as string;
                    return (
                      <tr key={o.id} className="hover:bg-[#fafaf9] transition-colors cursor-pointer" onClick={() => openDetail(o.id)}>
                        <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                          <button onClick={() => openDetail(o.id)} className="text-left hover:underline group">
                            <div className="font-mono text-[12px] font-semibold text-[#b45309] group-hover:text-[#92400e]">{o.order_number || o.orderNumber || o.id.slice(0, 12)}</div>
                            <div className="text-[11px] text-[#57534e] truncate max-w-[180px]">ID: {o.id.slice(0, 12)} • {o.items.length} items</div>
                            <div className="flex items-center gap-1 text-[11px] text-[#b45309] truncate max-w-[180px] mt-0.5">
                              <span className="material-symbols-outlined text-[12px]">mail</span>
                              <span className="truncate">{(o as unknown as { userEmail?: string }).userEmail ?? (o as unknown as { userName?: string }).userName ?? "—"}</span>
                            </div>
                          </button>
                        </td>
                        <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                          <StatusControl
                            orderId={o.id}
                            current={o.status}
                            allowed={allowed}
                            allStatuses={ORDER_STATUSES as unknown as string[]}
                            onChange={handleStatusChange as any}
                            updatingId={updating}
                            variant="order"
                          />
                        </td>
                        <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                          <StatusControl
                            orderId={o.id}
                            current={delivery}
                            allowed={allowedDelivery}
                            allStatuses={DELIVERY_STATUSES as unknown as string[]}
                            onChange={handleDeliveryChange as any}
                            updatingId={updating}
                            variant="delivery"
                          />
                        </td>
                         <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                           <select
                             value={payment}
                             onChange={(e) => handlePaymentChange(o.id, e.target.value as PaymentStatus)}
                             disabled={updating === o.id}
                             className="px-2 py-1 rounded-lg border border-[#d6d3d1] bg-[#fafaf9] text-[11px] font-semibold focus:border-[#b45309] outline-none disabled:opacity-50"
                           >
                             {PAYMENT_STATUSES.map((ps) => {
                               const isCurrent = ps === payment;
                               const isValid = isValidPaymentTransition(payment, ps);
                               return (
                                 <option
                                   key={ps}
                                   value={ps}
                                   disabled={!isValid && !isCurrent}
                                   title={!isValid && !isCurrent ? getDisabledReason(payment, ps) : undefined}
                                 >
                                   {getPaymentStatusConfig(ps).label}
                                   {isCurrent ? " (current)" : !isValid ? " (disabled)" : ""}
                                 </option>
                               );
                             })}
                           </select>
                         </td>
                        <td className="px-4 py-3 text-[12px] text-[#57534e] max-w-[160px]">
                          <div className="truncate">{o.items.map((it) => it.name ?? it.product_id).join(", ") || "—"}</div>
                          <div className="text-[11px]">{o.items.length} items</div>
                        </td>
                        <td className="px-4 py-3 font-semibold text-[13px] text-[#1c1917]">{formatUSD(o.total_amount ?? o.totalAmount ?? 0)}</td>
                        <td className="px-4 py-3 text-[12px] text-[#57534e]">{o.createdAt ? new Date(o.createdAt).toLocaleString() : "—"}</td>
                        <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                          <div className="flex justify-end items-center gap-1">
                            {updating === o.id && <span className="material-symbols-outlined animate-spin text-[16px] text-[#b45309] mr-2">progress_activity</span>}
                            <button onClick={() => openDetail(o.id)} className="p-2 rounded-lg hover:bg-[#fafaf9] border border-[#d6d3d1] text-[#57534e] hover:text-[#b45309]" title="View details">
                              <span className="material-symbols-outlined text-[18px]">visibility</span>
                            </button>
                            <button onClick={() => startDeleteVerification(o)} className="p-2 rounded-lg hover:bg-[#fef2f2] border border-[#d6d3d1] text-[#57534e] hover:text-[#b91c1c]" title="Delete order (ADMIN — verify first)">
                              <span className="material-symbols-outlined text-[18px]">delete_forever</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-[#e7e5e4] bg-[#fafaf9]">
                <button onClick={() => goPage(page - 1)} disabled={page <= 0} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40 hover:bg-[#fafaf9]">← Prev</button>
                <span className="text-[12px] text-[#57534e]">Page <span className="font-semibold text-[#1c1917]">{page + 1}</span> of {totalPages} • {totalElements} orders</span>
                <button onClick={() => goPage(page + 1)} disabled={page + 1 >= totalPages} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40 hover:bg-[#fafaf9]">Next →</button>
              </div>
            )}
          </>
        )}
      </div>

      {selectedId && (
        <div className="fixed inset-0 z-50 flex">
          <div className="flex-1 bg-black/40 backdrop-blur-sm" onClick={closeDetail} />
          <div className="w-full max-w-xl bg-white shadow-xl border-l border-[#d6d3d1] flex flex-col overflow-hidden">
            <div className="px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between shrink-0">
              <div>
                <h3 className="font-bold text-[16px] text-[#1c1917] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[#b45309]">receipt</span> Order {detail?.order_number || detail?.orderNumber || selectedId.slice(0, 8)}
                </h3>
                {detail && <p className="text-[11px] font-mono text-[#57534e]">{detail.id}</p>}
                {detail && ((detail as unknown as { userEmail?: string }).userEmail || (detail as unknown as { userName?: string }).userName) && (
                  <p className="text-[11px] text-[#b45309] flex items-center gap-1 mt-1">
                    <span className="material-symbols-outlined text-[12px]">mail</span> {(detail as unknown as { userEmail?: string }).userEmail ?? (detail as unknown as { userName?: string }).userName}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-1">
                {detail && (
                  <button onClick={() => startDeleteVerification(detail)} className="p-1.5 rounded-lg bg-[#fee2e2]/20 text-[#b91c1c] border border-[#fecaca] hover:bg-[#fef2f2]" title="Delete order — verify first (ADMIN)">
                    <span className="material-symbols-outlined text-[18px]">delete_forever</span>
                  </button>
                )}
                <button onClick={closeDetail} className="p-1.5 rounded-full hover:bg-[#fafaf9]">
                  <span className="material-symbols-outlined">close</span>
                </button>
              </div>
            </div>
            <div className="flex-1 overflow-auto p-6 space-y-5">
              {detailLoading ? (
                <div className="py-12 flex flex-col items-center gap-2">
                  <span className="material-symbols-outlined animate-spin text-[#b45309] text-[24px]">progress_activity</span>
                  <span className="text-[12px] text-[#57534e]">Loading details…</span>
                </div>
              ) : detail ? (
                <>
                  <div className="grid grid-cols-3 gap-3">
                    <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
                      <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Order</p>
                      <div className="mt-1"><StatusPill status={detail.status} /></div>
                      <p className="text-[10px] text-[#57534e] mt-1">Payment</p>
                      <span className="inline-flex px-1.5 py-0.5 rounded-full text-[10px] font-semibold border bg-amber-50 text-amber-700 border-amber-200">{detail.payment_status ?? (detail as unknown as { paymentStatus: string }).paymentStatus ?? "UNPAID"}</span>
                    </div>
                    <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
                      <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Delivery</p>
                      <span className="inline-flex px-2 py-1 rounded-full text-[11px] font-semibold border bg-white border-[#d6d3d1] text-[#57534e]">
                        {(detail.delivery_status ?? (detail as unknown as { deliveryStatus: string }).deliveryStatus) ?? "PLACED"}
                      </span>
                      <p className="text-[10px] text-[#57534e] mt-1 truncate">{detail.tracking_number ?? detail.trackingNumber ?? "No tracking"}</p>
                    </div>
                    <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
                      <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Total</p>
                      <p className="font-bold text-[16px] text-[#b45309]">{formatUSD(detail.total_amount ?? detail.totalAmount ?? 0)}</p>
                      <p className="text-[11px] text-[#57534e]">{detail.items.length} items • {detail.createdAt ? new Date(detail.createdAt).toLocaleDateString() : ""}</p>
                    </div>
                  </div>

                  <div className="p-4 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] space-y-3">
                    <h4 className="font-semibold text-[13px] text-[#1c1917] flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[16px] text-[#b45309]">local_shipping</span> Fulfillment
                    </h4>
                    <div className="grid grid-cols-1 gap-3">
                      <div>
                        <label className="text-[11px] font-semibold text-[#57534e]">Tracking Number</label>
                        <input value={fulfillment.trackingNumber} onChange={(e) => setFulfillment((p) => ({ ...p, trackingNumber: e.target.value }))} placeholder="e.g. TRK-123456" className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="text-[11px] font-semibold text-[#57534e]">Carrier</label>
                          <input value={fulfillment.carrier} onChange={(e) => setFulfillment((p) => ({ ...p, carrier: e.target.value }))} placeholder="DHL / UPS / FedEx" className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-[#57534e]">Est. Delivery</label>
                          <input type="date" value={fulfillment.estimatedDelivery} onChange={(e) => setFulfillment((p) => ({ ...p, estimatedDelivery: e.target.value }))} className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                        </div>
                      </div>
                    </div>
                    <button onClick={handleFulfillmentSave} disabled={fulfillmentSaving} className="w-full py-2 rounded-xl bg-[#b45309] text-white font-semibold text-[13px] hover:bg-[#92400e] disabled:opacity-50 flex items-center justify-center gap-1">
                      {fulfillmentSaving && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>} Save Fulfillment
                    </button>
                    <p className="text-[10px] text-[#57534e]">Stored via <span className="font-mono">PATCH /orders/{"{id}"}/fulfillment</span>. Filling tracking when setting delivery to <span className="font-mono">SHIPPING</span> is recommended.</p>
                  </div>

                  <div className="bg-[#fef3c7]/10 border border-[#b45309]/20 rounded-xl p-3 flex gap-2.5">
                    <span className="material-symbols-outlined text-[#b45309] text-[18px] shrink-0">account_tree</span>
                    <div className="text-[11px] leading-relaxed text-[#57534e]">
                      <p className="font-bold text-[#1c1917]">Workflow — Delivery drives Order</p>
                      <p className="mt-1"><span className="font-mono bg-white px-1 py-0.5 rounded border text-[10px]">PLACED (PROCESSING/UNPAID)</span> → <span className="font-semibold">SHIPPING</span> → auto <span className="font-mono">CONFIRMED</span> → <span className="font-semibold">ARRIVED</span> → <span className="font-semibold">COLLECTED</span> → auto <span className="font-mono">COMPLETED</span>.</p>
                    </div>
                  </div>

                  <div>
                    <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5">
                      <span className="material-symbols-outlined text-[16px] text-[#b45309]">inventory_2</span> Items
                    </h4>
                    <div className="divide-y divide-[#e7e5e4] border border-[#d6d3d1] rounded-xl overflow-hidden">
                      {detail.items.map((it, idx) => (
                        <div key={idx} className="flex gap-3 p-3 bg-white">
                          <div className="w-12 h-12 rounded-lg bg-[#fafaf9] flex items-center justify-center shrink-0 overflow-hidden">
                            {(it as unknown as { image?: string }).image ? (
                              <Image src={(it as unknown as { image: string }).image} alt={it.name ?? ""} width={48} height={48} unoptimized className="object-cover" />
                            ) : (
                              <span className="material-symbols-outlined text-[#a8a29e]">image</span>
                            )}
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-[13px] text-[#1c1917] truncate">{it.name ?? it.product_id}</p>
                            <p className="text-[11px] text-[#57534e]">Qty {it.quantity} × {formatUSD(it.price)} = {formatUSD(it.subtotal)}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  <AddressCard address={detail.shipping_address as any} />

                   <div>
                     <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5">
                       <span className="material-symbols-outlined text-[16px] text-[#b45309]">timeline</span> Status history
                     </h4>
                     {history.length ? (
                       <div className="space-y-2">
                         {history.map((h) => {
                           const typeCfg = getHistoryTypeConfig(h.statusType ?? "ORDER");
                           return (
                             <div key={h.id} className="flex gap-3 p-2.5 rounded-xl bg-[#fafaf9] border border-[#e7e5e4]">
                               <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[10px] font-semibold ${typeCfg.color}`}>
                                 <span className="material-symbols-outlined text-[12px]">{typeCfg.icon}</span>
                                 {typeCfg.label}
                               </span>
                               <StatusPill status={h.status} />
                               <div className="flex-1 min-w-0">
                                 <p className="text-[12px] text-[#1c1917]">{h.note || "—"}</p>
                                 <p className="text-[11px] text-[#57534e]">{new Date(h.createdAt).toLocaleString()} • by {h.changed_by ?? "system"}</p>
                               </div>
                             </div>
                           );
                         })}
                       </div>
                     ) : (
                       <p className="text-[12px] text-[#57534e] bg-[#fafaf9] p-3 rounded-xl border">No history yet.</p>
                     )}
                   </div>

                   <div className="space-y-3">
                     <div>
                       <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Order status <span className="font-normal normal-case tracking-normal text-[10px] text-[#b45309]">(auto from Delivery/Payment)</span></p>
                       <div className="flex flex-wrap gap-2">
                         {ORDER_STATUSES.map((ns) => {
                           const isCurrent = ns === detail.status;
                           const isValid = isValidOrderTransition(detail.status, ns);
                           return (
                             <button
                               key={ns}
                               onClick={() => isValid && !isCurrent && handleStatusChange(detail.id, ns as OrderStatus)}
                               disabled={(!isValid && !isCurrent) || !!updating}
                               title={!isValid && !isCurrent ? getDisabledReason(detail.status, ns) : undefined}
                               className={`px-3 py-1 rounded-full text-xs ${
                                 isCurrent
                                   ? "bg-blue-600 text-white"
                                   : isValid
                                     ? "bg-green-100 text-green-800 hover:bg-green-200"
                                     : "bg-gray-100 text-gray-400 cursor-not-allowed"
                               }`}
                             >
                               {getOrderStatusConfig(ns).label}
                             </button>
                           );
                         })}
                       </div>
                     </div>
                     <div className="grid grid-cols-2 gap-2">
                       <div>
                         <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Payment</p>
                         <select
                           value={(detail.payment_status ?? (detail as unknown as { paymentStatus: string }).paymentStatus) as string}
                           onChange={(e) => handlePaymentChange(detail.id, e.target.value as PaymentStatus)}
                           disabled={!!updating}
                           className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-lg px-2 py-2 text-[12px] font-medium focus:border-[#b45309] outline-none disabled:opacity-50"
                         >
                           {PAYMENT_STATUSES.map((ps) => {
                             const cur = (detail.payment_status ?? (detail as unknown as { paymentStatus: string }).paymentStatus ?? "UNPAID") as string;
                             const isCurrent = ps === cur;
                             const isValid = isValidPaymentTransition(cur, ps);
                             return (
                               <option
                                 key={ps}
                                 value={ps}
                                 disabled={!isValid && !isCurrent}
                                 title={!isValid && !isCurrent ? getDisabledReason(cur, ps) : undefined}
                               >
                                 {getPaymentStatusConfig(ps).label}
                                 {isCurrent ? " (current)" : !isValid ? " (disabled)" : ""}
                               </option>
                             );
                           })}
                         </select>
                       </div>
                       <div>
                         <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Delivery</p>
                         <select
                           value={(detail.delivery_status ?? (detail as unknown as { deliveryStatus: string }).deliveryStatus) as string}
                           onChange={(e) => handleDeliveryChange(detail.id, e.target.value as DeliveryStatus)}
                           disabled={!!updating}
                           className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-lg px-2 py-2 text-[12px] font-medium focus:border-[#b45309] outline-none disabled:opacity-50"
                         >
                           {DELIVERY_STATUSES.map((ds) => {
                             const cur = (detail.delivery_status ?? (detail as unknown as { deliveryStatus: string }).deliveryStatus ?? "PLACED") as string;
                             const isCurrent = ds === cur;
                             const isValid = isValidDeliveryTransition(cur, ds);
                             return (
                               <option
                                 key={ds}
                                 value={ds}
                                 disabled={!isValid && !isCurrent}
                                 title={!isValid && !isCurrent ? getDisabledReason(cur, ds) : undefined}
                               >
                                 {getDeliveryStatusConfig(ds).label}
                                 {isCurrent ? " (current)" : !isValid ? " (disabled)" : ""}
                               </option>
                             );
                           })}
                         </select>
                       </div>
                     </div>
                   </div>
                </>
              ) : (
                <p className="text-[13px] text-[#57534e]">Order not found.</p>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="bg-[#fef3c7]/20 border border-[#b45309]/20 rounded-xl p-4 flex items-start gap-2">
        <span className="material-symbols-outlined text-[#b45309] text-[20px]">info</span>
        <div className="text-[12px] text-[#57534e] leading-relaxed">
          <p><span className="font-semibold text-[#1c1917]">Lifecycle:</span> <span className="font-mono">PROCESSING → CONFIRMED → SHIPPED → DELIVERED → COMPLETED</span> (or <span className="font-mono">CANCELED</span>) • <span className="font-mono">Delivery: PLACED→SHIPPING→ARRIVED→COLLECTED→RETURNING→RETURNED</span> auto-syncs Order: <span className="font-mono">SHIPPING→SHIPPED</span>, <span className="font-mono">COLLECTED→COMPLETED</span>. Deletion requires verification.</p>
        </div>
      </div>

      {deleteTarget && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => !deleting && setDeleteTarget(null)} />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden border border-[#d6d3d1]">
            <div className="px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between bg-[#fee2e2]/10">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[#b91c1c]">warning</span>
                <h3 className="font-bold text-[15px] text-[#b91c1c]">Delete Order — Verify</h3>
              </div>
              <button onClick={() => !deleting && setDeleteTarget(null)} className="p-1.5 rounded-full hover:bg-[#fafaf9]">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex-1 overflow-auto p-6 space-y-4">
              {verifyLoading ? (
                <div className="py-8 flex flex-col items-center gap-2">
                  <span className="material-symbols-outlined animate-spin text-[#b45309] text-[24px]">progress_activity</span>
                  <span className="text-[12px] text-[#57534e]">Verifying with backend — GET /orders/{deleteTarget.id}/verify-delete …</span>
                </div>
              ) : verifyData ? (
                <>
                  <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 flex gap-2">
                    <span className="material-symbols-outlined text-amber-700 shrink-0">verified_user</span>
                    <div className="text-[11px] leading-relaxed text-amber-800">
                      <p className="font-semibold">Backend verification: order exists ✓</p>
                      <p>Admin has permission (ROLE_ADMIN) ✓ — this is your frontend confirmation step. Backend will re-verify existence, admin role, and stock before actual DELETE.</p>
                    </div>
                  </div>

                  <div className="bg-[#fafaf9] rounded-xl border border-[#d6d3d1] p-4 space-y-2">
                    <h4 className="font-semibold text-[13px] text-[#1c1917]">Order to delete</h4>
                    <div className="grid grid-cols-2 gap-2 text-[12px]">
                      <div><span className="text-[#57534e]">Order #</span><p className="font-mono font-semibold text-[#1c1917]">{verifyData.orderNumber ?? verifyData.order_number ?? deleteTarget.orderNumber ?? deleteTarget.id}</p></div>
                      <div><span className="text-[#57534e]">ID</span><p className="font-mono text-[11px] text-[#1c1917]">{verifyData.id ?? deleteTarget.id}</p></div>
                      <div><span className="text-[#57534e]">Customer</span><p className="font-medium text-[#1c1917] truncate">{verifyData.userEmail ?? verifyData.user_email ?? (deleteTarget as any).userEmail ?? "—"}</p></div>
                      <div><span className="text-[#57534e]">Total</span><p className="font-bold text-[#b45309]">{formatNPR(verifyData.totalAmount ?? verifyData.total_amount ?? deleteTarget.totalAmount ?? 0)}</p></div>
                      <div><span className="text-[#57534e]">Status</span><p><span className="inline-flex px-1.5 py-0.5 rounded-full text-[10px] font-semibold border bg-white">{verifyData.status ?? deleteTarget.status}</span></p></div>
                      <div><span className="text-[#57534e]">Payment</span><p className="text-[11px] font-medium">{verifyData.paymentStatus ?? verifyData.payment_status ?? (deleteTarget as any).paymentStatus ?? "—"}</p></div>
                      <div><span className="text-[#57534e]">Delivery</span><p className="text-[11px] font-medium">{verifyData.deliveryStatus ?? verifyData.delivery_status ?? (deleteTarget as any).deliveryStatus ?? "—"}</p></div>
                      <div><span className="text-[#57534e]">Items</span><p className="font-medium">{verifyData.items?.length ?? deleteTarget.items.length} items</p></div>
                      <div><span className="text-[#57534e]">Created</span><p className="text-[11px] text-[#57534e]">{new Date(verifyData.createdAt ?? verifyData.created_at ?? deleteTarget.createdAt).toLocaleString()}</p></div>
                      <div><span className="text-[#57534e]">Tracking</span><p className="font-mono text-[11px]">{verifyData.trackingNumber ?? verifyData.tracking_number ?? "—"}</p></div>
                    </div>
                    {verifyData.items && verifyData.items.length > 0 && (
                      <div className="pt-2 border-t border-[#e7e5e4]">
                        <p className="text-[11px] font-semibold text-[#57534e] mb-1">Items preview</p>
                        <div className="space-y-1 max-h-24 overflow-auto">
                          {verifyData.items.slice(0,3).map((it:any, idx:number) => (
                            <p key={idx} className="text-[11px] text-[#1c1917] truncate">{it.productName ?? it.name ?? it.productId} × {it.quantity} = {formatNPR(it.price * it.quantity)}</p>
                          ))}
                          {verifyData.items.length > 3 && <p className="text-[10px] text-[#57534e]">+{verifyData.items.length-3} more</p>}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="bg-[#fee2e2]/20 border border-error/30 rounded-xl p-3 space-y-3">
                    <p className="font-semibold text-[12px] text-[#b91c1c] flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">delete_forever</span> Permanent deletion — cannot be undone</p>
                    <p className="text-[11px] text-[#57534e]">This will delete the order, its items, payments, and history (cascade). {(() => { const isTerm = ["CANCELED","COMPLETED","DELIVERED"].includes(verifyData.status ?? deleteTarget.status) || verifyData.deliveryStatus === "RETURNED"; return isTerm ? "Order is terminal — safe to delete." : "Order is active — stock will be restored, backend requires ?force=true." })()}</p>

                    <label className="flex items-start gap-2 cursor-pointer">
                      <input type="checkbox" checked={confirmChecked} onChange={(e) => setConfirmChecked(e.target.checked)} className="mt-0.5 w-4 h-4" />
                      <span className="text-[12px] text-[#1c1917]">I understand this will <span className="font-bold text-[#b91c1c]">permanently delete</span> order <span className="font-mono font-semibold">{verifyData.orderNumber ?? deleteTarget.id.slice(0,8)}</span> and it cannot be recovered.</span>
                    </label>

                    {(() => {
                      const isTerm = ["CANCELED","COMPLETED","DELIVERED"].includes(verifyData.status ?? deleteTarget.status) || verifyData.deliveryStatus === "RETURNED";
                      return !isTerm ? (
                        <label className="flex items-start gap-2 cursor-pointer bg-amber-50 border border-amber-200 rounded-lg p-2">
                          <input type="checkbox" checked={forceChecked} onChange={(e) => setForceChecked(e.target.checked)} className="mt-0.5 w-4 h-4" />
                          <span className="text-[12px] text-amber-800">Force delete active order — I confirm deletion of non-terminal order <span className="font-mono">{verifyData.status}</span> (required for backend ?force=true).</span>
                        </label>
                      ) : null;
                    })()}

                    <div>
                      <label className="text-[11px] font-semibold text-[#1c1917]">Type order number to confirm <span className="font-mono bg-white px-1 py-0.5 rounded border">{verifyData.orderNumber ?? deleteTarget.id.slice(0,8)}</span></label>
                      <input value={confirmInput} onChange={(e) => setConfirmInput(e.target.value)} placeholder={verifyData.orderNumber ?? ""} className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] font-mono focus:border-error outline-none" />
                    </div>

                    {deleteError && (
                      <div className="bg-[#fef2f2] border border-[#fecaca] rounded-lg px-3 py-2 text-[#b91c1c] text-[12px] flex items-start gap-1">
                        <span className="material-symbols-outlined text-[16px] shrink-0">error</span> <span>{deleteError}</span>
                      </div>
                    )}
                  </div>
                </>
              ) : (
                <p className="text-[13px] text-[#57534e]">Verification failed — order may not exist.</p>
              )}
            </div>

            <div className="px-6 py-4 border-t border-[#d6d3d1] flex justify-end gap-2 bg-[#fafaf9]">
              <button onClick={() => setDeleteTarget(null)} disabled={deleting} className="px-4 py-2 rounded-xl border border-[#d6d3d1] bg-white text-[13px] font-semibold disabled:opacity-50">Cancel</button>
              <button onClick={confirmDelete} disabled={deleting || verifyLoading || !verifyData} className="px-5 py-2 rounded-xl bg-error text-on-error text-[13px] font-semibold hover:bg-[#7f1d1d] disabled:opacity-50 flex items-center gap-1.5">
                {deleting && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>} Delete permanently
              </button>
            </div>
          </div>
        </div>
      )}

      {toast && (
        <div className={`fixed bottom-4 right-4 z-50 px-4 py-3 rounded-xl shadow-lg border flex items-center gap-2 max-w-[360px] ${toast.type === "success" ? "bg-[#15803d] text-white border-[#15803d]" : "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]"}`}>
          <span className="material-symbols-outlined text-[18px]">{toast.type === "success" ? "check_circle" : "error"}</span>
          <span className="text-[13px] font-medium flex-1">{toast.msg}</span>
          <button onClick={() => setToast(null)} className="p-1 rounded-full hover:bg-black/10">
            <span className="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}

function AddressCard({ address }: { address: Record<string, unknown> | null | undefined }) {
  if (!address || typeof address !== "object" || Object.keys(address).length === 0) {
    return (
      <div className="p-4 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
        <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5">
          <span className="material-symbols-outlined text-[16px] text-[#b45309]">location_on</span> Shipping address
        </h4>
        <p className="text-[12px] text-[#57534e]">No snapshot (legacy order) — address not captured at checkout.</p>
      </div>
    );
  }
  const a = address as Record<string, string>;
  return (
    <div className="p-4 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
      <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5">
        <span className="material-symbols-outlined text-[16px] text-[#b45309]">location_on</span> Shipping address (snapshot)
      </h4>
      <div className="bg-white rounded-lg border p-3 space-y-1 text-[13px]">
        {a.label && <p className="font-semibold text-[#1c1917]">{a.label}</p>}
        <p className="text-[#1c1917]">{a.street ?? ""}{a.apt ? `, ${a.apt}` : ""}</p>
        <p className="text-[#57534e]">{[a.city, a.state, a.postalCode ?? a.postal_code, a.country].filter(Boolean).join(", ")}</p>
        {(a as any).phone && <p className="text-[12px] text-[#57534e]">Phone: {(a as any).phone}</p>}
      </div>
    </div>
  );
}

function StatusPill({ status }: { status: string }) {
  const cfg = getOrderStatusConfig(status);
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-[11px] font-semibold ${cfg.color}`}>
      <span className="material-symbols-outlined text-[12px]">{cfg.icon}</span>
      {cfg.label}
    </span>
  );
}

function getHistoryTypeConfig(type: string) {
  switch (type) {
    case "PAYMENT":
      return { label: "Payment", color: "bg-green-100 text-green-800 border-green-200", icon: "payments" };
    case "DELIVERY":
      return { label: "Delivery", color: "bg-purple-100 text-purple-800 border-purple-200", icon: "local_shipping" };
    case "FULFILLMENT":
      return { label: "Fulfillment", color: "bg-yellow-100 text-yellow-800 border-yellow-200", icon: "inventory_2" };
    case "ORDER":
    default:
      return { label: "Order", color: "bg-blue-100 text-blue-800 border-blue-200", icon: "receipt" };
  }
}
