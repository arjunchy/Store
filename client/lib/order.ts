import { apiClient } from "./api-client";
import type { Order, OrderItem, OrderStatus, OrderStatusHistory } from "./types";

type OrderResponse = {
  id: string;
  orderNumber: string;
  totalAmount: number;
  subtotal?: number;
  shippingCost?: number;
  tax?: number;
  paymentMethod?: string;
  status: OrderStatus;
  paymentStatus: string;
  deliveryStatus?: string;
  shippingAddress?: string | Record<string, unknown>;
  trackingNumber?: string | null;
  carrier?: string | null;
  estimatedDelivery?: string | null;
  items: {
    productId: string;
    productName: string;
    quantity: number;
    price: number;
    subtotal: number;
  }[];
  createdAt: string;
  updatedAt?: string;
  userId?: string;
  userEmail?: string;
  userName?: string;
};

type OrderStatusHistoryResponse = {
  id: string;
  orderId: string;
  status: OrderStatus;
  statusType?: string;
  note: string;
  changedBy: string;
  createdAt: string;
};

type PageResponse<T> = {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
};

type ProductImageResponse = {
  id: string;
  productId: string;
  url: string;
  altText: string;
  displayOrder: number;
  isPrimary: boolean;
};

function parseShippingAddress(raw: unknown): Order["shipping_address"] {
  if (!raw) return {} as Order["shipping_address"];
  if (typeof raw === "object") return raw as Order["shipping_address"];
  if (typeof raw === "string") {
    const s = raw.trim();
    if (!s || s === "{}" || s === "null") return {} as Order["shipping_address"];
    try {
      const parsed = JSON.parse(s);
      if (parsed && typeof parsed === "object") return parsed as Order["shipping_address"];
    } catch {
    }
    // if plain string, treat as street only if not JSON
    return { street: s } as unknown as Order["shipping_address"];
  }
  return {} as Order["shipping_address"];
}

function mapOrder(res: OrderResponse): Order {
  const rawAny = res as unknown as Record<string, unknown>;
  const saRaw = (rawAny.shippingAddress ?? rawAny.shipping_address ?? rawAny.shippingAddressJson ?? res.shippingAddress) as unknown;
  const sa = parseShippingAddress(saRaw);
  const tracking = (rawAny.trackingNumber as string) ?? (rawAny.tracking_number as string) ?? null;
  const carrier = (rawAny.carrier as string) ?? null;
  const est = (rawAny.estimatedDelivery as string) ?? (rawAny.estimated_delivery as string) ?? null;
  return {
    id: res.id,
    order_number: res.orderNumber,
    orderNumber: res.orderNumber,
    userId: res.userId,
    userEmail: res.userEmail,
    userName: res.userName,
    total_amount: res.totalAmount,
    totalAmount: res.totalAmount,
    subtotal: (rawAny.subtotal as number) ?? (res.subtotal as number),
    shipping: (rawAny.shippingCost as number) ?? (res.shippingCost as number),
    tax: (rawAny.tax as number) ?? (res.tax as number),
    paymentMethod: (rawAny.paymentMethod as Order["paymentMethod"]) ?? (res.paymentMethod as Order["paymentMethod"]),
    status: res.status,
    payment_status: res.paymentStatus as Order["payment_status"],
    delivery_status: (res.deliveryStatus as unknown as Order["delivery_status"]) ?? "PLACED",
    deliveryStatus: (res.deliveryStatus as unknown as Order["deliveryStatus"]) ?? "PLACED",
    shipping_address: sa,
    shippingAddress: sa,
    tracking_number: tracking,
    trackingNumber: tracking,
    carrier: carrier,
    estimated_delivery: est,
    estimatedDelivery: est,
    items: res.items.map(
      (it): OrderItem => ({
        product_id: it.productId,
        productId: it.productId,
        name: it.productName,
        quantity: it.quantity,
        price: it.price,
        subtotal: it.subtotal,
        image: "",
      })
    ),
    createdAt: res.createdAt,
  };
}

const orderImageCache = new Map<string, { url: string; ts: number }>();
const ORDER_IMAGE_TTL = 60_000;
async function getOrderCachedImage(pid: string): Promise<string> {
  const cached = orderImageCache.get(pid);
  if (cached && Date.now() - cached.ts < ORDER_IMAGE_TTL) return cached.url;
  try {
    const imgs = await apiClient.get<ProductImageResponse[]>(`/products/${pid}/images`, { auth: false });
    if (!Array.isArray(imgs) || imgs.length === 0) return "";
    const sorted = [...imgs].sort((a, b) => {
      if (a.isPrimary && !b.isPrimary) return -1;
      if (!a.isPrimary && b.isPrimary) return 1;
      return (a.displayOrder ?? 0) - (b.displayOrder ?? 0);
    });
    const primary = sorted.find((i) => i.isPrimary)?.url || sorted[0]?.url || "";
    if (primary) orderImageCache.set(pid, { url: primary, ts: Date.now() });
    return primary;
  } catch { return ""; }
}

