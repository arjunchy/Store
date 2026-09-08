"use client";

import { formatNPR } from "@/lib/format";

import { useEffect, useState } from "react";
import {
  getProducts,
  deleteProduct,
  getProductImages,
  uploadProductImage,
} from "@/lib/product";
import { getCategories } from "@/lib/category";
import type { Product, ProductImage } from "@/lib/types";
import type { Category } from "@/lib/types";
import ProductForm from "@/components/admin/ProductForm";
import ImageUploader from "@/components/admin/ImageUploader";
import ImageGallery from "@/components/admin/ImageGallery";

const formatUSD = formatNPR;
export default function AdminProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 20;
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Product | null>(null);

  const [imageManagerProduct, setImageManagerProduct] = useState<Product | null>(null);
  const [imageList, setImageList] = useState<ProductImage[]>([]);
  const [imageLoading, setImageLoading] = useState(false);
  const [imageError, setImageError] = useState("");

  const load = async () => {
    setLoading(true);
    try {
      const [prod, cats] = await Promise.all([getProducts().catch(() => [] as Product[]), getCategories().catch(() => [] as Category[])]);
      setProducts(prod);
      setCategories(cats);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = products.filter(
    (p) => !search || p.name.toLowerCase().includes(search.toLowerCase()) || p.id.toLowerCase().includes(search.toLowerCase())
  );
  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const paged = filtered.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  useEffect(() => { setPage(0); }, [search]);

  const openCreate = () => {
    setEditing(null);
    setShowForm(true);
  };

  const openEdit = (p: Product) => {
    setEditing(p);
    setShowForm(true);
  };

  const handleFormSuccess = async (saved: Product) => {
    setShowForm(false);
    setEditing(null);
    await load();
    if (!editing) {
      setTimeout(() => openImageManager(saved), 300);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this product? This will soft-delete it.")) return;
    try {
      await deleteProduct(id);
      await load();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  const fetchImages = async (productId: string) => {
    setImageLoading(true);
    setImageError("");
    try {
      const imgs = await getProductImages(productId).catch(() => [] as ProductImage[]);
      imgs.sort((a, b) => {
        const aP = (a.is_primary ?? a.isPrimary ?? false) ? 1 : 0;
        const bP = (b.is_primary ?? b.isPrimary ?? false) ? 1 : 0;
        if (aP !== bP) return bP - aP;
        return (a.display_order ?? a.displayOrder ?? 0) - (b.display_order ?? b.displayOrder ?? 0);
      });
      setImageList(imgs);
    } catch (e) {
      setImageError(e instanceof Error ? e.message : "Failed to load images");
    } finally {
      setImageLoading(false);
    }
  };

  const openImageManager = async (p: Product) => {
    setImageManagerProduct(p);
    setImageError("");
    await fetchImages(p.id);
  };

  const closeImageManager = () => {
    setImageManagerProduct(null);
    setImageList([]);
    load();
  };

  const handleFileUpload = async (file: File, opts?: { altText?: string; isPrimary?: boolean }) => {
    if (!imageManagerProduct) return;
    const nextOrder = imageList.length;
    await uploadProductImage(imageManagerProduct.id, file, {
      altText: opts?.altText || imageManagerProduct.name,
      displayOrder: nextOrder,
      isPrimary: opts?.isPrimary || imageList.length === 0,
    });
    await fetchImages(imageManagerProduct.id);
  };

  const handleGalleryRefresh = async () => {
    if (!imageManagerProduct) return;
    await fetchImages(imageManagerProduct.id);
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="font-bold text-[24px] text-[#1c1917] tracking-tight flex items-center gap-2">
            <span className="material-symbols-outlined text-[#b45309]">inventory_2</span>
            Products
          </h1>
          <p className="text-[13px] text-[#57534e]">Manage catalog, stock and new arrivals. Upload images via drag & drop — users see them instantly.</p>
        </div>
        <button onClick={openCreate} className="inline-flex items-center gap-1.5 px-4 py-2.5 bg-[#b45309] text-[#fafaf9] rounded-xl text-[13px] font-semibold hover:bg-[#92400e] transition-colors shadow-sm">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Add Product
        </button>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm p-4 flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">search</span>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name or ID..."
            className="w-full bg-white border border-[#d6d3d1] rounded-xl pl-10 pr-4 py-2.5 text-[13px] focus:border-[#b45309] focus:ring-1 focus:ring-[#b45309] outline-none placeholder:text-[#a8a29e] text-[#1c1917]"
          />
        </div>
        <div className="flex items-center gap-2 text-[12px] text-[#57534e]">
          <span>{filtered.length} of {products.length} products</span>
          <span className="w-px h-4 bg-[#d6d3d1]" />
          <span className="hidden sm:inline">{categories.length} categories</span>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-[#d6d3d1] shadow-sm overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <span className="material-symbols-outlined animate-spin text-[#b45309] text-[28px]">progress_activity</span>
          </div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center">
            <span className="material-symbols-outlined text-[#a8a29e] text-[48px]">inventory</span>
            <p className="text-[14px] text-[#57534e] mt-2">No products found.</p>
            <button onClick={openCreate} className="mt-3 text-[#b45309] font-semibold text-[13px] hover:underline">Create first product</button>
          </div>
        ) : (
          <>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="bg-[#b45309] text-[11px] font-semibold tracking-widest uppercase text-[#fafaf9]">
                <tr>
                  <th className="px-4 py-3">Product</th>
                  <th className="px-4 py-3">Category</th>
                  <th className="px-4 py-3">Price</th>
                  <th className="px-4 py-3">Stock</th>
                  <th className="px-4 py-3">Rating</th>
                  <th className="px-4 py-3">Images</th>
                  <th className="px-4 py-3">Flag</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
                    <tbody className="divide-y divide-[#e7e5e4]">
                    {paged.map((p) => (
                  <tr key={p.id} className="hover:bg-[#fafaf9] transition-colors">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-lg bg-[#fafaf9] overflow-hidden shrink-0 flex items-center justify-center border border-[#e7e5e4]">
                          {p.image ? (
                            <img
                              src={p.image}
                              alt={p.name}
                              className="w-full h-full object-cover"
                              onError={(e) => ((e.target as HTMLImageElement).style.display = "none")}
                            />
                          ) : p.images && p.images.length > 0 ? (
                            <img src={p.images[0].url} alt={p.name} className="w-full h-full object-cover" />
                          ) : (
                            <span className="material-symbols-outlined text-[#a8a29e]">image</span>
                          )}
                        </div>
                        <div className="min-w-0">
                          <div className="font-medium text-[13px] text-[#1c1917] truncate max-w-[240px]">{p.name}</div>
                          <div className="text-[11px] font-mono text-[#57534e] truncate">{p.id.slice(0, 8)}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-[12px] text-[#57534e]">
                      {categories.find((c) => c.id === (p.category_id ?? p.categoryId))?.name ?? (p.category_id ?? p.categoryId ?? "—")}
                    </td>
                    <td className="px-4 py-3 font-semibold text-[13px] text-[#b45309]">{formatUSD(p.price)}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`text-[11px] font-semibold px-1.5 py-0.5 rounded-full border ${
                          (p.stock_quantity ?? p.stockQuantity ?? 0) === 0
                            ? "bg-[#fef2f2] text-[#b91c1c] border-[#fecaca]"
                            : (p.stock_quantity ?? p.stockQuantity ?? 0) < 5
                            ? "bg-[#fffbeb] text-[#b45309] border-[#fde68a]"
                            : "bg-[#15803d]/10 text-[#15803d] border-[#15803d]/20"
                        }`}
                      >
                        {p.stock_quantity ?? p.stockQuantity ?? 0} in stock
                      </span>
                    </td>
                    <td className="px-4 py-3 text-[12px] text-[#57534e] flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px] text-[#b45309]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                      {p.rating?.toFixed(1) ?? "—"}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`text-[11px] font-semibold px-1.5 py-0.5 rounded-full border ${p.images && p.images.length > 0 ? "bg-[#fef3c7] text-[#b45309] border-[#b45309]/20" : "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]"}`}>
                        {p.images?.length ?? (p.image ? 1 : 0)} imgs
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {(p.is_new_arrival || p.isNewArrival) && (
                        <span className="text-[11px] font-semibold bg-[#b45309] text-white px-1.5 py-0.5 rounded">New</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => openImageManager(p)} className="p-1.5 rounded-lg hover:bg-[#fef3c7] text-[#57534e] hover:text-[#b45309] transition-colors" title="Manage images">
                          <span className="material-symbols-outlined text-[18px]">photo_library</span>
                        </button>
                        <button onClick={() => openEdit(p)} className="p-1.5 rounded-lg hover:bg-[#fafaf9] text-[#57534e] hover:text-[#b45309] transition-colors" title="Edit">
                          <span className="material-symbols-outlined text-[18px]">edit</span>
                        </button>
                        <button onClick={() => handleDelete(p.id)} className="p-1.5 rounded-lg hover:bg-[#fef2f2] text-[#57534e] hover:text-[#b91c1c] transition-colors" title="Delete">
                          <span className="material-symbols-outlined text-[18px]">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
          </div>
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-[#e7e5e4] bg-[#fafaf9]">
                <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page <= 0} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40 hover:bg-[#b45309] hover:text-white hover:border-[#b45309]">← Prev</button>
                <span className="text-[12px] text-[#57534e]">Page <span className="font-semibold text-[#1c1917]">{page + 1}</span> of {totalPages} • {filtered.length} products</span>
                <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page + 1 >= totalPages} className="px-3 py-1.5 rounded-lg border border-[#d6d3d1] bg-white text-[12px] font-semibold disabled:opacity-40 hover:bg-[#b45309] hover:text-white hover:border-[#b45309]">Next →</button>
              </div>
            )}
          </>
        )}
      </div>

      {showForm && (
        <ProductForm
          product={editing}
          categories={categories}
          onSuccess={handleFormSuccess}
          onCancel={() => {
            setShowForm(false);
            setEditing(null);
          }}
        />
      )}

      {imageManagerProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={closeImageManager} />
          <div className="relative bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden">
            <div className="px-6 py-4 border-b border-[#d6d3d1] flex items-center justify-between shrink-0">
              <div>
                <h3 className="font-semibold text-[16px] text-[#1c1917] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[#b45309]">photo_library</span>
                  Manage Images — {imageManagerProduct.name}
                </h3>
                <p className="text-[11px] text-[#57534e] font-mono">{imageManagerProduct.id.slice(0, 12)} — {imageList.length} image{imageList.length !== 1 ? "s" : ""} — stored at /uploads/products/{imageManagerProduct.id.slice(0, 8)}/</p>
              </div>
              <button onClick={closeImageManager} className="p-1.5 rounded-full hover:bg-[#fafaf9] text-[#57534e]">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex-1 overflow-auto px-6 py-5 space-y-5">
              {imageError && (
                <div className="bg-[#fef2f2] border border-[#fecaca] rounded-xl px-3 py-2 flex items-center gap-2 text-[#b91c1c] text-[13px]">
                  <span className="material-symbols-outlined text-[18px]">error</span> {imageError}
                </div>
              )}

              <div className="bg-[#fafaf9] rounded-xl p-4 border border-[#d6d3d1]/40">
                <h4 className="font-semibold text-[13px] text-[#1c1917] mb-3 flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-[18px] text-[#b45309]">cloud_upload</span>
                  Upload new images (drag & drop)
                </h4>
                <ImageUploader
                  onUpload={handleFileUpload}
                  multiple={true}
                  maxFiles={10}
                  isPrimaryDefault={imageList.length === 0}
                />
              </div>

              <div>
                <h4 className="font-semibold text-[13px] text-[#1c1917] mb-3 flex items-center gap-2">
                  Existing images
                  {imageLoading && <span className="material-symbols-outlined animate-spin text-[16px] text-[#b45309]">progress_activity</span>}
                </h4>
                {imageLoading ? (
                  <div className="py-8 flex justify-center">
                    <span className="material-symbols-outlined animate-spin text-[#b45309] text-[24px]">progress_activity</span>
                  </div>
                ) : (
                  <ImageGallery productId={imageManagerProduct.id} images={imageList} onRefresh={handleGalleryRefresh} onError={setImageError} />
                )}
              </div>

              <div className="bg-[#fef3c7]/15 border border-[#b45309]/20 rounded-xl p-3 flex gap-2">
                <span className="material-symbols-outlined text-[#b45309] text-[18px] shrink-0">lightbulb</span>
                <div className="text-[11px] leading-relaxed text-[#57534e]">
                  <span className="font-semibold text-[#1c1917]">File storage:</span> Images uploaded via <span className="font-mono text-[#b45309]">POST /api/products/{"{id}"}/images/upload</span> (multipart, 10MB max, jpg/png/webp/gif/svg) are saved to <span className="font-mono">uploads/products/{"{id}"}/{"{uuid}"}.jpg</span> and served at <span className="font-mono">/uploads/**</span>. Primary image appears as thumbnail everywhere.
                </div>
              </div>
            </div>

            <div className="px-6 py-4 border-t border-[#d6d3d1] flex justify-end shrink-0">
              <button onClick={closeImageManager} className="px-5 py-2 rounded-xl bg-[#b45309] text-white font-semibold text-[13px] hover:bg-[#92400e]">
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
