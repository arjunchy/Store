package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "POST /api/payments - Processing payment for orderId: {} by userId: {}",
                request.orderId(),
                userId
        );

        try {
            PaymentResponse response =
                    paymentService.processPayment(request, userId);

            log.info(
                    "POST /api/payments - Payment processed with status: {} transactionId: {} for orderId: {}",
                    response.status(),
                    response.transactionId(),
                    request.orderId()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "POST /api/payments - Failed for orderId: {} - {}",
                    request.orderId(),
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "POST /api/payments - Unexpected error for orderId: {}",
                    request.orderId(),
                    e
            );
            throw e;
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrder(
            @PathVariable String orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUserId();

        log.info(
                "GET /api/payments/order/{} - Fetching payments for userId: {}",
                orderId,
                userId
        );

        try {
            List<PaymentResponse> payments =
                    paymentService.getPaymentsForOrder(
                            orderId,
                            userId
                    );

            log.debug(
                    "GET /api/payments/order/{} - Retrieved {} payments for userId: {}",
                    orderId,
                    payments.size(),
                    userId
            );

            return ResponseEntity.ok(payments);

        } catch (IllegalArgumentException e) {
            log.warn(
                    "GET /api/payments/order/{} - Failed for userId: {} - {}",
                    orderId,
                    userId,
                    e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            log.error(
                    "GET /api/payments/order/{} - Unexpected error for userId: {}",
                    orderId,
                    userId,
                    e
            );
            throw e;
        }
    }
}