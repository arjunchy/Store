package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request, String userId);

    List<PaymentResponse> getPaymentsForOrder(String orderId, String userId);
}
