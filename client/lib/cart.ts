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

/**
 * Hydrate cart items with product images.
 * Backend CartItemResponse does NOT contain image — we fetch product images
 * so cart / mini-cart / checkout always show the admin-configured images.
 * Failures are silent (image stays empty, UI shows placeholder).
 */
async function hydrateCartImages(items: CartItem[]): Promise<CartItem[]> {
  if (items.length === 0) return items;
  const uniquePids = Array.from(new Set(items.map(i => i.product_id).filter(Boolean) as string[]));
  const imageMap = new Map<string, string>();
  await Promise.all(uniquePids.map(async (pid) => {
    try {
      const imgs = await apiClient.get<ProductImageResponse[]>(`/products/${pid}/images`, { auth: false });
      if (!Array.isArray(imgs) || imgs.length === 0) return;
      const sorted = [...imgs].sort((a, b) => {
        if (a.isPrimary && !b.isPrimary) return -1;
        if (!a.isPrimary && b.isPrimary) return 1;
        return (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
      });
      const primary = sorted.find((i) => i.isPrimary)?.url || sorted[0]?.url || "";
      if (primary) imageMap.set(pid, primary);
    } catch {}
  }));
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

