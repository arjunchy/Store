"use client";

import { useEffect, useState } from "react";
import { formatNPR } from "@/lib/format";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { getProducts } from "@/lib/product";
import type { Product } from "@/lib/types";
import { useWishlist } from "@/context/WishlistContext";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "@/context/CartContext";

function getMainImage(product: Product): string {
  const imgs = product.images ?? [];
  if (imgs.length === 0) return product.image || "";
  const sorted = [...imgs].sort((a, b) => {
    const aP = (a.is_primary ?? a.isPrimary ?? false) ? 1 : 0;
    const bP = (b.is_primary ?? b.isPrimary ?? false) ? 1 : 0;
    if (aP !== bP) return bP - aP;
    return (a.display_order ?? a.displayOrder ?? 0) - (b.display_order ?? b.displayOrder ?? 0);
  });
  return sorted.find((i) => i.is_primary ?? i.isPrimary)?.url || sorted[0]?.url || product.image || "";
}

function ProductCard({ product }: { product: Product }) {
  const mainImg = getMainImage(product);
  const { isWishlisted, toggle } = useWishlist();
  const { isAuthenticated } = useAuth();
  const { addToCart } = useCart();
  const router = useRouter();
  const [toggling, setToggling] = useState(false);
  const [cartBusy, setCartBusy] = useState(false);
  const [justAdded, setJustAdded] = useState(false);
  const wishlisted = isWishlisted(product.id);

  const handleWishlist = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      return;
    }
    if (toggling) return;
    setToggling(true);
    try {
      await toggle(product.id);
    } catch (err: unknown) {
      if ((err as { status?: number })?.status === 401) {
        router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      }
    } finally {
      setToggling(false);
    }
  };

  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (cartBusy) return;
    setCartBusy(true);
    try {
      await addToCart({
        id: product.id,
        product_id: product.id,
        slug: product.id,
        name: product.name,
        price: product.price,
        image: mainImg,
        qty: 1,
      } as any);
      setJustAdded(true);
      setTimeout(() => setJustAdded(false), 1500);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401 || status === 403) {
        router.push(`/login?next=${encodeURIComponent("/catalog")}`);
      }
    } finally {
      setCartBusy(false);
    }
  };

  return (
    <div className="bg-white rounded-xl overflow-hidden shadow-[0px_4px_6px_-1px_rgba(0,0,0,0.08),0px_2px_4px_-1px_rgba(0,0,0,0.04)] hover:shadow-lg hover:-translate-y-[3px] transition-all group flex flex-col relative border border-[#d6d3d1] hover:border-[#b45309]/30">
      <Link href={`/product/${product.id}`} className="h-48 relative overflow-hidden bg-[#fafaf9] flex items-center justify-center block">
        {mainImg ? (
          <Image
            src={mainImg}
            alt={product.name}
            fill
            unoptimized
            className="object-cover mix-blend-multiply opacity-90 group-hover:scale-105 transition-transform duration-300"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 25vw"
            onError={(e) => {
              (e.target as HTMLImageElement).style.display = "none";
            }}
          />
        ) : (
          <div className="w-full h-full bg-[#e7e5e4] flex flex-col items-center justify-center text-[#a8a29e] gap-1">
            <span className="material-symbols-outlined text-[44px]">image</span>
            <span className="text-[10px] leading-none">No image</span>
          </div>
        )}
        {product.images && product.images.length > 1 && (
          <span className="absolute bottom-2 right-2 bg-black/55 text-white text-[10px] px-1.5 py-0.5 rounded-full font-medium">
            +{product.images.length - 1}
          </span>
        )}
      </Link>

      <button
        onClick={handleWishlist}
        aria-label={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
        disabled={toggling}
        className={`absolute top-2 right-2 w-8 h-8 rounded-full flex items-center justify-center backdrop-blur border transition-all shadow-sm disabled:opacity-50
          ${wishlisted ? "bg-[#b91c1c] border-[#b91c1c] text-white" : "bg-white/90 border-[#d6d3d1] text-[#a8a29e] hover:text-[#b45309] hover:bg-white"}`}
      >
        {toggling ? (
          <span className="material-symbols-outlined text-[16px] animate-spin">progress_activity</span>
        ) : (
          <span className="material-symbols-outlined text-[16px]" style={{ fontVariationSettings: `'FILL' ${wishlisted ? 1 : 0}` }}>
            favorite
          </span>
        )}
      </button>

      <div className="p-4 flex flex-col flex-1 bg-white">
        <div className="flex justify-between items-start mb-1.5 gap-2">
          <Link href={`/product/${product.id}`} className="font-semibold text-[13px] leading-tight text-[#1c1917] line-clamp-2 flex-1 hover:text-[#b45309] transition-colors">
            {product.name}
          </Link>
        </div>

        <p className="font-body-sm text-[12px] text-[#57534e] mb-3 leading-snug line-clamp-2">
          {product.description}
        </p>

        <div className="flex justify-between items-center mt-auto pt-1">
          <span className="font-bold text-[15px] text-[#b45309]">{formatNPR(product.price)}</span>
          <button
            onClick={handleAddToCart}
            disabled={cartBusy}
            aria-label="Add to cart"
            className={`w-7 h-7 rounded-full transition-colors flex items-center justify-center disabled:opacity-50 ${justAdded ? "bg-[#15803d] text-white" : "bg-[#1c1917] group-hover:bg-[#b45309] group-hover:text-white text-white"}`}
          >
            {cartBusy ? (
              <span className="material-symbols-outlined text-[14px] animate-spin">progress_activity</span>
            ) : justAdded ? (
              <span className="material-symbols-outlined text-[14px]">check</span>
            ) : (
              <span className="material-symbols-outlined text-[14px]">add_shopping_cart</span>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function NewArrivals() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getProducts()
      .then((all) => {
        const arrivals = all.filter((p) => p.is_new_arrival || p.isNewArrival);
        setProducts(arrivals.length > 0 ? arrivals.slice(0, 4) : all.slice(0, 4));
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading || products.length === 0) return null;

  return (
    <section id="new-arrivals" className="w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl scroll-mt-20">
      <div className="bg-white rounded-xl p-4 md:p-6 lg:p-7 border border-[#d6d3d1]">
        <div className="flex justify-between items-end mb-6">
          <h2 className="font-headline-lg text-[20px] md:text-headline-lg text-[#1c1917] font-semibold tracking-tight">
            New Arrivals
          </h2>
          <Link href="/new-arrivals" className="font-label-md text-[12px] md:text-label-md text-[#b45309] hover:underline flex items-center gap-1 font-medium">
            View All <span className="material-symbols-outlined text-[14px]">arrow_forward</span>
          </Link>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-gutter">
          {products.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      </div>
    </section>
  );
}
