"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { useSearchParams, useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { listProducts } from "@/lib/product";
import { getCategories } from "@/lib/category";
import type { Product, Category } from "@/lib/types";
import { useCart } from "@/context/CartContext";
import { useWishlist } from "@/context/WishlistContext";
import { useAuth } from "@/context/AuthContext";

const formatUSD = formatNPR;
function getMainImg(p: Product) {
  const imgs = p.images ?? [];
  if (imgs.length === 0) return p.image || "";
  const sorted = [...imgs].sort((a, b) => {
    const aP = (a.is_primary ?? a.isPrimary ?? false) ? 1 : 0;
    const bP = (b.is_primary ?? b.isPrimary ?? false) ? 1 : 0;
    if (aP !== bP) return bP - aP;
    return (a.display_order ?? a.displayOrder ?? 0) - (b.display_order ?? b.displayOrder ?? 0);
  });
  return sorted.find((i) => i.is_primary ?? i.isPrimary)?.url || sorted[0]?.url || p.image || "";
}

export default function CatalogPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { addToCart } = useCart();
  const { isWishlisted, toggle } = useWishlist();
  const { isAuthenticated } = useAuth();
  const [wishlistBusy, setWishlistBusy] = useState<string | null>(null);
  const [cartBusy, setCartBusy] = useState<string | null>(null);
  const [cartToast, setCartToast] = useState<string | null>(null);
  const [addedIds, setAddedIds] = useState<Set<string>>(new Set());

  const [products, setProducts] = useState<Product[]>([]);
  const [total, setTotal] = useState(0);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const q = searchParams.get("q") || searchParams.get("search") || "";
  const cat = searchParams.get("category") || searchParams.get("categoryId") || "";
  const sort = (searchParams.get("sort") as "price_asc" | "price_desc" | "rating" | "newest" | null) || "newest";
  const minPrice = searchParams.get("minPrice") ? Number(searchParams.get("minPrice")) : undefined;
  const maxPrice = searchParams.get("maxPrice") ? Number(searchParams.get("maxPrice")) : undefined;
  const minRating = searchParams.get("minRating") ? Number(searchParams.get("minRating")) : undefined;
  const pageParam = searchParams.get("page");
  const page = pageParam ? Math.max(1, Number(pageParam) || 1) : 1;
  const isNewArrival = searchParams.get("isNewArrival") === "true";

  const [searchInput, setSearchInput] = useState(q);

  useEffect(() => {
    setSearchInput(q);
  }, [q]);

  const updateParams = useCallback(
    (patch: Record<string, string | undefined>) => {
      const params = new URLSearchParams(searchParams.toString());
      Object.entries(patch).forEach(([k, v]) => {
        if (v === undefined || v === "") params.delete(k);
        else params.set(k, v);
      });
      // keep page param when explicitly set, otherwise delete for filter change
      if (patch.page === undefined) params.delete("page");
      const qs = params.toString();
      router.replace(qs ? `/catalog?${qs}` : `/catalog`);
    },
    [router, searchParams]
  );

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    listProducts({
      search: q || undefined,
      category_id: cat || undefined,
      minPrice,
      maxPrice,
      minRating,
      isNewArrival,
      sort,
      page,
      limit: 12,
    })
      .then((res) => {
        if (!cancelled) {
          setProducts(res.data);
          setTotal(res.total);
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "Failed to load products");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [q, cat, sort, minPrice, maxPrice, minRating, isNewArrival, page]);

  const totalPages = Math.max(1, Math.ceil(total / 12));

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    updateParams({ q: searchInput || undefined, search: undefined });
  };

  const handleAdd = async (p: Product) => {
    if (cartBusy) return;
    setCartBusy(p.id);
    try {
      await addToCart({
        id: p.id,
        product_id: p.id,
        slug: p.id,
        name: p.name,
        price: p.price,
        image: getMainImg(p),
        qty: 1,
      } as any);
      setCartToast(p.name);
      setAddedIds((s) => new Set(s).add(p.id));
      setTimeout(() => setCartToast(null), 2500);
      setTimeout(() => setAddedIds((s) => { const n = new Set(s); n.delete(p.id); return n; }), 2000);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401 || status === 403) {
        router.push(`/login?next=${encodeURIComponent("/catalog")}`);
      } else {
        setCartToast(`Failed to add ${p.name}`);
        setTimeout(() => setCartToast(null), 2500);
      }
      console.error("[catalog] addToCart failed", err);
    } finally {
      setCartBusy(null);
    }
  };

  const handleWishlist = async (p: Product) => {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(`/product/${p.id}`)}`);
      return;
    }
    if (wishlistBusy) return;
    setWishlistBusy(p.id);
    try {
      await toggle(p.id);
    } catch (err: unknown) {
      if ((err as { status?: number })?.status === 401) {
        router.push(`/login?next=${encodeURIComponent(`/product/${p.id}`)}`);
      }
    } finally {
      setWishlistBusy(null);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <div className={`fixed bottom-6 right-6 z-50 transition-all duration-300 ${cartToast ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0 pointer-events-none"}`}>
        <div className="bg-[#0c0a09] text-white rounded-xl px-4 py-3 shadow-xl flex items-center gap-3 text-[13px] font-medium max-w-[320px]">
          <span className="material-symbols-outlined text-[18px] text-white">check_circle</span>
          <span className="line-clamp-1 flex-1">{cartToast?.startsWith("Failed") ? cartToast : `Added ${cartToast} to cart`}</span>
          <Link href="/cart" className="ml-2 underline decoration-white/40 hover:decoration-white shrink-0">View</Link>
        </div>
      </div>
      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl">
        <div className="flex flex-col lg:flex-row gap-6">
          <aside className="w-full lg:w-72 shrink-0">
            <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-5 sticky top-24 space-y-5">
              <div>
                <h3 className="font-semibold text-[14px] text-[#1c1917] mb-3 flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px] text-[#b45309]">tune</span> Filters
                </h3>
                <button
                  onClick={() => router.push("/catalog")}
                  className="text-[12px] text-[#b45309] hover:underline font-medium"
                >
                  Clear all filters
                </button>
              </div>

              <form onSubmit={handleSearch} className="relative">
                <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
                <input
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Search products..."
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl pl-10 pr-4 py-2.5 text-[13px] focus:border-[#b45309] outline-none"
                />
              </form>

              <div>
                <label className="block text-[12px] font-semibold text-[#1c1917] mb-2">Category</label>
                <select
                  value={cat}
                  onChange={(e) => updateParams({ category: e.target.value || undefined, categoryId: undefined })}
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] outline-none focus:border-[#b45309]"
                >
                  <option value="">All categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-semibold text-[#1c1917] mb-1">Min price</label>
                  <input
                    type="number"
                    placeholder="0"
                    value={searchParams.get("minPrice") ?? ""}
                    onChange={(e) => updateParams({ minPrice: e.target.value || undefined })}
                    className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2 text-[13px] outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-[#1c1917] mb-1">Max price</label>
                  <input
                    type="number"
                    placeholder="9999"
                    value={searchParams.get("maxPrice") ?? ""}
                    onChange={(e) => updateParams({ maxPrice: e.target.value || undefined })}
                    className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2 text-[13px] outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[12px] font-semibold text-[#1c1917] mb-2">Minimum rating</label>
                <div className="flex gap-1.5">
                  {[0, 3, 4, 4.5].map((r) => (
                    <button
                      key={r}
                      onClick={() => updateParams({ minRating: r ? String(r) : undefined })}
                      className={`flex-1 py-2 rounded-xl text-[12px] font-semibold border ${minRating === r ? "bg-[#b45309] text-white border-[#b45309]" : !minRating && r === 0 ? "bg-[#b45309] text-white border-[#b45309]" : "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1] hover:bg-[#fafaf9]"}`}
                    >
                      {r === 0 ? "Any" : `${r}+ ★`}
                    </button>
                  ))}
                </div>
              </div>

              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isNewArrival}
                  onChange={(e) => updateParams({ isNewArrival: e.target.checked ? "true" : undefined })}
                  className="w-4 h-4 rounded text-[#b45309]"
                />
                <span className="text-[13px] text-[#1c1917]">New arrivals only</span>
              </label>

              <div>
                <label className="block text-[12px] font-semibold text-[#1c1917] mb-2">Sort by</label>
                <select
                  value={sort}
                  onChange={(e) => updateParams({ sort: e.target.value })}
                  className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] outline-none"
                >
                  <option value="newest">Newest</option>
                  <option value="price_asc">Price: Low to High</option>
                  <option value="price_desc">Price: High to Low</option>
                  <option value="rating">Highest Rated</option>
                </select>
              </div>

              <p className="text-[11px] text-[#57534e] leading-relaxed bg-[#fafaf9] rounded-lg p-3">
                Showing {products.length} of {total} products • Page {page}/{totalPages}
              </p>
            </div>
          </aside>

          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between mb-4">
              <h1 className="font-bold text-[22px] text-[#1c1917] tracking-tight">Product Catalog</h1>
              <span className="text-[12px] text-[#57534e] bg-[#fafaf9] px-3 py-1.5 rounded-full">
                {total} {total === 1 ? "result" : "results"}
              </span>
            </div>

            {loading ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                {[...Array(6)].map((_, i) => (
                  <div key={i} className="bg-white rounded-2xl border border-[#d6d3d1] shadow-sm overflow-hidden flex flex-col animate-pulse">
                    <div className="h-56 bg-[#fafaf9]"></div>
                    <div className="p-5 flex flex-col gap-3">
                      <div className="h-4 bg-[#fafaf9] rounded w-3/4"></div>
                      <div className="h-3 bg-[#fafaf9] rounded w-full"></div>
                      <div className="h-3 bg-[#fafaf9] rounded w-5/6"></div>
                      <div className="mt-4 flex justify-between items-center">
                        <div className="h-5 bg-[#fafaf9] rounded w-1/3"></div>
                        <div className="h-8 bg-[#fafaf9] rounded-xl w-24"></div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : error ? (
              <div className="bg-[#fee2e2]/20 border border-[#fecaca] rounded-xl p-6 flex items-center gap-3 text-[#b91c1c]">
                <span className="material-symbols-outlined">error</span>
                <p className="text-[13px]">{error}</p>
              </div>
            ) : products.length === 0 ? (
              <div className="bg-white border border-[#d6d3d1] rounded-xl p-10 text-center">
                <span className="material-symbols-outlined text-[#a8a29e] text-[56px]">search_off</span>
                <h3 className="font-semibold text-[#1c1917] mt-3">No products found</h3>
                <p className="text-[13px] text-[#57534e] mt-1">Try adjusting search, category or price filters.</p>
                <button onClick={() => router.push("/catalog")} className="mt-4 px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold">
                  Clear filters
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
                  {products.map((p) => {
                    const img = getMainImg(p);
                    const wishlisted = isWishlisted(p.id);
                    const busy = wishlistBusy === p.id;
                    return (
                      <div key={p.id} className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden hover:shadow-md hover:-translate-y-1 transition-all flex flex-col relative">
                        <Link href={`/product/${p.id}`} className="h-56 relative bg-[#fafaf9] flex items-center justify-center overflow-hidden">
                          {img ? (
                            <Image src={img} alt={p.name} fill unoptimized className="object-cover" sizes="300px" onError={(e) => {(e.target as HTMLImageElement).style.display = "none";}} />
                          ) : (
                            <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">image</span>
                          )}
                          {p.is_new_arrival && <span className="absolute top-2 left-2 bg-[#b45309] text-white text-[10px] font-bold px-2 py-1 rounded-full">NEW</span>}
                          {p.images && p.images.length > 1 && <span className="absolute bottom-2 right-2 bg-black/60 text-white text-[10px] px-1.5 py-0.5 rounded-full">+{p.images.length - 1}</span>}
                        </Link>
                        <button
                          onClick={() => handleWishlist(p)}
                          aria-label={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
                          disabled={busy}
                          className={`absolute top-2 right-2 w-8 h-8 rounded-full flex items-center justify-center backdrop-blur border transition-all shadow-sm disabled:opacity-50 ${wishlisted ? "bg-[#fee2e2] border-error text-[#b91c1c]" : "bg-white/90 border-white/60 text-[#a8a29e] hover:text-[#b45309] hover:bg-white"}`}
                        >
                          {busy ? <span className="material-symbols-outlined text-[16px] animate-spin">progress_activity</span> : <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: `'FILL' ${wishlisted ? 1 : 0}` }}>favorite</span>}
                        </button>
                        <div className="p-4 flex flex-col flex-1 gap-2">
                          <Link href={`/product/${p.id}`} className="font-semibold text-[13px] leading-tight text-[#1c1917] hover:text-[#b45309] line-clamp-2">
                            {p.name}
                          </Link>
                          <p className="text-[12px] text-[#57534e] line-clamp-2 flex-1">{p.description}</p>
                          <div className="flex items-center gap-1.5">
                            <div className="flex text-[#b45309]">
                              {[...Array(5)].map((_, i) => (
                                <span key={i} className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: `'FILL' ${i < Math.round(p.rating) ? 1 : 0}` }}>
                                  star
                                </span>
                              ))}
                            </div>
                            <span className="text-[11px] text-[#57534e]">({p.review_count ?? 0})</span>
                            <span className="ml-auto text-[11px] font-mono text-[#57534e]">Stock: {p.stock_quantity ?? 0}</span>
                          </div>
                          <div className="flex items-center justify-between pt-2 border-t border-[#e7e5e4]">
                            <span className="font-bold text-[16px] text-[#b45309]">{formatUSD(p.price)}</span>
                            <button
                              onClick={() => handleAdd(p)}
                              disabled={cartBusy === p.id}
                              className={`px-3 py-2 rounded-xl text-[12px] font-semibold flex items-center gap-1 transition-all disabled:opacity-50 disabled:cursor-not-allowed ${addedIds.has(p.id) ? "bg-[#15803d] text-white" : "bg-[#b45309] text-white hover:bg-[#92400e]"}`}
                            >
                              {cartBusy === p.id ? <span className="material-symbols-outlined text-[14px] animate-spin">progress_activity</span> : addedIds.has(p.id) ? <span className="material-symbols-outlined text-[14px]">check</span> : <span className="material-symbols-outlined text-[14px]">add_shopping_cart</span>} {cartBusy === p.id ? "Adding…" : addedIds.has(p.id) ? "Added ✓" : "Add"}
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div className="flex items-center justify-between mt-8">
                  <button
                    disabled={page <= 1}
                    onClick={() => updateParams({ page: String(page - 1) })}
                    className="px-4 py-2 rounded-xl border border-[#d6d3d1] text-[13px] font-semibold disabled:opacity-40 hover:bg-[#fafaf9]"
                  >
                    ← Previous
                  </button>
                  <span className="text-[12px] text-[#57534e]">
                    Page {page} of {totalPages} • {total} total
                  </span>
                  <button
                    disabled={page >= totalPages}
                    onClick={() => updateParams({ page: String(page + 1) })}
                    className="px-4 py-2 rounded-xl border border-[#d6d3d1] text-[13px] font-semibold disabled:opacity-40 hover:bg-[#fafaf9]"
                  >
                    Next →
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
