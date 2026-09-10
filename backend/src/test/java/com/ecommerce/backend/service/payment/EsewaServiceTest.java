package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.EsewaInitiateResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EsewaServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private EsewaService esewaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String productCode = "EPAYTEST";
    private final String secretKey = "8gBm/:&EnhH.1/q";
    private final String testGatewayUrl = "https://rc-epay.esewa.com.np/api/epay/main/v2/form";
    private final String testStatusUrl = "https://rc-epay.esewa.com.np/api/epay/transaction/status/";
    private final String successUrl = "http://localhost:3000/esewa/callback";
    private final String failureUrl = "http://localhost:3000/esewa/callback";

    private User user;
    private Order order;

    @BeforeEach
    void setUp() throws Exception {
        setFieldValue("productCode", productCode);
        setFieldValue("secretKey", secretKey);
        setFieldValue("mode", "test");
        setFieldValue("testGatewayUrl", testGatewayUrl);
        setFieldValue("prodGatewayUrl", "https://epay.esewa.com.np/api/epay/main/v2/form");
        setFieldValue("testStatusUrl", testStatusUrl);
        setFieldValue("prodStatusUrl", "https://epay.esewa.com.np/api/epay/transaction/status/");
        setFieldValue("successUrl", successUrl);
        setFieldValue("failureUrl", failureUrl);
        setFieldValue("timeoutMs", 10000);

        esewaService.init();
        setFieldValue("restTemplate", restTemplate);

        user = User.builder().userId("user-1").username("testuser").email("test@test.com").build();
        order = Order.builder()
                .id("ord-1")
                .user(user)
                .orderNumber("ORD-001")
                .totalAmount(new BigDecimal("500.00"))
                .subtotal(new BigDecimal("400.00"))
                .tax(new BigDecimal("50.00"))
                .shippingCost(new BigDecimal("50.00"))
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .status(OrderStatus.PROCESSING)
                .build();
    }

    private void setFieldValue(String fieldName, Object value) throws Exception {
        Field field = EsewaService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(esewaService, value);
    }

    @Test
    void initiate_orderNotFound_throws() {
        when(orderRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> esewaService.initiate("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void initiate_wrongUser_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findByIdForUpdate("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> esewaService.initiate("ord-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void initiate_canceledOrder_throws() {
        order.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> esewaService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canceled");
    }

    @Test
    void initiate_alreadyPaid_throws() {
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> esewaService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void initiate_paidPaymentExists_throws() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID))
                .thenReturn(Optional.of(Payment.builder().status(PaymentStatus.PAID).build()));

        assertThatThrownBy(() -> esewaService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void initiate_success_returnsResponseWithSignature() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID)).thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("ord-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        EsewaInitiateResponse resp = esewaService.initiate("ord-1", "user-1");

        assertThat(resp.gatewayUrl()).isEqualTo(testGatewayUrl);
        assertThat(resp.productCode()).isEqualTo(productCode);
        assertThat(resp.transactionUuid()).isEqualTo("ord-1");
        // fmt() strips trailing zeros: 500.00 → "500"
        assertThat(resp.totalAmount()).isEqualTo("500");
        assertThat(resp.amount()).isEqualTo("400");
        assertThat(resp.taxAmount()).isEqualTo("50");
        assertThat(resp.productServiceCharge()).isEqualTo("0");
        assertThat(resp.productDeliveryCharge()).isEqualTo("50");
        assertThat(resp.successUrl()).isEqualTo(successUrl);
        assertThat(resp.failureUrl()).isEqualTo(failureUrl);
        assertThat(resp.signedFieldNames()).isEqualTo("total_amount,transaction_uuid,product_code");
        assertThat(resp.signature()).isNotNull().isNotEmpty();
        assertThat(resp.orderId()).isEqualTo("ord-1");
        assertThat(resp.orderNumber()).isEqualTo("ORD-001");

        // Verify the signature is valid — fmt() strips trailing zeros so 500.00 → "500"
        String expectedMessage = "total_amount=500,transaction_uuid=ord-1,product_code=EPAYTEST";
        String expectedSig = EsewaService.hmacSha256Base64(secretKey, expectedMessage);
        assertThat(resp.signature()).isEqualTo(expectedSig);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment savedPayment = captor.getValue();
        assertThat(savedPayment.getMethod()).isEqualTo(PaymentMethod.ESEWA);
        assertThat(savedPayment.getTransactionId()).isEqualTo("ord-1");
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(savedPayment.getPaymentUrl()).isEqualTo(testGatewayUrl);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void initiate_reusesExistingInitiatedPayment() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID)).thenReturn(Optional.empty());

        Payment existing = Payment.builder()
                .order(order)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("ord-1")
                .paymentUrl(testGatewayUrl)
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("ord-1"))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        EsewaInitiateResponse resp = esewaService.initiate("ord-1", "user-1");

        assertThat(resp.transactionUuid()).isEqualTo("ord-1");
        verify(paymentRepository).save(existing);
    }

    @Test
    void hmacSha256Base64_generatesConsistentSignature() {
        String message = "total_amount=500,transaction_uuid=ord-1,product_code=EPAYTEST";
        String sig1 = EsewaService.hmacSha256Base64(secretKey, message);
        String sig2 = EsewaService.hmacSha256Base64(secretKey, message);
        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1).isNotEmpty();
    }

    @Test
    void verify_missingDataAndUuid_throws() {
        assertThatThrownBy(() -> esewaService.verify(null, "", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transaction_uuid is required");
    }

    @Test
    void verify_orderNotFound_throws() {
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("missing", PaymentStatus.INITIATED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("missing"))
                .thenReturn(Optional.empty());
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> esewaService.verify(null, "missing", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void verify_withInvalidData_throws() throws Exception {
        // Valid base64 of JSON that is missing required fields (uuid, signed_field_names, signature)
        String json = objectMapper.writeValueAsString(Map.of("status", "COMPLETE"));
        String encodedData = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> esewaService.verify(encodedData, "ord-1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid eSewa response data");
    }

    @Test
    void verify_statusNotComplete_returnsNonPaid() {
        Payment existingPayment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("ord-1")
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.of(existingPayment));

        Map<String, Object> statusResp = new HashMap<>();
        statusResp.put("status", "PENDING");
        statusResp.put("transaction_code", "TXN-123");
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(statusResp);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, Object> result = esewaService.verify(null, "ord-1", null, "500");

        assertThat(result.get("status")).isEqualTo("PENDING");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void verify_withValidData_complete_marksPaid() throws Exception {
        Payment existingPayment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("ord-1")
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.of(existingPayment));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        // Build valid base64 data with correct signature
        // The verify code reconstructs the message from the decoded data fields,
        // so total_amount value in dataMap must match what we sign
        String signedFields = "total_amount,transaction_uuid,product_code";
        String message = "total_amount=500.00,transaction_uuid=ord-1,product_code=EPAYTEST";
        String signature = EsewaService.hmacSha256Base64(secretKey, message);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("transaction_uuid", "ord-1");
        dataMap.put("transaction_code", "TXN-ES-123");
        dataMap.put("status", "COMPLETE");
        dataMap.put("total_amount", "500.00");
        dataMap.put("product_code", "EPAYTEST");
        dataMap.put("signed_field_names", signedFields);
        dataMap.put("signature", signature);

        String json = objectMapper.writeValueAsString(dataMap);
        String encodedData = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        // Mock status check to return COMPLETE
        Map<String, Object> statusResp = new HashMap<>();
        statusResp.put("status", "COMPLETE");
        statusResp.put("transaction_code", "TXN-ES-123");
        ResponseEntity<Map> responseEntity = ResponseEntity.ok(statusResp);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        Map<String, Object> result = esewaService.verify(encodedData, "ord-1", "TXN-ES-123", "500.00");

        assertThat(result.get("orderId")).isEqualTo("ord-1");
        assertThat(result.get("orderNumber")).isEqualTo("ORD-001");
        assertThat(result.get("paymentStatus")).isEqualTo(OrderPaymentStatus.PAID.name());
        assertThat(result.get("transaction_id")).isEqualTo("TXN-ES-123");
        assertThat(result.get("amountPaid")).isEqualTo("500");

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void verify_alreadyPaid_returnsIdempotentSuccess_withoutStatusCheck() {
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        Payment paidPayment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("TXN-ES-123")
                .status(PaymentStatus.PAID)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("ord-1"))
                .thenReturn(Optional.of(paidPayment));

        Map<String, Object> result = esewaService.verify(null, "ord-1", null, "500");

        assertThat(result.get("orderId")).isEqualTo("ord-1");
        assertThat(result.get("paymentStatus")).isEqualTo(OrderPaymentStatus.PAID.name());
        assertThat(result.get("status")).isEqualTo("COMPLETE");
        assertThat(result.get("transaction_id")).isEqualTo("TXN-ES-123");
        assertThat(result.get("amountPaid")).isEqualTo("500");
        // Duplicate verify must not hit eSewa again, rewrite payment, or touch the cart
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).findByUserId(anyString());
    }

    @Test
    void verify_concurrentLoserSeesPaidAfterLock_returnsSuccess() {
        Payment initiatedPayment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("ord-1")
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.of(initiatedPayment));

        Map<String, Object> statusResp = new HashMap<>();
        statusResp.put("status", "COMPLETE");
        statusResp.put("transaction_code", "TXN-ES-123");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(statusResp));

        // Winner committed first: locked read now sees PAID
        Order paidOrder = Order.builder()
                .id("ord-1")
                .user(user)
                .orderNumber("ORD-001")
                .totalAmount(new BigDecimal("500.00"))
                .paymentStatus(OrderPaymentStatus.PAID)
                .status(OrderStatus.CONFIRMED)
                .build();
        Payment paidPayment = Payment.builder()
                .order(paidOrder)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ESEWA)
                .transactionId("TXN-ES-123")
                .status(PaymentStatus.PAID)
                .build();
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(paidOrder));
        when(paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc("ord-1"))
                .thenReturn(Optional.of(paidPayment));

        Map<String, Object> result = esewaService.verify(null, "ord-1", null, "500");

        assertThat(result.get("orderId")).isEqualTo("ord-1");
        assertThat(result.get("paymentStatus")).isEqualTo(OrderPaymentStatus.PAID.name());
        assertThat(result.get("transaction_id")).isEqualTo("TXN-ES-123");
        assertThat(result.get("amountPaid")).isEqualTo("500");
        // Loser must not rewrite payment/order or re-clear the cart
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartRepository, never()).findByUserId(anyString());
    }

    @Test
    void verify_signatureMismatch_throws() throws Exception {
        // Build valid base64 data with product_code included so the only failure is the signature
        String message = "total_amount=500.00,transaction_uuid=ord-1,product_code=EPAYTEST";
        String wrongSignature = EsewaService.hmacSha256Base64("wrong-secret", message);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("transaction_uuid", "ord-1");
        dataMap.put("transaction_code", "TXN-ES-123");
        dataMap.put("status", "COMPLETE");
        dataMap.put("total_amount", "500.00");
        dataMap.put("product_code", "EPAYTEST");
        dataMap.put("signed_field_names", "total_amount,transaction_uuid,product_code");
        dataMap.put("signature", wrongSignature);

        String json = objectMapper.writeValueAsString(dataMap);
        String encodedData = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> esewaService.verify(encodedData, "ord-1", "TXN-ES-123", "500.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signature verification failed");
    }
}
