"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { lookupKhalti } from "@/lib/payment";
import { useCart } from "@/context/CartContext";
import { clearCartBackup } from "@/lib/cart-backup";
import { clearCheckout } from "@/lib/checkout";

function CallbackInner() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { clearCart, refresh } = useCart();
  const [status, setStatus] = useState<"loading" | "success" | "failed" | "pending">("loading");
  const [detail, setDetail] = useState<any>(null);
  const [error, setError] = useState("");
  const [restoring, setRestoring] = useState(false);

  const pidx = searchParams.get("pidx") || "";
  const purchaseOrderId = searchParams.get("purchase_order_id") || searchParams.get("purchase_order_name") || "";
  const txnStatus = searchParams.get("status") || "";
  const amount = searchParams.get("amount") || searchParams.get("total_amount") || "";
  const transactionId = searchParams.get("transaction_id") || searchParams.get("tidx") || "";

  useEffect(() => {
    if (!pidx) {
      setStatus("failed");
      setError("Missing pidx – invalid callback");
      return;
    }
    let cancelled = false;
    async function verify() {
      try {
        const res = await lookupKhalti(pidx);
        if (cancelled) return;
        setDetail(res);
        const s = (res.status || txnStatus || "").toLowerCase();
        if (s === "completed") {
          setStatus("success");
          try {
            await clearCheckout().catch(() => {});
            try { await clearCart(); } catch {}
            clearCartBackup();
            try { sessionStorage.removeItem("khalti_pidx"); } catch {}
            await refresh().catch(() => {});
          } catch {}
        } else if (s === "pending" || s === "initiated") {
          setStatus("pending");
        } else if (s.includes("canceled") || s.includes("cancelled") || s === "expired" || s === "user canceled") {
          setStatus("failed");
        } else {
          if (txnStatus.toLowerCase() === "completed") {
            setStatus("success");
            try {
              await clearCheckout().catch(() => {});
              try { await clearCart(); } catch {}
              clearCartBackup();
              await refresh().catch(() => {});
            } catch {}
          }
          else if (txnStatus.toLowerCase().includes("canceled")) setStatus("failed");
          else setStatus("pending");
        }
      } catch (e: any) {
        if (!cancelled) {
          setError(e?.message || "Verification failed – please check Orders for final status. Use lookup for final truth.");
          setStatus("pending");
          setDetail({ pidx, status: txnStatus || "Unknown", amount, transaction_id: transactionId, purchase_order_id: purchaseOrderId });
        }
      }
    }
    verify();
    return () => { cancelled = true; };
  }, [pidx, txnStatus, amount, transactionId, purchaseOrderId]);

  const handleRestore = async () => {
    setRestoring(true);
    try {
      const orderId = (() => {
        try { return sessionStorage.getItem("khalti_orderId") || localStorage.getItem("apexcommerce_last_order_id")?.replace(/^#/, "") || ""; } catch { return ""; }
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

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full max-w-[720px] mx-auto px-5 py-10">
        <div className="bg-white rounded-2xl border border-[#d6d3d1] shadow-sm p-6 md:p-8 text-center">
          {status === "loading" && (
            <>
              <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
              <h1 className="font-bold text-[20px] text-[#1c1917] mt-3">Verifying Khalti payment…</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Checking <span className="font-mono">{pidx}</span> with Khalti lookup API</p>
              <p className="text-[11px] text-[#a8a29e] mt-2">Do not close this page</p>
            </>
          )}
          {status === "success" && (
            <>
              <div className="w-16 h-16 rounded-full bg-[#15803d] text-white flex items-center justify-center mx-auto"><span className="material-symbols-outlined text-[32px]">check_circle</span></div>
              <h1 className="font-bold text-[22px] text-[#1c1917] mt-4">Payment Completed</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Your Khalti payment was verified. Order <span className="font-mono font-semibold text-[#1c1917]">{purchaseOrderId || detail?.purchase_order_id || ""}</span> is confirmed. Cart cleared.</p>
              <div className="mt-4 bg-[#fafaf9] rounded-xl p-3 text-left text-[12px] border border-[#e7e5e4]">
                <div className="flex justify-between"><span className="text-[#57534e]">pidx</span><span className="font-mono text-[#1c1917]">{detail?.pidx || pidx}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">amount</span><span className="font-semibold">{detail?.total_amount ?? amount} paisa</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">transaction_id</span><span className="font-mono">{detail?.transaction_id || transactionId || "—"}</span></div>
                <div className="flex justify-between"><span className="text-[#57534e]">status</span><span className="text-[#15803d] font-semibold">{detail?.status || "Completed"}</span></div>
              </div>
              <div className="flex gap-2 justify-center mt-6">
                <Link href="/orders" className="px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold hover:bg-[#92400e]">View Orders</Link>
                <Link href="/catalog" className="px-5 py-2.5 border border-[#d6d3d1] rounded-xl text-[13px] font-semibold hover:bg-[#fafaf9]">Continue Shopping</Link>
              </div>
              <p className="text-[11px] text-[#a8a29e] mt-4">Order is now <span className="font-semibold text-[#15803d]">PAID/CONFIRMED</span> – tracked in Orders.</p>
            </>
          )}
          {status === "failed" && (
            <>
              <div className="w-16 h-16 rounded-full bg-[#b91c1c] text-white flex items-center justify-center mx-auto"><span className="material-symbols-outlined text-[32px]">cancel</span></div>
              <h1 className="font-bold text-[20px] text-[#1c1917] mt-4">Payment {detail?.status || txnStatus || "Failed"}</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Khalti reports <span className="font-semibold">{detail?.status || txnStatus}</span> for <span className="font-mono">{pidx}</span>. Your cart is preserved.</p>
              {error && <p className="text-[12px] text-[#b91c1c] mt-2">{error}</p>}
              <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3 mt-4 text-left text-[13px] text-amber-900">
                <p className="font-semibold flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">info</span> Your items are still saved</p>
                <p className="text-[12px] mt-1 text-amber-800">Payment did not complete — you can restore your cart and try again.</p>
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
          {status === "pending" && (
            <>
              <div className="w-16 h-16 rounded-full bg-[#b45309] text-white flex items-center justify-center mx-auto"><span className="material-symbols-outlined text-[28px]">hourglass_top</span></div>
              <h1 className="font-bold text-[20px] text-[#1c1917] mt-4">Payment Pending</h1>
              <p className="text-[13px] text-[#57534e] mt-1">Khalti status <span className="font-semibold">{detail?.status || "Pending"}</span>. Hold – do not provide service yet.</p>
              <p className="text-[11px] text-[#a8a29e] mt-2">Your cart is preserved until payment completes. pidx <span className="font-mono">{pidx}</span></p>
              <div className="flex gap-2 justify-center mt-6 flex-wrap">
                <button onClick={() => window.location.reload()} className="px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold">Re-verify</button>
                <button onClick={handleRestore} disabled={restoring} className="px-5 py-2.5 border border-[#d6d3d1] rounded-xl text-[13px] font-semibold bg-white">Restore Cart</button>
                <Link href="/orders" className="px-5 py-2.5 border border-[#d6d3d1] rounded-xl text-[13px] font-semibold">View Orders</Link>
              </div>
            </>
          )}
        </div>
        <p className="text-[11px] text-[#57534e] mt-4 text-center">Return URL: <span className="font-mono">{typeof window !== "undefined" ? window.location.href : ""}</span><br/>Only <b>Completed</b> is success – always use lookup for final truth.</p>
      </main>
      <Footer />
    </div>
  );
}

export default function PaymentCallbackPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><span className="material-symbols-outlined animate-spin text-[#b45309]">progress_activity</span></div>}>
      <CallbackInner />
    </Suspense>
  );
}
