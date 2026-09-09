import { apiClient } from "./api-client";
import type { CartItem } from "./types";

type CartResponse = {
  id: string;
  userId: string;
  items: {
    id: string;
    productId: string;
    productName: string;
    price: number;
    quantity: number;
    subtotal: number;
  }[];
  totalAmount: number;
  createdAt: string;
};

function mapCartItem(backend: CartResponse["items"][number]): CartItem {
  return {
    id: backend.id,
    cartItemId: backend.id,
    product_id: backend.productId,
    productId: backend.productId,
    slug: backend.productId,
    name: backend.productName,
    price: backend.price,
    image: "",
    qty: backend.quantity,
    quantity: backend.quantity,
    subtotal: backend.subtotal,
  };
}

type ProductImageResponse = {
  id: string;
  productId: string;
  url: string;
  altText: string;
  displayOrder: number;
  isPrimary: boolean;
};

const cartImageCache = new Map<string, { url: string; ts: number }>();
const CART_IMAGE_TTL = 60_000;
async function getCachedCartImage(pid: string): Promise<string> {
  const cached = cartImageCache.get(pid);
  if (cached && Date.now() - cached.ts < CART_IMAGE_TTL) return cached.url;
  try {
    const imgs = await apiClient.get<ProductImageResponse[]>(`/products/${pid}/images`, { auth: false });
    if (!Array.isArray(imgs) || imgs.length === 0) return "";
    const sorted = [...imgs].sort((a, b) => {
      if (a.isPrimary && !b.isPrimary) return -1;
      if (!a.isPrimary && b.isPrimary) return 1;
      return (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
    });
    const primary = sorted.find((i) => i.isPrimary)?.url || sorted[0]?.url || "";
    if (primary) cartImageCache.set(pid, { url: primary, ts: Date.now() });
    return primary;
  } catch { return ""; }
}

/**
 * Hydrate cart items with product images.
 * Backend CartItemResponse does NOT contain image — we fetch product images
 * so cart / mini-cart / checkout always show the admin-configured images.
 * Failures are silent (image stays empty, UI shows placeholder).
 * Now with 60s cache + concurrency limit to prevent N+1 flood.
 */
async function hydrateCartImages(items: CartItem[]): Promise<CartItem[]> {
  if (items.length === 0) return items;
  const uniquePids = Array.from(new Set(items.map(i => i.product_id).filter(Boolean) as string[]));
  const imageMap = new Map<string, string>();
  const chunkSize = 6;
  for (let i = 0; i < uniquePids.length; i += chunkSize) {
    const chunk = uniquePids.slice(i, i + chunkSize);
    const results = await Promise.all(chunk.map(async (pid) => ({ pid, url: await getCachedCartImage(pid) })));
    results.forEach(({ pid, url }) => { if (url) imageMap.set(pid, url); });
  }
  return items.map(it => {
    const img = imageMap.get(it.product_id);
    return img ? { ...it, image: img } : it;
  });
}

export async function getCart(): Promise<CartItem[]> {
  try {
    const res = await apiClient.get<CartResponse>("/cart", { auth: true });
    const items = res.items.map(mapCartItem);
    return hydrateCartImages(items);
  } catch (err: unknown) {
    const status = (err as { status?: number }).status;
    if (status === 401 || status === 403 || status === 404) return [];
    throw err;
  }
}

export async function addToCart(
  item: Omit<CartItem, "qty" | "quantity"> & { qty?: number; quantity?: number }
): Promise<CartItem[]> {
  const quantity = item.qty ?? item.quantity ?? 1;
  await apiClient.post<CartResponse>(
    "/cart/items",
    { productId: item.product_id || item.id || item.slug, quantity },
    { auth: true }
  );
  return getCart();
}

export async function updateQty(cartItemId: string, qty: number): Promise<CartItem[]> {
  if (qty <= 0) return removeFromCart(cartItemId);

  await apiClient.put<CartResponse>(
    `/cart/items/${cartItemId}`,
    { quantity: qty },
    { auth: true }
  );
  return getCart();
}

export async function removeFromCart(cartItemId: string): Promise<CartItem[]> {
  await apiClient.delete<CartResponse>(`/cart/items/${cartItemId}`, { auth: true });
  return getCart();
}

export async function clearCart(): Promise<void> {
  await apiClient.delete<CartResponse>("/cart", { auth: true });
}

export function calcSubtotal(cart: CartItem[]): number {
  return cart.reduce((sum, i) => sum + i.price * (i.qty ?? i.quantity ?? 0), 0);
}

export function calcCount(cart: CartItem[]): number {
  return cart.reduce((n, i) => n + (i.qty ?? i.quantity ?? 0), 0);
}

