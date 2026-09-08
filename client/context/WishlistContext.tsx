"use client";

import { createContext, useContext, useCallback, useEffect, useState, ReactNode, useRef } from "react";
import { useAuth } from "@/context/AuthContext";
import {
  getWishlist,
  toggleWishlist as toggleWishlistLib,
  addToWishlist as addToWishlistLib,
  removeFromWishlistByProductId,
  isWishlistedSync,
  clearWishlistCache,
} from "@/lib/wishlist";
import type { WishlistItem } from "@/lib/types";

type WishlistContextType = {
  items: WishlistItem[];
  ids: string[];
  count: number;
  loading: boolean;
  refreshing: boolean;
  isWishlisted: (productId: string) => boolean;
  toggle: (productId: string) => Promise<WishlistItem[]>;
  add: (productId: string) => Promise<WishlistItem | null>;
  remove: (productId: string) => Promise<void>;
  refresh: (force?: boolean) => Promise<WishlistItem[]>;
};

const WishlistContext = createContext<WishlistContextType | null>(null);

export function WishlistProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, user, loading: authLoading } = useAuth();
  const [items, setItems] = useState<WishlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const authRef = useRef(isAuthenticated);
  authRef.current = isAuthenticated;

  const refresh = useCallback(
    async (force = false): Promise<WishlistItem[]> => {
      if (!authRef.current) {
        setItems([]);
        setLoading(false);
        return [];
      }
      setRefreshing(true);
      try {
        const list = await getWishlist({ force });
        setItems(list);
        return list;
      } catch {
        return items;
      } finally {
        setRefreshing(false);
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (authLoading) return;
    if (!isAuthenticated) {
      clearWishlistCache();
      setItems([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    refresh(false).catch(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated, user?.id, authLoading]);

  useEffect(() => {
    const handler = async () => {
      if (!authRef.current) {
        setItems([]);
        return;
      }
      try {
        const list = await getWishlist({ force: false }).catch(() => [] as WishlistItem[]);
        setItems(list);
      } catch {
      }
    };
    window.addEventListener("apexcommerce:wishlist:updated", handler as EventListener);
    window.addEventListener("focus", handler as EventListener);
    return () => {
      window.removeEventListener("apexcommerce:wishlist:updated", handler as EventListener);
      window.removeEventListener("focus", handler as EventListener);
    };
  }, []);

  useEffect(() => {
    const onInvalid = () => {
      clearWishlistCache();
      setItems([]);
    };
    window.addEventListener("apexcommerce:auth:invalid", onInvalid);
    return () => window.removeEventListener("apexcommerce:auth:invalid", onInvalid);
  }, []);

  const isWishlisted = useCallback(
    (productId: string) => {
      if (!productId) return false;
      if (items.length > 0 || loading === false) {
        return items.some((w) => (w.productId || w.product_id) === productId);
      }
      return isWishlistedSync(productId);
    },
    [items, loading]
  );

  const toggle = useCallback(
    async (productId: string): Promise<WishlistItem[]> => {
      if (!productId) throw new Error("Product ID is required");
      if (!authRef.current) {
        const err = new Error("Please sign in to use wishlist") as Error & { status?: number };
        err.status = 401;
        throw err;
      }
      const wasWishlisted = items.some((w) => (w.productId || w.product_id) === productId);
      const optimistic: WishlistItem[] = wasWishlisted
        ? items.filter((w) => (w.productId || w.product_id) !== productId)
        : [
            ...items,
            {
              id: `optimistic-${productId}`,
              product_id: productId,
              productId,
              slug: productId,
              addedAt: new Date().toISOString(),
              createdAt: new Date().toISOString(),
            } as WishlistItem,
          ];
      setItems(optimistic);

      try {
        const next = await toggleWishlistLib(productId);
        setItems(next);
        return next;
      } catch (e) {
        setItems(items);
        throw e;
      }
    },
    [items]
  );

  const add = useCallback(
    async (productId: string) => {
      if (!authRef.current) {
        const err = new Error("Please sign in to use wishlist") as Error & { status?: number };
        err.status = 401;
        throw err;
      }
      const already = items.some((w) => (w.productId || w.product_id) === productId);
      if (already) return items.find((w) => (w.productId || w.product_id) === productId) || null;

      const optimistic: WishlistItem = {
        id: `optimistic-${productId}`,
        product_id: productId,
        productId,
        slug: productId,
        addedAt: new Date().toISOString(),
        createdAt: new Date().toISOString(),
      };
      setItems((prev) => [...prev, optimistic]);
      try {
        const saved = await addToWishlistLib(productId);
        const fresh = await getWishlist({ force: true });
        setItems(fresh);
        return saved;
      } catch (e) {
        setItems((prev) => prev.filter((w) => w.productId !== productId && w.product_id !== productId));
        throw e;
      }
    },
    [items]
  );

  const remove = useCallback(
    async (productId: string) => {
      const optimistic = items.filter((w) => (w.productId || w.product_id) !== productId);
      setItems(optimistic);
      try {
        await removeFromWishlistByProductId(productId);
        const fresh = await getWishlist({ force: false }).catch(() => optimistic);
        setItems(fresh);
      } catch (e) {
        setItems(items);
        throw e;
      }
    },
    [items]
  );

  const ids = items.map((w) => w.productId || w.product_id).filter(Boolean) as string[];

  return (
    <WishlistContext.Provider
      value={{
        items,
        ids,
        count: items.length,
        loading,
        refreshing,
        isWishlisted,
        toggle,
        add,
        remove,
        refresh,
      }}
    >
      {children}
    </WishlistContext.Provider>
  );
}

export function useWishlist() {
  const ctx = useContext(WishlistContext);
  if (!ctx) throw new Error("useWishlist must be used within WishlistProvider");
  return ctx;
}
