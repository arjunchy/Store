package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.ecommerce.backend.mapper.PaymentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private PaymentMapper paymentMapper;

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
