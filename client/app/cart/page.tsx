"use client";

import { formatNPR } from "@/lib/format";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context/AuthContext";
import { useWishlist } from "@/context/WishlistContext";

const formatUSD = formatNPR;
export default function CartPage() {
  return <CartInner />;
}

function CartInner() {
  const { cart, loading, setQty, removeFromCart, clearCart } = useCart();
  const { isAuthenticated } = useAuth();
  const { toggle: wishlistToggle, isWishlisted } = useWishlist();
  const router = useRouter();
  const [savedMap, setSavedMap] = useState<Record<string, boolean>>({});
  const [wishBusy, setWishBusy] = useState<string | null>(null);
  // visible cart excludes saved-for-later items from checkout totals
  const visibleCart = cart.filter(i => !savedMap[i.id]);
  const subtotal = visibleCart.reduce((s,i)=> s + i.price * (i.qty ?? i.quantity ?? 0),0);
  const visibleCount = visibleCart.reduce((n,i)=> n + (i.qty ?? i.quantity ?? 0),0);

  const isEmpty = !loading && cart.length === 0;

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
        <Navbar />
        <main className="flex-grow flex items-center justify-center">
          <span className="material-symbols-outlined animate-spin text-[#b45309] text-[32px]">progress_activity</span>
        </main>
        <Footer />
      </div>
    );
  }

  if (isEmpty) {
    return (
      <div className="min-h-screen flex flex-col bg-[#fafaf9]">
        <Navbar />
        <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1440px] mx-auto py-xl md:py-xxl">
          <div className="mb-lg">
            <h1 className="font-headline-lg text-headline-lg text-[#1c1917] mb-xs font-semibold tracking-tight">Shopping Cart</h1>
            <p className="font-body-md text-body-md text-[#57534e]">Review your items and proceed to checkout.</p>
          </div>
          <div className="flex flex-col lg:flex-row gap-gutter">
            <div className="flex-grow w-full lg:w-2/3 animate-fade-in-up">
              <div className="glass-card rounded-3xl p-xl flex flex-col items-center justify-center text-center h-[400px]">
                <div className="w-24 h-24 bg-[#b45309]/10 rounded-full flex items-center justify-center mb-6">
                  <span className="material-symbols-outlined text-[#b45309]" style={{ fontSize: "48px" }}>shopping_cart</span>
                </div>
                <h3 className="font-headline-md text-headline-md text-[#1c1917] mb-3 font-semibold">Your cart is empty</h3>
                <p className="font-body-md text-body-md text-[#57534e] mb-8 max-w-[40ch]">Looks like you haven&apos;t added anything to your cart yet. Discover our latest products and exclusive deals!</p>
                <div className="flex gap-3 justify-center">
                  <Link href="/catalog" className="bg-[#b45309] text-white font-label-md text-[15px] px-8 py-4 rounded-full hover:bg-[#92400e] transition-all shadow-lg hover:shadow-xl hover:-translate-y-1 inline-flex items-center justify-center gap-2">
                    Start Shopping <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                  </Link>
                  <Link href="/wishlist" className="border border-[#d6d3d1] text-[#1c1917] font-label-md text-[15px] px-8 py-4 rounded-full hover:bg-[#fafaf9] transition-colors inline-flex items-center justify-center gap-2">
                    <span className="material-symbols-outlined text-[18px]">favorite</span> Wishlist
                  </Link>
                </div>
              </div>
            </div>
            <div className="w-full lg:w-1/3 animate-fade-in-up" style={{ animationDelay: '0.1s' }}>
              <div className="glass-card rounded-3xl p-8 sticky top-24">
                <h2 className="font-headline-md text-headline-md text-[#1c1917] mb-6 font-semibold">Order Summary</h2>
                <div className="space-y-4 mb-6 pb-6 border-b border-[#e7e5e4]">
                  <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Subtotal</span><span className="font-medium">{formatUSD(0)}</span></div>
                  <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Shipping</span><span className="text-[#b45309] font-medium">Free</span></div>
                  <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Estimated Tax</span><span className="font-medium text-[#57534e]">Calculated at checkout</span></div>
                </div>
                <div className="flex justify-between items-end mb-8">
                  <span className="font-headline-md text-[18px] text-[#1c1917] font-semibold">Total</span>
                  <div className="text-right"><span className="font-bold text-[36px] leading-none text-[#b45309] block tracking-tighter">{formatUSD(0)}</span></div>
                </div>
                <button disabled className="w-full bg-[#fafaf9] text-[#57534e] font-label-md text-[15px] py-4 rounded-full cursor-not-allowed flex items-center justify-center gap-2">Proceed to Checkout <span className="material-symbols-outlined text-[18px]">lock</span></button>
                {!isAuthenticated && <p className="text-[12px] text-[#57534e] text-center mt-3">Sign in to checkout faster and track orders.</p>}
              </div>
            </div>
          </div>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-[#fafaf9] selection:bg-[#fef3c7] selection:text-white-container">
      <Navbar />
      <main className="flex-grow w-full px-margin-mobile md:px-margin-desktop max-w-[1440px] mx-auto py-xl md:py-xxl">
        <div className="mb-lg">
          <h1 className="font-headline-lg text-headline-lg text-[#1c1917] mb-xs font-semibold tracking-tight">Shopping Cart</h1>
          <p className="font-body-md text-body-md text-[#57534e]">Review your items and proceed to checkout. <span className="font-medium text-[#1c1917]">({visibleCart.length} {visibleCart.length === 1 ? "item" : "items"})</span></p>
        </div>

        <div className="flex flex-col lg:flex-row gap-gutter">
          <div className="flex-grow flex flex-col gap-4 w-full lg:w-2/3">
            {cart.map((item) => {
              const isSaved = !!savedMap[item.id];
              const wishlisted = isWishlisted(item.slug);
              const busy = wishBusy === item.id;
              if (isSaved) {
                return (
                  <div key={item.id} className="bg-[#fafaf9] rounded-xl p-3 flex items-center justify-between border border-[#d6d3d1]">
                    <p className="font-body-sm text-body-sm text-[#1c1917] flex items-center gap-2">
                      <span className="material-symbols-outlined text-[#b45309] text-[18px]">bookmark_added</span>
                      Saved for later — <span className="font-medium">{item.name}</span>
                      {wishlisted && <span className="text-[11px] bg-[#fee2e2] text-[#b91c1c] px-1.5 py-0.5 rounded-full font-semibold">♥ Wishlist</span>}
                    </p>
                    <div className="flex gap-2">
                      <button onClick={() => setSavedMap((m) => ({ ...m, [item.id]: false }))} className="font-label-md text-label-md text-[#b45309] hover:underline">Move back</button>
                      <Link href="/wishlist" className="font-label-md text-label-md text-[#b45309] hover:underline">View wishlist</Link>
                    </div>
                  </div>
                );
              }
              return (
                <div key={item.id} className="glass-card rounded-2xl p-4 md:p-6 flex flex-col sm:flex-row gap-6 shadow-sm hover:shadow-md transition-all duration-300">
                  <Link href={`/product/${item.slug}`} className="w-full sm:w-40 h-40 bg-[#fafaf9] rounded-lg overflow-hidden shrink-0 relative flex items-center justify-center">
                    {item.image ? (
                      <Image
                        src={item.image}
                        alt={item.name}
                        fill
                        unoptimized
                        className="object-cover"
                        sizes="160px"
                        onError={(e) => {
                          (e.target as HTMLImageElement).style.display = "none";
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex flex-col items-center justify-center gap-1">
                        <span className="material-symbols-outlined text-[#a8a29e] text-[36px]">image</span>
                        <span className="text-[10px] text-[#a8a29e]">No image</span>
                      </div>
                    )}
                  </Link>
                  <div className="flex-grow flex flex-col justify-between min-w-0">
                    <div>
                      <div className="flex justify-between items-start mb-xs gap-3">
                        <div className="min-w-0">
                          <Link href={`/product/${item.slug}`} className="font-headline-md text-headline-md text-[#1c1917] font-semibold leading-none hover:text-[#b45309]">
                            {item.name}
                          </Link>
                        </div>
                        <span className="font-headline-md text-headline-md text-[#b45309] font-semibold shrink-0">{formatUSD(item.price)}</span>
                      </div>
                    </div>
                    <div className="flex items-center justify-between mt-4 sm:mt-0 gap-3 flex-wrap">
                      <div className="flex items-center border border-[#d6d3d1] rounded-lg bg-white shrink-0">
                        <button aria-label="Decrease" onClick={() => setQty(item.id, Math.max(1, (item.qty ?? item.quantity ?? 1) - 1))} className="w-8 h-8 flex items-center justify-center text-[#57534e] hover:text-[#b45309] disabled:opacity-40">
                          <span className="material-symbols-outlined text-[18px]">remove</span>
                        </button>
                        <span className="w-8 text-center font-label-md text-label-md text-[#1c1917] font-semibold">{item.qty ?? item.quantity ?? 1}</span>
                        <button aria-label="Increase" onClick={() => { const cur = item.qty ?? item.quantity ?? 1; if (cur < 99) setQty(item.id, cur + 1); }} className="w-8 h-8 flex items-center justify-center text-[#57534e] hover:text-[#b45309] disabled:opacity-40">
                          <span className="material-symbols-outlined text-[18px]">add</span>
                        </button>
                      </div>
                      <div className="flex gap-3 md:gap-sm">
                        <button
                          disabled={busy}
                          onClick={async () => {
                            if (!isAuthenticated) {
                              router.push(`/login?next=${encodeURIComponent("/cart")}`);
                              return;
                            }
                            if (busy) return;
                            setWishBusy(item.id);
                            try {
                              await wishlistToggle(item.slug);
                              setSavedMap((m) => ({ ...m, [item.id]: true }));
                            } catch (err: unknown) {
                              if ((err as { status?: number })?.status === 401) router.push(`/login?next=${encodeURIComponent("/cart")}`);
                            } finally {
                              setWishBusy(null);
                            }
                          }}
                          className={`font-label-md text-label-md flex items-center gap-1.5 disabled:opacity-50 ${wishlisted ? "text-[#b91c1c]" : "text-[#57534e] hover:text-[#b45309]"}`}
                        >
                          {busy ? <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span> : <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: `'FILL' ${wishlisted ? 1 : 0}` }}>{wishlisted ? "favorite" : "bookmark"}</span>} <span className="hidden sm:inline">{wishlisted ? "Wishlisted" : "Save for later"}</span>
                        </button>
                        <button onClick={() => removeFromCart(item.id)} className="text-[#b91c1c] hover:text-on-error-container font-label-md text-label-md flex items-center gap-1.5">
                          <span className="material-symbols-outlined text-[18px]">delete</span> <span className="hidden sm:inline">Remove</span>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
            <button onClick={() => { if (confirm("Clear cart?")) clearCart(); }} className="self-start text-[13px] text-[#57534e] hover:text-[#b91c1c] underline">Clear cart</button>
          </div>

          <div className="w-full lg:w-1/3">
            <div className="glass-card rounded-3xl p-6 md:p-8 sticky top-24">
              <h2 className="font-headline-md text-headline-md text-[#1c1917] mb-6 font-semibold">Order Summary</h2>
              <div className="space-y-4 mb-6 pb-6 border-b border-[#e7e5e4]">
                <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Subtotal ({visibleCount} items)</span><span className="font-medium">{formatUSD(subtotal)}</span></div>
                <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Shipping</span><span className="text-[#b45309] font-medium">Free</span></div>
                <div className="flex justify-between font-body-md text-[15px] text-[#1c1917]"><span>Estimated Tax</span><span className="font-medium text-[#57534e]">Calculated at checkout</span></div>
              </div>
              <div className="flex justify-between items-end mb-8">
                <span className="font-headline-md text-[18px] text-[#1c1917] font-semibold">Total</span>
                <div className="text-right"><span className="font-bold text-[32px] md:text-[40px] leading-none text-[#b45309] block tracking-tighter">{formatUSD(subtotal)}</span></div>
              </div>
              {isAuthenticated ? (
                <Link href="/checkout/shipping" className="w-full bg-[#b45309] text-white font-label-md text-[15px] py-4 rounded-full hover:bg-[#92400e] transition-all shadow-lg hover:shadow-xl hover:-translate-y-1 flex items-center justify-center gap-2 group">
                  Proceed to Checkout <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform text-[20px]">arrow_forward</span>
                </Link>
              ) : (
                <Link href={`/login?next=${encodeURIComponent("/checkout/shipping")}`} className="w-full bg-[#b45309] text-white font-label-md text-[15px] py-4 rounded-full hover:bg-[#92400e] transition-all shadow-lg hover:shadow-xl hover:-translate-y-1 flex items-center justify-center gap-2 group">
                  Sign in to Checkout <span className="material-symbols-outlined group-hover:translate-x-1 transition-transform text-[20px]">arrow_forward</span>
                </Link>
              )}
              <div className="mt-6 text-center">
                <p className="font-label-sm text-[12px] text-[#57534e] flex items-center justify-center gap-1.5"><span className="material-symbols-outlined text-[16px]">lock</span> Secure encrypted checkout</p>
              </div>
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
