package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderDetailResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/orders - Placing order for userId: {} with addressId: {}",
                userId,
                request.addressId()
        );

        try {
            OrderResponse response =
                    orderService.placeOrder(userId, request);

            log.info(
                    "POST /api/orders - Successfully placed order with orderNumber: {} for userId: {}",
                    response.orderNumber(),
                    userId
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn(
                    "POST /api/orders - Place order failed for userId: {} - {}",
                    userId,
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "POST /api/orders - Unexpected error for userId: {}",
                    userId,
                    e
            );
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String search,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/orders - Fetching orders for userId: {} with pageable: {} status={} delivery={} payment={} search={}",
                userId,
                pageable, status, deliveryStatus, paymentStatus, search
        );

        try {
            Page<OrderResponse> page;
            if (status != null || deliveryStatus != null || paymentStatus != null || search != null) {
                page = orderService.getMyOrdersFiltered(userId, pageable, status, deliveryStatus, paymentStatus, search);
            } else {
                page = orderService.getAllOrders(userId, pageable);
            }

            log.debug(
                    "GET /api/orders - Retrieved {} orders for userId: {}",
                    page.getNumberOfElements(),
                    userId
            );

            return ResponseEntity.ok(page);

        } catch (IllegalArgumentException e) {
            log.warn("GET /api/orders - Invalid params for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(
                    "GET /api/orders - Failed to fetch orders for userId: {}",
                    userId,
                    e
            );
            throw e;
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/orders/{} - Fetching order for userId: {}",
                orderId,
                userId
        );

        try {
            OrderResponse response =
                    orderService.getOrderForUser(orderId, userId);

            log.debug(
                    "GET /api/orders/{} - Found order for userId: {}",
                    orderId,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "GET /api/orders/{} - Order not found for userId: {}",
                    orderId,
                    userId
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "GET /api/orders/{} - Unexpected error for userId: {}",
                    orderId,
                    userId,
                    e
            );
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Page<OrderResponse>> getAllOrdersForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deliveryStatus,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection,
            Pageable pageable) {

        log.info(
                "GET /api/orders/all - Fetching all orders for ADMIN with status: {} delivery={} payment={} search: {} sortBy={} dir={} pageable: {}",
                status, deliveryStatus, paymentStatus, search, sortBy, sortDirection, pageable
        );

        try {
            if (deliveryStatus != null || paymentStatus != null) {
                Pageable effectivePageable = pageable;
                if (sortBy != null && !sortBy.isBlank()) {
                    java.util.Set<String> allowedSort = java.util.Set.of("createdAt","orderNumber","totalAmount","status");
                    if (!allowedSort.contains(sortBy)) throw new IllegalArgumentException("Invalid sortBy: " + sortBy);
                    org.springframework.data.domain.Sort.Direction dir = "asc".equalsIgnoreCase(sortDirection) ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
                    effectivePageable = org.springframework.data.domain.PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), org.springframework.data.domain.Sort.by(dir, sortBy));
                }
                Page<OrderResponse> page = orderService.getAllOrders(effectivePageable, status, deliveryStatus, paymentStatus, search);
                return ResponseEntity.ok(page);
            }
            OrderStatus orderStatus = null;
            if (status != null && !status.isBlank()) {
                try {
                    String mapped = status.toUpperCase();
                    if ("PENDING".equals(mapped)) mapped = "PROCESSING";
                    if ("CANCELLED".equals(mapped)) mapped = "CANCELED";
                    orderStatus = OrderStatus.valueOf(mapped);
                } catch (IllegalArgumentException e) {
                    log.warn("GET /api/orders/all - Invalid status: {}", status);
                    throw new IllegalArgumentException("Invalid order status: " + status);
                }
            }

            Page<OrderResponse> page = orderService.getAllOrdersForAdmin(pageable, orderStatus, search);

            log.debug(
                    "GET /api/orders/all - Retrieved {} orders",
                    page.getNumberOfElements()
            );

            return ResponseEntity.ok(page);

        } catch (IllegalArgumentException e) {
            log.warn("GET /api/orders/all - Failed - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("GET /api/orders/all - Unexpected error", e);
            throw e;
        }
    }

    @GetMapping("/{orderId}/full")
    public ResponseEntity<OrderDetailResponse> getOrderFullDetails(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        log.info(
                "GET /api/orders/{}/full - Fetching full details for userId: {} isAdmin: {}",
                orderId,
                userId,
                isAdmin
        );

        try {
            OrderDetailResponse response = orderService.getOrderFullDetails(orderId, userId, isAdmin);

            log.debug(
                    "GET /api/orders/{}/full - Found order for userId: {}",
                    orderId,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "GET /api/orders/{}/full - Order not found for userId: {}",
                    orderId,
                    userId
            );
            throw e;
        } catch (Exception e) {
            log.error(
                    "GET /api/orders/{}/full - Unexpected error for userId: {}",
                    orderId,
                    userId,
                    e
            );
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/payment-status")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String ps = body.get("paymentStatus");
        String note = body.get("note");
        if (ps == null || ps.isBlank()) throw new IllegalArgumentException("paymentStatus is required");
        OrderPaymentStatus newPs = OrderPaymentStatus.valueOf(ps.toUpperCase());
        return ResponseEntity.ok(orderService.updatePaymentStatus(orderId, newPs, note, userDetails.getUserId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/delivery-status")
    public ResponseEntity<OrderResponse> updateDeliveryStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String ds = body.get("deliveryStatus");
        String note = body.get("note");
        if (ds == null || ds.isBlank()) throw new IllegalArgumentException("deliveryStatus is required");
        DeliveryStatus newDs = DeliveryStatus.valueOf(ds.toUpperCase());
        return ResponseEntity.ok(orderService.updateDeliveryStatus(orderId, newDs, note, userDetails.getUserId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/fulfillment")
    public ResponseEntity<OrderResponse> updateFulfillment(
            @PathVariable String orderId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String trackingNumber = body.get("trackingNumber");
        if (trackingNumber == null) trackingNumber = body.get("tracking_number");
        String carrier = body.get("carrier");
        String estimatedStr = body.get("estimatedDelivery");
        if (estimatedStr == null) estimatedStr = body.get("estimated_delivery");
        java.time.LocalDate estimated = null;
        if (estimatedStr != null && !estimatedStr.isBlank()) {
            try { estimated = java.time.LocalDate.parse(estimatedStr); }
            catch (Exception e) { throw new IllegalArgumentException("Invalid estimatedDelivery, expected YYYY-MM-DD"); }
        }
        if (trackingNumber != null && trackingNumber.length() > 100) throw new IllegalArgumentException("trackingNumber too long (max 100)");
        if (carrier != null && carrier.length() > 100) throw new IllegalArgumentException("carrier too long (max 100)");
        if ((trackingNumber == null || trackingNumber.isBlank()) && (carrier == null || carrier.isBlank()) && estimated == null) {
            throw new IllegalArgumentException("At least one fulfillment field (trackingNumber, carrier, estimatedDelivery) is required");
        }
        com.ecommerce.backend.dto.request.OrderFulfillmentRequest req = new com.ecommerce.backend.dto.request.OrderFulfillmentRequest(trackingNumber, carrier, estimated);
        String tn = (trackingNumber != null && !trackingNumber.trim().isEmpty()) ? trackingNumber : (body.containsKey("trackingNumber") || body.containsKey("tracking_number") ? trackingNumber : null);
        String ca = (carrier != null && !carrier.trim().isEmpty()) ? carrier : (body.containsKey("carrier") ? carrier : null);
        if (req.trackingNumber() != null || req.carrier() != null || req.estimatedDelivery() != null) {
            return ResponseEntity.ok(orderService.updateFulfillment(orderId, req, userDetails.getUserId()));
        }
        return ResponseEntity.ok(orderService.updateFulfillment(orderId, tn, ca, estimated, userDetails.getUserId()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelMyOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, userDetails.getUserId()));
    }

    @PatchMapping("/{orderId}/return")
    public ResponseEntity<OrderResponse> returnMyOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        orderService.getOrderForUser(orderId, userDetails.getUserId());
        return ResponseEntity.ok(orderService.updateDeliveryStatus(orderId, DeliveryStatus.RETURNING, "Return requested by user", userDetails.getUserId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{orderId}/verify-delete")
    public ResponseEntity<OrderDetailResponse> verifyDeleteOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("GET /api/orders/{}/verify-delete - Admin {} verifying delete", orderId, userDetails.getUserId());
        try {
            OrderDetailResponse detail = orderService.verifyDeleteOrder(orderId);
            log.info("Verified delete payload for order {} orderNumber={} status={}", orderId, detail.orderNumber(), detail.status());
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            log.warn("Verify delete failed for order {}: {}", orderId, e.getMessage());
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> deleteOrder(
            @PathVariable String orderId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("DELETE /api/orders/{} - Admin {} force={}", orderId, userDetails.getUserId(), force);
        try {
            OrderDetailResponse beforeDelete = orderService.verifyDeleteOrder(orderId);
            boolean isTerminal = beforeDelete.status() == OrderStatus.CANCELED
                    || beforeDelete.status() == OrderStatus.COMPLETED
                    || beforeDelete.status() == OrderStatus.DELIVERED
                    || beforeDelete.deliveryStatus() == DeliveryStatus.RETURNED
                    || beforeDelete.deliveryStatus() == DeliveryStatus.COLLECTED;
            if (!isTerminal && !force) {
                log.warn("Delete blocked for non-terminal order {} status={} without force", orderId, beforeDelete.status());
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("error", "Order is not in terminal state");
                body.put("message", "Order status is " + beforeDelete.status() + " — add ?force=true to confirm deletion of active order");
                body.put("orderId", orderId);
                body.put("orderNumber", beforeDelete.orderNumber());
                body.put("status", beforeDelete.status().name());
                body.put("deliveryStatus", beforeDelete.deliveryStatus().name());
                body.put("paymentStatus", beforeDelete.paymentStatus().name());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
            }
            orderService.deleteOrder(orderId, userDetails.getUserId());
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("message", "Order deleted successfully");
            resp.put("orderId", orderId);
            resp.put("orderNumber", beforeDelete.orderNumber());
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            log.warn("DELETE /api/orders/{} - Not found: {}", orderId, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.error("DELETE /api/orders/{} - Failed", orderId, e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, String> statusUpdate,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        String statusStr = statusUpdate.get("status");
        String note = statusUpdate.get("note");

        log.info(
                "PATCH /api/orders/{}/status - userId: {} requesting status: {} note: {}",
                orderId,
                userId,
                statusStr,
                note
        );

        try {
            if (statusStr == null || statusStr.isBlank()) {
                log.warn(
                        "PATCH /api/orders/{}/status - status is required",
                        orderId
                );
                throw new IllegalArgumentException("Status is required");
            }

            OrderStatus newStatus;

            try {
                newStatus =
                        OrderStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn(
                        "PATCH /api/orders/{}/status - Invalid status: {}",
                        orderId,
                        statusStr
                );
                throw new IllegalArgumentException(
                        "Invalid order status: " + statusStr
                );
            }

            OrderResponse response =
                    orderService.updateOrderStatus(
                            orderId,
                            newStatus,
                            note,
                            userId
                    );

            log.info(
                    "PATCH /api/orders/{}/status - Updated to {} for userId: {}",
                    orderId,
                    newStatus,
                    userId
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "PATCH /api/orders/{}/status - Failed - {}",
                    orderId,
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "PATCH /api/orders/{}/status - Unexpected error for orderId: {}",
                    orderId,
                    e
            );
            throw e;
        }
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<OrderStatusHistoryResponse>> getOrderHistory(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        log.info(
                "GET /api/orders/{}/history - Fetching history for userId: {} isAdmin: {}",
                orderId,
                userId,
                isAdmin
        );

        try {
            List<OrderStatusHistoryResponse> history =
                    orderService.getOrderHistory(orderId, userId, isAdmin);

            log.debug(
                    "GET /api/orders/{}/history - Retrieved {} entries",
                    orderId,
                    history.size()
            );

            return ResponseEntity.ok(history);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "GET /api/orders/{}/history - Failed for userId: {} - {}",
                    orderId,
                    userId,
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "GET /api/orders/{}/history - Unexpected error for orderId: {}",
                    orderId,
                    e
            );
            throw e;
        }
    }
}