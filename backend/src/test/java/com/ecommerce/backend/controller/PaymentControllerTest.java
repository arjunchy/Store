package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.EsewaVerifyRequest;
import com.ecommerce.backend.dto.request.KhaltiLookupRequest;
import com.ecommerce.backend.dto.response.EsewaInitiateResponse;
import com.ecommerce.backend.dto.response.KhaltiInitiateResponse;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.service.payment.EsewaService;
import com.ecommerce.backend.service.payment.KhaltiService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private EsewaService esewaService;

    @Mock
    private KhaltiService khaltiService;

    @InjectMocks
    private PaymentController paymentController;

    @Mock
    private CustomUserDetails userDetails;

    private PaymentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new PaymentResponse(
                "pay-1", "ord-1", new BigDecimal("500.00"),
                "ESEWA", "uuid-fresh-1", "TXN-123", "COMPLETED", LocalDateTime.now()
        );
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
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.verify("base64data", "ord-1", "TXN-ES-123", "500", "user-1"))
                .thenReturn(verifyResult);

        EsewaVerifyRequest request = new EsewaVerifyRequest("base64data", "ord-1", "TXN-ES-123", "500");

        ResponseEntity<Map<String, Object>> response = paymentController.verifyEsewa(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("COMPLETE");
        assertThat(response.getBody().get("paymentStatus")).isEqualTo("PAID");
        verify(esewaService).verify("base64data", "ord-1", "TXN-ES-123", "500", "user-1");
    }

    @Test
    void verifyEsewa_signatureFailed_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.verify(any(), any(), any(), any(), any()))
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
        when(userDetails.getUserId()).thenReturn("user-1");
        when(esewaService.verify(null, "ord-1", null, "500", "user-1"))
                .thenReturn(verifyResult);

        EsewaVerifyRequest request = new EsewaVerifyRequest(null, "ord-1", null, "500");

        ResponseEntity<Map<String, Object>> response = paymentController.verifyEsewa(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("paymentStatus")).isEqualTo("UNPAID");
    }

    private KhaltiInitiateResponse sampleKhaltiResponse() {
        return new KhaltiInitiateResponse(
                "pidx-1",
                "TXN-ours-1",
                "https://pay.khalti.com/pidx-1",
                null,
                1800,
                "ord-1",
                "ORD-001",
                "ApexCommerce Order ORD-001",
                15000
        );
    }

    @Test
    void initiateKhalti_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.initiate("ord-1", "user-1")).thenReturn(sampleKhaltiResponse());

        Map<String, String> body = new HashMap<>();
        body.put("orderId", "ord-1");

        ResponseEntity<KhaltiInitiateResponse> response = paymentController.initiateKhalti(body, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().pidx()).isEqualTo("pidx-1");
        assertThat(response.getBody().transactionId()).isEqualTo("TXN-ours-1");
        verify(khaltiService).initiate("ord-1", "user-1");
    }

    @Test
    void initiateKhalti_withSnakeCaseKey_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.initiate("ord-xyz", "user-1")).thenReturn(sampleKhaltiResponse());

        Map<String, String> body = new HashMap<>();
        body.put("order_id", "ord-xyz");

        ResponseEntity<KhaltiInitiateResponse> response = paymentController.initiateKhalti(body, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(khaltiService).initiate("ord-xyz", "user-1");
    }

    @Test
    void initiateKhalti_missingOrderId_throws() {
        assertThatThrownBy(() -> paymentController.initiateKhalti(new HashMap<>(), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId is required");
    }

    @Test
    void initiateKhalti_orderNotFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.initiate("missing", "user-1"))
                .thenThrow(new IllegalArgumentException("Order not found"));

        Map<String, String> body = new HashMap<>();
        body.put("orderId", "missing");

        assertThatThrownBy(() -> paymentController.initiateKhalti(body, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void lookupKhalti_success_returns200() {
        Map<String, Object> lookupResult = new HashMap<>();
        lookupResult.put("status", "Completed");
        lookupResult.put("transactionId", "TXN-ours-1");
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.lookup("pidx-1", "user-1")).thenReturn(lookupResult);

        ResponseEntity<Map<String, Object>> response =
                paymentController.lookupKhalti(new KhaltiLookupRequest("pidx-1"), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("Completed");
        verify(khaltiService).lookup("pidx-1", "user-1");
    }

    @Test
    void lookupKhalti_serviceError_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.lookup("pidx-1", "user-1"))
                .thenThrow(new IllegalArgumentException("Payment not found"));

        assertThatThrownBy(() -> paymentController.lookupKhalti(new KhaltiLookupRequest("pidx-1"), userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    void khaltiCallback_noAuth_throws() {
        assertThatThrownBy(() -> paymentController.khaltiCallback("pidx-1", "Completed", "KHALTI-TXN-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void khaltiCallback_success_delegatesToLookup() {
        Map<String, Object> lookupResult = new HashMap<>();
        lookupResult.put("status", "Completed");
        when(userDetails.getUserId()).thenReturn("user-1");
        when(khaltiService.lookup("pidx-1", "user-1")).thenReturn(lookupResult);

        ResponseEntity<Map<String, Object>> response =
                paymentController.khaltiCallback("pidx-1", "Completed", "KHALTI-TXN-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("Completed");
        verify(khaltiService).lookup("pidx-1", "user-1");
    }
}
