package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.ecommerce.backend.mapper.PaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentGatewayFactory paymentGatewayFactory;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private CartRepository cartRepository;

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String userId) {
        log.info("Processing payment for orderId: {} by userId: {} with method: {}", request.orderId(), request.method(), userId);
        try {
            // Use a pessimistic write lock on the order to serialize the
            // check-then-act below and prevent a concurrent double-payment.
            Order order = orderRepository.findByIdForUpdate(request.orderId())
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", request.orderId());
                        return new IllegalArgumentException("Order not found");
                    });

            if (!order.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - order {} does not belong to user {}", request.orderId(), userId);
                throw new IllegalArgumentException("Order not found");
            }

            if (order.getStatus() == com.ecommerce.backend.enums.OrderStatus.CANCELED) {
                log.warn("Cannot pay for canceled order {} by userId: {}", request.orderId(), userId);
                throw new IllegalArgumentException("Cannot pay for a canceled order");
            }

            if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
                log.warn("Order {} already paid, userId: {}", request.orderId(), userId);
                throw new IllegalArgumentException("Order already paid");
            }

            // Check unique paid payment with locking to prevent double pay race
            if (paymentRepository.findByOrderIdAndStatus(request.orderId(), PaymentStatus.PAID).isPresent()) {
                log.warn("Order {} already has a paid payment", request.orderId());
                throw new IllegalArgumentException("Order already paid");
            }

            // Amount is always derived from the server-side order total; the
            // client-supplied amount is ignored (never trusted) so it cannot be
            // used to spoof or alter the charged value.
            BigDecimal amount = order.getTotalAmount();
            if (amount == null) {
                log.warn("Order total amount is null for orderId: {}", request.orderId());
                throw new IllegalStateException("Order amount not available");
            }
            log.debug("Payment amount determined: {} for orderId: {}", amount, request.orderId());

            PaymentMethod method;
            try {
                method = PaymentMethod.valueOf(request.method().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid payment method: {} for orderId: {}", request.method(), request.orderId());
                throw new IllegalArgumentException("Invalid payment method: " + request.method());
            }

            PaymentGateway gateway = paymentGatewayFactory.getGateway(method);
            log.info("Using gateway {} for method {}", gateway.getMethod(), method);
            PaymentResult result;
            try {
                result = gateway.processPayment(request);
            } catch (Exception e) {
                log.error("Gateway processing failed for orderId: {} with method: {}", request.orderId(), request.method(), e);
                throw new IllegalStateException("Payment gateway error: " + e.getMessage(), e);
            }

            PaymentStatus status = result.isSuccess() ? PaymentStatus.PAID : PaymentStatus.EXPIRED;
            String transactionId = result.getTransactionId();
            if (transactionId == null || transactionId.isBlank()) {
                transactionId = "TXN-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                log.debug("Generated fallback transactionId: {}", transactionId);
            }

            Payment payment = Payment.builder()
                    .order(order)
                    .amount(amount)
                    .method(method)
                    .transactionId(transactionId)
                    .status(status)
                    .build();

            Payment saved = paymentRepository.save(payment);
            log.info("Saved payment with id: {} status: {} transactionId: {} for orderId: {}", saved.getId(), status, transactionId, request.orderId());

            try {
                if (result.isSuccess()) {
                    order.setPaymentStatus(OrderPaymentStatus.PAID);
                    if (order.getStatus() == com.ecommerce.backend.enums.OrderStatus.PROCESSING) {
                        order.setStatus(com.ecommerce.backend.enums.OrderStatus.CONFIRMED);
                        log.info("Order {} auto CONFIRMED on successful payment", order.getId());
                    }
                    log.info("Order {} paymentStatus updated to PAID", order.getId());
                } else {
                    order.setPaymentStatus(OrderPaymentStatus.EXPIRED);
                    log.warn("Order {} paymentStatus updated to EXPIRED due to: {}", order.getId(), result.getErrorMessage());
                }
                orderRepository.save(order);
                log.debug("Order payment status saved for orderId: {}", order.getId());
                if (result.isSuccess()) {
                    try {
                        cartRepository.findByUserId(userId).ifPresent(cart -> {
                            if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
                                int size = cart.getCartItems().size();
                                cart.getCartItems().clear();
                                cartRepository.save(cart);
                                log.info("Cleared {} items from cart for user {} after PAID order {}", size, userId, order.getId());
                            }
                        });
                    } catch (Exception ce) {
                        log.warn("Failed to clear cart after PAID order {} for user {}", order.getId(), userId, ce);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update order payment status for orderId: {}", order.getId(), e);
                throw e;
            }

            return paymentMapper.toResponse(saved);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process payment for orderId: {} by userId: {}", request.orderId(), userId, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(String orderId, String userId) {
        log.debug("Fetching payments for orderId: {} by userId: {}", orderId, userId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> {
                        log.warn("Order not found with id: {}", orderId);
                        return new IllegalArgumentException("Order not found");
                    });

            if (!order.getUser().getUserId().equals(userId)) {
                log.warn("Access denied - order {} does not belong to user {}", orderId, userId);
                throw new IllegalArgumentException("Order not found");
            }

            List<Payment> payments = paymentRepository.findByOrderId(orderId);
            log.info("Retrieved {} payments for orderId: {}", payments.size(), orderId);
            return payments.stream()
                    .map(paymentMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch payments for orderId: {} by userId: {}", orderId, userId, e);
            throw e;
        }
    }
}
