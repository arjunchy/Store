import { apiClient } from "./api-client";
import type { Product, ProductImage } from "./types";

type ProductResponse = {
  id: string;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  isNewArrival: boolean;
  rating: number;
  reviewCount: number;
  categoryId: string;
  createdAt: string;
};

type ProductImageResponse = {
  id: string;
  productId: string;
  url: string;
  altText: string;
  displayOrder: number;
  isPrimary: boolean;
};

const imageCache = new Map<string, { data: ProductImageResponse[]; ts: number }>();
const IMAGE_CACHE_TTL = 60_000;
async function fetchImagesCached(pid: string): Promise<ProductImageResponse[]> {
  const cached = imageCache.get(pid);
  if (cached && Date.now() - cached.ts < IMAGE_CACHE_TTL) return cached.data;
  try {
    const imgs = await apiClient.get<ProductImageResponse[]>(`/products/${pid}/images`, { auth: false });
    imageCache.set(pid, { data: imgs, ts: Date.now() });
    return imgs;
  } catch {
    return [];
  }
}
async function fetchImagesBatched(pids: string[]): Promise<Map<string, ProductImageResponse[]>> {
  const result = new Map<string, ProductImageResponse[]>();
  const chunkSize = 6;
  for (let i = 0; i < pids.length; i += chunkSize) {
    const chunk = pids.slice(i, i + chunkSize);
    const chunkResults = await Promise.all(chunk.map(async (pid) => ({ pid, imgs: await fetchImagesCached(pid) })));
    chunkResults.forEach(({ pid, imgs }) => result.set(pid, imgs));
  }
  return result;
}

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
  totalPages: number;
};

function mapProduct(res: ProductResponse, images: ProductImage[] = []): Product {
  const sortedImages = [...images].sort((a, b) => {
    const aP = a.is_primary ?? a.isPrimary ?? false;
    const bP = b.is_primary ?? b.isPrimary ?? false;
    if (aP && !bP) return -1;
    if (!aP && bP) return 1;
    const aO = a.display_order ?? a.displayOrder ?? 0;
    const bO = b.display_order ?? b.displayOrder ?? 0;
    return aO - bO;
  });
  return {
    id: res.id,
    slug: res.id,
    name: res.name,
    description: res.description,
    price: res.price,
    stock_quantity: res.stockQuantity,
    stockQuantity: res.stockQuantity,
    rating: res.rating,
    review_count: res.reviewCount,
    reviewCount: res.reviewCount,
    is_new_arrival: res.isNewArrival,
    isNewArrival: res.isNewArrival,
    category_id: res.categoryId,
    categoryId: res.categoryId,
    deleted_at: null,
    images: sortedImages,
    image: sortedImages.find((i) => i.is_primary ?? i.isPrimary)?.url || sortedImages[0]?.url || "",
  };
}

function mapProductImage(res: ProductImageResponse): ProductImage {
  return {
    id: res.id,
    product_id: res.productId,
    productId: res.productId,
    url: res.url,
    alt_text: res.altText,
    altText: res.altText,
    display_order: res.displayOrder,
    displayOrder: res.displayOrder,
    is_primary: res.isPrimary,
    isPrimary: res.isPrimary,
  };
}

export async function getProducts(): Promise<Product[]> {
  const page = await apiClient.get<PageResponse<ProductResponse>>("/products", {
    params: { page: 0, size: 20, sort: "createdAt,desc" },
  });
  const pids = page.content.map((p) => p.id);
  const imageMap = await fetchImagesBatched(pids);
  const products = page.content.map((p) => {
    const imgs = imageMap.get(p.id) || [];
    return mapProduct(p, imgs.map(mapProductImage));
  });
  return products;
}

export async function getProductById(id: string): Promise<Product | null> {
  try {
    const res = await apiClient.get<ProductResponse>(`/products/${id}`);
    const imgs = await apiClient
      .get<ProductImageResponse[]>(`/products/${id}/images`)
      .catch(() => [] as ProductImageResponse[]);
    return mapProduct(res, imgs.map(mapProductImage));
  } catch {
    return null;
  }
}

