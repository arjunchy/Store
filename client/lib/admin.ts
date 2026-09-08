import { apiClient } from "./api-client";
import type { Order, Product, User } from "./types";
import type { Product as ProductType } from "./types";

type UserResponse = {
  id: string;
  username: string;
  email: string;
  userRole: string;
  createdAt: string;
  deletedAt?: string | null;
  isDeleted?: boolean;
  deleted_at?: string | null;
};

function mapUser(res: UserResponse): User {
  const deleted = (res as any).deletedAt ?? (res as any).deleted_at ?? null;
  const isDeleted = (res as any).isDeleted ?? !!deleted;
  return {
    id: res.id,
    username: res.username,
    email: res.email,
    role: res.userRole === "ADMIN" ? "ROLE_ADMIN" : "ROLE_USER",
    createdAt: res.createdAt,
    isDeleted,
    deletedAt: deleted,
  } as User;
}

type AdminStatsResponse = {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  outOfStockProducts: number;
  recentOrders: Order[];
  recentUsers: User[];
};

type AdminStatsRaw = {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  pendingOrders: number;
  outOfStockProducts: number;
  recentOrders: any[];
  recentUsers: UserResponse[];
};

type OrderStatsResponse = Record<string, number>;
type RevenueStatsResponse = Record<string, number>;

function mapOrderRaw(r: any): Order {
  const saRaw = r.shippingAddress ?? r.shipping_address ?? r.shippingAddressJson ?? null;
  let sa: any = {};
  if (saRaw) {
    if (typeof saRaw === "string") { try { sa = JSON.parse(saRaw); } catch { sa = {}; } }
    else if (typeof saRaw === "object") sa = saRaw;
  }
  return {
    id: r.id,
    order_number: r.orderNumber ?? r.order_number ?? r.id,
    orderNumber: r.orderNumber ?? r.order_number ?? r.id,
    userId: r.userId,
    userEmail: r.userEmail ?? r.user_email,
    userName: r.userName ?? r.user_name ?? r.username,
    total_amount: r.totalAmount ?? r.total_amount ?? 0,
    totalAmount: r.totalAmount ?? r.total_amount ?? 0,
    subtotal: r.subtotal ?? undefined,
    shipping: r.shippingCost ?? r.shipping ?? undefined,
    tax: r.tax ?? undefined,
    paymentMethod: r.paymentMethod ?? undefined,
    status: r.status,
    payment_status: r.paymentStatus ?? r.payment_status ?? "UNPAID",
    paymentStatus: r.paymentStatus ?? r.payment_status ?? "UNPAID",
    delivery_status: r.deliveryStatus ?? r.delivery_status ?? "PLACED",
    deliveryStatus: r.deliveryStatus ?? r.delivery_status ?? "PLACED",
    shipping_address: sa as Order["shipping_address"],
    shippingAddress: sa as Order["shipping_address"],
    tracking_number: r.trackingNumber ?? r.tracking_number ?? null,
    trackingNumber: r.trackingNumber ?? r.tracking_number ?? null,
    carrier: r.carrier ?? null,
    estimated_delivery: r.estimatedDelivery ?? r.estimated_delivery ?? null,
    estimatedDelivery: r.estimatedDelivery ?? r.estimated_delivery ?? null,
    items: (r.items ?? []).map((it: any) => ({
      product_id: it.productId ?? it.product_id ?? "",
      productId: it.productId ?? it.product_id ?? "",
      name: it.productName ?? it.name ?? "",
      quantity: it.quantity ?? it.qty ?? 0,
      price: it.price ?? 0,
      subtotal: it.subtotal ?? (it.price ?? 0) * (it.quantity ?? 0),
      image: it.image ?? "",
    })),
    createdAt: r.createdAt ?? r.created_at ?? new Date().toISOString(),
  } as Order;
}

export async function getAdminStats(): Promise<AdminStatsResponse | null> {
  try {
    const res = await apiClient.get<AdminStatsRaw>("/admin/stats", { auth: true });
    const mapped: AdminStatsResponse = {
      totalUsers: res.totalUsers,
      totalProducts: res.totalProducts,
      totalOrders: res.totalOrders,
      totalRevenue: res.totalRevenue,
      pendingOrders: res.pendingOrders,
      outOfStockProducts: res.outOfStockProducts,
      recentOrders: (res.recentOrders ?? []).map(mapOrderRaw),
      recentUsers: (res.recentUsers ?? []).map(mapUser),
    };
    return mapped;
  } catch {
    return null;
  }
}

