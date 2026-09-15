# ApexCommerce

A full-stack e-commerce application with a Spring Boot REST API backend and a Next.js frontend. ApexCommerce supports product catalog management, shopping cart, wishlist, multi-step checkout, order processing, and integrated digital wallet payments (Khalti and eSewa) for the Nepalese market.

The application serves two user roles: **customers** who browse products and place orders, and **admins** who manage the entire catalog, orders, users, and view dashboard analytics.

---

## Features

### Customer Features

- User registration and JWT-based authentication (login, logout, token refresh)
- Product browsing with search, category filtering, price filtering, rating filtering, and new-arrival filtering
- Sorting by price, rating, name, and creation date (ascending/descending)
- Server-side pagination for product listings
- Product detail pages with multiple images and customer reviews
- Wishlist toggle (add/remove products)
- Shopping cart with add, update quantity, remove, and clear operations
- Guest cart stored in localStorage, merged to server cart on login
- Multi-step checkout: shipping address selection, delivery method selection, payment method selection, order review
- Address management (create, update, delete, set default)
- Order placement with server-side stock deduction and tax/shipping calculation
- Order history with status, delivery status, and payment status tracking
- Order cancellation and return requests
- Payment via Khalti (KPG-2) and eSewa (ePay v2)
- Product review and rating (1-5 stars, one review per user per product)

### Admin Features

- Admin dashboard with total users, products, orders, revenue, and recent orders
- Order statistics breakdown and revenue statistics
- Product CRUD with image upload, reorder, primary image selection, and multi-image creation
- Category management with parent-child hierarchy
- Order management: update order status, payment status, delivery status, fulfillment (tracking number, carrier, estimated delivery)
- Order deletion (with force-delete for non-terminal orders)
- User management: list all users (including soft-deleted), paginated search, role changes, activate/deactivate, hard delete
- Review management: list all reviews with rating filter
- Top products listing

---

## Technology Stack

### Backend

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 4.1.1 | Application framework |
| Spring Security | (managed) | Authentication and authorization |
| Spring Data JPA | (managed) | Database access |
| Hibernate | (managed) | ORM |
| MySQL | (via mysql-connector-j) | Production database |
| H2 | (test scope) | In-memory test database |
| JJWT | 0.11.5 | JWT token generation and validation |
| Lombok | 1.18.46 | Boilerplate reduction |
| Jackson | (managed) | JSON serialization |
| Jakarta Bean Validation | (managed) | Request validation |
| Maven | (managed) | Build tool |

### Frontend

| Technology | Version | Purpose |
|---|---|---|
| Next.js | 15.5.7 | React framework (App Router) |
| React | 19.0.0 | UI library |
| TypeScript | 5.7.3 | Type safety |
| Tailwind CSS | 3.4.17 | Utility-first CSS |
| PostCSS | 8.4.49 | CSS processing |
| Autoprefixer | 10.4.20 | CSS vendor prefixing |

### Testing

| Technology | Purpose |
|---|---|
| JUnit 5 | Test framework |
| Mockito | Mocking |
| AssertJ | Assertions |
| Spring Boot Test | Integration testing |
| Spring Security Test | Security testing |
| H2 Database | In-memory test database |
| MockMvc | Controller testing |

---

## Architecture

```
Next.js Frontend (port 3000)
        |
        | API rewrites (/api/* -> backend)
        v
Spring Boot REST API (port 8081)
        |
        v
    Controllers
        |
        v
     Services
        |
        v
   Repositories (Spring Data JPA)
        |
        v
      MySQL Database
```

**Backend layers:**
- **Controllers** handle HTTP requests and return DTOs
- **Services** contain business logic and transaction management
- **Repositories** provide data access via Spring Data JPA
- **Entities** map to database tables
- **DTOs** (Java records) separate API contracts from persistence
- **Mappers** convert between entities and DTOs

**Frontend layers:**
- **App Router pages** handle routing and server/client components
- **Contexts** manage global state (auth, cart, wishlist)
- **Lib utilities** provide API clients, type definitions, and helper functions
- **Components** encapsulate reusable UI elements

---

