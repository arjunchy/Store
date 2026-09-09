"use client";

import { formatNPR } from "@/lib/format";
import { useEffect, useState, use } from "react";
import Link from "next/link";
import Image from "next/image";
import { useParams } from "next/navigation";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { ProtectedRoute } from "@/components/AuthGuard";
import { orderApi } from "@/lib/order-api";
import { getOrderStatusConfig, getPaymentStatusConfig, getDeliveryStatusConfig } from "@/lib/statusConfig";
import type { Order, OrderStatusHistory } from "@/lib/types";

const formatUSD = formatNPR;

export default function OrderDetailsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return (
    <ProtectedRoute>
      <DetailsInner id={id} />
    </ProtectedRoute>
  );
}

function DetailsInner({ id }: { id: string }) {
  const [order, setOrder] = useState<any>(null);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [cancelling, setCancelling] = useState(false);
  const [paying, setPaying] = useState(false);
  const [payError, setPayError] = useState("");

  const load = async () => {
    if (!id) return;
    setLoading(true);
    setError("");
    try {
      const o = await orderApi.getOrderById(id);
      if (!o) { setError("Order not found"); return; }
      setOrder(o as any);
      const h = await orderApi.getHistory(id).catch(() => [] as OrderStatusHistory[]);
      setHistory(h);
    } catch (e: any) {
      setError(e?.message || "Failed to load order");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [id]);

  const handleCancel = async () => {
    if (!order || !confirm("Cancel this order? This cannot be undone.")) return;
    setCancelling(true);
    try {
      const updated = await orderApi.cancelOrder(order.id);
      // Optimistic update – keep interactivity, no full reload
      setOrder((prev: any) => prev ? { ...prev, status: updated.status, payment_status: (updated as any).payment_status ?? updated.paymentStatus, paymentStatus: (updated as any).paymentStatus, delivery_status: (updated as any).delivery_status, deliveryStatus: (updated as any).deliveryStatus } : prev);
      // Refresh history silently without toggling main loading
      orderApi.getHistory(order.id).then(setHistory).catch(()=>{});
    } catch (e: any) {
      alert(e?.message || "Cancel failed");
    } finally {
      setCancelling(false);
    }
  };

  const canCancel = order && (order.status === "PROCESSING" || order.status === "CONFIRMED");
  const payStatus = String(order?.payment_status ?? order?.paymentStatus ?? "UNPAID").toUpperCase();
  const payMethod = String(order?.paymentMethod ?? "esewa").toLowerCase();
  const needsPayment = order && (payStatus === "UNPAID" || payStatus === "EXPIRED") && order.status !== "CANCELED";

  const handlePayNow = async () => {
    if (!order || paying) return;
    setPaying(true);
    setPayError("");
    try {
      if (payMethod === "khalti") {
        const { initiateKhaltiPayment } = await import("@/lib/payment");
        const khalti = await initiateKhaltiPayment(order.id);
        const url = (khalti as any).paymentUrl || (khalti as any).payment_url;
        if (!url) throw new Error("Khalti did not return payment_url");
        try { sessionStorage.setItem("khalti_pidx", khalti.pidx); sessionStorage.setItem("khalti_orderId", order.id); } catch {}
        window.location.href = url;
        return;
      }
      const { initiateEsewaPayment, submitEsewaForm } = await import("@/lib/payment");
      const esewa = await initiateEsewaPayment(order.id);
      if (!esewa?.gatewayUrl || !esewa?.signature) throw new Error("eSewa did not return payment fields");
      try {
        sessionStorage.setItem("esewa_uuid", esewa.transactionUuid);
        sessionStorage.setItem("esewa_orderId", order.id);
      } catch {}
      submitEsewaForm(esewa);
    } catch (e: any) {
      setPayError(e?.data?.message || e?.message || "Failed to start payment – try again.");
      setPaying(false);
    }
  };
  const orderCfg = order ? getOrderStatusConfig(order.status) : null;
  const payCfg = order ? getPaymentStatusConfig(order.payment_status ?? order.paymentStatus ?? "") : null;
  const delCfg = order ? getDeliveryStatusConfig(order.delivery_status ?? order.deliveryStatus ?? "") : null;
  const addr = order?.shipping_address ?? order?.shippingAddress ?? {};

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
        <Navbar />
        <main className="flex-grow flex items-center justify-center">
          <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
        </main>
        <Footer />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
        <Navbar />
        <main className="flex-grow w-full max-w-[900px] mx-auto px-margin-mobile md:px-margin-desktop py-xl text-center">
          <span className="material-symbols-outlined text-[#b91c1c] text-[48px]">error</span>
          <h1 className="font-bold text-[20px] text-[#1c1917] mt-3">{error || "Order not found"}</h1>
          <Link href="/orders" className="mt-4 inline-flex bg-[#b45309] text-white px-6 py-3 rounded-xl text-[13px] font-semibold">Back to Orders</Link>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full max-w-[1100px] mx-auto px-margin-mobile md:px-margin-desktop py-xl">
        <div className="flex items-center gap-2 text-[12px] text-[#57534e] mb-4">
          <Link href="/orders" className="hover:text-[#b45309] flex items-center gap-1">
            <span className="material-symbols-outlined text-[16px]">arrow_back</span> Orders
          </Link>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="text-[#1c1917] font-semibold">{order.order_number || order.orderNumber || order.id.slice(0, 8)}</span>
        </div>

        <h1 className="font-bold text-[22px] text-[#1c1917] mb-1">Order #{order.order_number || order.orderNumber || order.id.slice(0, 8)}</h1>
        <p className="text-[12px] text-[#57534e] mb-4">Placed {new Date(order.createdAt).toLocaleString()} • ID: {order.id.slice(0, 12)}</p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <div className="p-4 rounded-xl bg-white border border-[#d6d3d1] shadow-sm text-center">
            <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Order</p>
            <span className={`mt-2 inline-flex items-center gap-1 px-3 py-1 rounded-full border text-[11px] font-semibold ${orderCfg?.color}`}>{order.status}</span>
            <p className="text-[11px] text-[#57534e] mt-1">{orderCfg?.label}</p>
          </div>
          <div className="p-4 rounded-xl bg-white border border-[#d6d3d1] shadow-sm text-center">
            <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Payment</p>
            <span className={`mt-2 inline-flex items-center gap-1 px-3 py-1 rounded-full border text-[11px] font-semibold ${payCfg?.color}`}>{order.payment_status ?? order.paymentStatus}</span>
            <p className="text-[11px] text-[#57534e] mt-1">{payCfg?.label}</p>
          </div>
          <div className="p-4 rounded-xl bg-white border border-[#d6d3d1] shadow-sm text-center">
            <p className="text-[11px] font-semibold tracking-widest text-[#57534e] uppercase">Delivery</p>
            <span className={`mt-2 inline-flex items-center gap-1 px-3 py-1 rounded-full border text-[11px] font-semibold ${delCfg?.color}`}>{order.delivery_status ?? order.deliveryStatus}</span>
            <p className="text-[11px] text-[#57534e] mt-1">{delCfg?.label}</p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
              <div className="px-6 py-4 border-b border-[#d6d3d1] bg-[#fafaf9]-bright">
                <h2 className="font-semibold text-[16px] text-[#1c1917]">Items ({order.items.length})</h2>
              </div>
              <div className="divide-y divide-[#e7e5e4]">
                {order.items.map((it: any, idx: number) => (
                  <div key={idx} className="flex items-center gap-4 p-4">
                    <div className="w-16 h-16 rounded-lg bg-[#fafaf9] overflow-hidden relative shrink-0 flex items-center justify-center">
                      {it.image ? <Image src={it.image} alt={it.name || "item"} fill unoptimized className="object-cover" sizes="64px" onError={(e) => {(e.target as HTMLImageElement).style.display="none";}} /> : <span className="material-symbols-outlined text-[#a8a29e]">image</span>}
                    </div>
                    <div className="flex-1 min-w-0">
                      <Link href={`/product/${it.product_id || it.productId}`} className="font-medium text-[13px] text-[#1c1917] hover:text-[#b45309] hover:underline line-clamp-2">{it.name || it.product_id}</Link>
                      <p className="text-[12px] text-[#57534e]">Qty: {it.quantity} • {formatUSD(it.price)} each</p>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="font-semibold text-[13px] text-[#1c1917]">Qty {it.quantity} × {formatUSD(it.price)}</p>
                      <p className="text-[12px] text-[#57534e]">Subtotal: {formatUSD(it.subtotal)}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="px-6 py-4 bg-[#fafaf9] flex justify-between items-center border-t border-[#d6d3d1]">
                <span className="font-semibold text-[#1c1917]">Total</span>
                <span className="font-bold text-[18px] text-[#b45309]">{formatUSD(order.total_amount ?? order.totalAmount ?? 0)}</span>
              </div>
            </div>

            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-6">
              <h2 className="font-semibold text-[16px] text-[#1c1917] mb-3 flex items-center gap-2"><span className="material-symbols-outlined text-[#b45309]">location_on</span> Shipping Address</h2>
              {addr && Object.keys(addr).length ? (
                <div className="space-y-1 text-[13px]">
                  {addr.label && <p className="font-semibold text-[#1c1917]">{addr.label}</p>}
                  <p className="text-[#1c1917]">{addr.street ?? ""}{addr.apt ? `, ${addr.apt}` : ""}</p>
                  <p className="text-[#57534e]">{[addr.city, addr.state, addr.postalCode ?? addr.postal_code, addr.country].filter(Boolean).join(", ")}</p>
                </div>
              ) : <p className="text-[13px] text-[#57534e]">No address snapshot.</p>}
            </div>

            {(order.tracking_number || order.trackingNumber || order.carrier || order.estimated_delivery || order.estimatedDelivery) && (
              <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-6">
                <h2 className="font-semibold text-[16px] text-[#1c1917] mb-3 flex items-center gap-2"><span className="material-symbols-outlined text-[#b45309]">local_shipping</span> Tracking</h2>
                <div className="space-y-1 text-[13px]">
                  <p><span className="text-[#57534e]">Tracking:</span> <span className="font-mono font-semibold text-[#1c1917]">{order.tracking_number ?? order.trackingNumber ?? "—"}</span></p>
                  <p><span className="text-[#57534e]">Carrier:</span> <span className="font-semibold text-[#1c1917]">{order.carrier ?? "—"}</span></p>
                  <p><span className="text-[#57534e]">ETA:</span> <span className="font-semibold text-[#1c1917]">{order.estimated_delivery ?? order.estimatedDelivery ? new Date(order.estimated_delivery ?? order.estimatedDelivery).toLocaleDateString() : "—"}</span></p>
                </div>
              </div>
            )}

            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-6">
              <h2 className="font-semibold text-[16px] text-[#1c1917] mb-4 flex items-center gap-2"><span className="material-symbols-outlined text-[#b45309]">history</span> Status History</h2>
              {history.length === 0 ? <p className="text-[13px] text-[#57534e]">No history yet.</p> : (
                <ol className="relative border-l border-[#d6d3d1] ml-3 space-y-4">
                  {history.map((h) => (
                    <li key={h.id} className="ml-6">
                      <span className="absolute flex items-center justify-center w-6 h-6 bg-[#fef3c7] rounded-full -left-3 ring-4 ring-surface-container-lowest"><span className="material-symbols-outlined text-[#b45309] text-[14px]">circle</span></span>
                      <div className="flex flex-wrap items-center gap-2">
                        <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold border bg-[#fafaf9]">{h.status}</span>
                        <span className="text-[11px] text-[#57534e]">{new Date(h.createdAt).toLocaleString()}</span>
                      </div>
                      {h.note && <p className="text-[12px] text-[#57534e] mt-1">{h.note}</p>}
                      {h.changed_by && <p className="text-[11px] font-mono text-[#57534e]">by {h.changed_by.slice(0, 12)}</p>}
                    </li>
                  ))}
                </ol>
              )}
            </div>

            <div className="flex gap-2 flex-wrap">
              {needsPayment && (
                <button onClick={handlePayNow} disabled={paying} className="px-6 py-3 rounded-xl bg-[#1AA16B] text-white text-[13px] font-semibold hover:brightness-95 disabled:opacity-50 flex items-center gap-1.5">
                  {paying ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span> : <span className="material-symbols-outlined text-[16px]">payments</span>}
                  {paying ? "Redirecting…" : `Pay Now with ${payMethod === "khalti" ? "Khalti" : "eSewa"}`}
                </button>
              )}
              {canCancel && (
                <button onClick={handleCancel} disabled={cancelling} className="px-6 py-3 rounded-xl bg-[#fee2e2] text-[#b91c1c] border border-[#fecaca] text-[13px] font-semibold hover:bg-[#fee2e2]/80 disabled:opacity-50 flex items-center gap-1.5">
                  {cancelling && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>} Cancel Order
                </button>
              )}
              <Link href="/orders" className="px-6 py-3 rounded-xl border border-[#d6d3d1] text-[13px] font-semibold hover:bg-[#fafaf9]">Back to Orders</Link>
              {canCancel && <span className="text-[11px] text-[#57534e] self-center">Cancel allowed while PROCESSING or CONFIRMED</span>}
            </div>
            {payError && <div className="mt-3 bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 text-[#b91c1c] text-[12px]">{payError}</div>}
            {needsPayment && (
              <div className="mt-3 bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 text-[12px] text-amber-900">
                <p className="font-semibold">Payment pending ({payStatus})</p>
                <p className="mt-0.5">Pay with {payMethod === "khalti" ? "Khalti" : "eSewa"} – order auto-marks <span className="font-semibold">PAID</span> after gateway success. No manual step.</p>
              </div>
            )}
          </div>

          <div className="w-full lg:w-80 shrink-0 space-y-4">
            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5">
              <h3 className="font-semibold text-[14px] text-[#1c1917] mb-3">Summary</h3>
              <div className="space-y-2 text-[13px]">
                <div className="flex justify-between"><span className="text-[#57534e]">Items</span><span className="font-medium">{order.items.length}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">Total</span><span className="font-bold text-[#b45309]">{formatUSD(order.total_amount ?? order.totalAmount ?? 0)}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">Status</span><span className="font-semibold">{order.status}</span></div>
              </div>
              <Link href="/catalog" className="mt-4 w-full py-2.5 rounded-xl border border-[#d6d3d1] text-center block text-[13px] font-semibold hover:bg-[#fafaf9]">Continue Shopping</Link>
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
