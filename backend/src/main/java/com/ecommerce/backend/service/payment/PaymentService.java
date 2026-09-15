package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    List<PaymentResponse> getPaymentsForOrder(String orderId, String userId);
}
