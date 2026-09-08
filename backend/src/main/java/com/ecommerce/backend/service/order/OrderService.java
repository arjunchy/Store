package com.ecommerce.backend.service.order;

import com.ecommerce.backend.dto.request.OrderFulfillmentRequest;
import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderDetailResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(String userId, OrderRequest request);

    OrderResponse getOrderForUser(String orderId, String userId);

    Page<OrderResponse> getAllOrders(String userId, Pageable pageable);

    Page<OrderResponse> getMyOrdersFiltered(String userId, Pageable pageable, String status, String deliveryStatus, String paymentStatus, String search);

    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus);

    OrderResponse updateOrderStatus(String orderId, OrderStatus newStatus, String note, String changedBy);

    List<OrderStatusHistoryResponse> getOrderHistory(String orderId, String userId);

    List<OrderStatusHistoryResponse> getOrderHistory(String orderId, String userId, boolean isAdmin);

    Page<OrderResponse> getAllOrdersForAdmin(Pageable pageable, OrderStatus status, String search);

    Page<OrderResponse> getAllOrders(Pageable pageable, String status, String deliveryStatus, String paymentStatus, String search);

    OrderDetailResponse getOrderFullDetails(String orderId);

    OrderDetailResponse getOrderFullDetails(String orderId, String requestingUserId, boolean isAdmin);

    OrderResponse cancelOrder(String orderId, String userId);

    OrderResponse updateOrderStatusWithNote(String orderId, OrderStatus status, String note, String changedBy);

    OrderResponse updatePaymentStatus(String orderId, OrderPaymentStatus newPaymentStatus, String note, String changedBy);

    OrderResponse updateDeliveryStatus(String orderId, DeliveryStatus newDeliveryStatus, String note, String changedBy);

    OrderResponse updateFulfillment(String orderId, String trackingNumber, String carrier, java.time.LocalDate estimatedDelivery, String changedBy);

    OrderResponse updateFulfillment(String orderId, OrderFulfillmentRequest request, String adminId);

    void deleteOrder(String orderId, String adminId);

    OrderDetailResponse verifyDeleteOrder(String orderId);
}