async function hydrateOrderImages(orders: Order[]): Promise<Order[]> {
  const pidSet = new Set<string>();
  for (const o of orders) for (const it of o.items) {
    const pid = it.product_id || it.productId || "";
    if (pid) pidSet.add(pid);
  }
  const pidList = Array.from(pidSet);
  const imageMap = new Map<string, string>();
  const chunkSize = 6;
  for (let i = 0; i < pidList.length; i += chunkSize) {
    const chunk = pidList.slice(i, i + chunkSize);
    const results = await Promise.all(chunk.map(async (pid) => ({ pid, url: await getOrderCachedImage(pid) })));
    results.forEach(({ pid, url }) => { if (url) imageMap.set(pid, url); });
  }
  return orders.map(order => ({
    ...order,
    items: order.items.map(it => {
      const pid = it.product_id || it.productId || "";
      const img = imageMap.get(pid);
      return img ? { ...it, image: img } : it;
    })
  }));
}

export async function getOrders(params?: { page?: number; size?: number }): Promise<Order[]> {
  const page = await apiClient.get<PageResponse<OrderResponse>>("/orders", {
    params: { page: params?.page ?? 0, size: params?.size ?? 20 },
    auth: true,
  });
  const content = Array.isArray(page?.content) ? page.content : [];
  const mapped = content.map(mapOrder);
  return hydrateOrderImages(mapped);
}

export async function getOrdersPaginated(params: { page?: number; size?: number; status?: string; search?: string }): Promise<{ content: Order[]; totalElements: number; totalPages: number; number: number }> {
  const page = await apiClient.get<PageResponse<OrderResponse> & { totalPages?: number }>("/orders", {
    params: { page: params.page ?? 0, size: params.size ?? 20, status: params.status, search: params.search },
    auth: true,
  });
  const mapped = (page.content ?? []).map(mapOrder);
  const hydrated = await hydrateOrderImages(mapped);
  return { content: hydrated, totalElements: page.totalElements ?? hydrated.length, totalPages: (page as any).totalPages ?? Math.ceil((page.totalElements ?? 0)/(params.size ?? 20)), number: page.number ?? params.page ?? 0 };
}

export async function getOrderById(id: string): Promise<Order | null> {
  try {
    const res = await apiClient.get<OrderResponse>(`/orders/${id}`, { auth: true });
    const mapped = mapOrder(res);
    const [hydrated] = await hydrateOrderImages([mapped]);
    return hydrated;
  } catch (e: any) {
    if (e?.status === 404) return null;
    throw e;
  }
}

export async function createOrder(params: {
  addressId: string;
  paymentMethod?: string;
  shipping?: number;
  tax?: number;
  shippingMethod?: string;
  deliveryMethod?: string;
}): Promise<Order> {
  const res = await apiClient.post<OrderResponse>(
    "/orders",
    {
      addressId: params.addressId,
      paymentMethod: params.paymentMethod,
      shipping: params.shipping,
      tax: params.tax,
      shippingMethod: params.shippingMethod || params.deliveryMethod,
      deliveryMethod: params.deliveryMethod || params.shippingMethod,
    },
    { auth: true }
  );
  return mapOrder(res);
}

export async function updateOrderStatus(
  orderId: string,
  status: OrderStatus,
  note?: string
): Promise<Order | null> {
  try {
    const res = await apiClient.patch<OrderResponse>(
      `/orders/${orderId}/status`,
      { status, note: note || "" },
      { auth: true }
    );
    return mapOrder(res);
  } catch (e) {
    const msg = (e as any)?.message || "Failed to update status";
    throw new Error(msg);
  }
}

export async function getStatusHistory(
  orderId: string
): Promise<OrderStatusHistory[]> {
  try {
    const res = await apiClient.get<OrderStatusHistoryResponse[]>(
      `/orders/${orderId}/history`,
      { auth: true }
    );
    return res.map((h) => ({
      id: h.id,
      order_id: h.orderId,
      status: h.status,
      statusType: h.statusType ?? "ORDER",
      note: h.note,
      changed_by: h.changedBy,
      createdAt: h.createdAt,
    }));
  } catch {
    return [];
  }
}

export async function cancelOrder(orderId: string): Promise<Order | null> {
  return updateOrderStatus(orderId, "CANCELED" as OrderStatus, "Canceled by user");
}