export async function getOrderStats(): Promise<OrderStatsResponse | null> {
  try {
    return await apiClient.get<OrderStatsResponse>("/admin/stats/orders", { auth: true });
  } catch {
    return null;
  }
}

export async function getRevenueStats(): Promise<RevenueStatsResponse | null> {
  try {
    return await apiClient.get<RevenueStatsResponse>("/admin/stats/revenue", { auth: true });
  } catch {
    return null;
  }
}

export async function getAdminOrders(params?: { page?: number; size?: number; status?: string; search?: string }) {
  const query: Record<string, string | number | undefined> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  };
  if (params?.status) query.status = params.status;
  if (params?.search) query.search = params.search;
  return apiClient.get<{ content: Order[]; totalElements: number; totalPages: number; number: number }>(
    "/admin/orders",
    { auth: true, params: query as Record<string, string | number | boolean | undefined> }
  );
}

export async function getAdminUsers(includeDeleted = false) {
  try {
    const res = await apiClient.get<UserResponse[]>(`/users/all`, { auth: true, params: { includeDeleted: String(includeDeleted) } });
    return res.map((r: any) => mapUser({ ...r, createdAt: r.createdAt } as UserResponse));
  } catch {
    try {
      const res2 = await apiClient.get<UserResponse[]>("/admin/users", { auth: true });
      return res2.map(mapUser);
    } catch {
      const res3 = await apiClient.get<UserResponse[]>("/users", { auth: true });
      return res3.map(mapUser);
    }
  }
}

export type PaginatedUsers = { content: User[]; totalElements: number; totalPages: number; number: number };

export async function getAdminUsersPaginated(params: { page?: number; size?: number; search?: string; includeDeleted?: boolean }): Promise<PaginatedUsers> {
  const q: Record<string, string | number | boolean | undefined> = {
    page: params.page ?? 0,
    size: params.size ?? 20,
    paged: true,
    includeDeleted: String(params.includeDeleted ?? false),
  };
  if (params.search) q.search = params.search;
  const raw = await apiClient.get<any>(`/users/all`, { auth: true, params: q as Record<string, string | number | boolean | undefined> });
  if (raw.content) {
    return {
      content: raw.content.map((r: any) => mapUser(r as UserResponse)) as User[],
      totalElements: raw.totalElements,
      totalPages: raw.totalPages,
      number: raw.number,
    };
  }
  const list = Array.isArray(raw) ? raw : [];
  return { content: list.map(mapUser) as User[], totalElements: list.length, totalPages: 1, number: 0 };
}

export async function changeUserRole(userId: string, role: "ADMIN" | "USER" | "ROLE_ADMIN" | "ROLE_USER") {
  const normalized = role.replace("ROLE_", "");
  const res = await apiClient.put<UserResponse>(`/users/${userId}/role`, { role: normalized }, { auth: true });
  return mapUser(res);
}

export async function toggleUserStatus(userId: string, activate: boolean) {
  const res = await apiClient.put<UserResponse>(`/users/${userId}/status`, { activate }, { auth: true });
  return mapUser(res);
}

export async function hardDeleteUser(userId: string) {
  await apiClient.delete(`/users/${userId}`, { auth: true });
}

export async function getAllOrdersAdmin(params?: { page?: number; size?: number; status?: string; search?: string; deliveryStatus?: string; paymentStatus?: string }) {
  const q: Record<string, string | number | undefined> = {
    page: params?.page ?? 0,
    size: params?.size ?? 20,
  };
  if (params?.status) q.status = params.status;
  if (params?.search) q.search = params.search;
  if (params?.deliveryStatus) q.deliveryStatus = params.deliveryStatus;
  if (params?.paymentStatus) q.paymentStatus = params.paymentStatus;
  const raw = await apiClient.get<{ content: any[]; totalElements: number }>(`/orders/all`, {
    auth: true,
    params: q as Record<string, string | number | boolean | undefined>,
  });
  return { content: raw.content.map(mapOrderRaw), totalElements: raw.totalElements, totalPages: (raw as any).totalPages, number: (raw as any).number };
}

export async function getOrderFullDetails(orderId: string) {
  try {
    const raw = await apiClient.get<any>(`/orders/${orderId}/full`, { auth: true });
    return mapOrderRaw(raw);
  } catch {
    return null;
  }
}

export async function getAllReviewsAdmin(page = 0, size = 20, minRating?: number) {
  const params: Record<string, string | number | undefined> = { page, size };
  if (minRating !== undefined) params.minRating = minRating;
  return apiClient.get<{ content: any[]; totalElements: number }>(`/reviews/all`, {
    auth: true,
    params: params as Record<string, string | number | boolean | undefined>,
  });
}
