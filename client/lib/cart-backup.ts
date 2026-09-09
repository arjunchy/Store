import type { CartItem } from "./types";

const CART_BACKUP_KEY = "apexcommerce_cart_backup";

export function stashCartBackup(cart: CartItem[]): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(CART_BACKUP_KEY, JSON.stringify(cart));
  } catch {}
}

export function getCartBackup(): CartItem[] | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = localStorage.getItem(CART_BACKUP_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CartItem[];
    if (!Array.isArray(parsed) || parsed.length === 0) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function clearCartBackup(): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.removeItem(CART_BACKUP_KEY);
  } catch {}
}

export function hasCartBackup(): boolean {
  return getCartBackup() !== null;
}
