"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { verifyEsewaPayment } from "@/lib/payment";
import { formatNPR } from "@/lib/format";
import { useCart } from "@/context/CartContext";
import { clearCartBackup } from "@/lib/cart-backup";
import { clearCheckout } from "@/lib/checkout";

function CallbackInner() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { clearCart, refresh } = useCart();
  const [status, setStatus] = useState<"loading" | "success" | "failed">("loading");
  const [detail, setDetail] = useState<any>(null);
  const [error, setError] = useState("");
  const [restoring, setRestoring] = useState(false);

  const data = searchParams.get("data") || "";

  // StrictMode (dev) double-invokes this effect with the same `data`, which
  // used to fire two verify POSTs that raced on cart-clear (loser got a 409
  // despite PAID). Share one request per `data` so only one POST is sent;
  // both effect invocations await the same promise.
  const verifyKeyRef = useRef<string>("");
  const verifyReqRef = useRef<Promise<any> | null>(null);

  useEffect(() => {
    if (!data) {
      setStatus("failed");
      setError("Missing eSewa response data – payment not verified. Your order stays UNPAID.");
      return;
    }
    if (verifyKeyRef.current !== data) {
      verifyKeyRef.current = data;
      verifyReqRef.current = null;
    }
    let cancelled = false;
    async function verify() {
      try {
        if (!verifyReqRef.current) verifyReqRef.current = verifyEsewaPayment({ data });
        const res = await verifyReqRef.current;
        if (cancelled) return;
        setDetail(res);
        const ps = String(res?.paymentStatus || res?.status || "").toUpperCase();
        const esewaStatus = String(res?.status || "").toUpperCase();
        if (ps === "PAID" || esewaStatus === "COMPLETE") {
          setStatus("success");
          try {
            await clearCheckout().catch(() => {});
            try { await clearCart(); } catch {}
            clearCartBackup();
            try {
              sessionStorage.removeItem("esewa_uuid");
              sessionStorage.removeItem("apexcommerce_pending_order_id");
            } catch {}
            await refresh().catch(() => {});
          } catch {}
        } else {
          setStatus("failed");
          setError(`eSewa reports ${res?.status || "non-COMPLETE"} – order not marked PAID. Cart preserved.`);
        }
      } catch (e: any) {
        if (!cancelled) {
          // Allow a later retry to issue a fresh request instead of reusing the rejected one.
          if (verifyKeyRef.current === data) verifyReqRef.current = null;
          setError(e?.data?.message || e?.message || "eSewa verification failed – order stays UNPAID. Retry from Orders.");
          setStatus("failed");
        }
      }
    }
    verify();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data]);

  const handleRestore = async () => {
    setRestoring(true);
    try {
      const orderId = (() => {
        try { return sessionStorage.getItem("esewa_orderId") || localStorage.getItem("apexcommerce_last_order_id")?.replace(/^#/, "") || ""; } catch { return ""; }
      })();
      const { apiClient } = await import("@/lib/api-client");
      if (orderId) {
        try {
          await apiClient.post(`/cart/restore-from-order/${orderId}`, {}, { auth: true });
          await refresh();
          router.push("/cart");
          return;
        } catch {}
      }
      const raw = localStorage.getItem("apexcommerce_cart_backup");
      if (raw) {
        const backup = JSON.parse(raw);
        if (Array.isArray(backup) && backup.length > 0) {
          const { addToCart } = await import("@/lib/cart");
          for (const it of backup) {
            try {
              await addToCart({ id: it.product_id || it.id, product_id: it.product_id || it.id, name: it.name, price: it.price, image: it.image, qty: it.qty ?? it.quantity ?? 1, quantity: it.qty ?? it.quantity ?? 1 } as any);
            } catch {}
          }
          await refresh();
        }
      }
      router.push("/cart");
    } catch {
      router.push("/cart");
    } finally {
      setRestoring(false);
    }
  };

  const orderId = detail?.orderId || detail?.order_id || "";
  const orderNumber = detail?.orderNumber || detail?.order_number || "";
  const txnCode = detail?.transaction_id || detail?.transaction_code || detail?.transactionCode || "";
  const amountPaidRaw = detail?.amountPaid ?? detail?.total_amount ?? detail?.totalAmount ?? "";
  const amountPaid = amountPaidRaw === "" || amountPaidRaw === null || amountPaidRaw === undefined || !isFinite(Number(amountPaidRaw))
    ? "—"
    : formatNPR(Number(amountPaidRaw));

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full max-w-[720px] mx-auto px-5 py-10">
        <div className="bg-white rounded-2xl border border-[#d6d3d1] shadow-sm p-6 md:p-8 text-center">
          {status === "loading" && (
            <>
              <span className="material-symbols-outlined animate-spin text-[#1AA16B] text-[32px]">progress_activity</span>
              <h1 className="font-bold text-[20px] text-[#1c1917] mt-3">Verifying eSewa payment…</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Confirming with eSewa status API – order auto-marks PAID on COMPLETE</p>
              <p className="text-[11px] text-[#a8a29e] mt-2">Do not close this page</p>
            </>
          )}
          {status === "success" && (
            <>
              <div className="w-16 h-16 rounded-full bg-[#15803d] text-white flex items-center justify-center mx-auto"><span className="material-symbols-outlined text-[32px]">check_circle</span></div>
              <h1 className="font-bold text-[22px] text-[#1c1917] mt-4">Payment Completed</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Your eSewa payment was verified. Order <span className="font-mono font-semibold text-[#1c1917]">{orderNumber || orderId.slice(0, 8)}</span> is confirmed. Cart cleared.</p>
              <div className="mt-4 bg-[#fafaf9] rounded-xl p-3 text-left text-[12px] border border-[#e7e5e4]">
                <div className="flex justify-between"><span className="text-[#57534e]">Transaction ID</span><span className="font-mono text-[#1c1917]">{txnCode || "—"}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">Amount Paid</span><span className="font-semibold text-[#1c1917]">{amountPaid}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">status</span><span className="text-[#15803d] font-semibold">{detail?.status || "COMPLETE"}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">payment</span><span className="text-[#15803d] font-semibold">PAID</span></div>
              </div>
              <div className="flex gap-2 justify-center mt-6">
                <Link href="/orders" className="px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold hover:bg-[#92400e]">View Orders</Link>
                <Link href="/catalog" className="px-5 py-2.5 border border-[#d6d3d1] rounded-xl text-[13px] font-semibold hover:bg-[#fafaf9]">Continue Shopping</Link>
              </div>
              <p className="text-[11px] text-[#a8a29e] mt-4">Order is now <span className="font-semibold text-[#15803d]">PAID/CONFIRMED</span> – tracked in Orders and Admin.</p>
            </>
          )}
          {status === "failed" && (
            <>
              <div className="w-16 h-16 rounded-full bg-[#b91c1c] text-white flex items-center justify-center mx-auto"><span className="material-symbols-outlined text-[32px]">cancel</span></div>
              <h1 className="font-bold text-[20px] text-[#1c1917] mt-4">Payment Not Completed</h1>
              <p className="text-[13px] text-[#57534e] mt-1">eSewa did not report COMPLETE. Your order stays UNPAID and your cart is preserved.</p>
              {error && <p className="text-[12px] text-[#b91c1c] mt-2">{error}</p>}
              <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 mt-4 text-left text-[13px] text-amber-900">
                <p className="font-semibold flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">info</span> Your items are still saved</p>
                <p className="text-[12px] mt-1 text-amber-800">Restore your cart and retry – no manual admin step needed once eSewa succeeds.</p>
              </div>
              <div className="flex gap-2 justify-center mt-6 flex-wrap">
                <button onClick={handleRestore} disabled={restoring} className="px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold hover:bg-[#92400e] disabled:opacity-50 flex items-center gap-1">
                  {restoring ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span> : <span className="material-symbols-outlined text-[16px]">shopping_cart</span>} {restoring ? "Restoring..." : "Restore Cart & Retry"}
                </button>
                <Link href="/cart" className="px-5 py-2.5 bg-[#0c0a09] text-white rounded-xl text-[13px] font-semibold">Go to Cart</Link>
                <Link href="/orders" className="px-5 py-2.5 border border-[#d6d3d1] rounded-xl text-[13px] font-semibold">View Orders</Link>
              </div>
            </>
          )}
        </div>
        <p className="text-[11px] text-[#57534e] mt-4 text-center">Only <b>COMPLETE</b> is success – always verified server-side via eSewa status API.</p>
      </main>
      <Footer />
    </div>
  );
}

export default function EsewaCallbackPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><span className="material-symbols-outlined animate-spin text-[#1AA16B]">progress_activity</span></div>}>
      <CallbackInner />
    </Suspense>
  );
}
