package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder() != null ? payment.getOrder().getId() : null,
                payment.getAmount(),
                payment.getMethod() != null? payment.getMethod().name() : null,
                payment.getTransactionId(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getCreatedAt()
        );
    }
}
