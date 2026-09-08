package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.OrderItemResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.dto.response.OrderStatusHistoryResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.OrderStatusHistory;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.DeliveryStatus;
import com.ecommerce.backend.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toOrderItemResponse_mapsAllFields() {
        Product product = Product.builder().id("prod-1").name("Laptop").build();
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(3)
                .price(new BigDecimal("100.00"))
                .build();

        OrderItemResponse response = mapper.toOrderItemResponse(item);

        assertThat(response.productId()).isEqualTo("prod-1");
        assertThat(response.productName()).isEqualTo("Laptop");
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.subtotal()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void toOrderItemResponse_handlesNull() {
        assertThat(mapper.toOrderItemResponse(null)).isNull();
    }

    @Test
    void toOrderItemResponse_handlesNullProduct() {
        OrderItem item = OrderItem.builder()
                .product(null)
                .quantity(1)
                .price(BigDecimal.TEN)
                .build();

        assertThat(mapper.toOrderItemResponse(item)).isNull();
    }

    @Test
    void toOrderResponse_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id("ord-1")
                .orderNumber("ORD-1234")
                .totalAmount(new BigDecimal("500.00"))
                .status(OrderStatus.PENDING)
                .paymentStatus(OrderPaymentStatus.PENDING)
                .createdAt(now)
                .build();
        Product product = Product.builder().id("prod-1").name("Phone").build();
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .price(new BigDecimal("250.00"))
                .build();

        OrderResponse response = mapper.toOrderResponse(order, List.of(item));

        assertThat(response.id()).isEqualTo("ord-1");
        assertThat(response.orderNumber()).isEqualTo("ORD-1234");
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.paymentStatus()).isEqualTo(OrderPaymentStatus.PENDING);
        assertThat(response.items()).hasSize(1);
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toOrderResponse_handlesNull() {
        assertThat(mapper.toOrderResponse(null, List.of())).isNull();
    }

    @Test
    void toOrderResponse_emptyItems() {
        Order order = Order.builder()
                .id("ord-2")
                .orderNumber("ORD-5678")
                .totalAmount(BigDecimal.ZERO)
                .status(OrderStatus.PENDING)
                .paymentStatus(OrderPaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        OrderResponse response = mapper.toOrderResponse(order, List.of());

        assertThat(response.items()).isEmpty();
    }

    @Test
    void toHistoryResponse_mapsAllFields() {
        Order order = Order.builder().id("ord-1").build();
        LocalDateTime now = LocalDateTime.now();
        OrderStatusHistory history = OrderStatusHistory.builder()
                .id("hist-1")
                .order(order)
                .status(OrderStatus.SHIPPED)
                .note("Package shipped")
                .changedBy("admin-1")
                .createdAt(now)
                .build();

        OrderStatusHistoryResponse response = mapper.toHistoryResponse(history);

        assertThat(response.id()).isEqualTo("hist-1");
        assertThat(response.orderId()).isEqualTo("ord-1");
        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.note()).isEqualTo("Package shipped");
        assertThat(response.changedBy()).isEqualTo("admin-1");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toHistoryResponse_handlesNull() {
        assertThat(mapper.toHistoryResponse(null)).isNull();
    }

    @Test
    void toHistoryResponse_handlesNullOrder() {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .id("hist-2")
                .order(null)
                .status(OrderStatus.PENDING)
                .note(null)
                .changedBy(null)
                .createdAt(LocalDateTime.now())
                .build();

        OrderStatusHistoryResponse response = mapper.toHistoryResponse(history);

        assertThat(response.orderId()).isNull();
        assertThat(response.note()).isNull();
        assertThat(response.changedBy()).isNull();
    }
}
