"use client";

import { formatNPR } from "@/lib/format";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { ProtectedRoute } from "@/components/AuthGuard";
import { useWishlist } from "@/context/WishlistContext";
import { useCart } from "@/context/CartContext";
import { getProductById } from "@/lib/product";
import type { Product } from "@/lib/types";

const formatUSD = formatNPR;
export default function WishlistPage() {
  return (
    <ProtectedRoute>
      <WishlistInner />
    </ProtectedRoute>
  );
}

function WishlistInner() {
  const { items, remove, loading: wishlistLoading } = useWishlist();
  const { addToCart } = useCart();
  const [products, setProducts] = useState<Record<string, Product | null>>({});
  const [loading, setLoading] = useState(true);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [addingId, setAddingId] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (wishlistLoading) return;
    let cancelled = false;
    const loadProducts = async () => {
      if (items.length === 0) {
        if (!cancelled) {
          setProducts({});
          setLoading(false);
        }
        return;
      }
      setLoading(true);
      try {
        const productResults = await Promise.all(items.map((item) => getProductById(item.product_id || item.productId!)));
        if (cancelled) return;
        const productMap: Record<string, Product | null> = {};
        items.forEach((item, index) => {
          const pid = item.product_id || item.productId!;
          productMap[pid] = productResults[index] ?? null;
        });
        setProducts(productMap);
      } catch {
        if (!cancelled) setProducts({});
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    loadProducts();
    return () => {
      cancelled = true;
    };
  }, [items, wishlistLoading]);

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2400);
  };

  const handleRemove = async (productId: string) => {
    if (removingId) return;
    setRemovingId(productId);
    try {
      await remove(productId);
      setProducts((current) => {
        const next = { ...current };
        delete next[productId];
        return next;
      });
      showToast("Removed from wishlist");
    } catch (error) {
      console.error("Failed to remove wishlist item:", error);
      showToast("Failed to remove — try again");
    } finally {
      setRemovingId(null);
    }
  };

  const handleAddToCart = async (productId: string) => {
    const product = products[productId];
    if (!product) return;
    if (addingId) return;
    setAddingId(productId);
    try {
      const img = getPrimaryImage(product);
      await addToCart({
        id: product.id,
        product_id: product.id,
        slug: product.id,
        name: product.name,
        price: product.price,
        image: img,
        qty: 1,
      } as any);
      showToast(`Added ${product.name} to cart`);
    } catch {}
    finally {
      setAddingId(null);
    }
  };

  const isLoading = wishlistLoading || loading;

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />

      <div className={`fixed bottom-6 left-1/2 -translate-x-1/2 z-50 transition-all duration-300 ${toast ? "translate-y-0 opacity-100" : "translate-y-4 opacity-0 pointer-events-none"}`}>
        <div className="bg-[#0c0a09] text-white rounded-full px-5 py-3 shadow-xl flex items-center gap-2 text-[13px] font-medium">
          <span className="material-symbols-outlined text-[18px]">{toast?.includes("Added") ? "check" : toast?.includes("Removed") ? "heart_minus" : "info"}</span>
          {toast}
        </div>
      </div>

      <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1100px] mx-auto py-xl md:py-xxl">
        <div className="mb-lg flex items-start justify-between gap-4">
          <div>
            <h1 className="font-headline-lg text-headline-lg text-[#1c1917] font-semibold tracking-tight">
              Wishlist
            </h1>
            <p className="font-body-md text-body-md text-[#57534e]">
              {isLoading ? "Loading your saved items…" : items.length === 0 ? "Items you've saved for later." : `${items.length} ${items.length === 1 ? "item" : "items"} saved`}
            </p>
          </div>
          {!isLoading && items.length > 0 && (
            <Link href="/catalog" className="hidden sm:inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-[#d6d3d1] text-[13px] font-semibold hover:bg-[#fafaf9]">
              <span className="material-symbols-outlined text-[16px]">explore</span> Browse catalog
            </Link>
          )}
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="bg-white rounded-xl border border-[#d6d3d1] p-4 flex flex-col gap-3 animate-pulse">
                <div className="w-full h-44 bg-[#fafaf9] rounded-lg" />
                <div className="h-4 bg-[#fafaf9] rounded w-3/4" />
                <div className="h-3 bg-[#fafaf9] rounded w-1/3" />
                <div className="flex gap-2 mt-2">
                  <div className="flex-1 h-10 bg-[#fafaf9] rounded-xl" />
                  <div className="w-10 h-10 bg-[#fafaf9] rounded-xl" />
                </div>
              </div>
            ))}
          </div>
        ) : items.length === 0 ? (
          <div className="bg-white rounded-xl border border-[#d6d3d1] p-xl text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px] mb-3">favorite</span>
            <h3 className="font-headline-md text-headline-md text-[#1c1917] font-semibold mb-1">Your wishlist is empty</h3>
            <p className="text-[#57534e] text-[14px] mb-4">Tap the heart on any product to save it here. It syncs across devices.</p>
            <div className="flex items-center justify-center gap-3">
              <Link href="/catalog" className="inline-flex bg-[#b45309] text-white font-label-md text-label-md px-lg py-3 rounded-xl hover:bg-[#92400e] transition-colors">
                Browse Catalog
              </Link>
              <Link href="/" className="inline-flex border border-[#d6d3d1] text-[#1c1917] font-label-md text-label-md px-lg py-3 rounded-xl hover:bg-[#fafaf9] transition-colors">
                Go home
              </Link>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {items.map((item) => {
              const pid = item.product_id || item.productId!;
              const product = products[pid];
              const image = getPrimaryImage(product);
              const isRemoving = removingId === pid;
              const isAdding = addingId === pid;
              return (
                <div key={pid} className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-4 flex flex-col gap-3 hover:shadow-md transition-shadow group">
                  <Link href={`/product/${pid}`} className="w-full h-44 bg-[#fafaf9] rounded-lg overflow-hidden relative flex items-center justify-center">
                    {image ? (
                      <Image src={image} alt={product?.name || "Product"} fill unoptimized className="object-cover group-hover:scale-105 transition-transform duration-300" sizes="340px" onError={(e) => { (e.target as HTMLImageElement).style.display = "none"; }} />
                    ) : (
                      <div className="w-full h-full flex flex-col items-center justify-center gap-1">
                        <span className="material-symbols-outlined text-[#a8a29e] text-[40px]">image</span>
                        <span className="text-[11px] text-[#a8a29e]">No image</span>
                      </div>
                    )}
                    <span className="absolute top-2 right-2 w-7 h-7 rounded-full bg-white/90 backdrop-blur flex items-center justify-center text-[#b91c1c] border border-white/60">
                      <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: "'FILL' 1" }}>favorite</span>
                    </span>
                  </Link>

                  <div className="flex-grow">
                    <Link href={`/product/${pid}`} className="font-semibold text-[14px] text-[#1c1917] hover:text-[#b45309] line-clamp-2">
                      {product?.name || pid}
                    </Link>
                    {product ? (
                      <div className="flex items-center gap-2 mt-1">
                        <p className="text-[13px] font-bold text-[#1c1917]">{formatUSD(product.price)}</p>
                        <span className={`w-1.5 h-1.5 rounded-full ${ (product.stock_quantity ?? product.stockQuantity ?? 0) > 0 ? "bg-[#15803d]" : "bg-error"}`} />
                        <span className="text-[11px] text-[#57534e]">{ (product.stock_quantity ?? product.stockQuantity ?? 0) > 0 ? "In stock" : "Out of stock"}</span>
                      </div>
                    ) : (
                      <p className="text-[11px] text-[#a8a29e] mt-1">Loading…</p>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => handleAddToCart(pid)}
                      disabled={isAdding || !product}
                      className="flex-1 bg-[#b45309] text-white text-center font-label-md text-label-md py-2.5 rounded-xl hover:bg-[#92400e] transition-colors disabled:opacity-50 flex items-center justify-center gap-1"
                    >
                      {isAdding ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span> : <><span className="material-symbols-outlined text-[16px]">add_shopping_cart</span> Add to cart</>}
                    </button>
                    <button
                      type="button"
                      onClick={() => handleRemove(pid)}
                      disabled={isRemoving}
                      aria-label="Remove from wishlist"
                      className="p-2.5 border border-[#d6d3d1] rounded-xl text-[#b91c1c] hover:bg-[#fee2e2]/20 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {isRemoving ? <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span> : <span className="material-symbols-outlined text-[18px]">delete</span>}
                    </button>
                  </div>
                  <Link href={`/product/${pid}`} className="text-center text-[12px] font-medium text-[#b45309] hover:underline">View details</Link>
                </div>
              );
            })}
          </div>
        )}
      </main>

      <Footer />
    </div>
  );
}

function getPrimaryImage(product: Product | null | undefined): string {
  if (!product) return "";
  const images = product.images ?? [];
  if (images.length === 0) return product.image || "";
  const sorted = [...images].sort((a, b) => {
    const aPrimary = Boolean(a.is_primary ?? a.isPrimary ?? false);
    const bPrimary = Boolean(b.is_primary ?? b.isPrimary ?? false);
    if (aPrimary !== bPrimary) return bPrimary ? 1 : -1;
    const aOrder = a.display_order ?? a.displayOrder ?? 0;
    const bOrder = b.display_order ?? b.displayOrder ?? 0;
    return aOrder - bOrder;
  });
  const primary = sorted.find((image) => image.is_primary ?? image.isPrimary);
  return primary?.url || sorted[0]?.url || product.image || "";
}
