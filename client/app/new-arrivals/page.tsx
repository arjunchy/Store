"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { listProducts } from "@/lib/product";
import type { Product } from "@/lib/types";
import { useWishlist } from "@/context/WishlistContext";
import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";

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

const formatUSD = formatNPR;
export default function NewArrivalsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const { isWishlisted, toggle } = useWishlist();
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const [wishlistBusy, setWishlistBusy] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    listProducts({ isNewArrival: true, limit: 50, sort: "newest" })
      .then((res) => setProducts(res.data))
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, []);

  const handleWishlist = async (p: Product) => {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent("/new-arrivals")}`);
      return;
    }
    if (wishlistBusy) return;
    setWishlistBusy(p.id);
    try {
      await toggle(p.id);
    } finally {
      setWishlistBusy(null);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl">
        <div className="mb-6">
          <h1 className="font-bold text-[26px] text-[#1c1917] tracking-tight">New Arrivals</h1>
          <p className="text-[13px] text-[#57534e] mt-1">Latest 4–5 fresh drops – directly from backend `isNewArrival=true`.</p>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="bg-white rounded-xl h-64 animate-pulse" />
            ))}
          </div>
        ) : error ? (
          <div className="bg-[#fee2e2]/20 border border-[#fecaca] rounded-xl p-6 text-[#b91c1c] text-[13px]">{error}</div>
        ) : products.length === 0 ? (
          <div className="bg-white border border-[#d6d3d1] rounded-xl p-10 text-center">
            <p className="text-[#57534e] text-[13px]">No new arrivals right now.</p>
            <Link href="/catalog" className="mt-3 inline-block px-4 py-2 bg-[#b45309] text-white rounded-xl text-[13px]">Browse Catalog</Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {products.map((p) => {
              const img = getMainImg(p);
              const wishlisted = isWishlisted(p.id);
              const busy = wishlistBusy === p.id;
              return (
                <div key={p.id} className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden hover:shadow-md transition-all flex flex-col relative">
                  <Link href={`/product/${p.id}`} className="h-48 relative bg-[#fafaf9] flex items-center justify-center overflow-hidden">
                    {img ? <Image src={img} alt={p.name} fill unoptimized className="object-cover" sizes="300px" /> : <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">image</span>}
                    <span className="absolute top-2 left-2 bg-[#b45309] text-white text-[10px] font-bold px-2 py-1 rounded-full">NEW</span>
                  </Link>
                  <button
                    onClick={() => handleWishlist(p)}
                    disabled={busy}
                    className={`absolute top-2 right-2 w-8 h-8 rounded-full flex items-center justify-center backdrop-blur border shadow-sm ${wishlisted ? "bg-[#fee2e2] border-error text-[#b91c1c]" : "bg-white/90 border-white/60 text-[#a8a29e]"}`}
                  >
                    {busy ? <span className="material-symbols-outlined text-[16px] animate-spin">progress_activity</span> : <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: `'FILL' ${wishlisted ? 1 : 0}` }}>favorite</span>}
                  </button>
                  <div className="p-4 flex flex-col flex-1 gap-2">
                    <Link href={`/product/${p.id}`} className="font-semibold text-[13px] leading-tight line-clamp-2 hover:text-[#b45309]">{p.name}</Link>
                    <p className="text-[12px] text-[#57534e] line-clamp-2 flex-1">{p.description}</p>
                    <div className="flex items-center justify-between pt-2 border-t border-[#e7e5e4]">
                      <span className="font-bold text-[16px]">{formatUSD(p.price)}</span>
                      <Link href={`/product/${p.id}`} className="px-3 py-1.5 bg-[#b45309] text-white rounded-full text-[12px] font-semibold">View</Link>
                    </div>
                  </div>
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
