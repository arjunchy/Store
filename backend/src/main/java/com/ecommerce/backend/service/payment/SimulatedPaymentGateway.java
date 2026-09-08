package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.enums.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SimulatedPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Simulated gateway processing payment for orderId: {} with method: {}", request.orderId(), request.method());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulated payment interrupted for orderId: {}", request.orderId(), e);
            return PaymentResult.builder()
                    .success(false)
                    .transactionId(null)
                    .errorMessage("Payment interrupted")
                    .build();
        }

        String transactionId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Simulated payment succeeded for orderId: {} transactionId: {}", request.orderId(), transactionId);
        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .errorMessage(null)
                .build();
    }

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.SIMULATED;
    }
}
