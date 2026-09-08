import { apiClient } from "./api-client";
import type { Review } from "./types";

type ReviewResponse = {
  id: string;
  productId: string;
  userId: string;
  rating: number;
  comment: string;
  isVerifiedPurchase: boolean;
  createdAt: string;
};

function mapReview(res: ReviewResponse): Review {
  return {
    id: res.id,
    userId: res.userId,
    productId: res.productId,
    product_id: res.productId,
    rating: res.rating,
    comment: res.comment,
    is_verified_purchase: res.isVerifiedPurchase,
    isVerifiedPurchase: res.isVerifiedPurchase,
    createdAt: res.createdAt,
  };
}

export async function getReviews(productId?: string): Promise<Review[]> {
  if (!productId) return [];
  return getReviewsByProduct(productId);
}

export async function getReviewsByProduct(productId: string): Promise<Review[]> {
  try {
    const res = await apiClient.get<ReviewResponse[]>(
      `/reviews/product/${productId}`,
      { auth: false }
    );
    return res.map(mapReview);
  } catch {
    return [];
  }
}

export async function submitReview(params: {
  userId: string;
  productId: string;
  rating: number;
  comment?: string;
}): Promise<Review> {
  const res = await apiClient.post<ReviewResponse>(
    "/reviews",
    {
      productId: params.productId,
      rating: params.rating,
      comment: params.comment,
    },
    { auth: true }
  );
  return mapReview(res);
}

export async function updateReview(
  id: string,
  patch: Partial<Pick<Review, "rating" | "comment">>
): Promise<Review | null> {
  try {
    const res = await apiClient.put<ReviewResponse>(
      `/reviews/${id}`,
      { rating: patch.rating, comment: patch.comment, productId: "" },
      { auth: true }
    );
    return mapReview(res);
  } catch {
    return null;
  }
}

export async function deleteReview(id: string): Promise<void> {
  await apiClient.delete(`/reviews/${id}`, { auth: true });
}

export function calcAverage(reviews: Review[]): { avg: number; count: number } {
  if (reviews.length === 0) return { avg: 0, count: 0 };
  const avg = reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
  return { avg: Math.round(avg * 10) / 10, count: reviews.length };
}
