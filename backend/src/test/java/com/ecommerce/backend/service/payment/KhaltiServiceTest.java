package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.KhaltiInitiateResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class KhaltiServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RestTemplate restTemplate;

    private KhaltiService khaltiService;

    private User user;
    private Order order;

    @BeforeEach
    void setUp() throws Exception {
        khaltiService = new KhaltiService(
                orderRepository, paymentRepository, orderItemRepository, cartRepository, new ObjectMapper());
        setFieldValue("secretKey", "test-secret");
        setFieldValue("mode", "sandbox");
        setFieldValue("sandboxUrl", "https://dev.khalti.com/api/v2/");
        setFieldValue("prodUrl", "https://khalti.com/api/v2/");
        setFieldValue("timeoutMs", 10000);
        setFieldValue("returnUrl", "http://localhost:3000/payment/callback");
        setFieldValue("websiteUrl", "http://localhost:3000");
        setFieldValue("restTemplate", restTemplate);

        user = User.builder().userId("user-1").username("testuser").email("test@test.com").build();
        order = Order.builder()
                .id("ord-1")
                .user(user)
                .orderNumber("ORD-001")
                .totalAmount(new BigDecimal("150.00"))
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .status(OrderStatus.PROCESSING)
                .build();
    }

    private void setFieldValue(String fieldName, Object value) throws Exception {
        Field field = KhaltiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(khaltiService, value);
    }

    private Payment initiatedPayment() {
        return Payment.builder()
                .order(order)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.KHALTI)
                .transactionId("TXN-ours-1")
                .pidx("pidx-abc")
                .paymentUrl("https://pay.khalti.com/pidx-abc")
                .status(PaymentStatus.INITIATED)
                .build();
    }

    private void mockOrderFound() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID)).thenReturn(Optional.empty());
    }

    private void mockGateway(Object body) {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<Map>((Map) body, HttpStatus.OK));
    }

    private Map<String, Object> initiateGatewayBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("payment_url", "https://pay.khalti.com/pidx-abc");
        body.put("expires_in", 1800);
        return body;
    }

    private Map<String, Object> completedBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Completed");
        body.put("transaction_id", "KHALTI-TXN-99");
        body.put("total_amount", 15000);
        body.put("purchase_order_id", "ORD-001");
        return body;
    }

    private void mockPaymentFound(Payment payment) {
        when(paymentRepository.findByPidxForUpdate("pidx-abc")).thenReturn(Optional.of(payment));
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
    }

    private void mockSaves() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void initiate_orderNotFound_throws() {
        when(orderRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> khaltiService.initiate("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void initiate_wrongUser_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).totalAmount(BigDecimal.TEN).build();
        when(orderRepository.findByIdForUpdate("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> khaltiService.initiate("ord-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void initiate_canceledOrder_throws() {
        order.setStatus(OrderStatus.CANCELED);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canceled");
    }

    @Test
    void initiate_alreadyPaidOrder_throws() {
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void initiate_paidPaymentExists_throws() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID))
                .thenReturn(Optional.of(Payment.builder().status(PaymentStatus.PAID).build()));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void initiate_nullAmount_throws() {
        order.setTotalAmount(null);
        mockOrderFound();

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("amount not available");
    }

    @Test
    void initiate_amountBelowMinimum_throws() {
        order.setTotalAmount(new BigDecimal("5.00"));
        mockOrderFound();

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than Rs. 10");
    }

    @Test
    void initiate_notConfigured_throws() throws Exception {
        setFieldValue("secretKey", "");
        mockOrderFound();

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void initiate_success_generatesOurSideTransactionId() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        mockGateway(initiateGatewayBody());

        KhaltiInitiateResponse resp = khaltiService.initiate("ord-1", "user-1");

        assertThat(resp.transactionId()).isNotNull().isNotBlank();
        assertThat(resp.transactionId()).startsWith("TXN-");
        assertThat(resp.transactionId()).isNotEqualTo("pidx-abc");
        assertThat(resp.transactionId()).isNotEqualTo("ord-1");
        assertThat(resp.pidx()).isEqualTo("pidx-abc");
        assertThat(resp.paymentUrl()).isEqualTo("https://pay.khalti.com/pidx-abc");
        assertThat(resp.expiresIn()).isEqualTo(1800);
        assertThat(resp.orderId()).isEqualTo("ord-1");
        assertThat(resp.amount()).isEqualTo(15000);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo(resp.transactionId());
        assertThat(saved.getPidx()).isEqualTo("pidx-abc");
        assertThat(saved.getMethod()).isEqualTo(PaymentMethod.KHALTI);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void initiate_reusesExistingInitiatedPayment() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        Payment existing = Payment.builder()
                .order(order)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.KHALTI)
                .transactionId("TXN-old-1")
                .pidx("pidx-old")
                .paymentUrl("https://pay.khalti.com/pidx-old")
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.of(existing));

        KhaltiInitiateResponse resp = khaltiService.initiate("ord-1", "user-1");

        assertThat(resp.pidx()).isEqualTo("pidx-old");
        assertThat(resp.transactionId()).isEqualTo("TXN-old-1");
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void initiate_existingInitiatedWithoutUrl_createsNew() {
        when(orderRepository.findByIdForUpdate("ord-1")).thenReturn(Optional.of(order));
        Payment existing = Payment.builder()
                .order(order)
                .method(PaymentMethod.KHALTI)
                .transactionId("TXN-old-1")
                .pidx("pidx-old")
                .status(PaymentStatus.INITIATED)
                .build();
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc("ord-1", PaymentStatus.INITIATED))
                .thenReturn(Optional.of(existing));
        when(paymentRepository.findByOrderIdAndStatus("ord-1", PaymentStatus.PAID)).thenReturn(Optional.empty());
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        mockGateway(initiateGatewayBody());

        KhaltiInitiateResponse resp = khaltiService.initiate("ord-1", "user-1");

        assertThat(resp.pidx()).isEqualTo("pidx-abc");
        assertThat(resp.transactionId()).startsWith("TXN-");
    }

    @Test
    void initiate_gatewayNon2xx_throws() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<Map>((Map) null, HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Khalti initiate failed");
    }

    @Test
    void initiate_gatewayMissingPidx_throws() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        Map<String, Object> body = new HashMap<>();
        body.put("payment_url", "https://pay.khalti.com/x");
        mockGateway(body);

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid Khalti response");
    }

    @Test
    void initiate_gateway401_throwsAuthFailed() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authentication failed");
    }

    @Test
    void initiate_gatewayError_throws() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("timeout"));

        assertThatThrownBy(() -> khaltiService.initiate("ord-1", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to initiate Khalti payment");
    }

    @Test
    void initiate_unparseableExpiresAt_stillSucceeds() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        Map<String, Object> body = initiateGatewayBody();
        body.put("expires_at", "not-a-date");
        mockGateway(body);

        KhaltiInitiateResponse resp = khaltiService.initiate("ord-1", "user-1");

        assertThat(resp.pidx()).isEqualTo("pidx-abc");
        assertThat(resp.expiresAt()).isNull();
    }

    @Test
    void initiate_productDetailsFailure_stillSucceeds() {
        mockOrderFound();
        when(orderItemRepository.findByOrderIdWithProduct("ord-1")).thenThrow(new RuntimeException("db down"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        mockGateway(initiateGatewayBody());

        KhaltiInitiateResponse resp = khaltiService.initiate("ord-1", "user-1");

        assertThat(resp.pidx()).isEqualTo("pidx-abc");
    }

    @Test
    void lookup_blankPidx_throws() {
        assertThatThrownBy(() -> khaltiService.lookup("", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pidx is required");
        assertThatThrownBy(() -> khaltiService.lookup(null, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pidx is required");
    }

    @Test
    void lookup_blankUserId_throws() {
        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    void lookup_wrongUser_throws() {
        when(paymentRepository.findByPidxForUpdate("pidx-abc")).thenReturn(Optional.of(initiatedPayment()));

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment not found");
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
        verify(orderRepository, never()).findByIdForUpdate(anyString());
    }

    @Test
    void lookup_notConfigured_throws() throws Exception {
        setFieldValue("secretKey", "");
        mockPaymentFound(initiatedPayment());

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
        verify(restTemplate, never()).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void lookup_unknownPidx_proxiesGatewayWithoutWrites() {
        when(paymentRepository.findByPidxForUpdate("pidx-abc")).thenReturn(Optional.empty());
        mockGateway(completedBody());

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(result.get("status")).isEqualTo("Completed");
        assertThat(result.get("orderId")).isNull();
        assertThat(result.get("transactionId")).isNull();
        assertThat(result.get("txnCode")).isEqualTo("KHALTI-TXN-99");
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderRepository, never()).findByIdForUpdate(anyString());
    }

    @Test
    void lookup_completed_marksPaidAndStoresGatewayCode() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        mockGateway(completedBody());

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getTransactionId()).isEqualTo("TXN-ours-1");
        assertThat(payment.getTransactionCode()).isEqualTo("KHALTI-TXN-99");
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.get("transactionId")).isEqualTo("TXN-ours-1");
        assertThat(result.get("txnCode")).isEqualTo("KHALTI-TXN-99");
        assertThat(result).doesNotContainKeys("transaction_id", "tidx", "transactionCode");
        assertThat(result.get("orderId")).isEqualTo("ord-1");
        assertThat(result.get("paymentStatus")).isEqualTo(OrderPaymentStatus.PAID.name());
    }

    @Test
    void lookup_completed_clearsNonEmptyCart() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        Cart cart = Cart.builder()
                .user(user)
                .cartItems(new ArrayList<>(List.of(CartItem.builder().quantity(1).build())))
                .build();
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        mockGateway(completedBody());

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(cart.getCartItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    @Test
    void lookup_completed_alreadyPaid_skipsCartClear() {
        Payment payment = initiatedPayment();
        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionCode("KHALTI-TXN-99");
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        order.setStatus(OrderStatus.CONFIRMED);
        mockPaymentFound(payment);
        mockSaves();
        mockGateway(completedBody());

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(result.get("paymentStatus")).isEqualTo(OrderPaymentStatus.PAID.name());
        verify(cartRepository, never()).findByUserId(anyString());
    }

    @Test
    void lookup_completed_confirmedOrderKeepsStatus() {
        order.setStatus(OrderStatus.CONFIRMED);
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        mockGateway(completedBody());

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Test
    void lookup_completed_withTidxOnly_storesItAsCode() {
        Payment payment = Payment.builder()
                .order(order)
                .amount(new BigDecimal("150.00"))
                .method(PaymentMethod.KHALTI)
                .transactionId("TXN-ours-2")
                .pidx("pidx-abc")
                .paymentUrl("https://pay.khalti.com/pidx-abc")
                .status(PaymentStatus.INITIATED)
                .build();
        mockPaymentFound(payment);
        mockSaves();
        when(cartRepository.findByUserId("user-1")).thenReturn(Optional.empty());

        Map<String, Object> gatewayBody = new HashMap<>();
        gatewayBody.put("pidx", "pidx-abc");
        gatewayBody.put("status", "Completed");
        gatewayBody.put("tidx", "KHALTI-TIDX-77");
        gatewayBody.put("total_amount", 15000);
        gatewayBody.put("purchase_order_id", "ORD-001");
        mockGateway(gatewayBody);

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getTransactionId()).isEqualTo("TXN-ours-2");
        assertThat(payment.getTransactionCode()).isEqualTo("KHALTI-TIDX-77");
        assertThat(result.get("txnCode")).isEqualTo("KHALTI-TIDX-77");
    }

    @Test
    void lookup_userCanceled_marksExpired() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "User canceled");
        mockGateway(body);

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.EXPIRED);
        assertThat(result.get("status")).isEqualTo("User canceled");
    }

    @Test
    void lookup_expired_marksExpired() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Expired");
        mockGateway(body);

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.EXPIRED);
    }

    @Test
    void lookup_refunded_marksRefunded() {
        Payment payment = initiatedPayment();
        payment.setStatus(PaymentStatus.PAID);
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Refunded");
        mockGateway(body);

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.REFUNDED);
    }

    @Test
    void lookup_partiallyRefunded_marksRefunded() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Partially Refunded");
        mockGateway(body);

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.REFUNDED);
    }

    @Test
    void lookup_pending_marksInitiated() {
        Payment payment = initiatedPayment();
        payment.setStatus(PaymentStatus.EXPIRED);
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Pending");
        mockGateway(body);

        khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void lookup_unknownStatus_leavesStateUnchanged() {
        Payment payment = initiatedPayment();
        mockPaymentFound(payment);
        mockSaves();
        Map<String, Object> body = new HashMap<>();
        body.put("pidx", "pidx-abc");
        body.put("status", "Failed");
        mockGateway(body);

        Map<String, Object> result = khaltiService.lookup("pidx-abc", "user-1");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.UNPAID);
        assertThat(result.get("status")).isEqualTo("Failed");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void lookup_purchaseOrderMismatch_throws() {
        mockPaymentFound(initiatedPayment());
        Map<String, Object> body = completedBody();
        body.put("purchase_order_id", "ORD-OTHER");
        mockGateway(body);

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to this order");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void lookup_amountMismatch_throws() {
        mockPaymentFound(initiatedPayment());
        Map<String, Object> body = completedBody();
        body.put("total_amount", 100);
        mockGateway(body);

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match order total");
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void lookup_emptyGatewayBody_throws() {
        mockPaymentFound(initiatedPayment());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(new ResponseEntity<Map>((Map) null, HttpStatus.OK));

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty lookup response");
    }

    @Test
    void lookup_gateway401_throwsAuthFailed() {
        mockPaymentFound(initiatedPayment());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authentication failed");
    }

    @Test
    void lookup_gatewayError_throws() {
        mockPaymentFound(initiatedPayment());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> khaltiService.lookup("pidx-abc", "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Lookup failed");
    }
}
