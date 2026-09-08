package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatedPaymentGatewayTest {

    private final SimulatedPaymentGateway gateway = new SimulatedPaymentGateway();

    @Test
    void processPayment_success() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("100.00"));

        PaymentResult result = gateway.processPayment(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTransactionId()).startsWith("SIM-");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void getMethod_returnsSimulated() {
        assertThat(gateway.getMethod()).isEqualTo(PaymentMethod.SIMULATED);
    }

    @Test
    void processPayment_uniqueTransactionIds() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("50.00"));

        PaymentResult r1 = gateway.processPayment(request);
        PaymentResult r2 = gateway.processPayment(request);

        assertThat(r1.getTransactionId()).isNotEqualTo(r2.getTransactionId());
    }

    @Test
    void processPayment_transactionIdFormat() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("10.00"));

        PaymentResult result = gateway.processPayment(request);

        assertThat(result.getTransactionId()).matches("SIM-[A-Z0-9]{8}");
    }
}
