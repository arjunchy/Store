import { apiClient } from "./api-client";
import type { ShippingAddress, DeliveryMethod, PaymentMethod, CardDetails } from "./types";

type AddressResponse = {
  id: string;
  label: string;
  street: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
  isDefault: boolean;
  createdAt: string;
};

function mapAddress(res: AddressResponse): ShippingAddress {
  return {
    id: res.id,
    label: res.label,
    street: res.street,
    city: res.city,
    state: res.state,
    postal_code: res.postalCode,
    zip: res.postalCode,
    country: res.country,
    is_default: res.isDefault,
    isDefault: res.isDefault,
    createdAt: res.createdAt,
  };
}

export type CheckoutShippingState = {
  addresses: ShippingAddress[];
  selectedId: string | null;
};

export type CheckoutDeliveryState = {
  method: DeliveryMethod;
};

export type CheckoutPaymentState = {
  method: PaymentMethod;
  card?: CardDetails;
  sameBilling: boolean;
};

const DELIVERY_KEY = "apexcommerce_delivery_method";
const PAYMENT_KEY = "apexcommerce_payment_method";

export async function getShipping(): Promise<CheckoutShippingState> {
  try {
    const res = await apiClient.get<AddressResponse[]>("/addresses", {
      auth: true,
    });
    const addresses = res.map(mapAddress);
    const selectedId = addresses.find((a) => a.is_default)?.id || addresses[0]?.id || null;
    return { addresses, selectedId };
  } catch {
    return { addresses: [], selectedId: null };
  }
}

export async function setShipping(state: CheckoutShippingState): Promise<void> {
  if (typeof window !== "undefined") {
    localStorage.setItem("apexcommerce_selected_address", state.selectedId || "");
  }
}

export async function selectAddress(id: string): Promise<void> {
  if (typeof window !== "undefined") {
    localStorage.setItem("apexcommerce_selected_address", id);
  }
}

export async function addAddress(
  addr: Omit<ShippingAddress, "id">
): Promise<ShippingAddress> {
  const res = await apiClient.post<AddressResponse>(
    "/addresses",
    {
      label: addr.label || "",
      street: addr.street,
      city: addr.city,
      state: addr.state || "",
      postalCode: addr.postal_code,
      country: addr.country,
      isDefault: addr.is_default,
    },
    { auth: true }
  );
  return mapAddress(res);
}

export async function updateAddress(
  id: string,
  patch: Partial<Omit<ShippingAddress, "id">>
): Promise<ShippingAddress> {
  const body: Record<string, unknown> = {};
  if (patch.label !== undefined) body.label = patch.label;
  if (patch.street !== undefined) body.street = patch.street;
  if (patch.city !== undefined) body.city = patch.city;
  if (patch.state !== undefined) body.state = patch.state;
  if (patch.postal_code !== undefined || patch.zip !== undefined) body.postalCode = patch.postal_code ?? patch.zip;
  if (patch.country !== undefined) body.country = patch.country;
  if (patch.is_default !== undefined || patch.isDefault !== undefined) body.isDefault = patch.is_default ?? patch.isDefault;
  const res = await apiClient.put<AddressResponse>(
    `/addresses/${id}`,
    body,
    { auth: true }
  );
  return mapAddress(res);
}

export async function deleteAddress(id: string): Promise<void> {
  await apiClient.delete(`/addresses/${id}`, { auth: true });
}

export async function setDefaultAddress(id: string): Promise<void> {
  await updateAddress(id, { is_default: true, isDefault: true });
}

export async function getDelivery(): Promise<CheckoutDeliveryState> {
  if (typeof window === "undefined") return { method: "standard" };
  const stored = localStorage.getItem(DELIVERY_KEY) || localStorage.getItem("apexcommerce_v1_checkout_delivery");
  if (!stored) return { method: "standard" };
  try {
    const parsed = JSON.parse(stored);
    // handle legacy string "standard"/"express" stored directly
    if (typeof parsed === "string") return { method: parsed as DeliveryMethod };
    if (parsed && typeof parsed.method === "string") {
      if (parsed.method === "standard" || parsed.method === "express") return { method: parsed.method };
    }
    return { method: "standard" };
  } catch {
    return { method: "standard" };
  }
}

export async function setDelivery(state: CheckoutDeliveryState): Promise<void> {
  if (typeof window !== "undefined") {
    localStorage.setItem(DELIVERY_KEY, JSON.stringify(state));
    localStorage.setItem("apexcommerce_v1_checkout_delivery", JSON.stringify(state));
  }
}

export async function getPayment(): Promise<CheckoutPaymentState> {
  if (typeof window === "undefined") return { method: "esewa", sameBilling: true };
  const stored = localStorage.getItem(PAYMENT_KEY);
  if (stored) {
    try {
      const parsed = JSON.parse(stored) as CheckoutPaymentState;
      if ((parsed.method as string) === "card" || (parsed.method as string) === "paypal" || (parsed.method as string) === "stripe") {
        return { method: "esewa", sameBilling: true };
      }
      if (parsed.method === "esewa" || parsed.method === "khalti") return parsed;
    } catch {}
  }
  return { method: "esewa", sameBilling: true };
}

export async function setPayment(state: CheckoutPaymentState): Promise<void> {
  if (typeof window !== "undefined") {
    localStorage.setItem(PAYMENT_KEY, JSON.stringify(state));
  }
}

let cachedShipping: { data: CheckoutShippingState; ts: number } | null = null;
const SHIPPING_CACHE_TTL = 5000;
export async function getSelectedAddress(): Promise<ShippingAddress | null> {
  let s: CheckoutShippingState;
  if (cachedShipping && Date.now() - cachedShipping.ts < SHIPPING_CACHE_TTL) {
    s = cachedShipping.data;
  } else {
    s = await getShipping();
    cachedShipping = { data: s, ts: Date.now() };
  }
  if (typeof window !== "undefined") {
    const selectedId = localStorage.getItem("apexcommerce_selected_address");
    if (selectedId) {
      const found = s.addresses.find((a) => a.id === selectedId);
      if (found) return found;
      // stale id -> clear
      if (s.addresses.length > 0) {
        localStorage.removeItem("apexcommerce_selected_address");
      }
    }
  }
  return s.addresses.find((a) => a.is_default) ?? s.addresses[0] ?? null;
}

export async function clearCheckout(): Promise<void> {
  if (typeof window !== "undefined") {
    localStorage.removeItem(DELIVERY_KEY);
    localStorage.removeItem("apexcommerce_v1_checkout_delivery");
    localStorage.removeItem(PAYMENT_KEY);
    localStorage.removeItem("apexcommerce_selected_address");
    cachedShipping = null;
  }
}
