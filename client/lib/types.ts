export type UserRole = "ROLE_USER" | "ROLE_ADMIN";

export type User = {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  isDeleted?: boolean;
  deletedAt?: string | null;
  createdAt: string;
};

export type Address = {
  id: string;
  userId?: string;
  label?: string;
  street: string;
  city: string;
  state?: string;
  postal_code: string;
  zip?: string;
  country: string;
  is_default: boolean;
  isDefault?: boolean;
  phone?: string;
  createdAt?: string;
};

export type ShippingAddress = Address & {
  name?: string;
  apt?: string;
};

export type Category = {
  id: string;
  name: string;
  description?: string;
  parent_id?: string | null;
  parentId?: string | null;
  isDeleted?: boolean;
  image?: string;
};

export type ProductImage = {
  id: string;
  product_id: string;
  productId?: string;
  url: string;
  alt_text?: string;
  altText?: string;
  display_order: number;
  displayOrder?: number;
  is_primary: boolean;
  isPrimary?: boolean;
};

export type Product = {
  id: string;
  slug: string;
  name: string;
  description: string;
  price: number;
  stock_quantity: number;
  stockQuantity?: number;
  stock?: number;
  rating: number;
  review_count: number;
  reviewCount?: number;
  reviews?: number;
  is_new_arrival: boolean;
  isNewArrival?: boolean;
  category_id: string;
  categoryId?: string;
  category?: string;
  deleted_at?: string | null;
  deletedAt?: string | null;
  images: ProductImage[];
  image?: string;
  breadcrumb?: string[];
  specs?: { label: string; value: string }[];
  configs?: string[];
  features?: string[];
};

export type Cart = {
  id: string;
  userId?: string;
  createdAt: string;
};

export type CartItem = {
  id: string;
  cartItemId?: string;
  product_id: string;
  productId?: string;
  slug: string;
  name: string;
  variant?: string;
  price: number;
  image: string;
  qty: number;
  quantity?: number;
  stock?: string;
  subtotal?: number;
};

export type WishlistItem = {
  id?: string;
  userId?: string;
  product_id: string;
  productId?: string;
  slug: string;
  addedAt: string;
  createdAt?: string;
};

export type OrderStatus = "PROCESSING" | "CONFIRMED" | "SHIPPED" | "DELIVERED" | "CANCELED" | "COMPLETED";
export type LegacyOrderStatus = "PENDING" | "CANCELLED";
export type PaymentStatus = "UNPAID" | "PAID" | "EXPIRED" | "REFUNDING" | "REFUNDED";
export type LegacyPaymentStatus = "PENDING" | "FAILED";
export type DeliveryStatus = "PLACED" | "SHIPPING" | "ARRIVED" | "COLLECTED" | "RETURNING" | "RETURNED";

export type OrderItem = {
  id?: string;
  order_id?: string;
  product_id: string;
  productId?: string;
  slug?: string;
  name?: string;
  image?: string;
  quantity: number;
  price: number;
  subtotal: number;
};

export type OrderStatusHistory = {
  id: string;
  order_id: string;
  status: OrderStatus;
  statusType?: string;
  note?: string;
  changed_by?: string;
  createdAt: string;
};

export type Order = {
  id: string;
  order_number: string;
  orderNumber?: string;
  userId?: string;
  userEmail?: string;
  userName?: string;
  total_amount: number;
  totalAmount?: number;
  total?: number;
  status: OrderStatus;
  payment_status: PaymentStatus;
  paymentStatus?: PaymentStatus;
  delivery_status?: DeliveryStatus;
  deliveryStatus?: DeliveryStatus;
  shipping_address: Address;
  shippingAddress?: Address;
  tracking_number?: string | null;
  trackingNumber?: string | null;
  carrier?: string | null;
  estimated_delivery?: string | null;
  estimatedDelivery?: string | null;
  items: OrderItem[];
  subtotal?: number;
  shipping?: number;
  tax?: number;
  deliveryMethod?: DeliveryMethod;
  paymentMethod?: PaymentMethod;
  createdAt: string;
  statusHistory?: OrderStatusHistory[];
};

export type PaymentMethodType = "ESEWA" | "KHALTI" | "CREDIT_CARD" | "PAYPAL" | "STRIPE";
export type PaymentMethod = "esewa" | "khalti";
export type LegacyPaymentMethod = "card" | "paypal" | "stripe";
export type PaymentStatusType = "UNPAID" | "PAID" | "EXPIRED" | "REFUNDING" | "REFUNDED";
export type LegacyPaymentStatusType = "PENDING" | "COMPLETED" | "FAILED";

export type Payment = {
  id: string;
  order_id: string;
  orderId?: string;
  method: PaymentMethodType;
  transaction_id: string;
  transactionId?: string;
  status: PaymentStatusType;
  amount: number;
  createdAt: string;
};

export type CardDetails = {
  number: string;
  expiry: string;
  name: string;
};

export type DeliveryMethod = "standard" | "express";

export type Review = {
  id: string;
  userId: string;
  productId: string;
  product_id?: string;
  rating: number;
  comment?: string;
  is_verified_purchase: boolean;
  isVerifiedPurchase?: boolean;
  userName?: string;
  createdAt: string;
  productSlug?: string;
};

export type CheckoutState = {
  shippingAddressId: string | null;
  deliveryMethod: DeliveryMethod;
  paymentMethod: PaymentMethod;
  card?: CardDetails;
  sameBilling: boolean;
};

export type ApiResponse<T> = {
  data: T;
  error?: string;
  meta?: { total?: number; page?: number };
};

export type Paginated<T> = {
  data: T[];
  total: number;
  page: number;
  limit: number;
};
