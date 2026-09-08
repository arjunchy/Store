"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { formatNPR } from "@/lib/format";
import { orderAdminApi } from "@/lib/order-api";
import { getOrderStatusConfig, getPaymentStatusConfig, getDeliveryStatusConfig, ORDER_STATUSES, DELIVERY_STATUSES, PAYMENT_STATUSES, isValidOrderTransition, isValidDeliveryTransition, isValidPaymentTransition, getDisabledReason } from "@/lib/statusConfig";
import type { Order, OrderStatus, DeliveryStatus, PaymentStatus, OrderStatusHistory } from "@/lib/types";

const formatUSD = formatNPR;

type Props = {
  orderId: string | null;
  onClose: () => void;
  onUpdated?: (order: Order) => void;
};

export default function OrderDrawer({ orderId, onClose, onUpdated }: Props) {
  const [order, setOrder] = useState<Order | null>(null);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [fulfillment, setFulfillment] = useState({ trackingNumber: "", carrier: "", estimatedDelivery: "" });
  const [savingFulfillment, setSavingFulfillment] = useState(false);
  const [updating, setUpdating] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [showDelete, setShowDelete] = useState(false);
  const [verifyData, setVerifyData] = useState<any>(null);
  const [verifyLoading, setVerifyLoading] = useState(false);
  const [confirmInput, setConfirmInput] = useState("");
  const [confirmChecked, setConfirmChecked] = useState(false);
  const [forceChecked, setForceChecked] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  useEffect(() => {
    if (!orderId) { setOrder(null); setHistory([]); return; }
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError("");
      try {
        const [o, h] = await Promise.all([
          orderAdminApi.getOrderFull(orderId).catch(() => null),
          orderAdminApi.getHistory(orderId).catch(() => [] as OrderStatusHistory[]),
        ]);
        if (cancelled) return;
        if (o) {
          setOrder(o);
          setFulfillment({
            trackingNumber: (o as any).trackingNumber ?? (o as any).tracking_number ?? "",
            carrier: (o as any).carrier ?? "",
            estimatedDelivery: (o as any).estimatedDelivery ?? (o as any).estimated_delivery ?? "",
          });
        }
        setHistory(h);
      } catch (e: any) {
        if (!cancelled) setError(e?.message || "Failed to load");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [orderId]);

  const handleStatus = async (next: OrderStatus) => {
    if (!order) return;
    setUpdating("status");
    try {
      const updated = await orderAdminApi.updateStatus(order.id, next);
      setOrder(updated);
      onUpdated?.(updated);
      const h = await orderAdminApi.getHistory(order.id).catch(() => history);
      setHistory(h);
    } catch (e: any) {
      alert(e?.message || "Status update failed");
    } finally {
      setUpdating(null);
    }
  };

  const handleDelivery = async (next: DeliveryStatus) => {
    if (!order) return;
    setUpdating("delivery");
    try {
      const updated = await orderAdminApi.updateDelivery(order.id, next);
      setOrder(updated);
      onUpdated?.(updated);
      const h = await orderAdminApi.getHistory(order.id).catch(() => history);
      setHistory(h);
    } catch (e: any) {
      alert(e?.message || "Delivery update failed");
    } finally {
      setUpdating(null);
    }
  };

  const handlePayment = async (next: PaymentStatus) => {
    if (!order) return;
    setUpdating("payment");
    try {
      const updated = await orderAdminApi.updatePayment(order.id, next);
      setOrder(updated);
      onUpdated?.(updated);
      const h = await orderAdminApi.getHistory(order.id).catch(() => history);
      setHistory(h);
    } catch (e: any) {
      alert(e?.message || "Payment update failed");
    } finally {
      setUpdating(null);
    }
  };

  const handleFulfillment = async () => {
    if (!order) return;
    if (!fulfillment.trackingNumber.trim() && !fulfillment.carrier.trim() && !fulfillment.estimatedDelivery) {
      alert("Enter at least one fulfillment field");
      return;
    }
    setSavingFulfillment(true);
    try {
      const updated = await orderAdminApi.updateFulfillment(order.id, {
        trackingNumber: fulfillment.trackingNumber.trim() || undefined,
        carrier: fulfillment.carrier.trim() || undefined,
        estimatedDelivery: fulfillment.estimatedDelivery || undefined,
      });
      setOrder(updated);
      onUpdated?.(updated);
      const h = await orderAdminApi.getHistory(order.id).catch(() => history);
      setHistory(h);
    } catch (e: any) {
      alert(e?.message || "Fulfillment update failed");
    } finally {
      setSavingFulfillment(false);
    }
  };

  const startDelete = async () => {
    if (!order) return;
    setShowDelete(true);
    setVerifyData(null);
    setConfirmInput("");
    setConfirmChecked(false);
    setForceChecked(false);
    setDeleteError("");
    setVerifyLoading(true);
    try {
      const v = await orderAdminApi.verifyDelete(order.id).catch(() => null);
      if (v) setVerifyData(v);
      else setVerifyData({ id: order.id, orderNumber: (order as any).orderNumber ?? order.id, status: order.status, deliveryStatus: (order as any).deliveryStatus, paymentStatus: (order as any).paymentStatus, totalAmount: (order as any).totalAmount, userEmail: (order as any).userEmail, items: order.items, createdAt: order.createdAt });
      const isTerm = ["CANCELED","COMPLETED","DELIVERED"].includes(order.status) || (order as any).deliveryStatus === "RETURNED";
      if (!isTerm) setDeleteError("Order is not terminal — check Force delete to proceed.");
    } catch (e: any) {
      setDeleteError(e?.message || "Verification failed");
    } finally {
      setVerifyLoading(false);
    }
  };

  const confirmDelete = async () => {
    if (!order || !verifyData) return;
    const expected = verifyData.orderNumber ?? verifyData.order_number ?? order.id;
    if (confirmInput.trim() !== expected) { setDeleteError(`Type "${expected}" to confirm`); return; }
    if (!confirmChecked) { setDeleteError("Check confirmation"); return; }
    const isTerm = ["CANCELED","COMPLETED","DELIVERED"].includes(order.status) || (order as any).deliveryStatus === "RETURNED";
    if (!isTerm && !forceChecked) { setDeleteError("Force delete required for non-terminal"); return; }
    setDeleting(true);
    setDeleteError("");
    try {
      const needsForce = !["CANCELED","COMPLETED","DELIVERED"].includes(order.status) && (order as any).deliveryStatus !== "RETURNED";
      await orderAdminApi.deleteOrder(order.id, needsForce ? forceChecked : false);
      setShowDelete(false);
      onUpdated?.(order);
      onClose();
    } catch (e: any) {
      setDeleteError(e?.data?.message ?? e?.message ?? "Delete failed");
    } finally {
      setDeleting(false);
    }
  };

  if (!orderId) return null;

  const orderCfg = order ? getOrderStatusConfig(order.status) : null;
  const payCfg = order ? getPaymentStatusConfig(order.payment_status ?? order.paymentStatus ?? "") : null;
  const delCfg = order ? getDeliveryStatusConfig(order.delivery_status ?? order.deliveryStatus ?? "") : null;
  const addr: any = order?.shipping_address ?? order?.shippingAddress ?? {};

  const deliveryCur = (order?.delivery_status ?? (order as any)?.deliveryStatus ?? "PLACED") as string;
  const paymentCur = (order?.payment_status ?? (order as any)?.paymentStatus ?? "UNPAID") as string;

  return (
    <div className="fixed inset-0 z-50 flex">
      <div className="flex-1 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="w-full max-w-xl bg-white shadow-xl border-l border-[#d6d3d1] flex flex-col overflow-hidden">
        <div className="px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between shrink-0">
          <div>
            <h3 className="font-bold text-[16px] text-[#1c1917] flex items-center gap-2">
              <span className="material-symbols-outlined text-[#b45309]">receipt</span> Order {order?.order_number || order?.orderNumber || orderId.slice(0, 8)}
            </h3>
            {order && <p className="text-[11px] font-mono text-[#57534e]">{order.id}</p>}
            {order && ((order as any).userEmail || (order as any).userName) && (
              <p className="text-[11px] text-[#b45309] flex items-center gap-1 mt-1">
                <span className="material-symbols-outlined text-[12px]">mail</span> {(order as any).userEmail ?? (order as any).userName}
              </p>
            )}
            {order && <p className="text-[11px] text-[#57534e]">Date: {new Date(order.createdAt).toLocaleString()}</p>}
          </div>
          <div className="flex items-center gap-1">
            <button onClick={startDelete} disabled={!order || loading} className="p-1.5 rounded-lg bg-[#fee2e2]/20 text-[#b91c1c] border border-[#fecaca] hover:bg-[#fef2f2] disabled:opacity-50" title="Delete order — verify first">
              <span className="material-symbols-outlined text-[18px]">delete_forever</span>
            </button>
            <button onClick={onClose} className="p-1.5 rounded-full hover:bg-[#fafaf9]">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-auto p-6 space-y-5">
          {loading ? (
            <div className="py-12 flex flex-col items-center gap-2">
              <span className="material-symbols-outlined animate-spin text-[#b45309] text-[24px]">progress_activity</span>
              <span className="text-[12px] text-[#57534e]">Loading…</span>
            </div>
          ) : error ? (
            <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 text-[#b91c1c] text-[13px]">{error}</div>
          ) : order ? (
            <>
              <div className="grid grid-cols-3 gap-3">
                <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] text-center">
                  <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Order</p>
                  <span className={`mt-1 inline-flex px-2 py-1 rounded-full border text-[11px] font-semibold ${orderCfg?.color}`}>{order.status}</span>
                </div>
                <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] text-center">
                  <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Payment</p>
                  <span className={`mt-1 inline-flex px-2 py-1 rounded-full border text-[11px] font-semibold ${payCfg?.color}`}>{order.payment_status ?? order.paymentStatus}</span>
                </div>
                <div className="p-3 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] text-center">
                  <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Delivery</p>
                  <span className={`mt-1 inline-flex px-2 py-1 rounded-full border text-[11px] font-semibold ${delCfg?.color}`}>{order.delivery_status ?? order.deliveryStatus}</span>
                </div>
              </div>

              <div className="p-4 rounded-xl bg-[#fafaf9] border border-[#d6d3d1] space-y-3">
                <h4 className="font-semibold text-[13px] text-[#1c1917] flex items-center gap-1.5"><span className="material-symbols-outlined text-[16px] text-[#b45309]">local_shipping</span> Fulfillment</h4>
                <div className="grid grid-cols-1 gap-3">
                  <div>
                    <label className="text-[11px] font-semibold text-[#57534e]">Tracking Number</label>
                    <input value={fulfillment.trackingNumber} onChange={(e) => setFulfillment((p) => ({ ...p, trackingNumber: e.target.value }))} placeholder="e.g. SHIP123456" className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-[11px] font-semibold text-[#57534e]">Carrier</label>
                      <input value={fulfillment.carrier} onChange={(e) => setFulfillment((p) => ({ ...p, carrier: e.target.value }))} placeholder="UPS" className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                    </div>
                    <div>
                      <label className="text-[11px] font-semibold text-[#57534e]">Estimated Delivery</label>
                      <input type="date" value={fulfillment.estimatedDelivery} onChange={(e) => setFulfillment((p) => ({ ...p, estimatedDelivery: e.target.value }))} className="w-full mt-1 bg-white border border-[#d6d3d1] rounded-lg px-3 py-2 text-[13px] focus:border-[#b45309] outline-none" />
                    </div>
                  </div>
                </div>
                <button onClick={handleFulfillment} disabled={savingFulfillment} className="w-full py-2 rounded-xl bg-[#b45309] text-white font-semibold text-[13px] hover:bg-[#92400e] disabled:opacity-50 flex items-center justify-center gap-1">
                  {savingFulfillment && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>} Save Fulfillment
                </button>
                <p className="text-[10px] text-[#57534e]">PATCH /orders/{"{id}"}/fulfillment — tracking via SHIPPING recommended.</p>
              </div>

              <div>
                <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5"><span className="material-symbols-outlined text-[16px] text-[#b45309]">inventory_2</span> Items</h4>
                <div className="divide-y divide-[#e7e5e4] border border-[#d6d3d1] rounded-xl overflow-hidden">
                  {order.items.map((it: any, idx: number) => (
                    <div key={idx} className="flex gap-3 p-3 bg-white">
                      <div className="w-12 h-12 rounded-lg bg-[#fafaf9] flex items-center justify-center shrink-0 overflow-hidden">
                        {it.image ? <Image src={it.image} alt={it.name ?? ""} width={48} height={48} unoptimized className="object-cover" /> : <span className="material-symbols-outlined text-[#a8a29e]">image</span>}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-[13px] text-[#1c1917] truncate">{it.name ?? it.product_id}</p>
                        <p className="text-[11px] text-[#57534e]">Qty {it.quantity} × {formatUSD(it.price)} = {formatUSD(it.subtotal)}</p>
                      </div>
                    </div>
                  ))}
                </div>
                <p className="text-right font-bold text-[14px] text-[#b45309] mt-2">Total: {formatUSD(order.total_amount ?? order.totalAmount ?? 0)}</p>
              </div>

              <div className="p-4 rounded-xl bg-[#fafaf9] border border-[#d6d3d1]">
                <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5"><span className="material-symbols-outlined text-[16px] text-[#b45309]">location_on</span> Shipping Address</h4>
                {addr && Object.keys(addr).length ? (
                  <div className="bg-white rounded-lg border p-3 space-y-1 text-[13px]">
                    {addr.label && <p className="font-semibold text-[#1c1917]">{addr.label}</p>}
                    <p className="text-[#1c1917]">{addr.street ?? ""}{addr.apt ? `, ${addr.apt}` : ""}</p>
                    <p className="text-[#57534e]">{[addr.city, addr.state, addr.postalCode ?? addr.postal_code, addr.country].filter(Boolean).join(", ")}</p>
                  </div>
                ) : <p className="text-[12px] text-[#57534e]">No snapshot.</p>}
              </div>

              <div className="space-y-3">
                <div>
                  <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Order Status</p>
                  <div className="flex flex-wrap gap-2">
                    {ORDER_STATUSES.map((ns) => {
                      const isCurrent = ns === order.status;
                      const isValid = isValidOrderTransition(order.status, ns);
                      return (
                        <button
                          key={ns}
                          onClick={() => isValid && !isCurrent && handleStatus(ns as OrderStatus)}
                          disabled={(!isValid && !isCurrent) || !!updating}
                          title={!isValid && !isCurrent ? getDisabledReason(order.status, ns) : undefined}
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
                     <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Delivery</p>
                     <select value={deliveryCur} onChange={(e) => handleDelivery(e.target.value as DeliveryStatus)} disabled={!!updating} className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-lg px-2 py-2 text-[12px] font-medium focus:border-[#b45309] outline-none disabled:opacity-50">
                       {DELIVERY_STATUSES.map((ds) => {
                         const isCurrent = ds === deliveryCur;
                         const isValid = isValidDeliveryTransition(deliveryCur, ds);
                         return (
                           <option
                             key={ds}
                             value={ds}
                             disabled={!isValid && !isCurrent}
                             title={!isValid && !isCurrent ? getDisabledReason(deliveryCur, ds) : undefined}
                           >
                             {getDeliveryStatusConfig(ds).label}
                             {isCurrent ? " (current)" : !isValid ? " (disabled)" : ""}
                           </option>
                         );
                       })}
                     </select>
                     <p className="text-[10px] text-[#57534e] mt-1">
                       {getAutoSyncHint(deliveryCur, paymentCur)}
                     </p>
                   </div>
                   <div>
                     <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase mb-1">Payment</p>
                     <select value={paymentCur} onChange={(e) => handlePayment(e.target.value as PaymentStatus)} disabled={!!updating} className="w-full bg-[#fafaf9] border border-[#d6d3d1] rounded-lg px-2 py-2 text-[12px] font-medium focus:border-[#b45309] outline-none disabled:opacity-50">
                       {PAYMENT_STATUSES.map((ps) => {
                         const isCurrent = ps === paymentCur;
                         const isValid = isValidPaymentTransition(paymentCur, ps);
                         return (
                           <option
                             key={ps}
                             value={ps}
                             disabled={!isValid && !isCurrent}
                             title={!isValid && !isCurrent ? getDisabledReason(paymentCur, ps) : undefined}
                           >
                             {getPaymentStatusConfig(ps).label}
                             {isCurrent ? " (current)" : !isValid ? " (disabled)" : ""}
                           </option>
                         );
                       })}
                     </select>
                   </div>
                 </div>
              </div>

               <div>
                 <h4 className="font-semibold text-[13px] text-[#1c1917] mb-2 flex items-center gap-1.5"><span className="material-symbols-outlined text-[16px] text-[#b45309]">timeline</span> History</h4>
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
                           <div className="flex-1 min-w-0">
                             <p className="text-[12px] text-[#1c1917]">{h.note || "—"}</p>
                             <p className="text-[11px] text-[#57534e]">{new Date(h.createdAt).toLocaleString()} • by {h.changed_by ?? "system"}</p>
                           </div>
                         </div>
                       );
                     })}
                   </div>
                 ) : <p className="text-[12px] text-[#57534e] bg-[#fafaf9] p-3 rounded-xl border">No history yet.</p>}
               </div>

              <div className="pt-4 border-t border-[#e7e5e4]">
                <button onClick={startDelete} className="w-full py-2.5 rounded-xl bg-[#fee2e2]/20 text-[#b91c1c] border border-[#fecaca] font-semibold text-[13px] hover:bg-[#fef2f2] flex items-center justify-center gap-1.5">
                  <span className="material-symbols-outlined text-[18px]">delete_forever</span> Delete Order (Admin)
                </button>
                <p className="text-[10px] text-[#57534e] text-center mt-1">Requires verification — permanent, stock restored if not delivered</p>
              </div>
            </>
          ) : <p className="text-[13px] text-[#57534e]">Order not found.</p>}
        </div>
      </div>

      {showDelete && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => !deleting && setShowDelete(false)} />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-md max-h-[90vh] flex flex-col overflow-hidden border border-[#d6d3d1]">
            <div className="px-6 py-4 border-b flex items-center justify-between bg-[#fee2e2]/10">
              <h3 className="font-bold text-[14px] text-[#b91c1c] flex items-center gap-2"><span className="material-symbols-outlined">warning</span> Verify Delete</h3>
              <button onClick={() => !deleting && setShowDelete(false)} className="p-1.5 rounded-full hover:bg-[#fafaf9]"><span className="material-symbols-outlined">close</span></button>
            </div>
            <div className="flex-1 overflow-auto p-6 space-y-4">
              {verifyLoading ? (
                <div className="py-6 flex flex-col items-center gap-2"><span className="material-symbols-outlined animate-spin text-[#b45309] text-[24px]">progress_activity</span><span className="text-[12px] text-[#57534e]">Verifying backend…</span></div>
              ) : verifyData ? (
                <>
                  <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 flex gap-2">
                    <span className="material-symbols-outlined text-amber-700 shrink-0">verified_user</span>
                    <p className="text-[11px] text-amber-800">Backend verified: order exists ✓. Frontend confirmation required.</p>
                  </div>
                  <div className="bg-[#fafaf9] rounded-xl border p-4 space-y-1 text-[12px]">
                    <p><span className="text-[#57534e]">Order #</span> <span className="font-mono font-semibold">{verifyData.orderNumber ?? verifyData.order_number ?? order?.id.slice(0,8)}</span></p>
                    <p><span className="text-[#57534e]">Customer</span> <span className="font-medium">{verifyData.userEmail ?? order?.userEmail ?? "—"}</span></p>
                    <p><span className="text-[#57534e]">Total</span> <span className="font-bold text-[#b45309]">{formatUSD(verifyData.totalAmount ?? verifyData.total_amount ?? 0)}</span></p>
                    <p><span className="text-[#57534e]">Status</span> {verifyData.status} • {verifyData.deliveryStatus} • {verifyData.paymentStatus}</p>
                  </div>
                  <div className="bg-[#fee2e2]/20 border border-error/30 rounded-xl p-3 space-y-3">
                    <label className="flex items-start gap-2 cursor-pointer"><input type="checkbox" checked={confirmChecked} onChange={(e) => setConfirmChecked(e.target.checked)} className="mt-0.5 w-4 h-4" /><span className="text-[12px]">I understand permanent deletion of <span className="font-mono font-semibold">{verifyData.orderNumber}</span></span></label>
                    {!["CANCELED","COMPLETED","DELIVERED"].includes(verifyData.status) && verifyData.deliveryStatus !== "RETURNED" && (
                      <label className="flex items-start gap-2 cursor-pointer bg-amber-50 border border-amber-200 rounded-lg p-2"><input type="checkbox" checked={forceChecked} onChange={(e) => setForceChecked(e.target.checked)} className="mt-0.5 w-4 h-4" /><span className="text-[12px] text-amber-800">Force delete active order ({verifyData.status})</span></label>
                    )}
                    <div>
                      <label className="text-[11px] font-semibold">Type <span className="font-mono bg-white px-1 rounded border">{verifyData.orderNumber}</span> to confirm</label>
                      <input value={confirmInput} onChange={(e) => setConfirmInput(e.target.value)} placeholder={verifyData.orderNumber} className="w-full mt-1 bg-white border rounded-lg px-3 py-2 text-[13px] font-mono focus:border-error outline-none" />
                    </div>
                    {deleteError && <div className="bg-[#fef2f2] border border-[#fecaca] rounded-lg px-3 py-2 text-[#b91c1c] text-[12px]">{deleteError}</div>}
                  </div>
                </>
              ) : <p className="text-[13px] text-[#57534e]">Verification failed.</p>}
            </div>
            <div className="px-6 py-4 border-t flex justify-end gap-2 bg-[#fafaf9]">
              <button onClick={() => setShowDelete(false)} disabled={deleting} className="px-4 py-2 rounded-xl border bg-white text-[13px] font-semibold">Cancel</button>
              <button onClick={confirmDelete} disabled={deleting || verifyLoading || !verifyData} className="px-5 py-2 rounded-xl bg-error text-on-error text-[13px] font-semibold disabled:opacity-50 flex items-center gap-1.5">{deleting && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>} Delete</button>
            </div>
           </div>
         </div>
       )}
     </div>
   );
}

function getAutoSyncHint(deliveryStatus: string, paymentStatus: string) {
  if (deliveryStatus === "SHIPPING") {
    return "Auto-sync: Order → SHIPPED";
  }
  if (deliveryStatus === "ARRIVED") {
    return "Auto-sync: Order → DELIVERED";
  }
  if (deliveryStatus === "COLLECTED") {
    return "Auto-sync: Order → COMPLETED";
  }
  if (deliveryStatus === "RETURNED" && paymentStatus === "PAID") {
    return "Auto-sync: Payment → REFUNDING";
  }
  return "";
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
