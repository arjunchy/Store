import { apiClient } from "./api-client";
import type { WishlistItem } from "./types";

const WISHLIST_CACHE_KEY = "apexcommerce_wishlist_cache";
const WISHLIST_CACHE_TIME_KEY = "apexcommerce_wishlist_cache_time";

const CACHE_TTL = 10_000;

type WishlistResponse = {
  id: string;
  productId: string;
  productName: string;
  createdAt: string;
};

let wishlistRequest: Promise<WishlistItem[]> | null = null;

function readWishlistCache(): WishlistItem[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(WISHLIST_CACHE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);

    if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === "object" && parsed[0] !== null && ("productId" in parsed[0] || "product_id" in parsed[0])) {
      return parsed.map((o: any) => ({
        id: String(o.id || ""),
        product_id: String(o.productId || o.product_id || o.slug || ""),
        productId: String(o.productId || o.product_id || o.slug || ""),
        slug: String(o.productId || o.product_id || o.slug || ""),
        addedAt: String(o.addedAt || o.createdAt || ""),
        createdAt: String(o.createdAt || o.addedAt || ""),
      })) as WishlistItem[];
    }

    if (Array.isArray(parsed) && parsed.every((x: unknown) => typeof x === "string")) {
      return (parsed as string[]).map((productId: string) => ({
        id: "",
        product_id: productId,
        productId,
        slug: productId,
        addedAt: "",
        createdAt: "",
      }));
    }

    if (parsed && typeof parsed === "object" && Array.isArray((parsed as any).productIds)) {
      return ((parsed as any).productIds as string[]).map((productId: string) => ({
        id: "",
        product_id: productId,
        productId,
        slug: productId,
        addedAt: "",
        createdAt: "",
      }));
    }

    return [];
  } catch {
    return [];
  }
}

function getCacheTimestamp(): number {
  if (typeof window === "undefined") return 0;
  try {
    const v = localStorage.getItem(WISHLIST_CACHE_TIME_KEY);
    if (!v) return 0;
    const n = Number(v);
    return Number.isFinite(n) ? n : 0;
  } catch {
    return 0;
  }
}

function isCacheFresh(): boolean {
  const ts = getCacheTimestamp();
  if (!ts) return false;
  return Date.now() - ts < CACHE_TTL;
}

function mapWishlistItem(res: WishlistResponse): WishlistItem {
  return {
    id: res.id,
    product_id: res.productId,
    productId: res.productId,
    slug: res.productId,
    addedAt: res.createdAt,
    createdAt: res.createdAt,
  };
}

function writeWishlistCache(list: WishlistItem[]): void {
  if (typeof window === "undefined") return;
  try {
    const clean: WishlistItem[] = list
      .filter((i) => i.productId || i.product_id)
      .map((i) => ({
        id: String(i.id || ""),
        product_id: String(i.productId || i.product_id),
        productId: String(i.productId || i.product_id),
        slug: String(i.productId || i.product_id),
        addedAt: String(i.addedAt || i.createdAt || ""),
        createdAt: String(i.createdAt || i.addedAt || ""),
      }));
    localStorage.setItem(WISHLIST_CACHE_KEY, JSON.stringify(clean));
    localStorage.setItem(WISHLIST_CACHE_TIME_KEY, String(Date.now()));
  } catch {
  }
}

function notifyWishlistChanged(): void {
  if (typeof window === "undefined") return;
  try {
    window.dispatchEvent(new Event("apexcommerce:wishlist:updated"));
  } catch {
  }
}

export function clearWishlistCache(): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(WISHLIST_CACHE_KEY);
    localStorage.removeItem(WISHLIST_CACHE_TIME_KEY);
    notifyWishlistChanged();
  } catch {
  }
}

export async function getWishlist(options?: { force?: boolean }): Promise<WishlistItem[]> {
  const force = options?.force ?? false;

  if (wishlistRequest) {
    return wishlistRequest;
  }

  if (!force && isCacheFresh()) {
    return readWishlistCache();
  }

  wishlistRequest = (async () => {
    try {
      const response = await apiClient.get<WishlistResponse[]>("/wishlist", { auth: true });
      const list = response.map(mapWishlistItem);
      writeWishlistCache(list);
      return list;
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) {
        writeWishlistCache([]);
        return [];
      }
      throw err;
    } finally {
      wishlistRequest = null;
    }
  })();

  return wishlistRequest;
}

