import { apiClient } from "./api-client";
import type { Payment } from "./types";

type PaymentResponse = {
  id: string;
  orderId: string;
  amount: number;
  method: string;
  transactionId: string;
  status: string;
  createdAt: string;
};

function mapPayment(res: PaymentResponse): Payment {
  return {
    id: res.id,
    order_id: res.orderId,
    orderId: res.orderId,
    method: res.method as Payment["method"],
    transaction_id: res.transactionId,
    transactionId: res.transactionId,
    status: res.status as Payment["status"],
    amount: res.amount,
    createdAt: res.createdAt,
  };
}

export async function getPaymentsByOrder(orderId: string): Promise<Payment[]> {
  try {
    const res = await apiClient.get<PaymentResponse[]>(
      `/payments/order/${orderId}`,
      { auth: true }
    );
    return res.map(mapPayment);
  } catch {
    return [];
  }
}

export async function getPaymentByOrder(orderId: string): Promise<Payment | null> {
  const list = await getPaymentsByOrder(orderId);
  return list[0] ?? null;
}

export async function createPayment(params: {
  orderId: string;
  method: string;
  amount: number;
}): Promise<Payment> {
  const res = await apiClient.post<PaymentResponse>(
    "/payments",
    {
      orderId: params.orderId,
      method: params.method,
      amount: params.amount,
    },
    { auth: true }
  );
  return mapPayment(res);
}

export type KhaltiInitiateResponse = {
  pidx: string;
  paymentUrl: string;
  payment_url?: string;
  expiresAt?: string;
  expires_at?: string;
  expiresIn?: number;
  expires_in?: number;
  orderId: string;
  purchaseOrderId?: string;
  amount: number;
};

export async function initiateKhaltiPayment(orderId: string): Promise<KhaltiInitiateResponse> {
  const res = await apiClient.post<KhaltiInitiateResponse>(
    "/payments/khalti/initiate",
    { orderId },
    { auth: true }
  );
  return {
    pidx: (res as any).pidx,
    paymentUrl: (res as any).paymentUrl || (res as any).payment_url,
    payment_url: (res as any).paymentUrl || (res as any).payment_url,
    expiresAt: (res as any).expiresAt || (res as any).expires_at,
    expires_at: (res as any).expiresAt || (res as any).expires_at,
    expiresIn: (res as any).expiresIn ?? (res as any).expires_in,
    expires_in: (res as any).expiresIn ?? (res as any).expires_in,
    orderId: (res as any).orderId || orderId,
    amount: (res as any).amount ?? 0,
  };
}

export async function lookupKhalti(pidx: string): Promise<{ pidx: string; status: string; total_amount?: number; transaction_id?: string; fee?: number; refunded?: boolean }> {
  const res = await apiClient.post<any>(
    "/payments/khalti/lookup",
    { pidx },
    { auth: true }
  );
  return res;
}

export type EsewaInitiateResponse = {
  gatewayUrl: string;
  productCode: string;
  transactionUuid: string;
  totalAmount: string;
  amount: string;
  taxAmount: string;
  productServiceCharge: string;
  productDeliveryCharge: string;
  successUrl: string;
  failureUrl: string;
  signedFieldNames: string;
  signature: string;
  orderId: string;
  orderNumber: string;
};

export async function initiateEsewaPayment(orderId: string): Promise<EsewaInitiateResponse> {
  const res = await apiClient.post<any>(
    "/payments/esewa/initiate",
    { orderId },
    { auth: true }
  );
  return res as EsewaInitiateResponse;
}

export async function verifyEsewaPayment(params: {
  data?: string;
  transactionUuid?: string;
  transactionCode?: string;
  totalAmount?: string;
}): Promise<any> {
  const res = await apiClient.post<any>(
    "/payments/esewa/verify",
    {
      data: params.data,
      transactionUuid: params.transactionUuid,
      transactionCode: params.transactionCode,
      totalAmount: params.totalAmount,
    },
    { auth: true }
  );
  return res;
}

/**
 * eSewa requires a form-POST (not a GET redirect). Build and auto-submit it.
 */
export function submitEsewaForm(init: EsewaInitiateResponse): void {
  const form = document.createElement("form");
  form.method = "POST";
  form.action = init.gatewayUrl;
  const fields: Record<string, string> = {
    amount: init.amount,
    tax_amount: init.taxAmount,
    total_amount: init.totalAmount,
    transaction_uuid: init.transactionUuid,
    product_code: init.productCode,
    product_service_charge: init.productServiceCharge,
    product_delivery_charge: init.productDeliveryCharge,
    success_url: init.successUrl,
    failure_url: init.failureUrl,
    signed_field_names: init.signedFieldNames,
    signature: init.signature,
  };
  for (const [k, v] of Object.entries(fields)) {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = k;
    input.value = v ?? "";
    form.appendChild(input);
  }
  document.body.appendChild(form);
  form.submit();
}
