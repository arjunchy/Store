"use client";

import { useState } from "react";
import type { ProductImage } from "@/lib/types";
import { deleteProductImage, reorderImages, setPrimaryImage } from "@/lib/product";

interface ImageGalleryProps {
  productId: string;
  images: ProductImage[];
  onRefresh: () => Promise<void> | void;
  onError?: (msg: string) => void;
}

export default function ImageGallery({ productId, images, onRefresh, onError }: ImageGalleryProps) {
  const [actionId, setActionId] = useState<string | null>(null);
  const [reordering, setReordering] = useState(false);

  const sorted = [...images].sort((a, b) => {
    const aP = a.is_primary ?? a.isPrimary ?? false;
    const bP = b.is_primary ?? b.isPrimary ?? false;
    if (aP !== bP) return (bP ? 1 : 0) - (aP ? 1 : 0);
    return (a.display_order ?? a.displayOrder ?? 0) - (b.display_order ?? b.displayOrder ?? 0);
  });

  const handleDelete = async (imageId: string) => {
    if (!confirm("Delete this image? This will remove the file from storage.")) return;
    setActionId(imageId);
    try {
      await deleteProductImage(productId, imageId);
      await onRefresh();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Delete failed";
      onError?.(msg);
      alert(msg);
    } finally {
      setActionId(null);
    }
  };

  const handleSetPrimary = async (imageId: string) => {
    setActionId(imageId);
    try {
      await setPrimaryImage(productId, imageId);
      await onRefresh();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to set primary";
      onError?.(msg);
      alert(msg);
    } finally {
      setActionId(null);
    }
  };

  const handleReorder = async (index: number, direction: -1 | 1) => {
    const newIndex = index + direction;
    if (newIndex < 0 || newIndex >= sorted.length) return;
    setReordering(true);
    try {
      const newSorted = [...sorted];
      const [moved] = newSorted.splice(index, 1);
      newSorted.splice(newIndex, 0, moved);

      const reorderRequests = newSorted.map((img, idx) => ({
        imageId: img.id,
        displayOrder: idx,
      }));

      await reorderImages(productId, reorderRequests);
      await onRefresh();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Reorder failed";
      onError?.(msg);
      alert(msg);
    } finally {
      setReordering(false);
    }
  };

  if (sorted.length === 0) {
    return (
      <div className="py-10 text-center border-2 border-dashed border-[#d6d3d1] rounded-xl bg-[#fafaf9]">
        <span className="material-symbols-outlined text-[#a8a29e] text-[40px]">hide_image</span>
        <p className="text-[13px] text-[#57534e] mt-1">No images yet. Use the uploader above.</p>
        <p className="text-[11px] text-[#57534e] mt-1">First uploaded image becomes primary automatically.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h4 className="font-semibold text-[13px] text-[#1c1917] flex items-center gap-2">
          <span className="material-symbols-outlined text-[16px] text-[#b45309]">photo_library</span>
          Gallery ({sorted.length})
          {reordering && <span className="material-symbols-outlined animate-spin text-[14px] text-[#b45309]">progress_activity</span>}
        </h4>
        <span className="text-[11px] text-[#57534e]">Use arrows to reorder • Star = primary</span>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {sorted.map((img, idx) => {
          const isPrimary = !!(img.is_primary ?? img.isPrimary);
          const isActing = actionId === img.id;
          return (
            <div
              key={img.id}
              className={`relative rounded-xl overflow-hidden border-2 bg-[#fafaf9] flex flex-col ${
                isPrimary ? "border-[#b45309] shadow-sm" : "border-[#d6d3d1]/40"
              }`}
            >
              <div className="aspect-[4/3] relative bg-white">
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={img.url}
                  alt={img.alt_text || img.altText || "product"}
                  className="w-full h-full object-cover"
                  onError={(e) => ((e.target as HTMLImageElement).style.opacity = "0.4")}
                />
                {isPrimary && (
                  <span className="absolute top-2 left-2 bg-[#b45309] text-white text-[10px] font-bold px-2 py-1 rounded-full flex items-center gap-1">
                    <span className="material-symbols-outlined text-[12px]">star</span> PRIMARY
                  </span>
                )}
                <span className="absolute bottom-2 right-2 bg-black/60 text-white text-[10px] px-1.5 py-0.5 rounded font-mono">
                  #{img.display_order ?? img.displayOrder ?? 0}
                </span>
                {isActing && (
                  <div className="absolute inset-0 bg-white/60 flex items-center justify-center">
                    <span className="material-symbols-outlined animate-spin text-[#b45309]">progress_activity</span>
                  </div>
                )}
              </div>

              <div className="p-3 space-y-2 flex-1 flex flex-col">
                <p className="text-[11px] font-mono text-[#57534e] truncate" title={img.url}>
                  {img.url}
                </p>
                <p className="text-[12px] text-[#1c1917] truncate">
                  {img.alt_text || img.altText || "— no alt text —"}
                </p>

                <div className="flex gap-1 pt-1">
                  {!isPrimary && (
                    <button
                      onClick={() => handleSetPrimary(img.id)}
                      disabled={!!actionId || reordering}
                      className="flex-1 py-1.5 rounded-lg bg-[#fef3c7]/40 hover:bg-[#fef3c7] text-[#b45309] text-[11px] font-semibold flex items-center justify-center gap-1 disabled:opacity-50"
                      title="Set as primary"
                    >
                      <span className="material-symbols-outlined text-[14px]">star</span> Primary
                    </button>
                  )}
                  {isPrimary && (
                    <span className="flex-1 py-1.5 rounded-lg bg-[#b45309] text-white text-[11px] font-semibold flex items-center justify-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">verified</span> Primary
                    </span>
                  )}
                  <button
                    onClick={() => handleDelete(img.id)}
                    disabled={!!actionId || reordering}
                    className="px-3 py-1.5 rounded-lg bg-[#fee2e2]/40 hover:bg-[#fee2e2] text-[#b91c1c] text-[11px] font-semibold flex items-center justify-center gap-1 disabled:opacity-50"
                    title="Delete"
                  >
                    <span className="material-symbols-outlined text-[14px]">delete</span>
                  </button>
                </div>

                <div className="flex gap-1">
                  <button
                    onClick={() => handleReorder(idx, -1)}
                    disabled={idx === 0 || !!actionId || reordering}
                    className="flex-1 py-1 rounded-lg border border-[#d6d3d1] hover:bg-white text-[11px] font-semibold flex items-center justify-center gap-1 disabled:opacity-30 disabled:cursor-not-allowed"
                    title="Move up"
                  >
                    <span className="material-symbols-outlined text-[14px]">arrow_upward</span> Up
                  </button>
                  <button
                    onClick={() => handleReorder(idx, 1)}
                    disabled={idx === sorted.length - 1 || !!actionId || reordering}
                    className="flex-1 py-1 rounded-lg border border-[#d6d3d1] hover:bg-white text-[11px] font-semibold flex items-center justify-center gap-1 disabled:opacity-30 disabled:cursor-not-allowed"
                    title="Move down"
                  >
                    <span className="material-symbols-outlined text-[14px]">arrow_downward</span> Down
                  </button>
                  <a
                    href={img.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-2 py-1 rounded-lg border border-[#d6d3d1] hover:bg-white text-[#57534e] flex items-center justify-center"
                    title="Open"
                  >
                    <span className="material-symbols-outlined text-[14px]">open_in_new</span>
                  </a>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="bg-[#fef3c7]/15 border border-[#b45309]/20 rounded-xl p-3 flex gap-2">
        <span className="material-symbols-outlined text-[#b45309] text-[18px] shrink-0">lightbulb</span>
        <div className="text-[11px] leading-relaxed text-[#57534e]">
          <span className="font-semibold text-[#1c1917]">How users see images:</span> Primary → card thumbnail & gallery hero. Order = displayOrder. Reorder with arrows. First image auto-primary if none exists.
        </div>
      </div>
    </div>
  );
}
