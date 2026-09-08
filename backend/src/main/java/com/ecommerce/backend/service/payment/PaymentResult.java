package com.ecommerce.backend.service.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResult {

    private boolean success;
    private String transactionId;
    private String errorMessage;
}
