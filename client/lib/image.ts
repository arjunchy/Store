
import type { Product, ProductImage } from "./types";

export const PLACEHOLDER_PRODUCT = "/placeholder-product.png";

export function getProductImageUrls(product: Product | null | undefined): string[] {
  if (!product) return [];
  const images = product.images ?? [];
  if (images.length === 0) {
    return product.image ? [product.image] : [];
  }
  const sorted = [...images].sort((a, b) => {
    const aPrimary = a.is_primary ?? a.isPrimary ?? false;
    const bPrimary = b.is_primary ?? b.isPrimary ?? false;
    if (aPrimary && !bPrimary) return -1;
    if (!aPrimary && bPrimary) return 1;
    const aOrder = a.display_order ?? a.displayOrder ?? 0;
    const bOrder = b.display_order ?? b.displayOrder ?? 0;
    return aOrder - bOrder;
  });
  const urls = sorted.map((i) => i.url).filter(Boolean) as string[];
  const seen = new Set<string>();
  const deduped: string[] = [];
  for (const u of urls) {
    if (!seen.has(u)) {
      seen.add(u);
      deduped.push(u);
    }
  }
  if (product.image && !seen.has(product.image)) {
    deduped.push(product.image);
  }
  return deduped;
}

export function getProductMainImage(product: Product | null | undefined): string {
  const urls = getProductImageUrls(product);
  return urls[0] || product?.image || "";
}

export function getProductThumbnails(product: Product | null | undefined, limit = 4): string[] {
  const urls = getProductImageUrls(product);
  if (urls.length <= 1) return [];
  return urls.slice(1, limit + 1);
}

/**
 * For CartItem / OrderItem that only has productId, we cannot resolve from
 * product object. This helper resolves imageUrl if caller already fetched
 * product images, else returns empty string (caller should hydrate).
 */
export function isValidImageUrl(url: string | undefined | null): boolean {
  if (!url) return false;
  return url.trim().length > 0;
}

/**
 * Image error fallback — use as <Image onError> handler or img onError.
 * Returns placeholder text icon instead of broken image.
 */
export function handleImageError(
  e: React.SyntheticEvent<HTMLImageElement, Event>
) {
  const target = e.currentTarget;
  target.style.display = "none";
}
