"use client";

import { useEffect, useState, useCallback } from "react";
import { getAllReviewsAdmin } from "@/lib/admin";
import { deleteReview, getReviewsByProduct } from "@/lib/review";
import { getProducts } from "@/lib/product";
import type { Product, Review } from "@/lib/types";

export default function AdminReviewsPage() {
  const [mode, setMode] = useState<"global" | "byProduct">("global");
  const [reviews, setReviews] = useState<Review[]>([]);
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [minRating, setMinRating] = useState<number | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [error, setError] = useState("");

  const [products, setProducts] = useState<Product[]>([]);
  const [expanded, setExpanded] = useState<string | null>(null);

  const loadGlobal = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const res = await getAllReviewsAdmin(page, PAGE_SIZE, minRating);
      const mapped: Review[] = res.content.map((r: any) => ({
        id: r.id,
        userId: r.userId ?? r.user_id ?? r.user?.userId ?? "",
        productId: r.productId ?? r.product_id ?? r.product?.id ?? "",
        rating: r.rating ?? 0,
        comment: r.comment ?? "",
        is_verified_purchase: r.isVerifiedPurchase ?? r.is_verified_purchase ?? r.verified ?? false,
        isVerifiedPurchase: r.isVerifiedPurchase ?? r.is_verified_purchase ?? false,
        createdAt: r.createdAt ?? r.created_at ?? new Date().toISOString(),
      }));
      setReviews(mapped);
      setTotalElements(res.totalElements);
      setTotalPages(Math.ceil(res.totalElements / PAGE_SIZE));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load reviews");
    } finally {
      setLoading(false);
    }
  }, [page, minRating]);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const p = await getProducts().catch(() => [] as Product[]);
      setProducts(p);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (mode === "global") loadGlobal();
    else loadProducts();
  }, [mode, loadGlobal, loadProducts]);

  useEffect(() => { setPage(0); }, [minRating, mode]);

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this review? This action cannot be undone (ADMIN moderation).")) return;
    setDeleting(id);
    try {
      await deleteReview(id);
      if (mode === "global") await loadGlobal();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Delete failed");
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-bold text-[24px] text-[#1c1917] tracking-tight flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">rate_review</span>
            Reviews Moderation
            {mode === "global" && totalElements > 0 && <span className="text-[11px] font-semibold bg-[#fafaf9] border border-[#d6d3d1] px-2 py-1 rounded-full text-[#57534e]">{totalElements} total</span>}
          </h1>
          <p className="text-[13px] text-[#57534e]">Global moderation via <span className="font-mono text-[#b45309]">GET /api/reviews/all</span> (ADMIN) • By-product lazy view.</p>
        </div>
        <div className="flex gap-1 bg-[#fafaf9] rounded-xl p-1 border border-[#d6d3d1]">
          <button onClick={() => setMode("global")} className={`px-3 py-1.5 rounded-lg text-[12px] font-semibold ${mode === "global" ? "bg-[#b45309] text-white" : "text-[#57534e] hover:text-[#1c1917]"}`}>Global</button>
          <button onClick={() => setMode("byProduct")} className={`px-3 py-1.5 rounded-lg text-[12px] font-semibold ${mode === "byProduct" ? "bg-[#b45309] text-white" : "text-[#57534e] hover:text-[#1c1917]"}`}>By Product</button>
        </div>
      </div>

      {mode === "global" && (
        <div className="bg-white rounded-xl border border-[#d6d3d1] p-3 flex items-center gap-2 overflow-x-auto">
          <span className="text-[12px] font-semibold text-[#57534e]">Filter:</span>
          {[
            { label: "All", value: undefined },
            { label: "5★", value: 5 },
            { label: "≥4", value: 4 },
            { label: "≥3", value: 3 },
          ].map((f) => (
            <button key={String(f.value)} onClick={() => setMinRating(f.value)} className={`px-3 py-1.5 rounded-full text-[12px] font-semibold border ${minRating === f.value ? "bg-[#b45309] text-white border-[#b45309]" : "bg-[#fafaf9] border-[#d6d3d1] text-[#57534e]"}`}>{f.label}</button>
          ))}
        </div>
      )}

      {error && (
        <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 text-[#b91c1c] text-[13px] flex items-center gap-2">
          <span className="material-symbols-outlined">error</span> {error}
        </div>
      )}

      {mode === "global" ? (
        <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center py-16"><span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span></div>
          ) : reviews.length === 0 ? (
            <div className="py-16 text-center">
              <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">reviews</span>
              <p className="text-[13px] text-[#57534e] mt-2">No reviews match filter.</p>
            </div>
          ) : (
            <>
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead className="bg-[#b45309] text-[11px] font-semibold tracking-widest uppercase text-[#fafaf9]">
                    <tr>
                      <th className="px-4 py-3">Rating</th>
                      <th className="px-4 py-3">Comment</th>
                      <th className="px-4 py-3">Verified</th>
                      <th className="px-4 py-3">Product / User</th>
                      <th className="px-4 py-3">Date</th>
                      <th className="px-4 py-3 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#e7e5e4]">
                    {reviews.map((r) => (
                      <tr key={r.id} className="hover:bg-[#fafaf9]">
                        <td className="px-4 py-3">
                          <span className="flex text-[#b45309] text-[14px]">
                            {[...Array(5)].map((_, i) => (
                              <span key={i} className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: `'FILL' ${i < r.rating ? 1 : 0}` }}>star</span>
                            ))}
                          </span>
                          <span className="text-[11px] font-semibold">{r.rating}/5</span>
                        </td>
                        <td className="px-4 py-3 text-[13px] text-[#1c1917] max-w-[360px] truncate">{r.comment || "— no comment —"}</td>
                        <td className="px-4 py-3">{r.is_verified_purchase || r.isVerifiedPurchase ? <span className="text-[10px] bg-[#15803d]/15 text-[#15803d] px-1.5 py-0.5 rounded-full font-medium">Verified</span> : <span className="text-[11px] text-[#57534e]">—</span>}</td>
                        <td className="px-4 py-3 text-[11px] font-mono text-[#57534e]">
                          <div>prod {r.productId.slice(0, 8)}</div>
                          <div>user {r.userId.slice(0, 8)}</div>
                          <div className="text-[10px]">{r.id.slice(0, 12)}</div>
                        </td>
                        <td className="px-4 py-3 text-[12px] text-[#57534e]">{new Date(r.createdAt).toLocaleDateString()}</td>
                        <td className="px-4 py-3 text-right">
                          <button onClick={() => handleDelete(r.id)} disabled={deleting === r.id} className="p-1.5 rounded-lg hover:bg-[#fef2f2] text-[#57534e] hover:text-[#b91c1c] disabled:opacity-50">
                            <span className={`material-symbols-outlined text-[18px] ${deleting === r.id ? "animate-spin" : ""}`}>{deleting === r.id ? "progress_activity" : "delete"}</span>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {totalPages > 1 && (
                <div className="flex items-center justify-between px-4 py-3 border-t border-[#e7e5e4] bg-[#fafaf9]">
                  <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page <= 0} className="px-3 py-1.5 rounded-lg border bg-white text-[12px] font-semibold disabled:opacity-40">← Prev</button>
                  <span className="text-[12px] text-[#57534e]">Page {page + 1} of {totalPages} • {totalElements} reviews</span>
                  <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page + 1 >= totalPages} className="px-3 py-1.5 rounded-lg border bg-white text-[12px] font-semibold disabled:opacity-40">Next →</button>
                </div>
              )}
            </>
          )}
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center py-16"><span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span></div>
          ) : products.length === 0 ? (
            <div className="py-16 text-center">
              <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">reviews</span>
              <p className="text-[13px] text-[#57534e] mt-2">No products to show reviews for.</p>
            </div>
          ) : (
            <div className="divide-y divide-[#e7e5e4]">
              {products.slice(0, 20).map((p) => (
                <ProductReviewRow key={p.id} product={p} expanded={expanded === p.id} onToggle={() => setExpanded(expanded === p.id ? null : p.id)} />
              ))}
            </div>
          )}
        </div>
      )}

      <div className="bg-[#ffedd5]/20 border border-[#ffedd5] rounded-xl p-4 flex items-start gap-2">
        <span className="material-symbols-outlined text-[#b45309]">info</span>
        <p className="text-[12px] text-[#57534e] leading-relaxed">
          Reviews API: <span className="font-mono text-[#b45309]">GET /api/reviews/all?minRating&page&size</span> (ADMIN paginated) • <span className="font-mono bg-white px-1 py-0.5 rounded border">GET /api/reviews/product/{"{productId}"}</span> lazy per product • DELETE admin.
        </p>
      </div>
    </div>
  );
}

