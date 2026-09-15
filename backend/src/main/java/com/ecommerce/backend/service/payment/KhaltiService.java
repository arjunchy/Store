package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.KhaltiInitiateResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.OrderItem;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KhaltiService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ObjectMapper objectMapper;

    private RestTemplate restTemplate;

    @Value("${khalti.secret-key:}")
    private String secretKey;

    @Value("${khalti.mode:sandbox}")
    private String mode;

    @Value("${khalti.sandbox-url:https://dev.khalti.com/api/v2/}")
    private String sandboxUrl;

    @Value("${khalti.prod-url:https://khalti.com/api/v2/}")
    private String prodUrl;

    @Value("${khalti.timeout-ms:10000}")
    private int timeoutMs;

    @Value("${app.payment.return-url:http://localhost:3000/payment/callback}")
    private String returnUrl;

    @Value("${app.payment.website-url:http://localhost:3000}")
    private String websiteUrl;

    public KhaltiService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
    }

    private String baseUrl() {
        return "sandbox".equalsIgnoreCase(mode) ? sandboxUrl : prodUrl;
    }

    private boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    @Transactional
    public KhaltiInitiateResponse initiate(String orderId, String userId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order not found");
        }

        if (order.getStatus() == com.ecommerce.backend.enums.OrderStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot pay for a canceled order");
        }

        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new IllegalArgumentException("Order already paid");
        }

        Optional<Payment> existingInitiated = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.INITIATED);

        if (existingInitiated.isPresent()) {
            Payment existingPayment = existingInitiated.get();

            if (existingPayment.getPaymentUrl() != null && existingPayment.getPidx() != null) {

                return new KhaltiInitiateResponse(
                        existingPayment.getPidx(),
                        existingPayment.getPaymentUrl(),
                        null,
                        1800,
                        orderId,
                        order.getOrderNumber(),
                        order.getOrderNumber(),
                        order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue()
                );
            }
        }

        if (paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID).isPresent()) {
            throw new IllegalArgumentException("Order already paid");
        }

        BigDecimal total = order.getTotalAmount();

        if (total == null) {
            throw new IllegalStateException("Order amount not available");
        }

        int amountPaisa = total.multiply(BigDecimal.valueOf(100)).intValueExact();

        if (amountPaisa < 1000) {
            throw new IllegalArgumentException("Amount should be greater than Rs. 10 (1000 paisa)");
        }

        if (!isConfigured()) {
            throw new IllegalStateException("Khalti not configured - set KHALTI_SECRET_KEY");
        }

        Map<String, Object> payload = new HashMap<>();

        payload.put("return_url", returnUrl);
        payload.put("website_url", websiteUrl);
        payload.put("amount", amountPaisa);
        payload.put("purchase_order_id", order.getOrderNumber());
        payload.put("purchase_order_name", "ApexCommerce Order " + order.getOrderNumber());

        Map<String, String> customerInfo = new HashMap<>();

        customerInfo.put("name", order.getUser().getUsername());
        customerInfo.put("email", order.getUser().getEmail());
        customerInfo.put("phone", "9800000000");

        payload.put("customer_info", customerInfo);

        try {
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);

            List<Map<String, Object>> productDetails = items.stream().map(item -> {

                                Map<String, Object> product = new HashMap<>();

                                product.put("identity", item.getProduct() != null ? item.getProduct().getId() : item.getId());
                                product.put("name", item.getProduct() != null ? item.getProduct().getName() : "Item");

                                int unitPrice = item.getPrice().multiply(BigDecimal.valueOf(100)).intValue();
                                int quantity = item.getQuantity() != null ? item.getQuantity() : 1;

                                product.put("unit_price", unitPrice);
                                product.put("quantity", quantity);
                                product.put("total_price", unitPrice * quantity);
                                return product;
                            })
                            .collect(Collectors.toList());

            if (!productDetails.isEmpty()) {
                payload.put("product_details", productDetails);
            }

        } catch (Exception e) {
            log.warn("Failed to build product_details for order {}", orderId, e);
        }

        payload.put("amount_breakdown", List.of(Map.of("label", "Total", "amount", amountPaisa)));

        String requestBody;

        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize Khalti request body", e);
        }

        String url = baseUrl().endsWith("/") ? baseUrl() + "epayment/initiate/" : baseUrl() + "/epayment/initiate/";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Khalti initiate failed: " + response.getStatusCode());
            }

            Map body = response.getBody();

            String responseBody;

            try {
                responseBody = objectMapper.writeValueAsString(body);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to serialize Khalti response body", e);
            }

            String pidx = (String) body.get("pidx");
            String paymentUrl = (String) body.get("payment_url");
            String expiresAtStr = (String) body.get("expires_at");
            Integer expiresIn = body.get("expires_in") != null ? ((Number) body.get("expires_in")).intValue() : 1800;
            OffsetDateTime expiresAt = null;

            try {
                if (expiresAtStr != null) {
                    expiresAt = OffsetDateTime.parse(expiresAtStr);
                }
            } catch (Exception e) {
                log.warn("Unable to parse Khalti expires_at: {}", expiresAtStr);
            }

            if (pidx == null || paymentUrl == null) {
                throw new IllegalStateException("Invalid Khalti response: " + body);
            }

            Payment payment = Payment.builder()
                            .order(order)
                            .amount(total)
                            .method(PaymentMethod.KHALTI)
                            .transactionId(pidx)
                            .pidx(pidx)
                            .paymentUrl(paymentUrl)
                            .status(PaymentStatus.INITIATED)
                            .requestBody(requestBody)
                            .responseBody(responseBody)
                            .build();

            paymentRepository.save(payment);

            return new KhaltiInitiateResponse(
                    pidx,
                    paymentUrl,
                    expiresAt,
                    expiresIn,
                    orderId,
                    order.getOrderNumber(),
                    order.getOrderNumber(),
                    amountPaisa
            );

        } catch (RestClientException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            boolean is401 = e instanceof org.springframework.web.client.HttpClientErrorException.Unauthorized || msg.contains("401");

            if (is401) {throw new IllegalStateException("Khalti authentication failed (401) - " + "invalid KHALTI_SECRET_KEY", e);
            }

            throw new IllegalStateException("Failed to initiate Khalti payment: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> lookup(String pidx, String userId) {

        if (pidx == null || pidx.isBlank()) {
            throw new IllegalArgumentException("pidx is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Authentication required");
        }

        Optional<Payment> opt = paymentRepository.findByPidx(pidx);

        Payment payment = opt.orElse(null);

        // Ownership check — a caller may only look up their own payment.
        // Without this, any authenticated user could confirm/expire/cancel
        // another user's order by guessing its pidx (IDOR).
        if (payment != null && payment.getOrder() != null
                && payment.getOrder().getUser() != null
                && !userId.equals(payment.getOrder().getUser().getUserId())) {
            log.warn("Khalti lookup denied: user {} does not own pidx {}", userId, pidx);
            throw new IllegalArgumentException("Payment not found");
        }

        String orderId = payment != null && payment.getOrder() != null ? payment.getOrder().getId() : null;

        if (!isConfigured()) {throw new IllegalStateException("Khalti not configured - set KHALTI_SECRET_KEY");
        }

        String url = baseUrl().endsWith("/") ? baseUrl() + "epayment/lookup/" : baseUrl() + "/epayment/lookup/";

        HttpHeaders headers = new HttpHeaders();

        headers.set("Authorization","Key " + secretKey);

        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> payload = Map.of("pidx", pidx);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map body = response.getBody();

            if (body == null) {
                throw new IllegalStateException("Empty lookup response");
            }

            String status = (String) body.get("status");

            String transactionId = (String) body.get("transaction_id");

            if (payment != null && status != null) {

                if ("Completed".equalsIgnoreCase(status)) {

                    // Amount + order binding checks — the lookup response must
                    // describe THIS order for its full total. Otherwise a
                    // cheaper/different Khalti transaction could confirm it.
                    assertLookupMatchesOrder(body, payment.getOrder(), pidx);

                    payment.setStatus(PaymentStatus.PAID);

                    payment.setTransactionId(transactionId != null ? transactionId : payment.getTransactionId());

                    payment.getOrder().setPaymentStatus(OrderPaymentStatus.PAID);

                    if (payment.getOrder().getStatus() == com.ecommerce.backend.enums.OrderStatus.PROCESSING) {

                        payment.getOrder().setStatus(com.ecommerce.backend.enums.OrderStatus.CONFIRMED);
                    }

                    try {
                        String uid = payment.getOrder().getUser() != null ? payment.getOrder().getUser().getUserId() : null;

                        if (uid != null) {
                            cartRepository.findByUserId(uid).ifPresent(cart -> {
                                        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
                                            cart.getCartItems().clear();
                                            cartRepository.save(cart);
                                        }
                                    });
                        }

                    } catch (Exception e) {
                        log.warn("Failed to clear cart after Khalti payment {}", pidx, e);
                    }

                } else if ("User canceled".equalsIgnoreCase(status) || "Expired".equalsIgnoreCase(status)) {

                    payment.setStatus(PaymentStatus.EXPIRED);

                    payment.getOrder().setPaymentStatus(OrderPaymentStatus.EXPIRED);

                } else if ("Refunded".equalsIgnoreCase(status) || "Partially Refunded".equalsIgnoreCase(status)) {

                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.getOrder().setPaymentStatus(OrderPaymentStatus.REFUNDED);
                } else if ("Pending".equalsIgnoreCase(status) || "Initiated".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.INITIATED);
                }

                paymentRepository.save(payment);
                orderRepository.save(payment.getOrder());
            }
            return body;

        } catch (RestClientException e) {

            String msg = e.getMessage() != null ? e.getMessage() : "";

            boolean is401 = e instanceof org.springframework.web.client.HttpClientErrorException.Unauthorized || msg.contains("401");

            if (is401) {
                throw new IllegalStateException("Khalti authentication failed (401) - " + "invalid KHALTI_SECRET_KEY", e);
            }
            throw new IllegalStateException("Lookup failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a Khalti {@code Completed} lookup response really belongs to
     * the given order: the paid {@code total_amount} (paisa) must equal the order
     * total, and {@code purchase_order_id} must match the order number when present.
     */
    private void assertLookupMatchesOrder(Map<?, ?> body, Order order, String pidx) {
        Object purchaseOrderId = body.get("purchase_order_id");
        if (purchaseOrderId != null && order.getOrderNumber() != null
                && !order.getOrderNumber().equals(String.valueOf(purchaseOrderId))) {
            log.warn("Khalti purchase_order_id mismatch for pidx {}: got {} expected order {}",
                    pidx, purchaseOrderId, order.getId());
            throw new IllegalArgumentException("Khalti transaction does not belong to this order");
        }
        Object totalAmount = body.get("total_amount");
        if (totalAmount instanceof Number paidPaisa && order.getTotalAmount() != null) {
            int expectedPaisa = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();
            if (paidPaisa.intValue() != expectedPaisa) {
                log.warn("Khalti amount mismatch for pidx {} order {}: paid {} paisa, expected {} paisa",
                        pidx, order.getId(), paidPaisa.intValue(), expectedPaisa);
                throw new IllegalArgumentException("Khalti amount does not match order total");
            }
        }
    }
}