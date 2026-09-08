package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.mapper.PaymentMapper;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentGatewayFactory paymentGatewayFactory;
    @Mock private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User user;
    private Order order;
    private PaymentGateway gateway;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").build();
        order = Order.builder().id("ord-1").user(user).totalAmount(new BigDecimal("200.00")).paymentStatus(OrderPaymentStatus.PENDING).build();
        gateway = mock(PaymentGateway.class);
    }

    @Test
    void processPayment_success() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("200.00"));
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(paymentGatewayFactory.getGateway(PaymentMethod.SIMULATED)).thenReturn(gateway);
        when(gateway.processPayment(request)).thenReturn(PaymentResult.builder().success(true).transactionId("TXN-123").build());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mock(Payment.class));
        when(orderRepository.save(any())).thenReturn(order);
        when(paymentMapper.toResponse(any())).thenReturn(
                new PaymentResponse("pay-1", "ord-1", new BigDecimal("200.00"), "SIMULATED", "TXN-123", "COMPLETED", LocalDateTime.now())
        );

        PaymentResponse response = paymentService.processPayment(request, "user-1");

        assertThat(response.status()).isEqualTo("COMPLETED");
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Test
    void processPayment_gatewayFailure_setsFailedStatus() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", new BigDecimal("200.00"));
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(paymentGatewayFactory.getGateway(PaymentMethod.SIMULATED)).thenReturn(gateway);
        when(gateway.processPayment(request)).thenReturn(PaymentResult.builder().success(false).errorMessage("Card declined").build());
        when(paymentRepository.save(any())).thenReturn(mock(Payment.class));
        when(orderRepository.save(any())).thenReturn(order);
        when(paymentMapper.toResponse(any())).thenReturn(
                new PaymentResponse("pay-1", "ord-1", new BigDecimal("200.00"), "SIMULATED", null, "FAILED", LocalDateTime.now())
        );

        PaymentResponse response = paymentService.processPayment(request, "user-1");

        assertThat(response.status()).isEqualTo("FAILED");
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(OrderPaymentStatus.FAILED);
    }

    @Test
    void processPayment_orderNotFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest("missing", "SIMULATED", BigDecimal.TEN), "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    void processPayment_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).build();
        when(orderRepository.findById("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest("ord-2", "SIMULATED", BigDecimal.TEN), "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void processPayment_alreadyPaid_throws() {
        order.setPaymentStatus(OrderPaymentStatus.PAID);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest("ord-1", "SIMULATED", BigDecimal.TEN), "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void processPayment_invalidMethod_throws() {
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(new PaymentRequest("ord-1", "BITCOIN", BigDecimal.TEN), "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payment method");
    }

    @Test
    void processPayment_nullAmount_usesOrderTotal() {
        PaymentRequest request = new PaymentRequest("ord-1", "SIMULATED", null);
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(paymentGatewayFactory.getGateway(PaymentMethod.SIMULATED)).thenReturn(gateway);
        when(gateway.processPayment(any())).thenReturn(PaymentResult.builder().success(true).transactionId("TXN-123").build());
        when(paymentRepository.save(any())).thenReturn(mock(Payment.class));
        when(orderRepository.save(any())).thenReturn(order);
        when(paymentMapper.toResponse(any())).thenReturn(
                new PaymentResponse("pay-1", "ord-1", new BigDecimal("200.00"), "SIMULATED", "TXN-123", "COMPLETED", LocalDateTime.now())
        );

        PaymentResponse response = paymentService.processPayment(request, "user-1");

        assertThat(response).isNotNull();
    }

    @Test
    void getPaymentsForOrder_success() {
        Payment payment = Payment.builder().id("pay-1").order(order).amount(BigDecimal.TEN).method(PaymentMethod.SIMULATED).status(PaymentStatus.COMPLETED).build();
        when(orderRepository.findById("ord-1")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId("ord-1")).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(
                new PaymentResponse("pay-1", "ord-1", BigDecimal.TEN, "SIMULATED", "TXN-123", "COMPLETED", LocalDateTime.now())
        );

        List<PaymentResponse> result = paymentService.getPaymentsForOrder("ord-1", "user-1");

        assertThat(result).hasSize(1);
    }

    @Test
    void getPaymentsForOrder_orderNotFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentsForOrder("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPaymentsForOrder_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Order otherOrder = Order.builder().id("ord-2").user(otherUser).build();
        when(orderRepository.findById("ord-2")).thenReturn(Optional.of(otherOrder));

        assertThatThrownBy(() -> paymentService.getPaymentsForOrder("ord-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
