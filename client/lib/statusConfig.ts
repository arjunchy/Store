import type { OrderStatus, PaymentStatus, DeliveryStatus } from "./types";

export const ORDER_STATUSES: OrderStatus[] = ["PROCESSING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELED", "COMPLETED"];
export const LEGACY_ORDER_STATUSES = ["PENDING", "CANCELLED"] as const;

export const DELIVERY_STATUSES: DeliveryStatus[] = ["PLACED", "SHIPPING", "ARRIVED", "COLLECTED", "RETURNING", "RETURNED"];

export const PAYMENT_STATUSES: PaymentStatus[] = ["UNPAID", "PAID", "EXPIRED", "REFUNDING", "REFUNDED"];
export const LEGACY_PAYMENT_STATUSES = ["PENDING", "FAILED"] as const;

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PROCESSING: "Processing",
  CONFIRMED: "Confirmed",
  SHIPPED: "Shipped",
  DELIVERED: "Delivered",
  CANCELED: "Canceled",
  COMPLETED: "Completed",
};

export const DELIVERY_STATUS_LABEL: Record<DeliveryStatus, string> = {
  PLACED: "Placed",
  SHIPPING: "Shipping",
  ARRIVED: "Arrived",
  COLLECTED: "Collected",
  RETURNING: "Returning",
  RETURNED: "Returned",
};

export const PAYMENT_STATUS_LABEL: Record<PaymentStatus, string> = {
  UNPAID: "Unpaid",
  PAID: "Paid",
  EXPIRED: "Expired",
  REFUNDING: "Refunding",
  REFUNDED: "Refunded",
};

export function getOrderStatusConfig(status: string) {
  const s = status as OrderStatus;
  switch (s) {
    case "PROCESSING":
      return { label: "Processing", color: "bg-[#dbeafe] text-[#1d4ed8] border-[#bfdbfe]", icon: "hourglass_top", terminal: false };
    case "CONFIRMED":
      return { label: "Confirmed", color: "bg-[#dbeafe] text-[#1d4ed8] border-[#bfdbfe]", icon: "verified", terminal: false };
    case "SHIPPED":
      return { label: "Shipped", color: "bg-[#f3e8ff] text-[#7e22ce] border-[#e9d5ff]", icon: "local_shipping", terminal: false };
    case "DELIVERED":
      return { label: "Delivered", color: "bg-[#15803d] text-white border-[#15803d]", icon: "task_alt", terminal: false };
    case "COMPLETED":
      return { label: "Completed", color: "bg-[#15803d] text-white border-[#15803d]", icon: "check_circle", terminal: true };
    case "CANCELED":
      return { label: "Canceled", color: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]", icon: "cancel", terminal: true };
    default:
      if (s === "PENDING") return { label: "Processing", color: "bg-[#dbeafe] text-[#1d4ed8] border-[#bfdbfe]", icon: "hourglass_top", terminal: false };
      if (s === "CANCELLED") return { label: "Canceled", color: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]", icon: "cancel", terminal: true };
      return { label: s, color: "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]", icon: "circle", terminal: false };
  }
}

export function getDeliveryStatusConfig(status: string) {
  switch (status as DeliveryStatus) {
    case "PLACED":
      return { label: "Placed", color: "bg-[#f5f5f4] text-[#57534e] border-[#d6d3d1]", icon: "package_2" };
    case "SHIPPING":
      return { label: "Shipping", color: "bg-[#dbeafe] text-[#1d4ed8] border-[#bfdbfe]", icon: "local_shipping" };
    case "ARRIVED":
      return { label: "Arrived", color: "bg-[#fef3c7] text-[#b45309] border-[#fde68a]", icon: "location_on" };
    case "COLLECTED":
      return { label: "Collected", color: "bg-[#15803d]/10 text-[#15803d] border-[#15803d]/20", icon: "task_alt" };
    case "RETURNING":
      return { label: "Returning", color: "bg-[#fff7ed] text-[#b45309] border-[#ffedd5]", icon: "assignment_return" };
    case "RETURNED":
      return { label: "Returned", color: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]", icon: "assignment_returned" };
    default:
      return { label: status, color: "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]", icon: "local_shipping" };
  }
}

