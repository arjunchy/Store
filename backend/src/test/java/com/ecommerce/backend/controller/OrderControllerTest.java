package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.service.order.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Mock
    private CustomUserDetails userDetails;

    private OrderResponse sampleResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sampleResponse = new OrderResponse(
                "ord-1", "ORD-1234", new BigDecimal("500.00"),
                OrderStatus.PENDING, OrderPaymentStatus.PENDING,
                DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void placeOrder_success_returns201() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(orderService.placeOrder(eq("user-1"), any(OrderRequest.class))).thenReturn(sampleResponse);

        OrderRequest request = new OrderRequest("addr-1");
        ResponseEntity<OrderResponse> response = orderController.placeOrder(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().orderNumber()).isEqualTo("ORD-1234");
        verify(orderService).placeOrder("user-1", request);
    }

    @Test
    void placeOrder_emptyCart_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(orderService.placeOrder(eq("user-1"), any())).thenThrow(new IllegalArgumentException("Cart is empty"));

        assertThatThrownBy(() -> orderController.placeOrder(new OrderRequest("addr-1"), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    void placeOrder_insufficientStock_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(orderService.placeOrder(eq("user-1"), any())).thenThrow(new IllegalStateException("Insufficient stock"));

        assertThatThrownBy(() -> orderController.placeOrder(new OrderRequest("addr-1"), userDetails))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getMyOrders_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        Page<OrderResponse> page = new PageImpl<>(List.of(sampleResponse), pageable, 1);
        when(orderService.getAllOrders("user-1", pageable)).thenReturn(page);

        ResponseEntity<Page<OrderResponse>> response = orderController.getMyOrders(null, null, null, null, pageable, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getMyOrders_empty_returnsEmptyPage() {
        when(userDetails.getUserId()).thenReturn("user-1");
        Page<OrderResponse> empty = new PageImpl<>(List.of(), pageable, 0);
        when(orderService.getAllOrders("user-1", pageable)).thenReturn(empty);

        ResponseEntity<Page<OrderResponse>> response = orderController.getMyOrders(null, null, null, null, pageable, userDetails);

        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getOrder_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(orderService.getOrderForUser("ord-1", "user-1")).thenReturn(sampleResponse);

        ResponseEntity<OrderResponse> response = orderController.getOrder("ord-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo("ord-1");
    }

    @Test
    void getOrder_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(orderService.getOrderForUser("missing", "user-1")).thenThrow(new IllegalArgumentException("Order not found"));

        assertThatThrownBy(() -> orderController.getOrder("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_success_returns200() {
        when(userDetails.getUserId()).thenReturn("admin-1");
        when(orderService.updateOrderStatus(eq("ord-1"), eq(OrderStatus.PROCESSING), eq(null), eq("admin-1"))).thenReturn(sampleResponse);

        ResponseEntity<OrderResponse> response = orderController.updateStatus("ord-1", Map.of("status", "PROCESSING"), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateStatus_withNote_success() {
        when(userDetails.getUserId()).thenReturn("admin-1");
        when(orderService.updateOrderStatus(eq("ord-1"), eq(OrderStatus.SHIPPED), eq("Shipped via FedEx"), eq("admin-1"))).thenReturn(sampleResponse);

        ResponseEntity<OrderResponse> response = orderController.updateStatus(
                "ord-1", Map.of("status", "SHIPPED", "note", "Shipped via FedEx"), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateStatus_nullStatus_throws() {
        when(userDetails.getUserId()).thenReturn("admin-1");

        assertThatThrownBy(() -> orderController.updateStatus("ord-1", Map.of(), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status is required");
    }

    @Test
    void updateStatus_invalidStatus_throws() {
        when(userDetails.getUserId()).thenReturn("admin-1");

        assertThatThrownBy(() -> orderController.updateStatus("ord-1", Map.of("status", "INVALID"), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid order status");
    }

    @Test
    void updateStatus_invalidTransition_propagates() {
        when(userDetails.getUserId()).thenReturn("admin-1");
        when(orderService.updateOrderStatus(eq("ord-1"), eq(OrderStatus.DELIVERED), any(), eq("admin-1")))
                .thenThrow(new IllegalArgumentException("Cannot transition"));

        assertThatThrownBy(() -> orderController.updateStatus("ord-1", Map.of("status", "DELIVERED"), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getOrderHistory_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doReturn(java.util.Collections.emptyList()).when(userDetails).getAuthorities();
        OrderStatusHistoryResponse history = new OrderStatusHistoryResponse("hist-1", "ord-1", OrderStatus.PENDING, "ORDER", "Order placed", "user-1", LocalDateTime.now());
        when(orderService.getOrderHistory("ord-1", "user-1", false)).thenReturn(List.of(history));

        ResponseEntity<List<OrderStatusHistoryResponse>> response = orderController.getOrderHistory("ord-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrderHistory_admin_success_returns200() {
        when(userDetails.getUserId()).thenReturn("admin-1");
        doReturn(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))).when(userDetails).getAuthorities();
        OrderStatusHistoryResponse history = new OrderStatusHistoryResponse("hist-1", "ord-1", OrderStatus.PENDING, "ORDER", "Order placed", "user-1", LocalDateTime.now());
        when(orderService.getOrderHistory("ord-1", "admin-1", true)).thenReturn(List.of(history));

        ResponseEntity<List<OrderStatusHistoryResponse>> response = orderController.getOrderHistory("ord-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getOrderHistory_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doReturn(java.util.Collections.emptyList()).when(userDetails).getAuthorities();
        when(orderService.getOrderHistory("missing", "user-1", false)).thenThrow(new IllegalArgumentException("Order not found"));

        assertThatThrownBy(() -> orderController.getOrderHistory("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
