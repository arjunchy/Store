package com.ecommerce.backend.service.order;

import com.ecommerce.backend.dto.response.OrderDetailResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.mapper.OrderMapper;
import com.ecommerce.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceEnhancedTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private User adminUser;
    private Order order;
    private Product product;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").username("john").email("john@example.com").build();
        adminUser = User.builder().userId("admin-1").username("admin").email("admin@example.com").build();
        product = Product.builder().id("prod-1").name("Laptop").price(new BigDecimal("1000")).build();
        order = Order.builder()
                .id("order-1")
                .user(user)
                .orderNumber("ORD-123")
                .totalAmount(new BigDecimal("1000"))
                .status(OrderStatus.PENDING)
                .paymentStatus(OrderPaymentStatus.PENDING)
                .shippingAddress("{\"city\":\"NYC\"}")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        orderItem = OrderItem.builder().id("item-1").order(order).product(product).quantity(1).price(new BigDecimal("1000")).build();

        lenient().when(orderMapper.toOrderResponse(any(Order.class), anyList())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            List<OrderItem> items = inv.getArgument(1);
            return new OrderResponse(o.getId(), o.getOrderNumber(), o.getTotalAmount(), o.getStatus(), o.getPaymentStatus(), o.getDeliveryStatus(),
                    items.stream().map(oi -> new com.ecommerce.backend.dto.response.OrderItemResponse(oi.getProduct().getId(), oi.getProduct().getName(), oi.getQuantity(), oi.getPrice(), oi.getPrice())).toList(),
                    o.getCreatedAt(), null, null, null);
        });

        lenient().when(orderMapper.toHistoryResponse(any(OrderStatusHistory.class))).thenAnswer(inv -> {
            OrderStatusHistory h = inv.getArgument(0);
            return new OrderStatusHistoryResponse(h.getId(), h.getOrder() != null ? h.getOrder().getId() : null, h.getStatus(), h.getStatusType() != null ? h.getStatusType().name() : "ORDER", h.getNote(), h.getChangedBy(), h.getCreatedAt());
        });
    }

    @Test
    void getAllOrdersForAdmin_success_withoutFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByAdminFilter(isNull(), isNull(), eq(pageable))).thenReturn(page);
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));

        Page<OrderResponse> result = orderService.getAllOrdersForAdmin(pageable, null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo("order-1");
        verify(orderRepository).findByAdminFilter(null, null, pageable);
    }

    @Test
    void getAllOrdersForAdmin_withStatusAndSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByAdminFilter(eq(OrderStatus.PENDING), eq("ORD-123"), eq(pageable))).thenReturn(page);
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));

        Page<OrderResponse> result = orderService.getAllOrdersForAdmin(pageable, OrderStatus.PENDING, "ORD-123");

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(orderRepository).findByAdminFilter(OrderStatus.PENDING, "ORD-123", pageable);
    }

    @Test
    void getAllOrdersForAdmin_blankSearch_normalizedToNull() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(), pageable, 0);
        when(orderRepository.findByAdminFilter(isNull(), isNull(), eq(pageable))).thenReturn(page);

        Page<OrderResponse> result = orderService.getAllOrdersForAdmin(pageable, null, "   ");

        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(orderRepository).findByAdminFilter(null, null, pageable);
    }

    @Test
    void getOrderFullDetails_success_asAdmin() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));
        OrderStatusHistory history = OrderStatusHistory.builder().id("h-1").order(order).status(OrderStatus.PENDING).note("Order placed").changedBy("user-1").createdAt(LocalDateTime.now()).build();
        when(orderStatusHistoryRepository.findByOrderId("order-1")).thenReturn(List.of(history));

        OrderDetailResponse detail = orderService.getOrderFullDetails("order-1");

        assertThat(detail.id()).isEqualTo("order-1");
        assertThat(detail.orderNumber()).isEqualTo("ORD-123");
        assertThat(detail.userId()).isEqualTo("user-1");
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.statusHistory()).hasSize(1);
    }

    @Test
    void getOrderFullDetails_notFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderFullDetails("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void getOrderFullDetails_withOwnership_adminCanAccessAny() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));
        when(orderStatusHistoryRepository.findByOrderId("order-1")).thenReturn(List.of());

        // admin-1 is not owner but isAdmin=true should allow
        OrderDetailResponse detail = orderService.getOrderFullDetails("order-1", "admin-1", true);
        assertThat(detail.id()).isEqualTo("order-1");
    }

    @Test
    void getOrderFullDetails_withOwnership_ownerCanAccess() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));
        when(orderStatusHistoryRepository.findByOrderId("order-1")).thenReturn(List.of());

        OrderDetailResponse detail = orderService.getOrderFullDetails("order-1", "user-1", false);
        assertThat(detail.id()).isEqualTo("order-1");
    }

    @Test
    void getOrderFullDetails_withOwnership_nonOwnerNonAdmin_throws() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.getOrderFullDetails("order-1", "other-user", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void updateOrderStatusWithNote_success() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemRepository.findByOrderIdWithProduct("order-1")).thenReturn(List.of(orderItem));

        OrderResponse resp = orderService.updateOrderStatusWithNote("order-1", OrderStatus.PROCESSING, "Processing", "admin-1");

        assertThat(resp.status()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderStatusHistoryRepository).save(argThat(h -> h.getStatus() == OrderStatus.PROCESSING && "Processing".equals(h.getNote())));
    }

    @Test
    void updateOrderStatusWithNote_invalidTransition_throws() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> orderService.updateOrderStatusWithNote("order-1", OrderStatus.PENDING, "note", "admin-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transition");
    }
}
