"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import {
  getCart,
  addToCart as addServerCart,
  updateQty as updateServerQty,
  removeFromCart as removeServerCart,
  clearCart as clearServerCart,
  calcSubtotal,
  calcCount,
} from "@/lib/cart";
import {
  getGuestCart,
  addGuestItem,
  updateGuestQty,
  removeGuestItem,
  clearGuestCart,
  mergeGuestCartToServer,
} from "@/lib/guestCart";
import type { CartItem } from "@/lib/types";
import { useAuth } from "@/context/AuthContext";

type AddInput = Omit<CartItem, "qty" | "quantity"> & { qty?: number; quantity?: number };

export function useCart() {
  const { isAuthenticated } = useAuth();
  const authRef = useRef(isAuthenticated);
  authRef.current = isAuthenticated;

  const [cart, setCart] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (authRef.current) {
      setLoading(true);
      try {
        const guestItems = getGuestCart();
        if (guestItems.length > 0) {
          try {
            await mergeGuestCartToServer();
          } catch {
          }
        }
        setCart(await getCart());
      } catch {
        setCart([]);
      } finally {
        setLoading(false);
      }
    } else {
      setCart(getGuestCart());
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh, isAuthenticated]);

  useEffect(() => {
    if (authRef.current) return;
    const onStorage = (e: StorageEvent) => {
      if (e.key === "apexcommerce_guest_cart" || e.key === null) {
        setCart(getGuestCart());
      }
    };
    const onCustom = () => {
      if (!authRef.current) setCart(getGuestCart());
    };
    window.addEventListener("storage", onStorage);
    window.addEventListener("apexcommerce_guest_cart_updated" as never, onCustom as never);
    return () => {
      window.removeEventListener("storage", onStorage);
      window.removeEventListener("apexcommerce_guest_cart_updated" as never, onCustom as never);
    };
  }, [isAuthenticated]);

  const addToCart = useCallback(async (item: AddInput) => {
    const qtyRaw = item.qty ?? item.quantity ?? 1;
    const qty = Math.max(1, Math.min(99, qtyRaw));
    if (authRef.current) {
      const pid = item.product_id || (item as any).productId || item.id || item.slug || "";
      const optimisticItem: CartItem = {
        id: pid || `tmp-${Date.now()}`,
        product_id: pid,
        productId: pid,
        slug: item.slug || pid,
        name: item.name,
        price: item.price,
        image: item.image || "",
        qty,
        quantity: qty,
        subtotal: item.price * qty,
      };
      const prev = cart;
      const existingIdx = prev.findIndex((c) => (c.product_id || (c as any).productId || c.slug) === pid);
      if (existingIdx >= 0) {
        const copy = [...prev];
        const ex = copy[existingIdx];
        const newQty = Math.min(99, (ex.qty ?? ex.quantity ?? 0) + qty);
        copy[existingIdx] = { ...ex, qty: newQty, quantity: newQty, subtotal: ex.price * newQty };
        setCart(copy);
      } else {
        setCart([...prev, optimisticItem]);
      }
      try {
        const next = await addServerCart({ ...item, qty, quantity: qty } as any);
        setCart(next);
        return next;
      } catch (e) {
        setCart(prev);
        throw e;
      }
    }
    const next = addGuestItem({ ...item, qty, quantity: qty } as any);
    setCart(next);
    return next;
  }, [cart]);

  const removeFromCart = useCallback(async (id: string) => {
    if (authRef.current) {
      const next = await removeServerCart(id);
      setCart(next);
      return next;
    }
    const next = removeGuestItem(id);
    setCart(next);
    return next;
  }, []);

  const setQty = useCallback(async (id: string, qty: number) => {
    const safeQty = Math.max(0, Math.min(99, qty));
    if (safeQty <= 0) return removeFromCart(id);
    if (authRef.current) {
      const next = await updateServerQty(id, safeQty);
      setCart(next);
      return next;
    }
    const next = updateGuestQty(id, safeQty);
    setCart(next);
    return next;
  }, [removeFromCart]);

  const clearCart = useCallback(async () => {
    try {
      if (authRef.current) {
        await clearServerCart();
      } else {
        clearGuestCart();
      }
      setCart([]);
    } catch {
      try { await refresh(); } catch {}
      throw new Error("Failed to clear cart");
    }
  }, [refresh]);

  return {
    cart,
    loading,
    count: calcCount(cart),
    subtotal: calcSubtotal(cart),
    addToCart,
    setQty,
    removeFromCart,
    clearCart,
    refresh,
  };
}