export async function addToWishlist(productId: string): Promise<WishlistItem | null> {
  if (!productId) throw new Error("Product ID is required");

  try {
    const response = await apiClient.post<WishlistResponse>("/wishlist", { productId }, { auth: true });
    const item = mapWishlistItem(response);
    const current = readWishlistCache();
    const exists = current.some((w) => (w.productId || w.product_id) === productId);
    if (!exists) {
      writeWishlistCache([...current.filter((w) => (w.productId || w.product_id) !== productId), item]);
    }
    const deduped = readWishlistCache();
    const map = new Map<string, WishlistItem>();
    for (const w of deduped) {
      const pid = w.productId || w.product_id;
      map.set(pid, w);
    }
    writeWishlistCache(Array.from(map.values()));
    notifyWishlistChanged();
    return item;
  } catch (err: unknown) {
    const status = (err as { status?: number })?.status;
    const msg = (err as Error)?.message || "";
    if (status === 400 && msg.toLowerCase().includes("already in wishlist")) {
      try {
        await getWishlist({ force: true });
      } catch {}
      const fresh = readWishlistCache();
      notifyWishlistChanged();
      return fresh.find((w) => (w.productId || w.product_id) === productId) || null;
    }
    throw err;
  }
}

export async function removeFromWishlist(wishlistId: string): Promise<void> {
  if (!wishlistId) throw new Error("Wishlist ID is required");

  const current = readWishlistCache();
  const item = current.find((w) => w.id === wishlistId);

  await apiClient.delete(`/wishlist/${wishlistId}`, { auth: true });

  if (item) {
    const pid = item.productId || item.product_id;
    writeWishlistCache(current.filter((w) => w.id !== wishlistId && (w.productId || w.product_id) !== pid));
  } else {
    const next = current.filter((w) => w.id !== wishlistId);
    if (next.length !== current.length) {
      writeWishlistCache(next);
    } else {
      clearWishlistCache();
      return;
    }
  }
  notifyWishlistChanged();
}

export async function removeFromWishlistByProductId(productId: string): Promise<void> {
  if (!productId) throw new Error("Product ID is required");

  let list = readWishlistCache();
  if (!isCacheFresh() || !list.some((w) => w.id)) {
    try {
      list = await getWishlist({ force: true });
    } catch {
      list = readWishlistCache();
    }
  }

  const existing = list.find((w) => (w.productId || w.product_id) === productId);
  if (!existing) {
    const cleaned = list.filter((w) => (w.productId || w.product_id) !== productId);
    if (cleaned.length !== list.length) {
      writeWishlistCache(cleaned);
      notifyWishlistChanged();
    }
    return;
  }

  if (existing.id) {
    await removeFromWishlist(existing.id);
  } else {
    writeWishlistCache(list.filter((w) => (w.productId || w.product_id) !== productId));
    notifyWishlistChanged();
    try {
      await getWishlist({ force: true });
    } catch {}
  }
}

export async function toggleWishlist(productId: string): Promise<WishlistItem[]> {
  if (!productId) throw new Error("Product ID is required");

  let list = readWishlistCache();

  const hasMissingIds = list.length > 0 && list.some((w) => !w.id);
  if (!isCacheFresh() || hasMissingIds) {
    try {
      list = await getWishlist({ force: hasMissingIds ? true : false });
    } catch {
      list = readWishlistCache();
    }
  }

  const existing = list.find((item) => (item.productId || item.product_id) === productId);

  if (existing) {
    if (existing.id) {
      await removeFromWishlist(existing.id);
    } else {
      await removeFromWishlistByProductId(productId);
    }
  } else {
    await addToWishlist(productId);
  }

  return readWishlistCache();
}

export async function isWishlisted(productId: string): Promise<boolean> {
  if (!productId) return false;
  if (!isCacheFresh()) {
    try {
      const list = await getWishlist();
      return list.some((item) => (item.productId || item.product_id) === productId);
    } catch {
      return readWishlistCache().some((item) => (item.productId || item.product_id) === productId);
    }
  }
  const list = readWishlistCache();
  return list.some((item) => (item.productId || item.product_id) === productId);
}

export function isWishlistedSync(productId: string): boolean {
  if (!productId) return false;
  const list = readWishlistCache();
  return list.some((item) => (item.productId || item.product_id) === productId);
}

export function getCachedWishlistProductIds(): string[] {
  if (typeof window === "undefined") return [];
  const list = readWishlistCache();
  return list.map((w) => w.productId || w.product_id).filter(Boolean) as string[];
}
