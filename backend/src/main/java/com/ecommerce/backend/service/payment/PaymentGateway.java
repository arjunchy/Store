package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.enums.PaymentMethod;

public interface PaymentGateway {

    PaymentResult processPayment(PaymentRequest request);

    PaymentMethod getMethod();
}
