package com.ecommerce.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EsewaVerifyRequest(
        String data,
        @JsonAlias({"transactionId", "transaction_uuid", "transaction_id"})
        String transactionUuid,
        @JsonAlias({"txnCode", "txn_code", "transaction_code", "transaction_id"})
        String transactionCode,
        @JsonAlias({"total_amount", "totalAmount"})
        String totalAmount
) {
}
