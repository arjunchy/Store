package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

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
}
