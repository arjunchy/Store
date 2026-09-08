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