## Project Structure

```
ecommerce/
├── backend/
│   ├── src/
│   │   ├── main/java/com/ecommerce/backend/
│   │   │   ├── BackendApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── DataInitializer.java
│   │   │   │   ├── ProductDataSeeder.java
│   │   │   │   ├── JsonConfig.java
│   │   │   │   ├── CustomUserDetails.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── AdminController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── ProductImageController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── CartController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── AddressController.java
│   │   │   │   ├── ReviewController.java
│   │   │   │   └── WishlistController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/       (17 record types)
│   │   │   │   └── response/      (19 record types)
│   │   │   ├── entity/            (14 entities)
│   │   │   ├── enums/             (6 enums)
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserNotFoundException.java
│   │   │   ├── mapper/            (10 mapper components)
│   │   │   ├── repository/        (14 repositories)
│   │   │   ├── security/
│   │   │   │   ├── JwtUtil.java
│   │   │   │   └── JwtAuthenticationFilter.java
│   │   │   ├── service/
│   │   │   │   ├── address/
│   │   │   │   ├── admin/
│   │   │   │   ├── auth/
│   │   │   │   ├── cart/
│   │   │   │   ├── category/
│   │   │   │   ├── order/
│   │   │   │   ├── payment/
│   │   │   │   ├── product/
│   │   │   │   ├── productimage/
│   │   │   │   ├── refreshtoken/
│   │   │   │   ├── review/
│   │   │   │   ├── storage/
│   │   │   │   ├── user/
│   │   │   │   └── wishlist/
│   │   │   └── specification/
│   │   │       └── ProductSpecification.java
│   │   ├── main/resources/
│   │   │   └── application.properties
│   │   └── test/
│   │       └── java/com/ecommerce/backend/
│   │           ├── controller/     (17 test files)
│   │           ├── service/        (20 test files)
│   │           ├── mapper/         (10 test files)
│   │           ├── security/       (2 test files)
│   │           ├── exception/      (1 test file)
│   │           ├── specification/  (1 test file)
│   │           └── config/         (1 test file)
│   └── pom.xml
│
├── client/
│   ├── app/                        (26 pages)
│   │   ├── layout.tsx
│   │   ├── page.tsx
│   │   ├── login/
│   │   ├── register/
│   │   ├── catalog/
│   │   ├── products/
│   │   ├── product/[slug]/
│   │   ├── new-arrivals/
│   │   ├── cart/
│   │   ├── wishlist/
│   │   ├── orders/
│   │   ├── orders/[id]/
│   │   ├── account/
│   │   ├── addresses/
│   │   ├── checkout/shipping/
│   │   ├── checkout/delivery/
│   │   ├── checkout/payment/
│   │   ├── checkout/review/
│   │   ├── order-confirmed/
│   │   ├── payment/callback/
│   │   ├── esewa/callback/
│   │   └── admin/
│   │       ├── layout.tsx
│   │       ├── page.tsx
│   │       ├── products/
│   │       ├── orders/
│   │       ├── categories/
│   │       ├── users/
│   │       └── reviews/
│   ├── components/                 (17 components)
│   ├── context/                    (4 contexts)
│   ├── hooks/                      (3 hooks)
│   ├── lib/                        (18 utility files)
│   ├── package.json
│   ├── next.config.mjs
│   ├── tailwind.config.ts
│   └── tsconfig.json
│
└── README.md
```

---

## Authentication & Security

### JWT Implementation

- **Access token**: 15-minute expiration, HS256-signed, type claim = `access`
- **Refresh token**: 7-day expiration, type claim = `refresh`, stored in the database
- **Password hashing**: BCrypt
- **Token storage**: Both access and refresh tokens are stored in `localStorage` on the client
- **Secret validation**: Application fails to start if JWT secret is less than 32 bytes

### Token Refresh Flow

1. Client sends expired/401 response to refresh endpoint with the refresh token
2. Backend validates refresh token type, expiration, and revocation status (with pessimistic locking)
3. Old refresh token is revoked; new access + refresh token pair is issued (token rotation)
4. Maximum 5 active refresh tokens per user; oldest is revoked when limit is reached

