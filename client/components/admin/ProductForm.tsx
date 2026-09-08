"use client";

import { useEffect, useRef, useState } from "react";
import type { Category, Product } from "@/lib/types";
import { createProductWithImages, updateProductWithImages } from "@/lib/product";

const ACCEPTED = ["image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"];
const MAX_SIZE = 10 * 1024 * 1024;

interface ProductFormProps {
  product?: Product | null;
  categories: Category[];
  onSuccess: (product: Product) => void;
  onCancel: () => void;
  onError?: (msg: string) => void;
}

export default function ProductForm({ product, categories, onSuccess, onCancel, onError }: ProductFormProps) {
  const isEdit = !!product;
  const [form, setForm] = useState({
    name: product?.name ?? "",
    description: product?.description ?? "",
    price: product ? String(product.price) : "",
    stockQuantity: product ? String(product.stock_quantity ?? product.stockQuantity ?? 0) : "",
    categoryId: (product?.category_id ?? product?.categoryId ?? categories[0]?.id) ?? "",
    isNewArrival: !!(product?.is_new_arrival ?? product?.isNewArrival),
  });
  const [files, setFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [dragActive, setDragActive] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (product) {
      setForm({
        name: product.name,
        description: product.description,
        price: String(product.price),
        stockQuantity: String(product.stock_quantity ?? product.stockQuantity ?? 0),
        categoryId: (product.category_id ?? product.categoryId ?? categories[0]?.id) ?? "",
        isNewArrival: !!(product.is_new_arrival ?? product.isNewArrival),
      });
    }
  }, [product, categories]);

  const previewsRef = useRef<string[]>([]);
  useEffect(() => {
    previewsRef.current = previews;
  }, [previews]);
  useEffect(() => {
    return () => {
      previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);

  const validateFile = (file: File): string | null => {
    if (!ACCEPTED.includes(file.type.toLowerCase())) return `Invalid type ${file.type}`;
    if (file.size > MAX_SIZE) return `Too large ${(file.size / 1024 / 1024).toFixed(1)}MB > 10MB`;
    if (file.size === 0) return "Empty file";
    return null;
  };

  const handleFiles = (list: FileList | File[]) => {
    const arr = Array.from(list);
    const valid: File[] = [];
    const errs: string[] = [];
    for (const f of arr) {
      const err = validateFile(f);
      if (err) errs.push(`${f.name}: ${err}`);
      else valid.push(f);
    }
    if (errs.length) setError(errs.join(" • "));
    if (valid.length === 0) return;

    const newPreviews = valid.map((f) => URL.createObjectURL(f));
    setFiles((prev) => [...prev, ...valid].slice(0, 10));
    setPreviews((prev) => [...prev, ...newPreviews].slice(0, 10));
    if (valid.length + files.length > 10) setError("Max 10 images");
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragActive(false);
    if (e.dataTransfer.files) handleFiles(e.dataTransfer.files);
  };

  const removeFile = (idx: number) => {
    URL.revokeObjectURL(previews[idx]);
    setFiles((prev) => prev.filter((_, i) => i !== idx));
    setPreviews((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = async () => {
    if (!form.name || !form.description || !form.price || !form.stockQuantity) {
      setError("All fields are required");
      return;
    }
    const price = parseFloat(form.price);
    const stock = parseInt(form.stockQuantity, 10);
    if (isNaN(price) || price < 0) {
      setError("Invalid price");
      return;
    }
    if (isNaN(stock) || stock < 0) {
      setError("Invalid stock");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const payload = {
        name: form.name,
        description: form.description,
        price,
        stockQuantity: stock,
        categoryId: form.categoryId || categories[0]?.id || "",
        isNewArrival: form.isNewArrival,
      };

      let saved: Product;
      if (isEdit && product) {
        saved = await updateProductWithImages(product.id, payload, files);
      } else {
        saved = await createProductWithImages(payload, files);
      }
      previews.forEach((url) => URL.revokeObjectURL(url));
      setFiles([]);
      setPreviews([]);
      onSuccess(saved);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to save product";
      setError(msg);
      onError?.(msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onCancel} />
      <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
        <div className="sticky top-0 bg-white px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between shrink-0">
          <h3 className="font-semibold text-[18px] text-[#1c1917] flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">{isEdit ? "edit" : "add_circle"}</span>
            {isEdit ? "Edit Product" : "Add Product"}
          </h3>
          <button onClick={onCancel} className="p-1.5 rounded-full hover:bg-[#fafaf9] text-[#57534e]">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="flex-1 overflow-auto px-6 py-5 space-y-4">
          {error && (
            <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-start gap-2 text-[#b91c1c] text-[13px]">
              <span className="material-symbols-outlined text-[18px] shrink-0">error</span>
              <span>{error}</span>
            </div>
          )}

          <div>
            <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Name *</label>
            <input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Aura Pro Wireless Headphones"
              className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none"
            />
          </div>

          <div>
            <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Description *</label>
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={3}
              placeholder="Compelling product description..."
              className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none resize-none"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Price (NPR) *</label>
              <input
                type="number"
                step="0.01"
                value={form.price}
                onChange={(e) => setForm({ ...form, price: e.target.value })}
                placeholder="2999.00"
                className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] outline-none"
              />
              <p className="text-[11px] text-[#57534e] mt-1">In NPR — e.g. 2999 = Rs. 2,999.00</p>
            </div>
            <div>
              <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Stock *</label>
              <input
                type="number"
                value={form.stockQuantity}
                onChange={(e) => setForm({ ...form, stockQuantity: e.target.value })}
                placeholder="50"
                className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] outline-none"
              />
            </div>
          </div>

          <div>
            <label className="block text-[12px] font-semibold text-[#1c1917] mb-1">Category</label>
            <select
              value={form.categoryId}
              onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
              className="w-full bg-[#fafaf9] border border-[#d6d3d1]/60 rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] outline-none"
            >
              <option value="">— Select category —</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={form.isNewArrival}
              onChange={(e) => setForm({ ...form, isNewArrival: e.target.checked })}
              className="w-4 h-4 rounded border-[#d6d3d1] text-[#b45309] focus:ring-[#b45309]"
            />
            <span className="text-[13px] text-[#1c1917]">Mark as New Arrival</span>
            <span className="ml-auto text-[11px] bg-[#fef3c7] text-[#b45309] px-1.5 py-0.5 rounded">Featured</span>
          </label>

          <div className="border-t border-[#d6d3d1] pt-4 space-y-3">
            <h4 className="font-semibold text-[13px] text-[#1c1917] flex items-center gap-2">
              <span className="material-symbols-outlined text-[#b45309] text-[18px]">add_photo_alternate</span>
              Product Images {isEdit ? "(new images will be added)" : "(upload with product)"}
              {files.length > 0 && <span className="ml-auto text-[11px] bg-[#fef3c7] text-[#b45309] px-1.5 py-0.5 rounded-full">{files.length} selected</span>}
            </h4>

            <div
              onDragOver={(e) => {
                e.preventDefault();
                setDragActive(true);
              }}
              onDragLeave={(e) => {
                e.preventDefault();
                setDragActive(false);
              }}
              onDrop={handleDrop}
              onClick={() => inputRef.current?.click()}
              className={`border-2 border-dashed rounded-xl p-4 text-center cursor-pointer transition-colors ${
                dragActive ? "border-[#b45309] bg-[#fef3c7]/20" : "border-[#d6d3d1] hover:border-[#b45309]/50 bg-[#fafaf9]"
              }`}
            >
              <input
                ref={inputRef}
                type="file"
                accept={ACCEPTED.join(",")}
                multiple={!isEdit}
                onChange={(e) => {
                  if (e.target.files) handleFiles(e.target.files);
                  e.target.value = "";
                }}
                className="hidden"
              />
              <div className="flex flex-col items-center gap-1">
                <span className="material-symbols-outlined text-[#b45309] text-[24px]">cloud_upload</span>
                <p className="text-[12px] font-semibold text-[#1c1917]">Drag & drop or click to select</p>
                <p className="text-[11px] text-[#57534e]">jpg, png, webp, gif, svg • Max 10MB • Up to 10 files • First = primary</p>
              </div>
            </div>

            {previews.length > 0 && (
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                {previews.map((url, idx) => (
                  <div key={url} className="relative rounded-xl overflow-hidden border border-[#d6d3d1]/40 bg-[#fafaf9] group">
                    <div className="aspect-square relative">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={url} alt={`preview ${idx}`} className="w-full h-full object-cover" />
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          removeFile(idx);
                        }}
                        className="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/60 text-white flex items-center justify-center hover:bg-error"
                      >
                        <span className="material-symbols-outlined text-[14px]">close</span>
                      </button>
                      {idx === 0 && <span className="absolute bottom-1 left-1 bg-[#b45309] text-white text-[9px] px-1 py-0.5 rounded font-bold">PRIMARY</span>}
                    </div>
                    <p className="p-1 text-[10px] truncate text-[#57534e]" title={files[idx]?.name}>
                      {files[idx]?.name}
                    </p>
                  </div>
                ))}
              </div>
            )}

            {isEdit && product && (
              <div className="bg-[#fafaf9] rounded-xl p-3 border border-[#e7e5e4] flex items-center justify-between">
                <div className="text-[11px] text-[#57534e] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px]">photo_library</span>
                  {product.images?.length ?? 0} existing images — manage after update
                </div>
              </div>
            )}

            <p className="text-[11px] text-[#57534e] bg-[#fef3c7]/10 border border-[#b45309]/20 rounded-lg px-3 py-2 flex gap-2">
              <span className="material-symbols-outlined text-[16px] text-[#b45309] shrink-0">info</span>
              {isEdit
                ? "New images are appended. First new image becomes primary only if none exists. Reorder/delete in gallery after update."
                : "Images uploaded together with product. First image becomes primary. You can add more after creation."}
            </p>
          </div>
        </div>

        <div className="sticky bottom-0 bg-white px-6 py-4 border-t border-[#d6d3d1] flex justify-end gap-2 shrink-0">
          <button onClick={onCancel} className="px-4 py-2 rounded-xl border border-[#d6d3d1] hover:bg-[#fafaf9] text-[13px] font-semibold" disabled={saving}>
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={saving}
            className="px-5 py-2 rounded-xl bg-[#b45309] text-white hover:bg-[#92400e] disabled:opacity-50 text-[13px] font-semibold flex items-center gap-1.5"
          >
            {saving && <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span>}
            {isEdit ? "Update Product" : `Create Product${files.length ? ` + ${files.length} images` : ""}`}
          </button>
        </div>
      </div>
    </div>
  );
}
