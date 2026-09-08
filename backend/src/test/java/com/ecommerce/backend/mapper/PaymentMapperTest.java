package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toResponse_mapsAllFields() {
        Order order = Order.builder().id("ord-1").build();
        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .id("pay-1")
                .order(order)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-123")
                .status(PaymentStatus.COMPLETED)
                .createdAt(now)
                .build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.id()).isEqualTo("pay-1");
        assertThat(response.orderId()).isEqualTo("ord-1");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.method()).isEqualTo("CREDIT_CARD");
        assertThat(response.transactionId()).isEqualTo("TXN-123");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_handlesNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_handlesNullOrderAndMethod() {
        Payment payment = Payment.builder()
                .id("pay-2")
                .order(null)
                .amount(BigDecimal.TEN)
                .method(null)
                .transactionId(null)
                .status(null)
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.orderId()).isNull();
        assertThat(response.method()).isNull();
        assertThat(response.transactionId()).isNull();
        assertThat(response.status()).isNull();
    }

    @Test
    void toResponse_mapsFailedStatus() {
        Order order = Order.builder().id("ord-2").build();
        Payment payment = Payment.builder()
                .id("pay-3")
                .order(order)
                .amount(new BigDecimal("200.00"))
                .method(PaymentMethod.PAYPAL)
                .transactionId("TXN-456")
                .status(PaymentStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.method()).isEqualTo("PAYPAL");
    }
}
