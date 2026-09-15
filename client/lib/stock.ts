type StockLike = {
  stock_quantity?: number | null;
  stockQuantity?: number | null;
};

/** Single source of truth for reading stock off a product (both snake + camel shapes). */
export function getStockQuantity(p: StockLike): number {
  return p.stock_quantity ?? p.stockQuantity ?? 0;
}

export function isOutOfStock(p: StockLike): boolean {
  return getStockQuantity(p) <= 0;
}

export function outOfStockMessage(name: string): string {
  return `"${name}" is out of stock`;
}

export function lowStockMessage(name: string, stock: number, inCart = 0): string {
  if (inCart > 0) {
    return `Only ${stock} available for "${name}" (you already have ${inCart} in your cart)`;
  }
  return `Only ${stock} left for "${name}"`;
}

/**
 * Detect backend stock errors (e.g. "Insufficient stock for product X: available N",
 * "Product not available") and return a display-ready message.
 * Returns null when the error is not stock-related.
 */
export function parseStockError(err: unknown): string | null {
  const data = err as { data?: { message?: string }; message?: string } | null;
  const raw =
    (typeof data?.data?.message === "string" && data.data.message) ||
    (typeof data?.message === "string" && data.message) ||
    "";
  if (!raw) return null;
  if (/insufficient stock|not available|out of stock/i.test(raw)) return raw;
  return null;
}