### Security Configuration

- Stateless HTTP sessions (no server-side session)
- CSRF disabled
- CORS configured via `CORS_ALLOWED_ORIGINS` environment variable (defaults to `http://localhost:3000,http://localhost:3001`)
- Custom 401 and 403 JSON error responses
- JWT authentication filter processes Bearer tokens before Spring Security's default filter
- Refresh tokens are rejected by the JWT filter (only access tokens are authenticated)

### Public Endpoints (no authentication required)

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/users` (registration)
- `GET /api/products/**` (except `/api/products/stats`)
- `GET /api/categories/**`
- `GET /api/reviews/product/{productId}`
- `GET /api/products/{id}/images`
- `GET /api/payments/khalti/callback`
- `GET /uploads/**`

---

## User Roles

| Role | Access |
|---|---|
| `USER` | Browse products, manage cart/wishlist/addresses, place orders, write reviews, manage own account |
| `ADMIN` | All USER permissions plus admin dashboard, product/category/order/user/review management, order status updates, analytics |

Admin role is enforced via `@PreAuthorize("hasRole('ADMIN')")` on controller methods and inline role checks in services.

An admin user is seeded at startup: `ecommerce@gmail.com` / `ecommerce@gmail`.

---

## Product Catalog

### Products

- UUID primary key, name, description (TEXT), price, stock quantity, rating, review count
- `isNewArrival` flag for new arrival filtering
- Optimistic locking via `@Version` field for concurrent update protection
- Soft delete via `deletedAt` timestamp with custom `@SQLDelete` that increments version

### Categories

- UUID primary key, name, optional parent category for hierarchy
- Self-referencing parent-child hierarchy with self-parent rejection and cycle protection
- Deletion nullifies parent reference on child categories

### Product Images

- UUID primary key, URL, alt text, display order, primary flag
- Stored locally in the `uploads/` directory with UUID filenames
- Supported formats: JPEG, JPG, PNG, WebP, GIF
- Magic byte validation to prevent uploading disguised files
- Max file size: 10MB
- Admin-only upload, delete, reorder, and primary selection

### Search & Filtering

- Case-insensitive partial-name match, category filter (single category ID)
- Price range filter (min/max)
- New arrival filter
- Minimum rating filter
- Sortable by: name, price, createdAt, rating, stockQuantity, reviewCount
- Paginated results

---

## Cart

- One cart per authenticated user (created automatically on first item add)
- Add product with quantity (stock validation with pessimistic locking)
- Update item quantity (stock validation)
- Remove individual items
- Clear entire cart
- Restore cart from a cancelled order
- Unique constraint: one cart item per product per cart

### Guest Cart

- Guest users can add items to a cart stored in `localStorage` under the key `apexcommerce_guest_cart`
- On login, the `AuthContext` merges the guest cart items into the server-side cart
- A custom event `apexcommerce_guest_cart_updated` coordinates between components

---

## Wishlist

- Toggle product in/out of wishlist (POST with productId)
- Retrieve all wishlist items for the current user
- Remove specific wishlist item by wishlist ID
- Unique constraint: one wishlist entry per user per product
- Frontend caches wishlist in `localStorage` with a 10-second TTL

---

## Address Management

- Create, update, delete addresses
- Each address belongs to a single user
- Fields: label, street, city, state, postal code, country, isDefault flag
- Addresses are stored as JSON in the order's `shippingAddress` field at checkout time

---

## Checkout & Orders

### Checkout Flow

1. **Shipping**: User selects or creates a shipping address
2. **Delivery**: User selects delivery method (standard = free, express = 15.00)
3. **Payment**: User selects payment method (Khalti or eSewa)
4. **Review**: User reviews order summary and places the order
5. **Confirmation**: Order is created with `PROCESSING` status

### Order Creation

- Reads all items from the user's cart
- Deducts stock using pessimistic locking (`SELECT ... FOR UPDATE`) per product
- Calculates subtotal from product prices and quantities
- Applies 8% tax server-side on the subtotal
- Applies shipping based on delivery method (0 for standard, 15.00 for express)
- Creates `OrderItem` records with price-at-purchase snapshots
- Generates a unique order number (`ORD-XXXXXXXX`, retried on collision)
- Stores shipping address as JSON
- Records initial `OrderStatusHistory` entry
- Preserves the cart at order creation; the cart is cleared only after successful wallet verification

### Order Statuses

**OrderStatus** (the overall order lifecycle):

| Status | Description |
|---|---|
| `PROCESSING` | Order placed, awaiting confirmation |
| `CONFIRMED` | Order confirmed (auto-set after successful payment) |
| `SHIPPED` | Order has been shipped |
| `DELIVERED` | Order delivered to customer |
| `CANCELED` | Order cancelled (stock restored) |
| `COMPLETED` | Order fully completed |

**OrderPaymentStatus**:

| Status | Description |
|---|---|
| `UNPAID` | No payment received |
| `PAID` | Full payment received |
| `EXPIRED` | Payment window expired |
| `REFUNDING` | Refund in progress |
| `REFUNDED` | Payment refunded |

**DeliveryStatus**:

| Status | Description |
|---|---|
| `PLACED` | Order placed |
| `SHIPPING` | In transit |
| `ARRIVED` | Arrived at destination |
| `COLLECTED` | Collected by customer |
| `RETURNING` | Return in progress |
| `RETURNED` | Returned |

### Order Features

- Customer order listing with optional filtering by status, delivery status, payment status, and search
- Full order details (items, payments, status history)
- Customer cancellation (restores stock)
- Customer return request (sets delivery status to `RETURNING`)
- Admin order management: update status, payment status, delivery status, fulfillment details (tracking number, carrier, estimated delivery)
- Order status history with notes and actor tracking (`OrderStatusHistory` entity)
- Admin order deletion with cascade (removes order items, payments, status history); requires force flag for non-terminal orders

---

## Payments

### Transaction ID model (both gateways)

- **`transactionId`** — our server-generated reference, a fresh `TXN-` + UUID value per initiate call. Stored in `payments.transaction_id` (unique). It is never the order ID.
- **`transactionCode`** — the payment gateway's reference. Stored in `payments.txn_code`. For eSewa this is the gateway `transaction_code`; for Khalti it is the lookup response's `transaction_id` (falling back to `tidx`).
- **`pidx`** — Khalti's payment session ID, stored in its own `payments.pidx` column and used for all Khalti gateway calls. Never confused with `transactionId`.
- API responses expose only the canonical keys `transactionId`, `transactionCode` (plus legacy alias `txnCode` on Khalti lookup), and `orderId` — raw gateway keys never leak to the client.

### Khalti (KPG-2)

1. **Initiate**: Backend calls Khalti's `/epayment/initiate/` API with order amount (in paisa), customer info, and product details
2. Khalti returns a `pidx` and `payment_url`
3. Frontend redirects user to Khalti's payment page
4. **Callback**: After payment, Khalti redirects to `/payment/callback` with `pidx`
5. **Lookup**: Frontend calls `POST /api/payments/khalti/lookup` with the `pidx`
6. Backend calls Khalti's `/epayment/lookup/` API to verify the transaction
7. On `Completed` status: Payment marked `PAID`, Order marked `CONFIRMED`, cart cleared
8. On `User canceled` or `Expired`: Payment marked `EXPIRED`
9. On `Refunded` or `Partially Refunded`: Payment marked `REFUNDED`
10. On `Pending` or `Initiated`: Payment stays `INITIATED` (frontend keeps polling/retrying lookup)

- Sandbox/production mode configurable via `KHALTI_MODE` (default: `sandbox`)
- Idempotent: returns existing `pidx` if already initiated and not expired
- Minimum amount: Rs. 10 (1000 paisa)

### eSewa (ePay v2)

1. **Initiate**: Backend generates a fresh server-side `transaction_uuid` (`TXN-` + UUID, never the order ID) and computes an HMAC-SHA256 signature over `total_amount`, `transaction_uuid`, and `product_code`
2. Returns form fields (gateway URL, signed payload, success/failure URLs) to the frontend
3. Frontend submits a hidden form POST to eSewa's gateway
4. **Callback**: eSewa redirects to `/esewa/callback` with base64-encoded `data`
5. **Verify**: Frontend calls `POST /api/payments/esewa/verify` with the decoded data
6. Backend validates the HMAC-SHA256 signature
7. Backend performs a server-to-server status check via eSewa's transaction status API
8. On `COMPLETE`: Payment marked `PAID`, Order marked `CONFIRMED`, cart cleared
9. On non-`COMPLETE`: Payment marked `EXPIRED`

- Test/production mode configurable via `ESEWA_MODE`
- Product code configurable via `ESEWA_PRODUCT_CODE` (default: `EPAYTEST`)
- Idempotent: already-paid orders return success without re-processing

---

## Reviews

- One review per user per product (unique constraint enforced)
- Rating: 1-5 (validated with `@Min(1)` and `@Max(5)`)
- Optional text comment
- `isVerifiedPurchase` flag (set based on order history)
- Product's average `rating` and `reviewCount` are recalculated after each review create/update/delete
- Admin can view all reviews with optional minimum rating filter
- Admin can delete any review; users can only delete their own

---

## Product Images

- Local file storage in `uploads/` directory
- File validation: content type check, extension check, magic byte validation
- Supported types: JPEG, JPG, PNG, WebP, GIF
- Max upload size: 10MB (configured in `application.properties`)
- Image operations (admin only):
  - Upload single image with alt text, display order, and primary flag
  - Upload URL reference via `ProductImageRequest`
  - Delete image (removes the physical file only for local `/uploads/` files; external URLs skip local deletion)
  - Set primary image (unsets previous primary)
  - Reorder images (batch update display order)
  - Create multiple images during product creation/update (multipart)

---

## Database Model

### Entities and Relationships

```
User (users)
├── Cart (1:1)              → carts.user_id
├── Orders (1:N)            → orders.user_id
├── Addresses (1:N)         → addresses.user_id
├── Wishlists (1:N)         → wishlists.user_id
├── Reviews (1:N)           → reviews.user_id
└── RefreshTokens (1:N)     → refresh_tokens.user_id

Product (products)
├── Category (N:1)          → products.category_id
├── ProductImages (1:N)     → product_images.product_id
├── Reviews (1:N)           → reviews.product_id
├── CartItems (1:N)         → cart_items.product_id
├── OrderItems (1:N)        → order_items.product_id
└── Wishlists (N:N)         → wishlists.product_id

Order (orders)
├── User (N:1)              → orders.user_id
├── OrderItems (1:N)        → order_items.order_id
├── Payments (1:N)          → payments.order_id
└── OrderStatusHistory (1:N) → order_status_history.order_id

Category (categories)
└── Parent (N:1)            → categories.parent_id (self-referencing)

Cart (carts)
└── CartItems (1:N)         → cart_items.cart_id

CartItem (cart_items)
├── Cart (N:1)              → cart_items.cart_id
└── Product (N:1)           → cart_items.product_id

OrderItem (order_items)
├── Order (N:1)             → order_items.order_id
└── Product (N:1)           → order_items.product_id

Payment (payments)
└── Order (N:1)             → payments.order_id
    Key columns: `transaction_id` (unique, server-generated `TXN-` reference),
    `txn_code` (gateway reference), `pidx` (Khalti session ID)

RefreshToken (refresh_tokens)
└── User (N:1)              → refresh_tokens.user_id

Wishlist (wishlists)
├── User (N:1)              → wishlists.user_id
└── Product (N:1)           → wishlists.product_id
```

### Key Design Decisions

- All entities use **UUID** primary keys (`GenerationType.UUID`)
- **Soft delete** on `User` and `Product` (via `@SQLDelete` and `@SQLRestriction`)
- **Optimistic locking** on `Product` (`@Version version` field; custom `@SQLDelete` increments version on soft delete)
- **Pessimistic locking** on stock deduction during order creation and cart operations (`SELECT ... FOR UPDATE`)
- `Order.shippingAddress` stored as **JSON** (snapshot of address at order time)
- `OrderItem.price` stores the **price at time of purchase**
- `OrderStatusHistory` provides a full **audit trail** of all status changes

---

## API Overview

### Authentication

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/auth/login` | Login with email/password | Public |
| `POST` | `/api/auth/refresh` | Refresh token pair | Public |
| `POST` | `/api/auth/logout` | Revoke refresh token | Public |

### Users

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/users` | Register new user | Public |
| `GET` | `/api/users/me` | Get current user | Authenticated |
| `PUT` | `/api/users/me` | Update current user | Authenticated |
| `DELETE` | `/api/users/me` | Soft-delete current user | Authenticated |
| `GET` | `/api/users` | Get all active users | Admin |
| `GET` | `/api/users/all` | Get users (paginated, includes deleted) | Admin |
| `PUT` | `/api/users/{id}/role` | Change user role | Admin |
| `PUT` | `/api/users/{id}/status` | Activate/deactivate user | Admin |
| `DELETE` | `/api/users/{id}` | Hard-delete user | Admin |

### Products

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/products` | List products (paginated) | Public |
| `GET` | `/api/products/search` | Search/filter products | Public |
| `GET` | `/api/products/{id}` | Get product by ID | Public |
| `GET` | `/api/products/{id}/details` | Get product details | Public |
| `GET` | `/api/products/category/{categoryId}` | Get products by category | Public |
| `GET` | `/api/products/stats` | Product statistics | Admin |
| `POST` | `/api/products` | Create product | Admin |
| `PUT` | `/api/products/{id}` | Update product | Admin |
| `DELETE` | `/api/products/{id}` | Delete product | Admin |
| `POST` | `/api/products/with-images` | Create product with images | Admin |
| `PUT` | `/api/products/{id}/with-images` | Update product with images | Admin |

### Product Images

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/products/{productId}/images` | Get all images for product | Public |
| `GET` | `/api/products/{productId}/images/primary` | Get primary image | Public |
| `POST` | `/api/products/{productId}/images` | Add image (URL) | Admin |
| `POST` | `/api/products/{productId}/images/upload` | Upload image (file) | Admin |
| `PUT` | `/api/products/{productId}/images/{imageId}/primary` | Set primary image | Admin |
| `PUT` | `/api/products/{productId}/images/reorder` | Reorder images | Admin |
| `DELETE` | `/api/products/{productId}/images/{imageId}` | Delete image | Admin |

### Categories

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/categories` | List categories | Public |
| `GET` | `/api/categories/{id}` | Get category by ID | Public |
| `POST` | `/api/categories` | Create category | Admin |
| `PUT` | `/api/categories/{id}` | Update category | Admin |
| `DELETE` | `/api/categories/{id}` | Delete category | Admin |

### Cart

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/cart` | Get current cart | Authenticated |
| `POST` | `/api/cart/items` | Add item to cart | Authenticated |
| `PUT` | `/api/cart/items/{cartItemId}` | Update item quantity | Authenticated |
| `DELETE` | `/api/cart/items/{cartItemId}` | Remove item | Authenticated |
| `DELETE` | `/api/cart` | Clear cart | Authenticated |
| `POST` | `/api/cart/restore-from-order/{orderId}` | Restore cart from order | Authenticated |

### Wishlist

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/wishlist` | Get wishlist | Authenticated |
| `POST` | `/api/wishlist` | Add product to wishlist | Authenticated |
| `DELETE` | `/api/wishlist/{wishlistId}` | Remove from wishlist | Authenticated |

### Addresses

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/addresses` | Get all addresses | Authenticated |
| `GET` | `/api/addresses/{id}` | Get address by ID | Authenticated |
| `POST` | `/api/addresses` | Create address | Authenticated |
| `PUT` | `/api/addresses/{id}` | Update address | Authenticated |
| `DELETE` | `/api/addresses/{id}` | Delete address | Authenticated |

### Orders

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/orders` | Place order | Authenticated |
| `GET` | `/api/orders` | Get my orders | Authenticated |
| `GET` | `/api/orders/{orderId}` | Get order summary | Authenticated |
| `GET` | `/api/orders/{orderId}/full` | Get full order details | Authenticated (admin check inline) |
| `GET` | `/api/orders/{orderId}/history` | Get order status history | Authenticated (admin check inline) |
| `PATCH` | `/api/orders/{orderId}/cancel` | Cancel order | Authenticated |
| `PATCH` | `/api/orders/{orderId}/return` | Request return | Authenticated |
| `GET` | `/api/orders/all` | Get all orders | Admin |
| `PATCH` | `/api/orders/{orderId}/status` | Update order status | Admin |
| `PATCH` | `/api/orders/{orderId}/payment-status` | Update payment status | Admin |
| `PATCH` | `/api/orders/{orderId}/delivery-status` | Update delivery status | Admin |
| `PATCH` | `/api/orders/{orderId}/fulfillment` | Update fulfillment details | Admin |
| `GET` | `/api/orders/{orderId}/verify-delete` | Preview delete impact | Admin |
| `DELETE` | `/api/orders/{orderId}` | Delete order | Admin |

### Payments

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/payments/order/{orderId}` | Get payments for order | Authenticated |
| `POST` | `/api/payments/khalti/initiate` | Initiate Khalti payment | Authenticated |
| `POST` | `/api/payments/khalti/lookup` | Verify Khalti transaction | Authenticated |
| `GET` | `/api/payments/khalti/callback` | Khalti redirect callback | Public |
| `POST` | `/api/payments/esewa/initiate` | Initiate eSewa payment | Authenticated |
| `POST` | `/api/payments/esewa/verify` | Verify eSewa payment | Authenticated |

### Reviews

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/reviews/product/{productId}` | Get reviews for product | Public |
| `POST` | `/api/reviews` | Create review | Authenticated |
| `PUT` | `/api/reviews/{reviewId}` | Update review | Authenticated |
| `DELETE` | `/api/reviews/{reviewId}` | Delete review | Authenticated (admin can delete any) |
| `GET` | `/api/reviews/all` | Get all reviews | Admin |

### Admin Dashboard

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/admin/stats` | Dashboard statistics | Admin |
| `GET` | `/api/admin/stats/orders` | Order statistics | Admin |
| `GET` | `/api/admin/stats/revenue` | Revenue statistics | Admin |
| `GET` | `/api/admin/orders` | All orders (paginated) | Admin |
| `GET` | `/api/admin/users` | All users | Admin |
| `GET` | `/api/admin/products` | All products (paginated) | Admin |
| `GET` | `/api/admin/products/top` | Top products | Admin |

---

## Environment Configuration

### Backend (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `server.port` | `8081` | Backend server port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/ecommerce?createDatabaseIfNotExist=true&...` | MySQL connection URL (database auto-created if missing) |
| `spring.datasource.username` | `root` | Database username |
| `spring.datasource.password` | `mysql` | Database password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema management strategy |
| `jwt.secret` | (env `JWT_SECRET`) | HS256 signing secret (min 32 bytes; app fails to start otherwise) |
| `jwt.expiration` | `900000` | Access token lifetime (15 min) |
| `jwt.refresh-expiration` | `604800000` | Refresh token lifetime (7 days) |
| `app.upload.dir` | `uploads/` | Local file storage path |
| `app.upload.base-url` | `http://localhost:8081/uploads/` | Base URL for uploaded files |
| `khalti.secret-key` | (env `KHALTI_SECRET_KEY`, no default) | Khalti API secret |
| `khalti.mode` | `sandbox` (env `KHALTI_MODE`) | Khalti environment (`sandbox`/`prod`) |
| `khalti.timeout-ms` | `10000` | Khalti API call timeout |
| `app.payment.return-url` | `http://localhost:3000/payment/callback` (env `APP_RETURN_URL`) | Khalti redirect target |
| `app.payment.website-url` | `http://localhost:3000` (env `APP_WEBSITE_URL`) | Base site URL for payment links |
| `esewa.product-code` | `EPAYTEST` (env `ESEWA_PRODUCT_CODE`) | eSewa product code |
| `esewa.secret-key` | `8gBm/:&EnhH.1/q` (env `ESEWA_SECRET_KEY`) | eSewa HMAC secret (test default) |
| `esewa.mode` | `test` (env `ESEWA_MODE`) | eSewa environment (`test`/`prod`) |
| `esewa.success-url` / `esewa.failure-url` | `<website-url>/esewa/callback` (env `ESEWA_SUCCESS_URL` / `ESEWA_FAILURE_URL`) | eSewa redirect targets |
| `esewa.timeout-ms` | `10000` | eSewa status-check timeout |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | CORS origins |

### Frontend (`client/.env` copied from `.env.example`)

| Variable | Description |
|---|---|
| `NEXT_PUBLIC_API_URL` | Backend URL for API proxying (default: `http://localhost:8081`) |
| `BACKEND_URL` | Alternative backend URL for server-side rewrites |
| `NEXT_PUBLIC_APP_URL` | Public app URL (default: `http://localhost:3000`) |

---

## Local Setup

### Prerequisites

- Java 21
- Node.js and npm (the repo declares no `engines` field)
- MySQL 8 running on `localhost:3306`

### Database

1. The `ecommerce` database is created automatically on first run (`createDatabaseIfNotExist=true` in the connection URL); ensure MySQL 8 is running on `localhost:3306`
2. The schema is managed automatically by Hibernate (`ddl-auto=update`)

### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend starts on `http://localhost:8081`. An admin user (`ecommerce@gmail.com` / `ecommerce@gmail`) and sample products are seeded automatically on first run.

### Frontend

```bash
cd client
cp .env.example .env
npm install
npm run dev
```

The frontend starts on `http://localhost:3000`. API requests to `/api/*` are proxied to the backend.

---

## Testing

### Framework

- **JUnit 5** as the test framework
- **Mockito** for mocking dependencies
- **AssertJ** for fluent assertions
- **MockMvc** for controller endpoint testing
- **H2 in-memory database** for repository and integration tests

### Test Coverage

The test suite includes (52 test files):

- **Controller tests**: 17 files covering all 12 controllers via MockMvc (Order, Product, ProductImage, Review, and User each have an additional enhanced test)
- **Service tests**: 20 files covering auth, user, product, product-image, category, cart, order, address, wishlist, review, admin, refresh-token, storage, and payment services (eSewa + Khalti), plus order/product-image/review enhanced tests
- **Mapper tests**: All 10 mappers with bidirectional conversion verification
- **Security tests**: `JwtUtilTest`, `JwtAuthenticationFilterTest`
- **Exception tests**: `GlobalExceptionHandlerTest`
- **Config tests**: `CustomUserDetailsServiceTest`
- **Specification tests**: `ProductSpecificationTest`

### Running Tests

```bash
cd backend
mvn test
```

---

## Key Engineering Decisions

- **Dual-token JWT authentication** with token rotation on refresh and pessimistic locking to prevent token replay
- **Pessimistic locking** (`SELECT ... FOR UPDATE`) on stock deduction during order creation and cart operations to prevent overselling
- **Optimistic locking** (`@Version`) on products to detect concurrent update conflicts
- **Soft delete** on users and products to preserve referential integrity and audit trails
- **Server-side validation** of shipping costs and tax to prevent client-side tampering
- **Server-to-server payment verification** (Khalti lookup API, eSewa status check API) as the source of truth for payment status
- **Server-generated payment references**: `transactionId` (`TXN-` + UUID, never the order ID) identifies our payment attempt; `transactionCode` stores the gateway's reference; Khalti's `pidx` lives in its own column
- **Pessimistic locking on payment lookup** (`findByPidxForUpdate`) so concurrent gateway callbacks cannot double-process a payment
- **HMAC-SHA256 signature verification** for eSewa payment responses
- **Order status history** with full audit trail (who changed what, when, with notes)
- **Cart merging** from guest localStorage to server-side cart on user login
- **Magic byte validation** on uploaded images to prevent disguised file uploads
- **Path traversal protection** on file storage operations
- **Refresh token revocation** with maximum active token limit per user
- **Global exception handler** providing consistent JSON error responses across the API
