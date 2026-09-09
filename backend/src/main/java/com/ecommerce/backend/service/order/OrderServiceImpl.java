package com.ecommerce.backend.service.order;

import com.ecommerce.backend.dto.response.OrderDetailResponse;
import com.ecommerce.backend.dto.response.OrderItemResponse;
import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse placeOrder(String userId, OrderRequest request) {
        log.info("Placing order for userId: {} with addressId: {}", userId, request.addressId());
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found with id: {}", userId);
                        return new IllegalArgumentException("User not found");
                    });

            Cart cart = cartRepository.findByUserId(userId)
                    .orElseThrow(() -> {
                        log.warn("Cart not found for userId: {}", userId);
                        return new IllegalArgumentException("Cart is empty");
                    });

            if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
                log.warn("Cart is empty for userId: {}", userId);
                throw new IllegalArgumentException("Cart is empty");
            }

            Address address = addressRepository.findById(request.addressId())
                    .orElseThrow(() -> {
                        log.warn("Address not found with id: {}", request.addressId());
                        return new IllegalArgumentException("Address not found");
                    });

            if (!address.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - address {} does not belong to user {}", request.addressId(), userId);
                throw new IllegalArgumentException("Address not found");
            }

            String shippingAddressJson = convertAddressToJson(address);

            Order order = Order.builder()
                    .user(user)
                    .orderNumber("TMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .totalAmount(BigDecimal.ZERO)
                    .subtotal(BigDecimal.ZERO)
                    .shippingCost(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO)
                    .paymentMethod(request.paymentMethod() != null && !request.paymentMethod().isBlank() ? request.paymentMethod().toLowerCase() : null)
                    .status(OrderStatus.PROCESSING)
                    .paymentStatus(OrderPaymentStatus.UNPAID)
                    .deliveryStatus(DeliveryStatus.PLACED)
                    .shippingAddress(shippingAddressJson)
                    .build();

            Order savedOrder = saveOrderWithRetry(order);
            log.info("Created order with id: {} and orderNumber: {} for userId: {}", savedOrder.getId(), savedOrder.getOrderNumber(), userId);

            BigDecimal total = BigDecimal.ZERO;
            for (CartItem cartItem : cart.getCartItems()) {
                Product product = cartItem.getProduct();
                // Use pessimistic lock to prevent oversell
                Product freshProduct = productRepository.findByIdForUpdate(product.getId())
                        .orElseGet(() -> productRepository.findById(product.getId())
                        .orElseThrow(() -> {
                            log.warn("Product not found with id: {}", product.getId());
                            return new IllegalArgumentException("Product not found: " + product.getName());
                        }));

                if (freshProduct.getStockQuantity() < cartItem.getQuantity()) {
                    log.warn("Insufficient stock for product {} (id: {}): requested {}, available {}", freshProduct.getName(), freshProduct.getId(), cartItem.getQuantity(), freshProduct.getStockQuantity());
                    throw new IllegalStateException("Insufficient stock for product " + freshProduct.getName());
                }
                if (freshProduct.getDeletedAt() != null) {
                    throw new IllegalArgumentException("Product not available: " + freshProduct.getName());
                }

                BigDecimal price = freshProduct.getPrice();
                BigDecimal subtotal = price.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                OrderItem orderItem = OrderItem.builder()
                        .order(savedOrder)
                        .product(freshProduct)
                        .quantity(cartItem.getQuantity())
                        .price(price)
                        .build();

                orderItemRepository.save(orderItem);
                log.debug("Created orderItem for product {} quantity {} price {}", freshProduct.getId(), cartItem.getQuantity(), price);

                freshProduct.setStockQuantity(freshProduct.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(freshProduct);
                log.debug("Updated stock for product {} to {}", freshProduct.getId(), freshProduct.getStockQuantity());

                total = total.add(subtotal);
            }

            BigDecimal subtotalAmount = total;

            // Derive tax server-side (8%) rather than trusting the client value,
            // so the client cannot zero out or inflate the tax charge.
            BigDecimal tax = subtotalAmount.multiply(new BigDecimal("0.08"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // Shipping derived strictly from allowed method, not arbitrary client amount.
            BigDecimal shipping;
            String methodRaw = request.shippingMethod() != null ? request.shippingMethod() : request.deliveryMethod();
            if (methodRaw != null && !methodRaw.isBlank()) {
                String m = methodRaw.trim().toLowerCase();
                if ("express".equals(m)) shipping = new BigDecimal("15.00");
                else if ("standard".equals(m)) shipping = BigDecimal.ZERO;
                else throw new IllegalArgumentException("Invalid shipping method: must be standard or express");
            } else {
                // Legacy fallback: strict validation of BigDecimal to prevent arbitrary inflation/waiving
                if (request.shipping() == null || request.shipping().compareTo(BigDecimal.ZERO) == 0) {
                    shipping = BigDecimal.ZERO;
                } else if (request.shipping().compareTo(new BigDecimal("15")) == 0 || request.shipping().compareTo(new BigDecimal("15.00")) == 0) {
                    shipping = new BigDecimal("15.00");
                } else {
                    throw new IllegalArgumentException("Invalid shipping amount: must be 0 or 15");
                }
            }

            BigDecimal grandTotal = subtotalAmount.add(shipping).add(tax);
            savedOrder.setSubtotal(subtotalAmount);
            savedOrder.setShippingCost(shipping);
            savedOrder.setTax(tax);
            savedOrder.setTotalAmount(grandTotal);
            orderRepository.save(savedOrder);
            log.info("Order totals for orderId: {} subtotal={} shipping={} tax={} total={}", savedOrder.getId(), subtotalAmount, shipping, tax, grandTotal);

            try {
                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(savedOrder)
                        .status(OrderStatus.PROCESSING)
                        .statusType(OrderStatusHistory.HistoryStatusType.ORDER)
                        .note("Order placed — awaiting confirmation")
                        .changedBy(userId)
                        .build();
                orderStatusHistoryRepository.save(history);
                log.debug("Created initial OrderStatusHistory for orderId: {}", savedOrder.getId());
            } catch (Exception e) {
                log.error("Failed to create initial status history for orderId: {}", savedOrder.getId(), e);
                throw e;
            }

            log.info("Order {} placed for userId: {} — cart preserved until payment confirmed", savedOrder.getId(), userId);

            return toOrderResponse(savedOrder);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to place order for userId: {} with addressId: {}", userId, request.addressId(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderForUser(String orderId, String userId) {
        log.debug("Fetching order by id: {} for userId: {}", orderId, userId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });

            if (!order.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - order {} does not belong to user {}", orderId, userId);
                throw new IllegalArgumentException("Order not found");
            }

            return toOrderResponse(order);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch order {} for userId: {}", orderId, userId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String userId, Pageable pageable) {
        log.debug("Fetching all orders for userId: {} with pageable: {}", userId, pageable);
        try {
            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be ≤100");
            Page<Order> page = orderRepository.findByUserId(userId, pageable);
            log.info("Retrieved {} orders for userId: {}", page.getNumberOfElements(), userId);
            return page.map(this::toOrderResponse);
        } catch (Exception e) {
            log.error("Failed to fetch orders for userId: {}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrdersFiltered(String userId, Pageable pageable, String status, String deliveryStatus, String paymentStatus, String search) {
        log.debug("Fetching filtered my orders for userId: {} status={} delivery={} payment={} search={} pageable={}", userId, status, deliveryStatus, paymentStatus, search, pageable);
        try {
            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be ≤100");
            String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
            if (normalizedSearch != null && normalizedSearch.length() > 100) normalizedSearch = normalizedSearch.substring(0,100);
            OrderStatus os = null; DeliveryStatus ds = null; OrderPaymentStatus ps = null;
            if (status != null && !status.isBlank()) {
                try { os = OrderStatus.valueOf(mapLegacyStatus(status)); } catch (Exception e) { throw new IllegalArgumentException("Invalid order status: " + status); }
            }
            if (deliveryStatus != null && !deliveryStatus.isBlank()) {
                try { ds = DeliveryStatus.valueOf(deliveryStatus.toUpperCase()); } catch (Exception e) { throw new IllegalArgumentException("Invalid delivery status: " + deliveryStatus); }
            }
            if (paymentStatus != null && !paymentStatus.isBlank()) {
                try { ps = OrderPaymentStatus.valueOf(mapLegacyPaymentStatus(paymentStatus)); } catch (Exception e) { throw new IllegalArgumentException("Invalid payment status: " + paymentStatus); }
            }
            if (os == null && ds == null && ps == null && normalizedSearch == null) {
                Page<Order> page = orderRepository.findByUserId(userId, pageable);
                return page.map(this::toOrderResponse);
            }
            Page<Order> page = orderRepository.findByUserIdFiltered(userId, os, ds, ps, normalizedSearch, pageable);
            log.info("Retrieved {} filtered orders for userId: {}", page.getNumberOfElements(), userId);
            return page.map(this::toOrderResponse);
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) {
            log.error("Failed to fetch filtered orders for userId: {}", userId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus) {
        return updateOrderStatus(orderId, newStatus, null, null);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus, String note, String changedBy) {
        log.info("Updating order status for orderId: {} to {} by {}", orderId, newStatus, changedBy);
        try {
            Order order = orderRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });

            if (!isValidTransition(order.getStatus(), newStatus)) {
                log.warn("Invalid status transition from {} to {} for orderId: {}", order.getStatus(), newStatus, orderId);
                throw new IllegalArgumentException("Cannot transition from " + order.getStatus() + " to " + newStatus);
            }

            OrderStatus oldStatus = order.getStatus();
            boolean shouldRestore = newStatus == OrderStatus.CANCELED && (oldStatus == OrderStatus.PROCESSING || oldStatus == OrderStatus.CONFIRMED);
            boolean goodsNotShipped = order.getDeliveryStatus() == DeliveryStatus.PLACED;
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.CANCELED) {
                if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
                    order.setPaymentStatus(OrderPaymentStatus.REFUNDING);
                    log.info("Auto payment REFUNDING for canceled order {}", orderId);
                } else if (order.getPaymentStatus() == OrderPaymentStatus.UNPAID) {
                    order.setPaymentStatus(OrderPaymentStatus.EXPIRED);
                    log.info("Auto payment EXPIRED for canceled order {}", orderId);
                }
                if (order.getDeliveryStatus() == DeliveryStatus.SHIPPING || order.getDeliveryStatus() == DeliveryStatus.ARRIVED) {
                    order.setDeliveryStatus(DeliveryStatus.RETURNING);
                    log.info("Auto delivery RETURNING for canceled order {}", orderId);
                }
                if (shouldRestore && goodsNotShipped) {
                    try { restoreStockForOrder(order); } catch (Exception e) { log.warn("Stock restore failed for canceled order {}", orderId, e); }
                } else if (shouldRestore && !goodsNotShipped) {
                    log.warn("Skipping stock restore for canceled order {} because goods have already shipped (delivery={})", orderId, order.getDeliveryStatus());
                }
            }
            if (newStatus == OrderStatus.SHIPPED) {
                if (order.getDeliveryStatus() == DeliveryStatus.PLACED) {
                    order.setDeliveryStatus(DeliveryStatus.SHIPPING);
                    log.info("Auto delivery SHIPPING for shipped order {}", orderId);
                }
            } else if (newStatus == OrderStatus.DELIVERED) {
                if (order.getDeliveryStatus() == DeliveryStatus.PLACED || order.getDeliveryStatus() == DeliveryStatus.SHIPPING) {
                    order.setDeliveryStatus(DeliveryStatus.ARRIVED);
                    log.info("Auto delivery ARRIVED for delivered order {}", orderId);
                }
            } else if (newStatus == OrderStatus.COMPLETED) {
                if (order.getDeliveryStatus() != DeliveryStatus.RETURNED && order.getDeliveryStatus() != DeliveryStatus.RETURNING) {
                    order.setDeliveryStatus(DeliveryStatus.COLLECTED);
                    log.info("Auto delivery COLLECTED for completed order {}", orderId);
                }
            }
            Order saved = orderRepository.save(order);
            log.info("Successfully updated order {} to status {} (payment={}, delivery={})", orderId, newStatus, saved.getPaymentStatus(), saved.getDeliveryStatus());

            try {
                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(saved)
                        .status(newStatus)
                        .statusType(OrderStatusHistory.HistoryStatusType.ORDER)
                        .note(note)
                        .changedBy(changedBy)
                        .build();
                orderStatusHistoryRepository.save(history);
                log.debug("Created OrderStatusHistory for orderId: {} with status {}", orderId, newStatus);
            } catch (Exception e) {
                log.error("Failed to create status history for orderId: {}", orderId, e);
                throw e;
            }

            return toOrderResponse(saved);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to update order status for orderId: {}", orderId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(String orderId, String userId) {
        return getOrderHistory(orderId, userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(String orderId, String userId, boolean isAdmin) {
        log.debug("Fetching order history for orderId: {} by userId: {} isAdmin: {}", orderId, userId, isAdmin);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });

            if (!isAdmin && !order.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - order {} does not belong to user {}", orderId, userId);
                throw new IllegalArgumentException("Order not found");
            }

            List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderId(orderId);
            log.info("Retrieved {} history entries for orderId: {} (isAdmin={})", history.size(), orderId, isAdmin);
            return history.stream()
                    .map(orderMapper::toHistoryResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch order history for orderId: {}", orderId, e);
            throw e;
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Order saveOrderWithRetry(Order order) {
        for (int i = 0; i < 3; i++) {
            try {
                order.setOrderNumber(generateOrderNumber());
                return orderRepository.save(order);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                if (i == 2) throw e;
                log.warn("Order number collision, retrying {}/3", i + 1);
            }
        }
        throw new IllegalStateException("Failed to generate unique order number");
    }

    private String convertAddressToJson(Address address) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("label", address.getLabel());
            map.put("street", address.getStreet());
            map.put("city", address.getCity());
            map.put("state", address.getState());
            map.put("postalCode", address.getPostalCode());
            map.put("country", address.getCountry());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert address to JSON for addressId: {}", address.getId(), e);
            throw new IllegalStateException("Failed to process shipping address", e);
        } catch (Exception e) {
            log.error("Unexpected error converting address to JSON for addressId: {}", address.getId(), e);
            throw e;
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        try {
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
            return orderMapper.toOrderResponse(order, items);
        } catch (Exception e) {
            log.error("Failed to build order response for orderId: {}", order.getId(), e);
            throw e;
        }
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (to == from) return false;
        // Block deprecated targets - map should have converted PENDING->PROCESSING, CANCELLED->CANCELED
        if (to == OrderStatus.PENDING || to == OrderStatus.CANCELLED) return false;
        // Deprecated sources map to canonical
        OrderStatus canonicalFrom = from == OrderStatus.PENDING ? OrderStatus.PROCESSING : (from == OrderStatus.CANCELLED ? OrderStatus.CANCELED : from);
        return switch (canonicalFrom) {
            case PROCESSING -> to == OrderStatus.CONFIRMED || to == OrderStatus.CANCELED;
            case CONFIRMED -> to == OrderStatus.SHIPPED || to == OrderStatus.CANCELED;
            case SHIPPED -> to == OrderStatus.DELIVERED;
            case DELIVERED -> to == OrderStatus.COMPLETED;
            case CANCELED, COMPLETED -> false;
            default -> false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersForAdmin(Pageable pageable, OrderStatus status, String search) {
        log.debug("Fetching all orders for admin with status: {} search: {} pageable: {}", status, search, pageable);
        try {
            String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
            if (normalizedSearch != null) {
                normalizedSearch = normalizedSearch.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                if (normalizedSearch.length() > 100) normalizedSearch = normalizedSearch.substring(0, 100);
            }
            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be ≤100");
            Page<Order> page = orderRepository.findByAdminFilter(status, normalizedSearch, pageable);
            log.info("Retrieved {} orders for admin (total {} across {} pages) status={} search={}", page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages(), status, normalizedSearch);
            return page.map(this::toOrderResponse);
        } catch (Exception e) {
            log.error("Failed to fetch all orders for admin with status: {} search: {}", status, search, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable, String status, String deliveryStatus, String paymentStatus, String search) {
        log.debug("Fetching all orders with status={} delivery={} payment={} search={} pageable={}", status, deliveryStatus, paymentStatus, search, pageable);
        try {
            if (pageable.getPageSize() > 100) throw new IllegalArgumentException("Page size must be ≤100");
            String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
            if (normalizedSearch != null) {
                normalizedSearch = normalizedSearch.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                if (normalizedSearch.length() > 100) normalizedSearch = normalizedSearch.substring(0, 100);
            }
            OrderStatus os = null; DeliveryStatus ds = null; OrderPaymentStatus ps = null;
            if (status != null && !status.isBlank()) {
                try { os = OrderStatus.valueOf(mapLegacyStatus(status)); } catch (Exception e) { throw new IllegalArgumentException("Invalid order status: " + status); }
            }
            if (deliveryStatus != null && !deliveryStatus.isBlank()) {
                try { ds = DeliveryStatus.valueOf(deliveryStatus.toUpperCase()); } catch (Exception e) { throw new IllegalArgumentException("Invalid delivery status: " + deliveryStatus); }
            }
            if (paymentStatus != null && !paymentStatus.isBlank()) {
                try { ps = OrderPaymentStatus.valueOf(mapLegacyPaymentStatus(paymentStatus)); } catch (Exception e) { throw new IllegalArgumentException("Invalid payment status: " + paymentStatus); }
            }
            Page<Order> page = orderRepository.findAdminFiltered(os, ds, ps, normalizedSearch, pageable);
            log.info("Retrieved {} orders (total {} across {} pages)", page.getNumberOfElements(), page.getTotalElements(), page.getTotalPages());
            return page.map(this::toOrderResponse);
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) {
            log.error("Failed to fetch all orders with filters", e);
            throw e;
        }
    }

    private String mapLegacyStatus(String s) {
        String u = s.toUpperCase();
        if ("PENDING".equals(u)) return "PROCESSING";
        if ("CANCELLED".equals(u)) return "CANCELED";
        return u;
    }
    private String mapLegacyPaymentStatus(String s) {
        String u = s.toUpperCase();
        if ("PENDING".equals(u)) return "UNPAID";
        if ("FAILED".equals(u)) return "EXPIRED";
        return u;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, String userId) {
        log.info("Cancel order {} by user {}", orderId, userId);
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUser().getUserId().equals(userId)) throw new IllegalArgumentException("Order not found");
        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only PROCESSING or CONFIRMED orders can be canceled");
        }
        boolean shouldRestore = order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.CONFIRMED;
        // Only restore stock if goods have not left the warehouse (avoid oversell
        // when a package is already SHIPPING/ARRIVED/COLLECTED/DELIVERED).
        boolean goodsNotShipped = order.getDeliveryStatus() == DeliveryStatus.PLACED;
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            order.setPaymentStatus(OrderPaymentStatus.REFUNDING);
            log.info("Auto payment REFUNDING for canceled order {}", orderId);
        } else if (order.getPaymentStatus() == OrderPaymentStatus.UNPAID) {
            order.setPaymentStatus(OrderPaymentStatus.EXPIRED);
        }
        order.setStatus(OrderStatus.CANCELED);
        if (order.getDeliveryStatus() == DeliveryStatus.SHIPPING || order.getDeliveryStatus() == DeliveryStatus.ARRIVED) {
            order.setDeliveryStatus(DeliveryStatus.RETURNING);
        }
        if (shouldRestore && goodsNotShipped) {
            try { restoreStockForOrder(order); } catch (Exception e) { log.warn("Stock restore failed for cancel order {}", orderId, e); }
        } else if (shouldRestore && !goodsNotShipped) {
            log.warn("Skipping stock restore for canceled order {} because goods have already shipped (delivery={})", orderId, order.getDeliveryStatus());
        }
        Order saved = orderRepository.save(order);
        try {
            OrderStatusHistory h = OrderStatusHistory.builder().order(saved).status(OrderStatus.CANCELED).statusType(OrderStatusHistory.HistoryStatusType.ORDER).note("Canceled by user").changedBy(userId).build();
            orderStatusHistoryRepository.save(h);
        } catch (Exception e) { log.warn("Failed to log cancel history for {}", orderId, e); }
        return toOrderResponse(saved);
    }

    private void restoreStockForOrder(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items == null || items.isEmpty()) items = orderItemRepository.findByOrderIdWithProduct(order.getId());
        for (OrderItem oi : items) {
            try {
                Product p = oi.getProduct();
                if (p == null) continue;
                Product fresh = productRepository.findByIdForUpdate(p.getId()).orElseGet(() -> productRepository.findById(p.getId()).orElse(null));
                if (fresh == null) continue;
                int current = fresh.getStockQuantity() != null ? fresh.getStockQuantity() : 0;
                int restore = oi.getQuantity() != null ? oi.getQuantity() : 0;
                fresh.setStockQuantity(current + restore);
                productRepository.save(fresh);
                log.info("Restored stock for product {} +{} -> {} (order cancel {})", fresh.getId(), restore, fresh.getStockQuantity(), order.getId());
            } catch (Exception ex) {
                log.warn("Failed to restore stock for item {} order {}", oi.getId(), order.getId(), ex);
            }
        }
    }

    @Override
    @Transactional
    public OrderResponse updateFulfillment(String orderId, com.ecommerce.backend.dto.request.OrderFulfillmentRequest request, String adminId) {
        return updateFulfillment(orderId, request.trackingNumber(), request.carrier(), request.estimatedDelivery(), adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse verifyDeleteOrder(String orderId) {
        log.info("Verifying delete for orderId: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toOrderDetailResponse(order);
    }

    @Override
    @Transactional
    public void deleteOrder(String orderId, String adminId) {
        log.info("Admin {} requesting delete for orderId: {}", adminId, orderId);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        log.warn("DELETE VERIFICATION: orderId={} orderNumber={} userId={} userEmail={} total={} status={} payment={} delivery={} items={} createdAt={} adminId={}",
                order.getId(), order.getOrderNumber(),
                order.getUser() != null ? order.getUser().getUserId() : "null",
                order.getUser() != null ? order.getUser().getEmail() : "null",
                order.getTotalAmount(), order.getStatus(), order.getPaymentStatus(), order.getDeliveryStatus(),
                order.getOrderItems() != null ? order.getOrderItems().size() : 0,
                order.getCreatedAt(), adminId);

        boolean isTerminal = order.getStatus() == OrderStatus.CANCELED
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getDeliveryStatus() == DeliveryStatus.RETURNED;
        if (!isTerminal) {
            log.warn("Deleting non-terminal order {} status={} — admin confirmed via frontend verification, proceeding with stock restoration", orderId, order.getStatus());
        }

        boolean shouldRestoreStock = order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.CONFIRMED;
        // Only restore stock if goods have not left the warehouse (same rule as cancel,
        // preventing oversell when a package is already SHIPPING/ARRIVED/COLLECTED/DELIVERED).
        boolean goodsNotShipped = order.getDeliveryStatus() == DeliveryStatus.PLACED;
        if (shouldRestoreStock && goodsNotShipped) {
            try {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                if (items == null || items.isEmpty()) {
                    items = orderItemRepository.findByOrderIdWithProduct(order.getId());
                }
                for (OrderItem oi : items) {
                    try {
                        Product p = oi.getProduct();
                        if (p != null) {
                            Product fresh = productRepository.findByIdForUpdate(p.getId()).orElseGet(() -> productRepository.findById(p.getId()).orElse(null));
                            if (fresh != null) {
                                int current = fresh.getStockQuantity() != null ? fresh.getStockQuantity() : 0;
                                int restore = oi.getQuantity() != null ? oi.getQuantity() : 0;
                                fresh.setStockQuantity(current + restore);
                                productRepository.save(fresh);
                                log.info("Restored stock for product {} +{} → {} (order delete {})", fresh.getId(), restore, fresh.getStockQuantity(), orderId);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to restore stock for item {} in order {}", oi.getId(), orderId, ex);
                    }
                }
            } catch (Exception e) {
                log.warn("Stock restoration failed for order {} — proceeding with delete", orderId, e);
            }
        }

        try {
            orderRepository.delete(order);
            orderRepository.flush();
            log.info("Successfully deleted order {} orderNumber={} by admin {}", orderId, order.getOrderNumber(), adminId);
        } catch (Exception e) {
            log.error("Failed to delete order {} by admin {}", orderId, adminId, e);
            throw new IllegalStateException("Failed to delete order: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderFullDetails(String orderId) {
        log.debug("Fetching full details for orderId: {}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });
            return toOrderDetailResponse(order);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch full details for orderId: {}", orderId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderFullDetails(String orderId, String requestingUserId, boolean isAdmin) {
        log.debug("Fetching full details for orderId: {} by userId: {} isAdmin: {}", orderId, requestingUserId, isAdmin);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });

            if (!isAdmin && !order.getUser().getUserId().equals(requestingUserId)) {
                log.warn("Access denied - order {} does not belong to user {} and not admin", orderId, requestingUserId);
                throw new IllegalArgumentException("Order not found");
            }

            return toOrderDetailResponse(order);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch full details for orderId: {} by userId: {}", orderId, requestingUserId, e);
            throw e;
        }
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusWithNote(String orderId, OrderStatus status, String note, String changedBy) {
        log.info("Updating order status with note for orderId: {} to {} by {} note: {}", orderId, status, changedBy, note);
        return updateOrderStatus(orderId, status, note, changedBy);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(String orderId, OrderPaymentStatus newPaymentStatus, String note, String changedBy) {
        log.info("Updating payment status for orderId: {} to {} by {}", orderId, newPaymentStatus, changedBy);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!isValidPaymentTransition(order.getPaymentStatus(), newPaymentStatus)) {
            log.warn("Invalid payment transition {} -> {} for order {}", order.getPaymentStatus(), newPaymentStatus, orderId);
            throw new IllegalArgumentException("Cannot transition payment from " + order.getPaymentStatus() + " to " + newPaymentStatus);
        }
        order.setPaymentStatus(newPaymentStatus);
        if (newPaymentStatus == OrderPaymentStatus.PAID && order.getStatus() == OrderStatus.PROCESSING && order.getStatus() != OrderStatus.CANCELED) {
            order.setStatus(OrderStatus.CONFIRMED);
            log.info("Auto order CONFIRMED for paid order {}", orderId);
        }
        if (newPaymentStatus == OrderPaymentStatus.REFUNDED && order.getDeliveryStatus() == DeliveryStatus.RETURNED) {
            log.info("Payment refunded for returned order {}", orderId);
        }
        Order saved = orderRepository.save(order);
        try {
            OrderStatusHistory h = OrderStatusHistory.builder().order(saved).status(saved.getStatus()).statusType(OrderStatusHistory.HistoryStatusType.PAYMENT).note((note != null ? note + " | " : "") + "payment:" + newPaymentStatus).changedBy(changedBy).build();
            orderStatusHistoryRepository.save(h);
        } catch (Exception e) {
            log.warn("Failed to log payment history for {}", orderId, e);
        }
        return toOrderResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updateDeliveryStatus(String orderId, DeliveryStatus newDeliveryStatus, String note, String changedBy) {
        log.info("Updating delivery status for orderId: {} to {} by {}", orderId, newDeliveryStatus, changedBy);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELED && newDeliveryStatus != DeliveryStatus.RETURNED) {
            log.warn("Cannot update delivery for canceled order {}", orderId);
            throw new IllegalArgumentException("Order is canceled — delivery locked");
        }
        if (!isValidDeliveryTransition(order.getDeliveryStatus(), newDeliveryStatus)) {
            log.warn("Invalid delivery transition {} -> {} for order {}", order.getDeliveryStatus(), newDeliveryStatus, orderId);
            throw new IllegalArgumentException("Cannot transition delivery from " + order.getDeliveryStatus() + " to " + newDeliveryStatus);
        }
        order.setDeliveryStatus(newDeliveryStatus);
        if (newDeliveryStatus == DeliveryStatus.SHIPPING && (order.getStatus() == OrderStatus.PROCESSING || order.getStatus() == OrderStatus.CONFIRMED)) {
            order.setStatus(OrderStatus.SHIPPED);
            log.info("Auto order SHIPPED on shipping for order {}", orderId);
        }
        if (newDeliveryStatus == DeliveryStatus.ARRIVED && order.getStatus() == OrderStatus.SHIPPED) {
            order.setStatus(OrderStatus.DELIVERED);
            log.info("Auto order DELIVERED on arrived for order {}", orderId);
        }
        if (newDeliveryStatus == DeliveryStatus.COLLECTED) {
            order.setStatus(OrderStatus.COMPLETED);
            log.info("Auto order COMPLETED on collected for order {}", orderId);
        }
        if (newDeliveryStatus == DeliveryStatus.RETURNED) {
            if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
                order.setPaymentStatus(OrderPaymentStatus.REFUNDING);
                log.info("Auto payment REFUNDING on returned for order {}", orderId);
            }
        }
        Order saved = orderRepository.save(order);
        try {
            OrderStatusHistory h = OrderStatusHistory.builder().order(saved).status(saved.getStatus()).statusType(OrderStatusHistory.HistoryStatusType.DELIVERY).note((note != null ? note + " | " : "") + "delivery:" + newDeliveryStatus).changedBy(changedBy).build();
            orderStatusHistoryRepository.save(h);
        } catch (Exception e) {
            log.warn("Failed to log delivery history for {}", orderId, e);
        }
        return toOrderResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse updateFulfillment(String orderId, String trackingNumber, String carrier, java.time.LocalDate estimatedDelivery, String changedBy) {
        log.info("Updating fulfillment for orderId: {} by {} tracking={} carrier={} estimatedDelivery={}", orderId, changedBy, trackingNumber, carrier, estimatedDelivery);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELED && order.getDeliveryStatus() != DeliveryStatus.RETURNED) {
            log.warn("Cannot update fulfillment for canceled order {} unless delivery is RETURNED", orderId);
            if (trackingNumber != null || carrier != null || estimatedDelivery != null) {
                throw new IllegalArgumentException("Order is canceled — fulfillment locked");
            }
        }
        boolean changed = false;
        if (trackingNumber != null) {
            String trimmedTn = trackingNumber.trim();
            if (trimmedTn.length() > 100) throw new IllegalArgumentException("trackingNumber too long (max 100)");
            order.setTrackingNumber(trimmedTn.isBlank() ? null : trimmedTn);
            changed = true;
        }
        if (carrier != null) {
            String trimmedCa = carrier.trim();
            if (trimmedCa.length() > 100) throw new IllegalArgumentException("carrier too long (max 100)");
            order.setCarrier(trimmedCa.isBlank() ? null : trimmedCa);
            changed = true;
        }
        if (estimatedDelivery != null) {
            order.setEstimatedDelivery(estimatedDelivery);
            changed = true;
        }
        if (!changed) {
            log.warn("No fulfillment fields provided for order {}", orderId);
            throw new IllegalArgumentException("No fulfillment fields provided");
        }
        Order saved = orderRepository.save(order);
        try {
            String note = "fulfillment: tracking=" + saved.getTrackingNumber() + " carrier=" + saved.getCarrier() + " estimatedDelivery=" + saved.getEstimatedDelivery();
            OrderStatusHistory h = OrderStatusHistory.builder().order(saved).status(saved.getStatus()).statusType(OrderStatusHistory.HistoryStatusType.FULFILLMENT).note(note).changedBy(changedBy).build();
            orderStatusHistoryRepository.save(h);
        } catch (Exception e) {
            log.warn("Failed to log fulfillment history for {}", orderId, e);
        }
        return toOrderResponse(saved);
    }

    private boolean isValidPaymentTransition(OrderPaymentStatus from, OrderPaymentStatus to) {
        if (to == from) return false;
        if (to == OrderPaymentStatus.PENDING || to == OrderPaymentStatus.FAILED) return false;
        OrderPaymentStatus cf = from == OrderPaymentStatus.PENDING ? OrderPaymentStatus.UNPAID : (from == OrderPaymentStatus.FAILED ? OrderPaymentStatus.EXPIRED : from);
        if (cf == OrderPaymentStatus.EXPIRED || cf == OrderPaymentStatus.REFUNDED) return false;
        return switch (cf) {
            case UNPAID -> to == OrderPaymentStatus.PAID || to == OrderPaymentStatus.EXPIRED;
            case PAID -> to == OrderPaymentStatus.REFUNDING || to == OrderPaymentStatus.REFUNDED;
            case REFUNDING -> to == OrderPaymentStatus.REFUNDED;
            default -> false;
        };
    }

    private boolean isValidDeliveryTransition(DeliveryStatus from, DeliveryStatus to) {
        if (to == from) return false;
        return switch (from) {
            case PLACED -> to == DeliveryStatus.SHIPPING;
            case SHIPPING -> to == DeliveryStatus.ARRIVED;
            case ARRIVED -> to == DeliveryStatus.COLLECTED || to == DeliveryStatus.RETURNING;
            case COLLECTED -> to == DeliveryStatus.RETURNING;
            case RETURNING -> to == DeliveryStatus.RETURNED;
            case RETURNED -> false;
        };
    }

    private OrderDetailResponse toOrderDetailResponse(Order order) {
        try {
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(order.getId());
            List<OrderItemResponse> itemResponses = items.stream()
                    .map(orderMapper::toOrderItemResponse)
                    .collect(Collectors.toList());

            List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderId(order.getId());
            List<OrderStatusHistoryResponse> historyResponses = history.stream()
                    .map(orderMapper::toHistoryResponse)
                    .collect(Collectors.toList());

            User user = order.getUser();

            return new OrderDetailResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    user != null ? user.getUserId() : null,
                    user != null ? user.getEmail() : null,
                    user != null ? user.getUsername() : null,
                    order.getTotalAmount(),
                    order.getStatus(),
                    order.getPaymentStatus(),
                    order.getDeliveryStatus(),
                    order.getShippingAddress(),
                    order.getTrackingNumber(),
                    order.getCarrier(),
                    order.getEstimatedDelivery(),
                    itemResponses,
                    historyResponses,
                    order.getCreatedAt(),
                    order.getUpdatedAt()
            );
        } catch (Exception e) {
            log.error("Failed to build order detail response for orderId: {}", order.getId(), e);
            throw e;
        }
    }
}
