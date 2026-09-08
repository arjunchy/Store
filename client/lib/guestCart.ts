import type { CartItem } from "./types";
import { addToCart as addToServerCart } from "./cart";

const GUEST_CART_KEY = "apexcommerce_guest_cart";

function isBrowser() {
  return typeof window !== "undefined";
}

function keyOf(item: { id?: string; product_id?: string; productId?: string; slug?: string }): string {
  return item.product_id || (item as any).productId || item.id || item.slug || "";
}

export function getGuestCart(): CartItem[] {
  if (!isBrowser()) return [];
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as CartItem[];
    if (!Array.isArray(parsed)) return [];
    return parsed;
  } catch {
    return [];
  }
}

function save(items: CartItem[]): CartItem[] {
  if (isBrowser()) {
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items));
    try { window.dispatchEvent(new Event("apexcommerce_guest_cart_updated")); } catch {}
  }
  return items;
}

export function addGuestItem(
  item: Omit<CartItem, "qty" | "quantity"> & { qty?: number; quantity?: number }
): CartItem[] {
  const rawQty = item.qty ?? item.quantity ?? 1;
  const qty = Math.max(1, Math.min(99, rawQty));
  const id = keyOf(item);
  if (!id) return getGuestCart();
  const items = getGuestCart();
  const existing = items.find((i) => keyOf(i) === id);

  if (existing) {
    const newQty = Math.min(99, (existing.qty ?? 1) + qty);
    existing.qty = newQty;
    existing.quantity = newQty;
  } else {
    const newItem: CartItem = {
      id,
      product_id: id,
      productId: id,
      slug: (item.slug && item.slug !== id) ? item.slug : id,
      name: item.name,
      price: item.price,
      image: item.image ?? "",
      qty,
      quantity: qty,
      subtotal: item.price * qty,
    };
    items.push(newItem);
  }
  return save(items);
}

export function updateGuestQty(id: string, qty: number): CartItem[] {
  if (qty <= 0) return removeGuestItem(id);
  const items = getGuestCart();
  const existing = items.find((i) => keyOf(i) === id);
  if (existing) {
    existing.qty = qty;
    existing.quantity = qty;
  }
  return save(items);
}

export function removeGuestItem(id: string): CartItem[] {
  return save(getGuestCart().filter((i) => keyOf(i) !== id));
}

export function clearGuestCart(): void {
  if (isBrowser()) {
    localStorage.removeItem(GUEST_CART_KEY);
    try { window.dispatchEvent(new Event("apexcommerce_guest_cart_updated")); } catch {}
  }
}

let merging = false;
export async function mergeGuestCartToServer(): Promise<void> {
  if (merging) return;
  const items = getGuestCart();
  if (items.length === 0) return;
  merging = true;
  const failedKeys: string[] = [];
  try {
    for (const it of items) {
      const key = keyOf(it);
      try {
        await addToServerCart({
          id: key,
          product_id: key,
          productId: key,
          slug: it.slug || key,
          name: it.name,
          price: it.price,
          image: it.image,
          qty: it.qty,
        });
      } catch {
        failedKeys.push(key);
      }
    }
  } finally {
    merging = false;
  }

  if (failedKeys.length === 0) {
    clearGuestCart();
  } else {
    const remaining = items.filter((it) => failedKeys.includes(keyOf(it)));
    save(remaining);
  }
}
