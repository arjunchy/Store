import { apiClient } from "./api-client";
import type { Order, OrderStatus, DeliveryStatus, PaymentStatus, OrderStatusHistory } from "./types";

export type OrderFilterParams = {
  page?: number;
  size?: number;
  status?: string;
  deliveryStatus?: string;
  paymentStatus?: string;
  search?: string;
  sortBy?: string;
  sortDirection?: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type CreateOrderRequest = {
  addressId: string;
  paymentMethod?: string;
  shipping?: number;
  tax?: number;
};

export type FulfillmentData = {
  trackingNumber?: string;
  carrier?: string;
  estimatedDelivery?: string;
};

function mapOrderRaw(r: any): Order {
  const saRaw = r.shippingAddress ?? r.shipping_address ?? null;
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
    userEmail: r.userEmail,
    userName: r.userName,
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

export const orderApi = {
  getMyOrders: async (params: OrderFilterParams = {}): Promise<Page<Order>> => {
    const raw = await apiClient.get<any>("/orders", { auth: true, params: params as any });
    if (raw.content) {
      return { ...raw, content: raw.content.map(mapOrderRaw) } as Page<Order>;
    }
    const arr = Array.isArray(raw) ? raw : [];
    return { content: arr.map(mapOrderRaw), totalElements: arr.length, totalPages: 1, number: 0, size: arr.length };
  },
  getOrderById: async (id: string): Promise<Order | null> => {
    try {
      try {
        const raw = await apiClient.get<any>(`/orders/${id}/full`, { auth: true });
        return mapOrderRaw(raw);
      } catch {
        const raw = await apiClient.get<any>(`/orders/${id}`, { auth: true });
        return mapOrderRaw(raw);
      }
    } catch { return null; }
  },
  createOrder: async (data: CreateOrderRequest): Promise<Order> => {
    const raw = await apiClient.post<any>("/orders", data, { auth: true });
    return mapOrderRaw(raw);
  },
  cancelOrder: async (id: string): Promise<Order> => {
    const raw = await apiClient.patch<any>(`/orders/${id}/cancel`, {}, { auth: true });
    return mapOrderRaw(raw);
  },
  getHistory: async (id: string): Promise<OrderStatusHistory[]> => {
    const raw = await apiClient.get<any[]>(`/orders/${id}/history`, { auth: true });
    return raw.map((h: any) => ({
      id: h.id,
      order_id: h.orderId ?? h.order_id ?? h.id,
      status: h.status,
      statusType: h.statusType ?? "ORDER",
      note: h.note,
      changed_by: h.changedBy ?? h.changed_by ?? "system",
      createdAt: h.createdAt ?? h.created_at ?? new Date().toISOString(),
    }));
  },
};

export const orderAdminApi = {
  getOrders: async (params: OrderFilterParams = {}): Promise<Page<Order>> => {
    const raw = await apiClient.get<any>("/orders/all", { auth: true, params: params as any });
    return { ...raw, content: raw.content.map(mapOrderRaw) } as Page<Order>;
  },
  getOrderFull: async (id: string): Promise<Order | null> => {
    try {
      const raw = await apiClient.get<any>(`/orders/${id}/full`, { auth: true });
      return mapOrderRaw(raw);
    } catch { return null; }
  },
  updateStatus: async (id: string, status: OrderStatus, note?: string): Promise<Order> => {
    const raw = await apiClient.patch<any>(`/orders/${id}/status`, { status, note: note ?? "" }, { auth: true });
    return mapOrderRaw(raw);
  },
  updateDelivery: async (id: string, deliveryStatus: DeliveryStatus, note?: string): Promise<Order> => {
    const raw = await apiClient.patch<any>(`/orders/${id}/delivery-status`, { deliveryStatus, note: note ?? "" }, { auth: true });
    return mapOrderRaw(raw);
  },
  updatePayment: async (id: string, paymentStatus: PaymentStatus, note?: string): Promise<Order> => {
    const raw = await apiClient.patch<any>(`/orders/${id}/payment-status`, { paymentStatus, note: note ?? "" }, { auth: true });
    return mapOrderRaw(raw);
  },
  updateFulfillment: async (id: string, data: FulfillmentData): Promise<Order> => {
    const raw = await apiClient.patch<any>(`/orders/${id}/fulfillment`, data as any, { auth: true });
    return mapOrderRaw(raw);
  },
  getHistory: async (id: string): Promise<OrderStatusHistory[]> => orderApi.getHistory(id),
  verifyDelete: async (id: string): Promise<any> => {
    const raw = await apiClient.get<any>(`/orders/${id}/verify-delete`, { auth: true });
    return raw;
  },
  deleteOrder: async (id: string, force: boolean = false): Promise<{ message: string; orderId: string; orderNumber: string }> => {
    const qs = force ? "?force=true" : "";
    const raw = await apiClient.delete<any>(`/orders/${id}${qs}`, { auth: true });
    return raw as { message: string; orderId: string; orderNumber: string };
  },
};
