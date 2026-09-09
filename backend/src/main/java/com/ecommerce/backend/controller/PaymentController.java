package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.EsewaVerifyRequest;
import com.ecommerce.backend.dto.request.KhaltiLookupRequest;
import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.EsewaInitiateResponse;
import com.ecommerce.backend.dto.response.KhaltiInitiateResponse;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.service.payment.EsewaService;
import com.ecommerce.backend.service.payment.KhaltiService;
import com.ecommerce.backend.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private KhaltiService khaltiService;

    @Autowired
    private EsewaService esewaService;

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

    @PostMapping("/khalti/initiate")
    public ResponseEntity<KhaltiInitiateResponse> initiateKhalti(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String orderId = body.get("orderId");
        if (orderId == null) orderId = body.get("order_id");
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        String userId = userDetails.getUserId();
        log.info("POST /api/payments/khalti/initiate orderId={} userId={}", orderId, userId);
        KhaltiInitiateResponse resp = khaltiService.initiate(orderId, userId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/khalti/lookup")
    public ResponseEntity<Map<String, Object>> lookupKhalti(
            @Valid @RequestBody KhaltiLookupRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("POST /api/payments/khalti/lookup pidx={} userId={}", request.pidx(), userDetails.getUserId());
        Map<String, Object> result = khaltiService.lookup(request.pidx());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/khalti/callback")
    public ResponseEntity<Map<String, Object>> khaltiCallback(
            @RequestParam String pidx,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "transaction_id") String transactionId) {
        // Public callback support – frontend also calls lookup via POST, this is convenience
        log.info("GET /api/payments/khalti/callback pidx={} status={}", pidx, status);
        Map<String, Object> result = khaltiService.lookup(pidx);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/esewa/initiate")
    public ResponseEntity<EsewaInitiateResponse> initiateEsewa(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String orderId = body.get("orderId");
        if (orderId == null) orderId = body.get("order_id");
        if (orderId == null) orderId = body.get("transaction_uuid");
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        String userId = userDetails.getUserId();
        log.info("POST /api/payments/esewa/initiate orderId={} userId={}", orderId, userId);
        EsewaInitiateResponse resp = esewaService.initiate(orderId, userId);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/esewa/verify")
    public ResponseEntity<Map<String, Object>> verifyEsewa(
            @RequestBody EsewaVerifyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("POST /api/payments/esewa/verify uuid={} hasData={}",
                request.transactionUuid(), request.data() != null && !request.data().isBlank());
        Map<String, Object> result = esewaService.verify(
                request.data(), request.transactionUuid(), request.transactionCode(), request.totalAmount());
        return ResponseEntity.ok(result);
    }
}