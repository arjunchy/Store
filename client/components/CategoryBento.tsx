"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { getCategories } from "@/lib/category";
import type { Category } from "@/lib/types";

export default function CategoryBento() {
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
  }, []);

  const nameFallbackMap: Record<string, string> = {
    pants: "https://images.unsplash.com/photo-1542272604-787c3835535d?q=80&w=600&auto=format&fit=crop",
    shirts: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=600&auto=format&fit=crop",
    shirt: "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?q=80&w=600&auto=format&fit=crop",
    caps: "https://images.unsplash.com/photo-1521369909029-2afed882baee?q=80&w=600&auto=format&fit=crop",
    cap: "https://images.unsplash.com/photo-1521369909029-2afed882baee?q=80&w=600&auto=format&fit=crop",
    bags: "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?q=80&w=600&auto=format&fit=crop",
    bag: "https://images.unsplash.com/photo-1590874103328-eac38a683ce7?q=80&w=600&auto=format&fit=crop",
    belts: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?q=80&w=600&auto=format&fit=crop",
    belt: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?q=80&w=600&auto=format&fit=crop",
    shoes: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=600&auto=format&fit=crop",
    socks: "https://images.unsplash.com/photo-1582966772680-860e372bb558?q=80&w=600&auto=format&fit=crop",
    electronics: "https://images.unsplash.com/photo-1498049794561-7780e7231661?q=80&w=600&auto=format&fit=crop",
    fashion: "https://images.unsplash.com/photo-1483985988355-763728e1935b?q=80&w=600&auto=format&fit=crop",
    home: "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?q=80&w=600&auto=format&fit=crop",
  };

  function resolveCategoryImage(cat: Category): string | null {
    if (cat.image) return cat.image;
    const exact = (nameFallbackMap as Record<string, string>)[cat.id];
    if (exact) return exact;
    const n = cat.name.toLowerCase();
    if (n.includes("pant")) return nameFallbackMap.pants;
    if (n.includes("shirt")) return nameFallbackMap.shirts;
    if (n.includes("cap")) return nameFallbackMap.caps;
    if (n.includes("bag")) return nameFallbackMap.bags;
    if (n.includes("belt")) return nameFallbackMap.belts;
    if (n.includes("shoe")) return nameFallbackMap.shoes;
    if (n.includes("sock")) return nameFallbackMap.socks;
    if (n.includes("electr")) return nameFallbackMap.electronics;
    if (n.includes("fashion") || n.includes("apparel") || n.includes("cloth")) return nameFallbackMap.fashion;
    if (n.includes("home") || n.includes("kitchen") || n.includes("living")) return nameFallbackMap.home;
    return null;
  }

  const displayCategories = (() => {
    const leaves = categories.filter((c) => !!(c.parent_id || c.parentId));
    const source = leaves.length > 0 ? leaves : categories;
    const filtered = source.filter((c) => !["men", "women"].includes(c.name.toLowerCase()));
    const seen = new Map<string, Category>();
    for (const c of filtered) {
      const key = c.name.toLowerCase().trim();
      if (!seen.has(key)) seen.set(key, c);
    }
    if (seen.size === 0) {
      const tops = categories.filter((c) => !c.parent_id && !c.parentId && !["men", "women"].includes(c.name.toLowerCase()));
      for (const c of tops) {
        const key = c.name.toLowerCase().trim();
        if (!seen.has(key)) seen.set(key, c);
      }
    }
    return Array.from(seen.values()).slice(0, 6);
  })();

  if (displayCategories.length === 0) return null;

  return (
    <section id="categories" className="w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl scroll-mt-20">
      <h2 className="font-headline-lg text-headline-lg text-[#1c1917] mb-lg text-left font-semibold tracking-tight">
        Shop by Category
      </h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-gutter">
        {displayCategories.map((cat) => {
          const img = resolveCategoryImage(cat);
          return (
            <Link
              key={cat.id}
              href={`/catalog?category=${cat.id}`}
              className="relative rounded-xl overflow-hidden group cursor-pointer shadow-sm hover:shadow-md transition-shadow min-h-[200px] bg-[#fafaf9] border border-[#d6d3d1] hover:border-[#b45309]/50"
            >
              {img ? (
                <Image
                  src={img}
                  alt={cat.name}
                  fill
                  unoptimized
                  className="object-cover transition-transform duration-500 group-hover:scale-[1.03]"
                  sizes="(max-width: 768px) 100vw, 33vw"
                  onError={(e) => {
                    (e.target as HTMLImageElement).style.display = "none";
                  }}
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-[#fafaf9]">
                  <span className="material-symbols-outlined text-[56px] text-[#b45309] opacity-25">category</span>
                </div>
              )}
              <div className="absolute inset-0 bg-gradient-to-t from-[#0c0a09]/60 to-transparent" />
              <div className="absolute bottom-0 left-0 p-4 w-full">
                <h3 className="font-semibold text-white text-[16px] md:text-[18px]">{cat.name}</h3>
              </div>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
