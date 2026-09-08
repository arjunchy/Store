"use client";

import { formatNPR } from "@/lib/format";

import { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import type { Product } from "@/lib/types";
import type { CartItem } from "@/lib/types";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context/AuthContext";
import { useWishlist } from "@/context/WishlistContext";
import { getReviewsByProduct, submitReview, updateReview, deleteReview } from "@/lib/review";
import { getCategories } from "@/lib/category";
import type { Review } from "@/lib/types";

export default function ProductClient({ product }: { product: Product }) {
  const [activeImg, setActiveImg] = useState(0);
  const [qty, setQty] = useState(1);
  const [added, setAdded] = useState(false);
  const [tab, setTab] = useState<"desc" | "specs" | "reviews">("desc");
  const [reviews, setReviews] = useState<Review[]>([]);
  const [wishlistBusy, setWishlistBusy] = useState(false);
  const [wishlistToast, setWishlistToast] = useState<string | null>(null);
  const [categoryName, setCategoryName] = useState<string>("");
  const [newRating, setNewRating] = useState(5);
  const [newComment, setNewComment] = useState("");
  const [submittingReview, setSubmittingReview] = useState(false);
  const [reviewError, setReviewError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editRating, setEditRating] = useState(5);
  const [editComment, setEditComment] = useState("");
  const [editSaving, setEditSaving] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);
  const { addToCart } = useCart();
  const { isAuthenticated, user } = useAuth();
  const { isWishlisted, toggle } = useWishlist();
  const wishlist = isWishlisted(product.id);
  const router = useRouter();

  const refreshReviews = async () => {
    const r = await getReviewsByProduct(product.id);
    setReviews(r);
  };

  useEffect(() => {
    let active = true;
    getReviewsByProduct(product.id).then((r) => {
      if (active) setReviews(r);
    });
    return () => {
      active = false;
    };
  }, [product.id]);

  useEffect(() => {
    const catId = product.category_id || product.categoryId || "";
    if (catId) {
      getCategories().then((cats) => {
        const found = cats.find((c) => c.id === catId);
        if (found) setCategoryName(found.name);
      }).catch(() => {});
    }
  }, [product.id, product.category_id, product.categoryId]);

  const galleryUrls = (() => {
    const imgs = product.images || [];
    if (imgs.length === 0) return product.image ? [product.image] : [];
    const sorted = [...imgs].sort((a, b) => {
      const aP = (a.is_primary ?? a.isPrimary ?? false) ? 1 : 0;
      const bP = (b.is_primary ?? b.isPrimary ?? false) ? 1 : 0;
      if (aP !== bP) return bP - aP;
      const aO = a.display_order ?? a.displayOrder ?? 0;
      const bO = b.display_order ?? b.displayOrder ?? 0;
      return aO - bO;
    });
    const urls = sorted.map((i) => i.url).filter(Boolean) as string[];
    const seen = new Set<string>();
    const deduped: string[] = [];
    for (const u of urls) {
      if (!seen.has(u)) {
        seen.add(u);
        deduped.push(u);
      }
    }
    if (product.image && !seen.has(product.image)) deduped.push(product.image);
    return deduped;
  })();

  const mainImgUrl = galleryUrls[0] || product.image || "";
  const displayImages = galleryUrls.length > 0 ? galleryUrls : mainImgUrl ? [mainImgUrl] : [];
  const currentImg = displayImages[activeImg] || mainImgUrl || "";

  const avgRating = reviews.length > 0 ? Math.round((reviews.reduce((s, r) => s + r.rating, 0) / reviews.length) * 10) / 10 : 0;

  const handleAddToCart = async () => {
    try {
      await addToCart({
        id: product.id,
        product_id: product.id,
        slug: product.id,
        name: product.name,
        price: product.price,
        image: mainImgUrl,
        qty,
      } as unknown as Omit<CartItem, "qty"> & { qty?: number });
      setAdded(true);
      setTimeout(() => setAdded(false), 2000);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401 || status === 403) {
        router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      } else {
        console.error("[product] addToCart failed", err);
      }
    }
  };

  const handleWishlist = async () => {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      return;
    }
    if (wishlistBusy) return;
    setWishlistBusy(true);
    try {
      await toggle(product.id);
      setWishlistToast(wishlist ? "Removed from wishlist" : "Added to wishlist");
      setTimeout(() => setWishlistToast(null), 2200);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401 || status === 403) {
        router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      }
    } finally {
      setWishlistBusy(false);
    }
  };

  const handleBuyNow = () => {
    handleAddToCart()
      .then(() => router.push("/cart"))
      .catch(() => {});
  };

  const handleSubmitReview = async () => {
    if (!isAuthenticated) {
      router.push(`/login?next=${encodeURIComponent(`/product/${product.id}`)}`);
      return;
    }
    if (!newComment.trim() || newRating < 1) {
      setReviewError("Please provide rating and comment");
      return;
    }
    setSubmittingReview(true);
    setReviewError(null);
    try {
      await submitReview({ productId: product.id, rating: newRating, comment: newComment.trim(), userId: "" });
      await refreshReviews();
      setNewComment("");
      setNewRating(5);
      setTab("reviews");
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Failed to submit review";
      setReviewError(msg.includes("already") ? "You have already reviewed this product. You can edit your existing review below." : msg);
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleStartEdit = (rev: Review) => {
    setEditingId(rev.id);
    setEditRating(rev.rating);
    setEditComment(rev.comment || "");
    setEditError(null);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditComment("");
    setEditError(null);
  };

  const handleSaveEdit = async (rev: Review) => {
    if (!editComment.trim() || editRating < 1) {
      setEditError("Please provide rating and comment");
      return;
    }
    setEditSaving(true);
    setEditError(null);
    try {
      const updated = await updateReview(rev.id, { rating: editRating, comment: editComment.trim(), productId: rev.productId || product.id });
      if (!updated) throw new Error("You can only edit your own review");
      await refreshReviews();
      setEditingId(null);
    } catch (e: unknown) {
      setEditError(e instanceof Error ? e.message : "Failed to update review");
    } finally {
      setEditSaving(false);
    }
  };

  const handleDeleteOwn = async (rev: Review) => {
    if (!confirm("Delete your review?")) return;
    try {
      await deleteReview(rev.id);
      await refreshReviews();
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  const stockQuantity = product.stock_quantity ?? product.stockQuantity ?? 0;
  const reviewCountDisplay = product.review_count ?? product.reviewCount ?? reviews.length;
  const ratingDisplay = product.rating || avgRating;

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9]">
      <Navbar />
      
      <div className={`fixed bottom-8 right-8 z-50 transition-all duration-500 ${added ? "translate-y-0 opacity-100" : "translate-y-10 opacity-0 pointer-events-none"}`}>
        <div className="bg-[#fafaf9]est text-[#1c1917] border border-[#d6d3d1] shadow-xl rounded-xl p-4 flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-[#b45309]/10 flex items-center justify-center text-[#b45309]">
            <span className="material-symbols-outlined text-[20px]">check</span>
          </div>
          <div>
            <p className="font-semibold text-[14px]">Added to Cart</p>
            <p className="text-[12px] text-[#57534e]">{product.name} ({qty})</p>
          </div>
          <Link href="/cart" className="ml-4 text-[#b45309] font-semibold text-[13px] hover:underline">View Cart</Link>
        </div>
      </div>
      <div className={`fixed bottom-8 left-1/2 -translate-x-1/2 z-50 transition-all duration-300 ${wishlistToast ? "translate-y-0 opacity-100" : "translate-y-6 opacity-0 pointer-events-none"}`}>
        <div className="bg-[#0c0a09] text-white rounded-full px-4 py-2.5 shadow-xl flex items-center gap-2 text-[13px] font-medium">
          <span className="material-symbols-outlined text-[18px]">{wishlist ? "heart_minus" : "favorite"}</span>
          {wishlistToast}
          {wishlistToast?.startsWith("Added") && <Link href="/wishlist" className="ml-2 underline decoration-white/40 hover:decoration-white">View</Link>}
        </div>
      </div>

      <main className="flex-grow w-full max-w-[1440px] mx-auto px-margin-mobile md:px-margin-desktop py-xl">
        <div className="flex items-center gap-1 font-body-sm text-body-sm text-[#57534e] mb-6 flex-wrap">
          <Link href="/" className="hover:text-[#b45309] transition-colors text-[13px]">Home</Link>
          <span className="material-symbols-outlined text-[16px]">chevron_right</span>
          <Link href="/catalog" className="hover:text-[#b45309] transition-colors text-[13px]">Catalog</Link>
          {categoryName && (
            <>
              <span className="material-symbols-outlined text-[16px]">chevron_right</span>
              <Link href={`/catalog?category=${product.category_id || product.categoryId}`} className="hover:text-[#b45309] transition-colors text-[13px]">{categoryName}</Link>
            </>
          )}
          <span className="material-symbols-outlined text-[16px]">chevron_right</span>
          <span className="text-[#1c1917] font-semibold text-[13px] line-clamp-1">{product.name}</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-8 lg:gap-xxl">
          <div className="md:col-span-7 flex flex-col gap-4">
            <div className="w-full bg-[#fafaf9] rounded-2xl overflow-hidden shadow-md group relative aspect-[4/3] border border-[#e7e5e4]">
              {currentImg ? (
                <Image
                  src={currentImg}
                  alt={product.name}
                  fill
                  unoptimized
                  className="object-cover transition-transform duration-700 group-hover:scale-105"
                  sizes="(max-width: 768px) 100vw, 58vw"
                  priority
                  onError={(e) => {
                    (e.target as HTMLImageElement).style.display = "none";
                  }}
                />
              ) : (
                <div className="w-full h-full flex flex-col items-center justify-center gap-2 bg-[#fafaf9]">
                  <span className="material-symbols-outlined text-[#a8a29e] text-[64px]">image</span>
                  <span className="text-[12px] text-[#a8a29e]">No image available</span>
                </div>
              )}
              <button onClick={handleWishlist} className={`absolute top-3 right-3 w-9 h-9 rounded-full flex items-center justify-center backdrop-blur bg-white/90 border shadow-sm ${wishlist ? "text-[#b91c1c] border-error" : "text-[#a8a29e]"}`}>
                <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: `'FILL' ${wishlist ? 1 : 0}` }}>favorite</span>
              </button>
              {product.is_new_arrival || product.isNewArrival ? <span className="absolute top-3 left-3 bg-[#b45309] text-white text-[11px] font-bold px-2 py-1 rounded-full">NEW ARRIVAL</span> : null}
              {galleryUrls.length === 0 && (
                <span className="absolute bottom-4 left-4 bg-black/60 text-white backdrop-blur-md text-[11px] px-3 py-1.5 rounded-full font-medium">Image not yet added</span>
              )}
            </div>
            {displayImages.length > 1 && (
              <div className="grid grid-cols-4 gap-2">
                {displayImages.slice(0, 8).map((img, idx) => (
                  <button
                    key={`${img}-${idx}`}
                    onClick={() => setActiveImg(idx)}
                    className={`bg-[#fafaf9] rounded-lg overflow-hidden border-2 cursor-pointer h-24 relative ${
                      activeImg === idx ? "border-[#b45309]" : "border-transparent hover:border-[#d6d3d1]"
                    }`}
                    aria-label={`View image ${idx + 1}`}
                  >
                    <Image
                      src={img}
                      alt={`${product.name} view ${idx + 1}`}
                      fill
                      unoptimized
                      className="object-cover"
                      sizes="160px"
                      onError={(e) => {
                        (e.target as HTMLImageElement).style.display = "none";
                      }}
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="md:col-span-5 flex flex-col gap-5">
            <div className="flex flex-col gap-2">
              <div className="flex items-center gap-2">
                {categoryName && <span className="bg-[#fef3c7] text-white-container text-[11px] font-bold px-2 py-1 rounded">{categoryName}</span>}
                {stockQuantity > 0 && stockQuantity < 10 && <span className="bg-[#fee2e2] text-[#b91c1c] text-[11px] font-bold px-2 py-1 rounded animate-pulse">Only {stockQuantity} left!</span>}
              </div>
              <h1 className="font-semibold text-[26px] leading-tight text-[#1c1917]">{product.name}</h1>
              <div className="flex items-center gap-2">
                <div className="flex text-[#b45309] text-[18px]">
                  {[...Array(5)].map((_, i) => (
                    <span
                      key={i}
                      className="material-symbols-outlined text-[18px]"
                      style={{ fontVariationSettings: `'FILL' ${i < Math.round(ratingDisplay) ? 1 : 0}` }}
                    >
                      star
                    </span>
                  ))}
                </div>
                <span className="font-semibold text-[13px] text-[#1c1917]">{ratingDisplay}</span>
                <button onClick={() => setTab("reviews")} className="font-body-sm text-body-sm text-[#57534e] underline hover:text-[#b45309] text-[13px]">({reviewCountDisplay} reviews)</button>
                <span className="ml-2 text-[11px] bg-[#15803d]/10 text-[#15803d] px-1.5 py-0.5 rounded font-medium">Verified</span>
              </div>
            </div>

            <div className="flex flex-col gap-2 py-3 border-y border-[#e7e5e4]">
              <div className="flex items-baseline gap-3">
                <span className="font-bold text-[32px] leading-none text-[#1c1917]">{formatNPR(product.price)}</span>
              </div>
              <p className="text-[12px] text-[#57534e]">Inclusive of all taxes</p>
              <div className="flex items-center gap-1.5">
                <span className={`w-2 h-2 rounded-full ${stockQuantity > 0 ? "bg-[#15803d]" : "bg-error"}`} />
                <span className="font-medium text-[13px] text-[#57534e]">
                  {stockQuantity > 0 ? `In Stock • ${stockQuantity} available` : "Out of Stock"}
                </span>
              </div>
            </div>

            <div className="flex flex-col gap-3 mt-1">
              <div className="flex gap-3 h-12">
                <div className="flex items-center border border-[#d6d3d1] rounded-lg bg-white w-32 shrink-0">
                  <button onClick={() => setQty((q) => Math.max(1, q - 1))} className="w-10 h-full flex items-center justify-center text-[#57534e] hover:text-[#b45309] hover:bg-[#fafaf9] rounded-l-lg transition-colors">
                    <span className="material-symbols-outlined text-[20px]">remove</span>
                  </button>
                  <input className="w-full text-center font-semibold text-[14px] bg-transparent border-none focus:ring-0 p-0 text-[#1c1917]" readOnly value={qty} />
                  <button onClick={() => setQty((q) => Math.min(q + 1, stockQuantity || 99))} className="w-10 h-full flex items-center justify-center text-[#57534e] hover:text-[#b45309] hover:bg-[#fafaf9] rounded-r-lg transition-colors">
                    <span className="material-symbols-outlined text-[20px]">add</span>
                  </button>
                </div>
                <button onClick={handleAddToCart} disabled={stockQuantity <= 0} className="flex-1 bg-[#b45309] text-white font-semibold text-[14px] rounded-lg flex items-center justify-center gap-1.5 hover:bg-[#92400e] transition-colors shadow-sm hover:shadow-md hover:-translate-y-px duration-200 disabled:opacity-50 disabled:cursor-not-allowed">
                  <span className="material-symbols-outlined text-[20px]">{added ? "check" : "shopping_cart"}</span>
                  {added ? "Added!" : stockQuantity <= 0 ? "Out of Stock" : "Add to Cart"}
                </button>
              </div>
              <div className="flex gap-3 h-12">
                <button onClick={handleBuyNow} className="flex-1 bg-[#ff9f00] text-white font-semibold text-[14px] rounded-lg flex items-center justify-center hover:bg-[#fb8c00] transition-colors shadow-sm">Buy Now</button>
                <button onClick={handleWishlist} disabled={wishlistBusy} aria-label="Wishlist" className={`w-12 h-12 border rounded-lg flex items-center justify-center shadow-sm hover:-translate-y-px transition-all disabled:opacity-50 ${wishlist ? "bg-[#fee2e2] border-error text-[#b91c1c]" : "bg-white border-[#d6d3d1] text-[#57534e] hover:text-[#b45309] hover:border-[#b45309]"}`} title={isAuthenticated ? (wishlist ? "Remove from wishlist" : "Add to wishlist") : "Sign in to wishlist"}>
                  {wishlistBusy ? <span className="material-symbols-outlined animate-spin text-[20px]">progress_activity</span> : <span className="material-symbols-outlined" style={{ fontVariationSettings: `'FILL' ${wishlist ? 1 : 0}` }}>favorite</span>}
                </button>
              </div>
              <button onClick={() => { if (navigator.share) navigator.share({ title: product.name, url: window.location.href }); else { navigator.clipboard.writeText(window.location.href); alert("Link copied!"); } }} className="text-[12px] text-[#57534e] hover:text-[#b45309] flex items-center gap-1 self-start"><span className="material-symbols-outlined text-[16px]">share</span> Share</button>
            </div>
          </div>
        </div>

        <div className="mt-12 border-t border-[#e7e5e4] pt-8">
          <div className="flex gap-8 border-b border-[#e7e5e4] mb-8 overflow-x-auto">
            <button onClick={() => setTab("desc")} className={`pb-3 font-semibold text-[15px] border-b-2 relative top-[1px] whitespace-nowrap transition-colors ${tab === "desc" ? "text-[#b45309] border-[#b45309]" : "text-[#57534e] border-transparent hover:text-[#b45309]"}`}>Description</button>
            <button onClick={() => setTab("specs")} className={`pb-3 font-semibold text-[15px] border-b-2 relative top-[1px] whitespace-nowrap transition-colors ${tab === "specs" ? "text-[#b45309] border-[#b45309]" : "text-[#57534e] border-transparent hover:text-[#b45309]"}`}>Specifications</button>
            <button onClick={() => setTab("reviews")} className={`pb-3 font-semibold text-[15px] border-b-2 relative top-[1px] whitespace-nowrap transition-colors ${tab === "reviews" ? "text-[#b45309] border-[#b45309]" : "text-[#57534e] border-transparent hover:text-[#b45309]"}`}>Reviews ({reviews.length || reviewCountDisplay})</button>
          </div>

          {tab === "desc" ? (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-12">
              <div className="md:col-span-2 space-y-6 font-body-md text-[15px] leading-relaxed text-[#57534e]">
                <p>{product.description}</p>
                <div className="bg-[#fafaf9] rounded-xl p-4 border border-[#e7e5e4]">
                  <h4 className="font-semibold text-[14px] text-[#1c1917] mb-2">Product Details</h4>
                  <div className="grid grid-cols-2 gap-2 text-[13px]">
                    <span className="text-[#57534e]">Category</span><span className="font-medium text-[#1c1917]">{categoryName || product.categoryId}</span>
                    <span className="text-[#57534e]">SKU</span><span className="font-mono text-[#1c1917]">{product.id.slice(0, 8).toUpperCase()}</span>
                    <span className="text-[#57534e]">Availability</span><span className={stockQuantity > 0 ? "text-[#15803d] font-medium" : "text-[#b91c1c]"}>{stockQuantity > 0 ? `In stock (${stockQuantity})` : "Out of stock"}</span>
                    <span className="text-[#57534e]">Brand</span><span className="font-medium">ApexCommerce</span>
                  </div>
                </div>
              </div>
              <div className="md:col-span-1">
                <div className="bg-white border border-[#d6d3d1] rounded-xl p-4 sticky top-24">
                  <h4 className="font-semibold text-[14px] mb-3">Services</h4>
                  <ul className="space-y-2 text-[12px] text-[#57534e]">
                    <li className="flex gap-2"><span className="material-symbols-outlined text-[16px] text-[#b45309]">verified_user</span> 2-Year Warranty</li>
                    <li className="flex gap-2"><span className="material-symbols-outlined text-[16px] text-[#b45309]">local_shipping</span> Free Shipping over Rs. 5,000</li>
                    <li className="flex gap-2"><span className="material-symbols-outlined text-[16px] text-[#b45309]">assignment_return</span> 30-Day Easy Returns</li>
                    <li className="flex gap-2"><span className="material-symbols-outlined text-[16px] text-[#b45309]">support_agent</span> 24/7 Customer Support</li>
                  </ul>
                </div>
              </div>
            </div>
          ) : tab === "specs" ? (
            <div className="max-w-3xl mb-12">
              <div className="bg-white border border-[#d6d3d1] rounded-xl overflow-hidden">
                <div className="grid grid-cols-3 gap-4 p-4 bg-[#fafaf9] font-semibold text-[12px] text-[#57534e] uppercase tracking-wide">
                  <span>Attribute</span><span className="col-span-2">Details</span>
                </div>
                {[
                  ["Category", categoryName || "—"],
                  ["Price", `${formatNPR(product.price)}`],
                  ["Stock", `${stockQuantity} units`],
                  ["Rating", `${ratingDisplay} / 5`],
                  ["Reviews", `${reviewCountDisplay}`],
                  ["SKU", product.id.slice(0, 12)],
                  ["Brand", "ApexCommerce"],
                ].map(([k, v]) => (
                  <div key={k} className="grid grid-cols-3 gap-4 p-4 border-t border-[#e7e5e4] text-[13px]">
                    <span className="text-[#57534e]">{k}</span><span className="col-span-2 font-medium text-[#1c1917]">{v}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-12 gap-8">
              <div className="md:col-span-4 flex flex-col gap-6 bg-[#fafaf9] rounded-xl p-6 border border-[#d6d3d1]/20 h-fit">
                <div>
                  <h3 className="font-semibold text-[18px] text-[#1c1917] mb-3">Customer Reviews</h3>
                  <div className="flex items-center gap-3">
                    <span className="font-bold text-[42px] leading-none text-[#1c1917]">{ratingDisplay}</span>
                    <div className="flex flex-col">
                      <div className="flex text-[#b45309] text-[18px]">
                        {[...Array(5)].map((_, i) => (
                          <span key={i} className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: `'FILL' ${i < Math.round(ratingDisplay) ? 1 : 0}` }}>star</span>
                        ))}
                      </div>
                      <span className="text-[13px] text-[#57534e]">Based on {reviews.length || reviewCountDisplay} reviews</span>
                    </div>
                  </div>
                </div>
                <button onClick={() => document.getElementById("write-review")?.scrollIntoView({ behavior: "smooth" })} className="w-full py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold">Write a Review</button>
              </div>
              <div className="md:col-span-8 flex flex-col gap-8">
                <div id="write-review" className="bg-white border border-[#d6d3d1] rounded-xl p-4">
                  <h4 className="font-semibold text-[14px] text-[#1c1917] mb-3">Write a review</h4>
                  {!isAuthenticated ? (
                    <p className="text-[13px] text-[#57534e]">Please <Link href={`/login?next=${encodeURIComponent(`/product/${product.id}`)}`} className="text-[#b45309] underline">login</Link> to write a review.</p>
                  ) : (
                    <>
                      <div className="flex items-center gap-1 mb-3">
                        {[1, 2, 3, 4, 5].map((s) => (
                          <button key={s} onClick={() => setNewRating(s)} className={`p-1 ${s <= newRating ? "text-[#b45309]" : "text-[#a8a29e]"}`}>
                            <span className="material-symbols-outlined text-[24px]" style={{ fontVariationSettings: `'FILL' ${s <= newRating ? 1 : 0}` }}>star</span>
                          </button>
                        ))}
                        <span className="ml-2 text-[13px] text-[#57534e]">{newRating} / 5</span>
                      </div>
                      <textarea value={newComment} onChange={(e) => setNewComment(e.target.value)} placeholder="Share your experience..." rows={3} className="w-full bg-white border border-[#d6d3d1] rounded-xl px-3 py-2.5 text-[13px] focus:border-[#b45309] outline-none resize-none" />
                      {reviewError && <p className="text-[12px] text-[#b91c1c] mt-2">{reviewError}</p>}
                      <button onClick={handleSubmitReview} disabled={submittingReview || !newComment.trim()} className="mt-3 px-5 py-2.5 bg-[#b45309] text-white rounded-xl text-[13px] font-semibold hover:bg-[#92400e] disabled:opacity-50 flex items-center gap-1">
                        {submittingReview ? <span className="material-symbols-outlined animate-spin text-[16px]">progress_activity</span> : null} Submit Review
                      </button>
                    </>
                  )}
                </div>

                {reviews.length > 0 ? (
                  reviews.map((rev) => {
                    const isOwner = isAuthenticated && user?.id === rev.userId;
                    const initials = (rev.userId || "??").slice(0, 2).toUpperCase();
                    const displayName = (rev as any).userName || (rev as any).username || "User";
                    const verified = (rev as any).is_verified_purchase ?? rev.isVerifiedPurchase;
                    const stars = rev.rating;
                    const text = rev.comment || "";
                    const date = rev.createdAt ? new Date(rev.createdAt).toLocaleDateString() : "";
                    const isEditing = editingId === rev.id;
                    return (
                    <div key={rev.id} className="border-b border-[#e7e5e4] pb-6 last:border-0">
                      <div className="flex justify-between items-start mb-3">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-[#fafaf9] flex items-center justify-center font-semibold text-[#b45309] text-[14px]">{initials}</div>
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-semibold text-[13px] text-[#1c1917]">{displayName}</span>
                              {isOwner && <span className="bg-[#0c0a09] text-white text-[10px] px-1.5 py-0.5 rounded font-medium">You</span>}
                              {verified && (
                                <span className="bg-[#b45309]/10 text-[#b45309] text-[10px] px-1.5 py-0.5 rounded flex items-center gap-1 font-medium">
                                  <span className="material-symbols-outlined text-[12px]">check_circle</span> Verified Buyer
                                </span>
                              )}
                            </div>
                            <div className="flex text-[#b45309] text-[14px] mt-0.5">
                              {[...Array(5)].map((_, i) => (
                                <span key={i} className="material-symbols-outlined text-[14px]" style={{ fontVariationSettings: `'FILL' ${i < stars ? 1 : 0}` }}>star</span>
                              ))}
                            </div>
                          </div>
                        </div>
                        <span className="text-[13px] text-[#57534e]">{date}</span>
                      </div>
                      {isEditing ? (
                        <div className="bg-[#fafaf9] border border-[#d6d3d1] rounded-xl p-3 space-y-3">
                          <div className="flex items-center gap-1">
                            {[1,2,3,4,5].map(s=> (
                              <button key={s} onClick={()=>setEditRating(s)} className={`p-1 ${s<=editRating ? "text-[#b45309]" : "text-[#a8a29e]"}`}>
                                <span className="material-symbols-outlined text-[20px]" style={{fontVariationSettings:`'FILL' ${s<=editRating?1:0}`}}>star</span>
                              </button>
                            ))}
                            <span className="ml-2 text-[12px] text-[#57534e]">{editRating}/5</span>
                          </div>
                          <textarea value={editComment} onChange={e=>setEditComment(e.target.value)} rows={3} className="w-full bg-white border border-[#d6d3d1] rounded-xl px-3 py-2 text-[13px] focus:border-[#b45309] outline-none resize-none" placeholder="Edit your review..." />
                          {editError && <p className="text-[12px] text-[#b91c1c]">{editError}</p>}
                          <div className="flex gap-2">
                            <button onClick={()=>handleSaveEdit(rev)} disabled={editSaving} className="px-4 py-2 bg-[#b45309] text-white rounded-xl text-[12px] font-semibold hover:bg-[#92400e] disabled:opacity-50 flex items-center gap-1">
                              {editSaving && <span className="material-symbols-outlined animate-spin text-[14px]">progress_activity</span>} Save
                            </button>
                            <button onClick={handleCancelEdit} disabled={editSaving} className="px-4 py-2 border border-[#d6d3d1] rounded-xl text-[12px] font-semibold hover:bg-white">Cancel</button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p className="text-[14px] leading-relaxed text-[#57534e]">{text || "— no comment —"}</p>
                          <div className="mt-3 flex gap-3 text-[12px] text-[#57534e] items-center">
                            <button className="hover:text-[#b45309] flex items-center gap-1"><span className="material-symbols-outlined text-[14px]">thumb_up</span> Helpful</button>
                            <button className="hover:text-[#b45309]">Report</button>
                            {isOwner && (
                              <span className="ml-auto flex gap-1">
                                <button onClick={()=>handleStartEdit(rev)} className="px-2.5 py-1 rounded-lg bg-[#fafaf9] border border-[#d6d3d1] hover:border-[#b45309] hover:text-[#b45309] text-[11px] font-semibold flex items-center gap-1">
                                  <span className="material-symbols-outlined text-[14px]">edit</span> Edit
                                </button>
                                <button onClick={()=>handleDeleteOwn(rev)} className="px-2.5 py-1 rounded-lg bg-[#fef2f2] border border-[#fecaca] hover:bg-[#fee2e2] text-[#b91c1c] text-[11px] font-semibold flex items-center gap-1">
                                  <span className="material-symbols-outlined text-[14px]">delete</span> Delete
                                </button>
                              </span>
                            )}
                          </div>
                        </>
                      )}
                    </div>
                    );
                  })
                ) : (
                  <p className="text-[#57534e] text-[14px]">No reviews yet. Be the first to review this product!</p>
                )}
              </div>
            </div>
          )}
        </div>
      </main>
      <Footer />
    </div>
  );
}