export function getPaymentStatusConfig(status: string) {
  switch (status as PaymentStatus) {
    case "UNPAID":
      return { label: "Unpaid", color: "bg-[#f5f5f4] text-[#57534e] border-[#d6d3d1]", icon: "hourglass_empty" };
    case "PAID":
      return { label: "Paid", color: "bg-[#15803d]/10 text-[#15803d] border-[#15803d]/20", icon: "payments" };
    case "EXPIRED":
      return { label: "Expired", color: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]", icon: "timer_off" };
    case "REFUNDING":
      return { label: "Refunding", color: "bg-[#fff7ed] text-[#b45309] border-[#ffedd5]", icon: "replay" };
    case "REFUNDED":
      return { label: "Refunded", color: "bg-[#f3e8ff] text-[#7e22ce] border-[#e9d5ff]", icon: "receipt" };
    default:
      if (status === "PENDING") return { label: "Unpaid", color: "bg-[#f5f5f4] text-[#57534e] border-[#d6d3d1]", icon: "hourglass_empty" };
      if (status === "FAILED") return { label: "Expired", color: "bg-[#fee2e2] text-[#b91c1c] border-[#fecaca]", icon: "error" };
      return { label: status, color: "bg-[#fafaf9] text-[#57534e] border-[#d6d3d1]", icon: "payments" };
  }
}

export const ORDER_ALLOWED: Record<string, string[]> = {
  PROCESSING: ["CONFIRMED", "CANCELED"],
  CONFIRMED: ["SHIPPED", "CANCELED"],
  SHIPPED: ["DELIVERED"],
  DELIVERED: ["COMPLETED"],
  CANCELED: [],
  COMPLETED: [],
};

export const DELIVERY_ALLOWED: Record<string, string[]> = {
  PLACED: ["SHIPPING"],
  SHIPPING: ["ARRIVED"],
  ARRIVED: ["COLLECTED", "RETURNING"],
  COLLECTED: ["RETURNING"],
  RETURNING: ["RETURNED"],
  RETURNED: [],
};

export const PAYMENT_ALLOWED: Record<string, string[]> = {
  UNPAID: ["PAID", "EXPIRED"],
  PAID: ["REFUNDING", "REFUNDED"],
  REFUNDING: ["REFUNDED"],
  EXPIRED: [],
  REFUNDED: [],
};

export function isValidOrderTransition(from: string, to: string): boolean {
  return (ORDER_ALLOWED[from] || []).includes(to);
}

export function isValidDeliveryTransition(from: string, to: string): boolean {
  return (DELIVERY_ALLOWED[from] || []).includes(to);
}

export function isValidPaymentTransition(from: string, to: string): boolean {
  return (PAYMENT_ALLOWED[from] || []).includes(to);
}

export function getDisabledReason(from: string, to: string): string {
  if (to === "SHIPPED" && from === "PROCESSING") return "Must confirm order first (CONFIRMED)";
  if (to === "DELIVERED" && from === "PROCESSING") return "Must ship order first (SHIPPED)";
  if (to === "DELIVERED" && from === "CONFIRMED") return "Must ship order first (SHIPPED)";
  if (to === "COMPLETED" && from === "PROCESSING") return "Must deliver order first (DELIVERED)";
  if (to === "COMPLETED" && from === "CONFIRMED") return "Must deliver order first (DELIVERED)";
  if (to === "COMPLETED" && from === "SHIPPED") return "Must deliver order first (DELIVERED)";
  if (to === "CANCELED" && from === "COMPLETED") return "Cannot cancel a completed order";
  if (to === "CANCELED" && from === "DELIVERED") return "Cannot cancel a delivered order";
  if (to === "CANCELED" && from === "SHIPPED") return "Cannot cancel a shipped order";
  if (to === "CANCELED" && from === "CANCELED") return "Already canceled";
  if (to === "COMPLETED" && from === "COMPLETED") return "Already completed";
  return "Not allowed from current status";
}
