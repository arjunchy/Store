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
        const serverCart = await getCart();
        if (serverCart.length === 0) {
          try {
            const backupRaw = localStorage.getItem("apexcommerce_cart_backup");
            const pendingSnapshot = localStorage.getItem("apexcommerce_last_order_snapshot");
            if (backupRaw && pendingSnapshot) {
              const backup = JSON.parse(backupRaw);
              if (Array.isArray(backup) && backup.length > 0) {
                setCart(serverCart);
                setLoading(false);
                return;
              }
            }
          } catch {}
        }
        setCart(serverCart);
      } catch {
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
    const prev = cart;
    try {
      if (authRef.current) {
        await clearServerCart();
        setCart([]);
      } else {
        clearGuestCart();
        setCart([]);
      }
    } catch (e) {
      setCart(prev);
      throw new Error("Failed to clear cart");
    }
  }, [cart]);

  const restoreCart = useCallback(async () => {
    try {
      if (authRef.current) {
        const { apiClient } = await import("@/lib/api-client");
        try {
          const backup = (() => {
            try {
              const raw = localStorage.getItem("apexcommerce_cart_backup");
              return raw ? JSON.parse(raw) : null;
            } catch { return null; }
          })();
          if (backup && Array.isArray(backup) && backup.length > 0) {
            const lastOrderId = localStorage.getItem("apexcommerce_last_order_id") || "";
            const cleanId = lastOrderId.replace(/^#/, "");
            if (cleanId) {
              try {
                const restored = await apiClient.post<any>(`/cart/restore-from-order/${cleanId}`, {}, { auth: true });
                if (restored && restored.items) {
                  setCart(await getCart());
                  return;
                }
              } catch {}
            }
            for (const it of backup) {
              try {
                await addServerCart({ id: it.product_id || it.id, product_id: it.product_id || it.id, name: it.name, price: it.price, image: it.image, qty: it.qty ?? it.quantity ?? 1, quantity: it.qty ?? it.quantity ?? 1 } as any);
              } catch {}
            }
            setCart(await getCart());
            return;
          }
        } catch {}
        setCart(await getCart());
      } else {
        setCart(getGuestCart());
      }
    } catch {
      try { setCart(getGuestCart()); } catch {}
    }
  }, []);

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
    restoreCart,
  };
}