export type ProductFilter = {
  category_id?: string;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  inStock?: boolean;
  isNewArrival?: boolean;
  search?: string;
  sort?: "price_asc" | "price_desc" | "rating" | "newest";
  page?: number;
  limit?: number;
};

export async function listProducts(
  filter: ProductFilter = {}
): Promise<{ data: Product[]; total: number }> {
  const params: Record<string, string | number | boolean | undefined> = {
    page: (filter.page ?? 1) - 1,
    size: filter.limit ?? 20,
  };

  if (filter.search) params.name = filter.search;
  if (filter.category_id) params.categoryId = filter.category_id;
  if (filter.minPrice !== undefined) params.minPrice = filter.minPrice;
  if (filter.maxPrice !== undefined) params.maxPrice = filter.maxPrice;
  if (filter.minRating !== undefined) params.minRating = filter.minRating;
  if (filter.isNewArrival) params.isNewArrival = true;

  let sortParam = "createdAt,desc";
  if (filter.sort === "price_asc") sortParam = "price,asc";
  if (filter.sort === "price_desc") sortParam = "price,desc";
  if (filter.sort === "rating") sortParam = "rating,desc";
  if (filter.sort === "newest") sortParam = "createdAt,desc";
  params.sort = sortParam;

  const page = await apiClient.get<PageResponse<ProductResponse>>(
    "/products/search",
    { params }
  );

  const pids = page.content.map((p) => p.id);
  const imageMap = await fetchImagesBatched(pids);
  const products = page.content.map((p) => {
    const imgs = imageMap.get(p.id) || [];
    return mapProduct(p, imgs.map(mapProductImage));
  });

  return { data: products, total: page.totalElements };
}

export type CreateProductPayload = {
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  categoryId: string;
  isNewArrival?: boolean;
};

export async function createProduct(payload: CreateProductPayload): Promise<Product> {
  const res = await apiClient.post<ProductResponse>("/products", payload, { auth: true });
  return mapProduct(res);
}

export async function updateProduct(id: string, payload: Partial<CreateProductPayload>): Promise<Product> {
  const res = await apiClient.put<ProductResponse>(`/products/${id}`, payload, { auth: true });
  return mapProduct(res);
}

export async function deleteProduct(id: string): Promise<void> {
  await apiClient.delete(`/products/${id}`, { auth: true });
}

export type CreateProductImagePayload = {
  url: string;
  altText?: string;
  displayOrder?: number;
  isPrimary?: boolean;
};

export async function getProductImages(productId: string): Promise<ProductImage[]> {
  const res = await apiClient.get<ProductImageResponse[]>(`/products/${productId}/images`, { auth: false });
  return res.map(mapProductImage);
}

export async function addProductImage(productId: string, payload: CreateProductImagePayload): Promise<ProductImage> {
  const res = await apiClient.post<ProductImageResponse>(
    `/products/${productId}/images`,
    {
      url: payload.url,
      altText: payload.altText || "",
      displayOrder: payload.displayOrder ?? 0,
      isPrimary: payload.isPrimary ?? false,
    },
    { auth: true }
  );
  return mapProductImage(res);
}

export async function deleteProductImage(productId: string, imageId: string): Promise<void> {
  await apiClient.delete(`/products/${productId}/images/${imageId}`, { auth: true });
}

export async function uploadProductImage(
  productId: string,
  file: File,
  opts?: { altText?: string; displayOrder?: number; isPrimary?: boolean }
): Promise<ProductImage> {
  const form = new FormData();
  form.append("file", file);
  if (opts?.altText) form.append("altText", opts.altText);
  if (opts?.displayOrder !== undefined) form.append("displayOrder", String(opts.displayOrder));
  form.append("isPrimary", String(opts?.isPrimary ?? false));
  const res = await apiClient.post<ProductImageResponse>(`/products/${productId}/images/upload`, form, { auth: true });
  return mapProductImage(res);
}