function ProductReviewRow({ product, expanded, onToggle }: { product: Product; expanded: boolean; onToggle: () => void }) {
  const [reviews, setReviews] = useState<Review[]>([]);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  const load = async () => {
    const r = await getReviewsByProduct(product.id).catch(() => [] as Review[]);
    setReviews(r);
    setLoaded(true);
  };

  useEffect(() => {
    if (expanded && !loaded) load();
  }, [expanded, loaded]);

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this review? This action cannot be undone (ADMIN moderation).")) return;
    setDeleting(id);
    try {
      await deleteReview(id);
      await load();
    } catch (e) {
      alert(e instanceof Error ? e.message : "Delete failed");
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="group">
      <div className="px-5 py-4 flex items-center gap-4 hover:bg-[#fafaf9] transition-colors">
        <div className="w-10 h-10 rounded-lg bg-[#fafaf9] overflow-hidden shrink-0 flex items-center justify-center">
          {product.image ? <img src={product.image} alt={product.name} className="w-full h-full object-cover" /> : <span className="material-symbols-outlined text-[#a8a29e]">image</span>}
        </div>
        <div className="flex-1 min-w-0">
          <div className="font-medium text-[13px] text-[#1c1917] truncate">{product.name}</div>
          <div className="text-[11px] text-[#57534e] font-mono truncate">{product.id.slice(0, 12)}</div>
        </div>
        <button onClick={onToggle} className={`p-1.5 rounded-lg border text-[12px] font-medium flex items-center gap-1 transition-colors ${expanded ? "bg-[#b45309] text-white border-[#b45309]" : "bg-[#fafaf9] border-[#d6d3d1] text-[#57534e] hover:text-[#b45309]"}`}>
          <span className="material-symbols-outlined text-[16px]">{expanded ? "expand_less" : "expand_more"}</span>
          <span className="hidden sm:inline">{expanded ? "Hide" : "Manage"}</span>
        </button>
      </div>
      {expanded && (
        <div className="px-5 pb-4 bg-[#fafaf9] border-t border-[#d6d3d1]/20">
          {!loaded ? (
            <div className="py-6 flex justify-center"><span className="material-symbols-outlined animate-spin text-[#b45309]">progress_activity</span></div>
          ) : reviews.length === 0 ? (
            <p className="py-6 text-center text-[12px] text-[#57534e]">No reviews yet for this product.</p>
          ) : (
            <div className="space-y-2 pt-3">
              {reviews.map((r) => (
                <div key={r.id} className="bg-white rounded-xl border border-[#d6d3d1]/40 p-3 flex gap-3">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="flex text-[#b45309] text-[14px]">
                        {[...Array(5)].map((_, i) => (
                          <span key={i} className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: `'FILL' ${i < r.rating ? 1 : 0}` }}>star</span>
                        ))}
                      </span>
                      <span className="text-[11px] font-semibold text-[#1c1917]">{r.rating}/5</span>
                      {r.is_verified_purchase && <span className="text-[10px] bg-[#15803d]/15 text-[#15803d] px-1.5 py-0.5 rounded-full font-medium">Verified</span>}
                      <span className="text-[11px] text-[#57534e] ml-auto">{new Date(r.createdAt).toLocaleDateString()}</span>
                    </div>
                    <p className="text-[13px] text-[#1c1917] mt-1 line-clamp-2">{r.comment || "— no comment —"}</p>
                  </div>
                  <button onClick={() => handleDelete(r.id)} disabled={deleting === r.id} className="self-start p-1.5 rounded-lg hover:bg-[#fef2f2] text-[#57534e] hover:text-[#b91c1c] disabled:opacity-50 shrink-0">
                    <span className={`material-symbols-outlined text-[18px] ${deleting === r.id ? "animate-spin" : ""}`}>{deleting === r.id ? "progress_activity" : "delete"}</span>
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
