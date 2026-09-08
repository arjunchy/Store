"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context/AuthContext";
import { useWishlist } from "@/context/WishlistContext";

export default function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const pathname = usePathname();
  const router = useRouter();
  const isCart = pathname === "/cart";
  const isHome = pathname === "/";
  const { user, logout, isAdmin } = useAuth();

  const isCatalog = pathname === "/catalog" || pathname?.startsWith("/catalog");
  const isNewArrivals = pathname === "/new-arrivals" || pathname?.startsWith("/new-arrivals");
  const navLinks = [
    { label: "Home", href: "/", active: isHome },
    { label: "Catalog", href: "/catalog", active: !!isCatalog },
    { label: "New Arrivals", href: "/new-arrivals", active: !!isNewArrivals },
  ];

  const { count: cartCount } = useCart();
  const { count: wishlistCount } = useWishlist();
  const [searchQuery, setSearchQuery] = useState("");
  const [cartPulse, setCartPulse] = useState(false);
  const prevCartCountRef = useRef(cartCount);
  useEffect(() => {
    if (cartCount > prevCartCountRef.current) {
      setCartPulse(true);
      const t = setTimeout(() => setCartPulse(false), 600);
      return () => clearTimeout(t);
    }
    prevCartCountRef.current = cartCount;
  }, [cartCount]);

  const handleSearch = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const q = searchQuery.trim();
    if (!q) {
      router.push("/catalog");
    } else {
      router.push(`/catalog?q=${encodeURIComponent(q)}`);
    }
    setMobileOpen(false);
  };

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    };
    if (userMenuOpen) document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [userMenuOpen]);

  useEffect(() => {
    setMobileOpen(false);
    setUserMenuOpen(false);
  }, [pathname]);

  const handleLogout = async () => {
    await logout();
    setUserMenuOpen(false);
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-50 w-full bg-[#0c0a09] border-b border-[#b45309]/20 smooth-transition">
      <div className="flex justify-between items-center w-full px-margin-mobile md:px-margin-desktop py-sm max-w-[1440px] mx-auto">
        <Link
          href="/"
          className="font-headline-md text-[22px] md:text-headline-md font-bold text-[#b45309] tracking-tight leading-none shrink-0"
        >
          ApexCommerce
        </Link>

        <nav className="hidden md:flex gap-gutter items-center">
          {navLinks.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className={
                link.active
                  ? "font-body-md text-body-md text-[#b45309] border-b-2 border-[#b45309] pb-1 font-medium"
                  : "font-body-md text-body-md text-[#fafaf9]/80 hover:text-[#b45309] transition-colors pb-1 border-b-2 border-transparent"
              }
            >
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-1 md:gap-sm">
          <form onSubmit={handleSearch} className="relative hidden md:block">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px] pointer-events-none">
              search
            </span>
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-[#1c1917] border border-[#292524] rounded-xl pl-10 pr-4 py-2 font-body-sm text-body-sm text-[#fafaf9] placeholder:text-[#a8a29e] focus:ring-2 focus:ring-[#b45309] focus:border-[#b45309] w-64 transition-all outline-none"
              placeholder="Search catalog..."
              type="text"
              aria-label="Search"
            />
          </form>

          <button
            aria-label="Search"
            onClick={() => router.push("/catalog")}
            className="md:hidden text-[#fafaf9] hover:text-[#b45309] transition-colors flex items-center justify-center p-2 rounded-full hover:bg-[#1c1917] w-9 h-9"
          >
            <span className="material-symbols-outlined text-[20px]">search</span>
          </button>

          <Link
            href="/wishlist"
            aria-label={`Wishlist with ${wishlistCount} items`}
            className={`p-2 rounded-full flex items-center justify-center w-9 h-9 relative transition-colors ${pathname === "/wishlist" ? "text-[#b45309] bg-[#b45309]/20" : "text-[#fafaf9] hover:text-[#b45309] hover:bg-[#1c1917]"}`}
          >
            <span
              className="material-symbols-outlined text-[20px]"
              style={{ fontVariationSettings: `'FILL' ${wishlistCount > 0 ? 1 : 0}` }}
            >
              favorite
            </span>
            {wishlistCount > 0 && (
              <span className="absolute top-0 right-0 bg-[#b45309] text-white font-label-sm text-[10px] leading-none rounded-full h-4 min-w-4 px-1 flex items-center justify-center translate-x-1 -translate-y-1 font-medium">
                {wishlistCount > 99 ? "99+" : wishlistCount}
              </span>
            )}
          </Link>

          <Link
            href="/cart"
            aria-label={`Shopping cart with ${cartCount} items`}
            className={
              isCart
                ? "p-2 text-[#b45309] border-b-2 border-[#b45309] flex items-center justify-center relative w-9 h-9"
                : "p-2 text-[#fafaf9] hover:text-[#b45309] transition-colors rounded-full hover:bg-[#1c1917] flex items-center justify-center relative w-9 h-9"
            }
          >
            <span
              className={`material-symbols-outlined text-[20px] transition-transform ${cartPulse ? "animate-[bounce_0.5s_ease]" : ""}`}
              style={{ fontVariationSettings: `'FILL' ${isCart || cartPulse ? 1 : 0}` }}
            >
              shopping_cart
            </span>
            {cartCount > 0 && (
              <span className={`absolute top-0 right-0 bg-[#b45309] text-white font-label-sm text-[10px] leading-none rounded-full h-4 w-4 flex items-center justify-center translate-x-1 -translate-y-1 font-medium transition-transform ${cartPulse ? "scale-125" : "scale-100"}`}>
                {cartCount}
              </span>
            )}
          </Link>

          {user ? (
            <div className="relative" ref={userMenuRef}>
              <button
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                aria-label="Account menu"
                className="hidden md:flex p-2 text-[#fafaf9] hover:text-[#b45309] transition-colors rounded-full hover:bg-[#1c1917] items-center justify-center w-9 h-9"
              >
                <span className="material-symbols-outlined text-[20px]">person</span>
              </button>
              {userMenuOpen && (
                <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-xl shadow-lg border border-[#d6d3d1] py-2 z-50">
                  <div className="px-4 py-2 border-b border-[#e7e5e4]">
                    <p className="font-semibold text-[13px] text-[#1c1917] truncate">{user.username}</p>
                    <p className="text-[11px] text-[#57534e] truncate">{user.email}</p>
                  </div>
                  <Link
                    href="/orders"
                    onClick={() => setUserMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2.5 text-[13px] text-[#1c1917] hover:bg-[#fafaf9] hover:text-[#b45309] transition-colors"
                  >
                    <span className="material-symbols-outlined text-[18px] text-[#b45309]">receipt_long</span>
                    My Orders
                  </Link>
                  <Link
                    href="/wishlist"
                    onClick={() => setUserMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2.5 text-[13px] text-[#1c1917] hover:bg-[#fafaf9] hover:text-[#b45309] transition-colors"
                  >
                    <span className="material-symbols-outlined text-[18px] text-[#b45309]">favorite</span>
                    Wishlist
                  </Link>
                  <Link
                    href="/addresses"
                    onClick={() => setUserMenuOpen(false)}
                    className="flex items-center gap-2 px-4 py-2.5 text-[13px] text-[#1c1917] hover:bg-[#fafaf9] hover:text-[#b45309] transition-colors"
                  >
                    <span className="material-symbols-outlined text-[18px] text-[#b45309]">location_on</span>
                    Addresses
                  </Link>
                  {isAdmin && (
                    <Link
                      href="/admin"
                      onClick={() => setUserMenuOpen(false)}
                      className="flex items-center gap-2 px-4 py-2.5 text-[13px] text-[#1c1917] hover:bg-[#fafaf9] hover:text-[#b45309] transition-colors"
                    >
                      <span className="material-symbols-outlined text-[18px] text-[#b45309]">admin_panel_settings</span>
                      Admin Panel
                    </Link>
                  )}
                  <div className="border-t border-[#e7e5e4] mt-1 pt-1">
                    <button
                      onClick={handleLogout}
                      className="flex items-center gap-2 px-4 py-2.5 text-[13px] text-[#b91c1c] hover:bg-[#fef2f2] w-full transition-colors"
                    >
                      <span className="material-symbols-outlined text-[18px]">logout</span>
                      Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <Link
              href="/login"
              className="hidden md:flex p-2 text-[#fafaf9] hover:text-[#b45309] transition-colors rounded-full hover:bg-[#1c1917] items-center justify-center w-9 h-9"
            >
              <span className="material-symbols-outlined text-[20px]">person</span>
            </Link>
          )}

          <button
            aria-label="Toggle menu"
            aria-expanded={mobileOpen}
            onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden text-[#fafaf9] hover:text-[#b45309] flex items-center justify-center p-2 rounded-full hover:bg-[#1c1917] w-9 h-9 ml-1"
          >
            <span className="material-symbols-outlined text-[22px]">
              {mobileOpen ? "close" : "menu"}
            </span>
          </button>
        </div>
      </div>

      {mobileOpen && (
        <div className="md:hidden border-t border-[#292524] bg-[#0c0a09] px-margin-mobile py-md">
          <nav className="flex flex-col gap-1">
            {navLinks.map((link) => (
              <Link
                key={link.label}
                href={link.href}
                onClick={() => setMobileOpen(false)}
                className={`py-3 px-3 rounded-lg font-body-md text-body-md transition-colors ${link.active
                  ? "bg-[#b45309] text-white font-semibold"
                  : "text-[#a8a29e] hover:bg-[#1c1917] hover:text-[#fafaf9]"
                  }`}
              >
                {link.label}
              </Link>
            ))}
            {user ? (
              <>
                <Link
                  href="/orders"
                  onClick={() => setMobileOpen(false)}
                  className="py-3 px-3 rounded-lg font-body-md text-body-md text-[#a8a29e] hover:bg-[#1c1917] hover:text-[#fafaf9] transition-colors"
                >
                  My Orders
                </Link>
                <Link
                  href="/wishlist"
                  onClick={() => setMobileOpen(false)}
                  className="py-3 px-3 rounded-lg font-body-md text-body-md text-[#a8a29e] hover:bg-[#1c1917] hover:text-[#fafaf9] transition-colors"
                >
                  Wishlist
                </Link>
                <button
                  onClick={() => { handleLogout(); setMobileOpen(false); }}
                  className="py-3 px-3 rounded-lg font-body-md text-body-md text-[#b91c1c] hover:bg-[#fef2f2] text-left transition-colors"
                >
                  Sign Out
                </button>
              </>
            ) : (
              <Link
                href="/login"
                onClick={() => setMobileOpen(false)}
                className="py-3 px-3 rounded-lg font-body-md text-body-md text-[#b45309] font-semibold hover:bg-[#b45309]/10 transition-colors"
              >
                Sign In
              </Link>
            )}
          </nav>
          <form onSubmit={handleSearch} className="relative mt-3">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#a8a29e] text-[18px]">
              search
            </span>
            <input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-[#1c1917] border border-[#292524] rounded-xl pl-10 pr-4 py-3 font-body-sm text-body-sm text-[#fafaf9] placeholder:text-[#a8a29e] focus:ring-2 focus:ring-[#b45309] outline-none"
              placeholder="Search catalog..."
              type="text"
            />
          </form>
        </div>
      )}
    </header>
  );
}