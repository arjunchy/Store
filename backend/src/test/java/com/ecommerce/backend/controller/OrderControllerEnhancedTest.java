package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.response.OrderDetailResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.enums.UserRole;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerEnhancedTest {

    @Mock private OrderService orderService;
    @InjectMocks private OrderController orderController;

    private OrderResponse orderResponse;
    private OrderDetailResponse detailResponse;
    private CustomUserDetails adminDetails;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        orderResponse = new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PROCESSING, OrderPaymentStatus.UNPAID, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        detailResponse = new OrderDetailResponse(
                "order-1", "ORD-123", "user-1", "john@example.com", "john",
                new BigDecimal("1000"), OrderStatus.PROCESSING, OrderPaymentStatus.UNPAID, DeliveryStatus.PLACED, "{\"city\":\"NYC\"}", null, null, null,
                List.of(), List.of(), LocalDateTime.now(), LocalDateTime.now()
        );

        User adminUser = User.builder().userId("admin-1").username("admin").email("admin@example.com").passwordHash("hash").userRole(UserRole.ADMIN).build();
        adminDetails = new CustomUserDetails(adminUser);

        User normalUser = User.builder().userId("user-1").username("john").email("john@example.com").passwordHash("hash").userRole(UserRole.USER).build();
        userDetails = new CustomUserDetails(normalUser);
    }

    @Test
    void getAllOrdersForAdmin_success_returns200() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), pageable, 1);
        when(orderService.getAllOrdersForAdmin(eq(pageable), eq(OrderStatus.PROCESSING), eq("ORD"))).thenReturn(page);

        ResponseEntity<Page<OrderResponse>> resp = orderController.getAllOrdersForAdmin("PENDING", null, null, "ORD", null, "desc", pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getTotalElements()).isEqualTo(1);
        verify(orderService).getAllOrdersForAdmin(pageable, OrderStatus.PROCESSING, "ORD");
    }

    @Test
    void getAllOrdersForAdmin_noFilter_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), pageable, 1);
        when(orderService.getAllOrdersForAdmin(eq(pageable), isNull(), isNull())).thenReturn(page);

        ResponseEntity<Page<OrderResponse>> resp = orderController.getAllOrdersForAdmin(null, null, null, null, null, "desc", pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderService).getAllOrdersForAdmin(pageable, null, null);
    }

    @Test
    void getAllOrdersForAdmin_invalidStatus_throws() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> orderController.getAllOrdersForAdmin("INVALID", null, null, null, null, "desc", pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid order status");
    }

    @Test
    void getAllOrdersForAdmin_blankStatus_treatedAsNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderResponse> page = new PageImpl<>(List.of(), pageable, 0);
        when(orderService.getAllOrdersForAdmin(eq(pageable), isNull(), isNull())).thenReturn(page);

        ResponseEntity<Page<OrderResponse>> resp = orderController.getAllOrdersForAdmin("   ", null, null, null, null, "desc", pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderService).getAllOrdersForAdmin(pageable, null, null);
    }

    @Test
    void getOrderFullDetails_asAdmin_success() {
        when(orderService.getOrderFullDetails("order-1", "admin-1", true)).thenReturn(detailResponse);

        ResponseEntity<OrderDetailResponse> resp = orderController.getOrderFullDetails("order-1", adminDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().id()).isEqualTo("order-1");
        verify(orderService).getOrderFullDetails("order-1", "admin-1", true);
    }

    @Test
    void getOrderFullDetails_asOwner_success() {
        when(orderService.getOrderFullDetails("order-1", "user-1", false)).thenReturn(detailResponse);

        ResponseEntity<OrderDetailResponse> resp = orderController.getOrderFullDetails("order-1", userDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(orderService).getOrderFullDetails("order-1", "user-1", false);
    }

    @Test
    void getOrderFullDetails_notFound_propagates() {
        when(orderService.getOrderFullDetails(anyString(), anyString(), anyBoolean())).thenThrow(new IllegalArgumentException("Order not found"));
        assertThatThrownBy(() -> orderController.getOrderFullDetails("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_success_returns200() {
        OrderResponse updated = new OrderResponse("order-1", "ORD-123", new BigDecimal("1000"), OrderStatus.PROCESSING, OrderPaymentStatus.PENDING, DeliveryStatus.PLACED, List.of(), LocalDateTime.now(), null, null, null);
        when(orderService.updateOrderStatus(eq("order-1"), eq(OrderStatus.PROCESSING), eq("note"), eq("admin-1"))).thenReturn(updated);

        ResponseEntity<OrderResponse> resp = orderController.updateStatus("order-1", Map.of("status", "PROCESSING", "note", "note"), adminDetails);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().status()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateStatus_invalidStatus_throws() {
        assertThatThrownBy(() -> orderController.updateStatus("order-1", Map.of("status", "INVALID"), adminDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateStatus_missingStatus_throws() {
        assertThatThrownBy(() -> orderController.updateStatus("order-1", Map.of("note", "test"), adminDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status is required");
    }
}
