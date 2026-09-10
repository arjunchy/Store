package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.EsewaVerifyRequest;
import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.EsewaInitiateResponse;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.service.payment.EsewaService;
import com.ecommerce.backend.service.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private EsewaService esewaService;

    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private CustomUserDetails userDetails;

    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new PaymentResponse(
                "pay-1", "ord-1", new BigDecimal("500.00"),
                "SIMULATED", "TXN-123", "COMPLETED", LocalDateTime.now()
        );
    }

    @Test
    void processPayment_success_returns201() {
        when(userDetails.getUserId()).thenReturn("user-1");
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("500.00"));
        when(paymentService.processPayment(any(PaymentRequest.class), eq("user-1"))).thenReturn(sampleResponse);

        ResponseEntity<PaymentResponse> response = paymentController.processPayment(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo("pay-1");
        assertThat(response.getBody().status()).isEqualTo("COMPLETED");
        verify(paymentService).processPayment(request, "user-1");
    }

    @Test
    void processPayment_orderNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(paymentService.processPayment(any(), eq("user-1"))).thenThrow(new IllegalArgumentException("Order not found"));

        assertThatThrownBy(() -> paymentController.processPayment(
                new PaymentRequest("missing", "SIMULATED", new BigDecimal("100")), userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processPayment_alreadyPaid_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(paymentService.processPayment(any(), eq("user-1"))).thenThrow(new IllegalArgumentException("Order already paid"));

        assertThatThrownBy(() -> paymentController.processPayment(
                new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("100")), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void getPaymentsByOrder_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(paymentService.getPaymentsForOrder("ord-1", "user-1")).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<PaymentResponse>> response = paymentController.getPaymentsByOrder("ord-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getPaymentsByOrder_empty_returnsEmptyList() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(paymentService.getPaymentsForOrder("ord-1", "user-1")).thenReturn(List.of());

        ResponseEntity<List<PaymentResponse>> response = paymentController.getPaymentsByOrder("ord-1", userDetails);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getPaymentsByOrder_orderNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(paymentService.getPaymentsForOrder("missing", "user-1")).thenThrow(new IllegalArgumentException("Order not found"));

        assertThatThrownBy(() -> paymentController.getPaymentsByOrder("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private EsewaInitiateResponse sampleEsewaResponse() {
        return new EsewaInitiateResponse(
                "https://rc-epay.esewa.com.np/api/epay/main/v2/form",
                "EPAYTEST",
                "ord-1",
                "500",
                "400",
                "50",
                "0",
                "50",
                "http://localhost:3000/esewa/callback",
                "http://localhost:3000/esewa/callback",
                "total_amount,transaction_uuid,product_code",
                "abc123signature",
                "ord-1",
                "ORD-001"
        );
    }

    @Test
    void initiateEsewa_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.initiate("ord-1", "user-1")).thenReturn(sampleEsewaResponse());

        Map<String, String> body = new HashMap<>();
        body.put("orderId", "ord-1");

        ResponseEntity<EsewaInitiateResponse> response = paymentController.initiateEsewa(body, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().gatewayUrl()).isEqualTo("https://rc-epay.esewa.com.np/api/epay/main/v2/form");
        assertThat(response.getBody().productCode()).isEqualTo("EPAYTEST");
        assertThat(response.getBody().transactionUuid()).isEqualTo("ord-1");
        assertThat(response.getBody().signature()).isEqualTo("abc123signature");
        verify(esewaService).initiate("ord-1", "user-1");
    }

    @Test
    void initiateEsewa_withOrderNumberKey_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.initiate("ord-xyz", "user-1")).thenReturn(sampleEsewaResponse());

        Map<String, String> body = new HashMap<>();
        body.put("order_id", "ord-xyz");

        ResponseEntity<EsewaInitiateResponse> response = paymentController.initiateEsewa(body, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(esewaService).initiate("ord-xyz", "user-1");
    }

    @Test
    void initiateEsewa_missingOrderId_throws() {
        Map<String, String> body = new HashMap<>();

        assertThatThrownBy(() -> paymentController.initiateEsewa(body, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required");
    }

    @Test
    void initiateEsewa_blankOrderId_throws() {
        Map<String, String> body = new HashMap<>();
        body.put("orderId", "");

        assertThatThrownBy(() -> paymentController.initiateEsewa(body, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required");
    }

    @Test
    void initiateEsewa_orderNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.initiate("missing", "user-1"))
                .thenThrow(new IllegalArgumentException("Order not found"));

        Map<String, String> body = new HashMap<>();
        body.put("orderId", "missing");

        assertThatThrownBy(() -> paymentController.initiateEsewa(body, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void verifyEsewa_success_returns200() {
        Map<String, Object> verifyResult = new HashMap<>();
        verifyResult.put("status", "COMPLETE");
        verifyResult.put("orderId", "ord-1");
        verifyResult.put("paymentStatus", "PAID");
        verifyResult.put("transaction_id", "TXN-ES-123");
        when(esewaService.verify("base64data", "ord-1", "TXN-ES-123", "500"))
                .thenReturn(verifyResult);

        EsewaVerifyRequest request = new EsewaVerifyRequest("base64data", "ord-1", "TXN-ES-123", "500");

        ResponseEntity<Map<String, Object>> response = paymentController.verifyEsewa(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETE");
        assertThat(response.getBody().get("paymentStatus")).isEqualTo("PAID");
        verify(esewaService).verify("base64data", "ord-1", "TXN-ES-123", "500");
    }

    @Test
    void verifyEsewa_signatureFailed_propagates() {
        when(esewaService.verify(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("eSewa signature verification failed"));

        EsewaVerifyRequest request = new EsewaVerifyRequest("base64data", "ord-1", "TXN-ES-123", "500");

        assertThatThrownBy(() -> paymentController.verifyEsewa(request, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature verification failed");
    }

    @Test
    void verifyEsewa_statusNotComplete_propagatesNonPaid() {
        Map<String, Object> verifyResult = new HashMap<>();
        verifyResult.put("status", "PENDING");
        verifyResult.put("orderId", "ord-1");
        verifyResult.put("paymentStatus", "UNPAID");
        when(esewaService.verify(null, "ord-1", null, "500"))
                .thenReturn(verifyResult);

        EsewaVerifyRequest request = new EsewaVerifyRequest(null, "ord-1", null, "500");

        ResponseEntity<Map<String, Object>> response = paymentController.verifyEsewa(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("paymentStatus")).isEqualTo("UNPAID");
    }
}