export async function setPrimaryImage(productId: string, imageId: string): Promise<ProductImage> {
  const res = await apiClient.put<ProductImageResponse>(`/products/${productId}/images/${imageId}/primary`, {}, { auth: true });
  return mapProductImage(res);
}

export type ReorderRequest = { imageId: string; displayOrder: number };

export async function reorderImages(productId: string, orders: ReorderRequest[]): Promise<ProductImage[]> {
  const res = await apiClient.put<ProductImageResponse[]>(`/products/${productId}/images/reorder`, orders, { auth: true });
  return res.map(mapProductImage);
}

export async function getPrimaryImage(productId: string): Promise<ProductImage | null> {
  try {
    const res = await apiClient.get<ProductImageResponse>(`/products/${productId}/images/primary`, { auth: false });
    return mapProductImage(res);
  } catch {
    return null;
  }
}

export type ProductDetail = Product & { images: ProductImage[] };

export async function getProductDetail(id: string): Promise<ProductDetail | null> {
  try {
    const res = await apiClient.get<ProductResponse & { images?: ProductImageResponse[] }>(`/products/${id}/details`, { auth: false });
    const images = (res as unknown as { images: ProductImageResponse[] }).images ?? [];
    let mappedImages = images.map(mapProductImage);
    if (mappedImages.length === 0) {
      try {
        mappedImages = await getProductImages(id);
      } catch {}
    }
    const base = mapProduct(res as ProductResponse, mappedImages);
    return { ...base, images: mappedImages };
  } catch {
    return null;
  }
}

export async function createProductWithImages(payload: CreateProductPayload, files?: File[]): Promise<Product> {
  if (!files || files.length === 0) {
    return createProduct(payload);
  }
  const form = new FormData();
  form.append("product", new Blob([JSON.stringify(payload)], { type: "application/json" }));
  files.forEach((f) => form.append("files", f));
  const res = await apiClient.post<ProductResponse>(`/products/with-images`, form, { auth: true });
  return mapProduct(res);
}

export async function updateProductWithImages(id: string, payload: Partial<CreateProductPayload>, files?: File[]): Promise<Product> {
  if (!files || files.length === 0) {
    return updateProduct(id, payload as CreateProductPayload);
  }
  const form = new FormData();
  const fullPayload = {
    name: payload.name ?? "",
    description: payload.description ?? "",
    price: payload.price ?? 0,
    stockQuantity: payload.stockQuantity ?? 0,
    categoryId: payload.categoryId ?? "",
    isNewArrival: payload.isNewArrival ?? false,
  };
  form.append("product", new Blob([JSON.stringify(fullPayload)], { type: "application/json" }));
  files.forEach((f) => form.append("files", f));
  const res = await apiClient.put<ProductResponse>(`/products/${id}/with-images`, form, { auth: true });
  return mapProduct(res);
}

export async function getProductStats(): Promise<{ totalProducts: number; outOfStockProducts: number }> {
  const res = await apiClient.get<{ totalProducts: number; outOfStockProducts: number }>(`/products/stats`, { auth: true });
  return res;
}

export function getMainImage(product: Product | null | undefined): string {
  if (!product) return "";
  return product.image || product.images?.find((i) => i.is_primary ?? i.isPrimary)?.url || product.images?.[0]?.url || "";
}

export function getGalleryImages(product: Product | null | undefined): string[] {
  if (!product) return [];
  const sorted = [...(product.images ?? [])].sort((a, b) => {
    const aP = a.is_primary ?? a.isPrimary ?? false;
    const bP = b.is_primary ?? b.isPrimary ?? false;
    if (aP && !bP) return -1;
    if (!aP && bP) return 1;
    return (a.display_order ?? a.displayOrder ?? 0) - (b.display_order ?? b.displayOrder ?? 0);
  });
  const urls = sorted.map((i) => i.url).filter(Boolean) as string[];
  const seen = new Set<string>();
  const deduped = urls.filter((u) => {
    if (seen.has(u)) return false;
    seen.add(u);
    return true;
  });
  if (deduped.length === 0 && product.image) return [product.image];
  return deduped;
}
