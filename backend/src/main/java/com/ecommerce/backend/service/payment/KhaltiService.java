package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.KhaltiInitiateResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.ecommerce.backend.repository.OrderItemRepository;
import com.ecommerce.backend.entity.OrderItem;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KhaltiService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
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

    public KhaltiService(OrderRepository orderRepository, PaymentRepository paymentRepository, OrderItemRepository orderItemRepository, CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
    }

    @PostConstruct
    void initRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(factory);
        log.info("Khalti RestTemplate initialized with timeout {}ms", timeoutMs);
    }

    private String baseUrl() {
        return "sandbox".equalsIgnoreCase(mode) ? sandboxUrl : prodUrl;
    }

    private boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    @Transactional
    public KhaltiInitiateResponse initiate(String orderId, String userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Order not found");
        }
        if (order.getStatus() == com.ecommerce.backend.enums.OrderStatus.CANCELED) {
            throw new IllegalArgumentException("Cannot pay for a canceled order");
        }
        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            throw new IllegalArgumentException("Order already paid");
        }
        // Idempotency: if already initiated and not expired, return same
        Optional<Payment> existingInitiated = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.INITIATED);
        if (existingInitiated.isPresent()) {
            Payment p = existingInitiated.get();
            if (p.getPaymentUrl() != null && p.getPidx() != null) {
                log.info("Returning existing Khalti pidx {} for order {}", p.getPidx(), orderId);
                return new KhaltiInitiateResponse(p.getPidx(), p.getPaymentUrl(), null, 1800, orderId, order.getOrderNumber(), order.getOrderNumber(), order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue());
            }
        }
        // Also check if already paid
        if (paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID).isPresent()) {
            throw new IllegalArgumentException("Order already paid");
        }

        BigDecimal total = order.getTotalAmount();
        if (total == null) throw new IllegalStateException("Order amount not available");
        int amountPaisa = total.multiply(BigDecimal.valueOf(100)).intValueExact();
        if (amountPaisa < 1000) {
            throw new IllegalArgumentException("Amount should be greater than Rs. 10 (1000 paisa)");
        }

        if (!isConfigured()) {
            log.error("Khalti secret not configured for order {} – set KHALTI_SECRET_KEY env (test-admin.khalti.com live_secret_key)", orderId);
            throw new IllegalStateException("Khalti not configured – set KHALTI_SECRET_KEY (get from test-admin.khalti.com). Order remains UNPAID until Khalti payment completes.");
        }

        // Build Khalti payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("return_url", returnUrl);
        payload.put("website_url", websiteUrl);
        payload.put("amount", amountPaisa);
        payload.put("purchase_order_id", order.getOrderNumber());
        payload.put("purchase_order_name", "ApexCommerce Order " + order.getOrderNumber());

        // customer_info
        try {
            Map<String, String> customerInfo = new HashMap<>();
            customerInfo.put("name", order.getUser().getUsername());
            customerInfo.put("email", order.getUser().getEmail());
            customerInfo.put("phone", "9800000000"); // fallback, Khalti docs test number
            payload.put("customer_info", customerInfo);
        } catch (Exception ignored) {}

        // product_details from order items
        try {
            List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(orderId);
            List<Map<String, Object>> productDetails = items.stream().map(it -> {
                Map<String, Object> m = new HashMap<>();
                m.put("identity", it.getProduct() != null ? it.getProduct().getId() : it.getId());
                m.put("name", it.getProduct() != null ? it.getProduct().getName() : "Item");
                int unitPrice = it.getPrice().multiply(BigDecimal.valueOf(100)).intValue();
                int totalPrice = unitPrice * (it.getQuantity() != null ? it.getQuantity() : 1);
                m.put("unit_price", unitPrice);
                m.put("quantity", it.getQuantity());
                m.put("total_price", totalPrice);
                return m;
            }).collect(Collectors.toList());
            if (!productDetails.isEmpty()) payload.put("product_details", productDetails);
        } catch (Exception e) {
            log.warn("Failed to build product_details for Khalti order {}", orderId, e);
        }

        // amount_breakdown – single entry equal to amount
        payload.put("amount_breakdown", List.of(Map.of("label", "Total", "amount", amountPaisa)));

        String url = baseUrl().endsWith("/") ? baseUrl() + "epayment/initiate/" : baseUrl() + "/epayment/initiate/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        log.info("Initiating Khalti payment for order {} amount {} paisa p_order_id {}", orderId, amountPaisa, order.getOrderNumber());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Khalti initiate failed: " + response.getStatusCode());
            }
            Map body = response.getBody();
            String pidx = (String) body.get("pidx");
            String paymentUrl = (String) body.get("payment_url");
            String expiresAtStr = (String) body.get("expires_at");
            Integer expiresIn = body.get("expires_in") != null ? ((Number) body.get("expires_in")).intValue() : 1800;
            OffsetDateTime expiresAt = null;
            try { if (expiresAtStr != null) expiresAt = OffsetDateTime.parse(expiresAtStr); } catch (Exception ignored) {}

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
                    .build();
            paymentRepository.save(payment);
            log.info("Khalti pidx {} created for order {}", pidx, orderId);
            return new KhaltiInitiateResponse(pidx, paymentUrl, expiresAt, expiresIn, orderId, order.getOrderNumber(), order.getOrderNumber(), amountPaisa);
        } catch (RestClientException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            boolean is401 = e instanceof org.springframework.web.client.HttpClientErrorException.Unauthorized || msg.contains("401");
            if (is401) {
                log.error("Khalti 401 Unauthorized for order {} – invalid secret. Get live_secret_key from test-admin.khalti.com (OTP 987654) and set KHALTI_SECRET_KEY env.", orderId);
                throw new IllegalStateException("Khalti authentication failed (401) – invalid KHALTI_SECRET_KEY. Please set correct live_secret_key from test-admin.khalti.com", e);
            }
            log.error("Khalti initiate call failed for order {}", orderId, e);
            throw new IllegalStateException("Failed to initiate Khalti payment: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Object> lookup(String pidx) {
        if (pidx == null || pidx.isBlank()) throw new IllegalArgumentException("pidx is required");

        // Check local Payment first
        Optional<Payment> opt = paymentRepository.findByPidx(pidx);
        Payment payment = opt.orElse(null);
        String orderId = payment != null && payment.getOrder() != null ? payment.getOrder().getId() : null;

        if (!isConfigured()) {
            log.error("Khalti lookup failed – not configured for pidx {} – set KHALTI_SECRET_KEY", pidx);
            throw new IllegalStateException("Khalti not configured – set KHALTI_SECRET_KEY");
        }

        String url = baseUrl().endsWith("/") ? baseUrl() + "epayment/lookup/" : baseUrl() + "/epayment/lookup/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Key " + secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> payload = Map.of("pidx", pidx);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        log.info("Khalti lookup for pidx {}", pidx);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map body = response.getBody();
            if (body == null) throw new IllegalStateException("Empty lookup response");

            String status = (String) body.get("status");
            String transactionId = (String) body.get("transaction_id");
            // Update local payment if exists
            if (payment != null && status != null) {
                if ("Completed".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setTransactionId(transactionId != null ? transactionId : payment.getTransactionId());
                    payment.getOrder().setPaymentStatus(OrderPaymentStatus.PAID);
                    if (payment.getOrder().getStatus() == com.ecommerce.backend.enums.OrderStatus.PROCESSING) {
                        payment.getOrder().setStatus(com.ecommerce.backend.enums.OrderStatus.CONFIRMED);
                    }
                    log.info("Khalti Completed for pidx {} order {}: PAID", pidx, orderId);
                    try {
                        String uid = payment.getOrder().getUser() != null ? payment.getOrder().getUser().getUserId() : null;
                        if (uid != null) {
                            cartRepository.findByUserId(uid).ifPresent(cart -> {
                                if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
                                    int size = cart.getCartItems().size();
                                    cart.getCartItems().clear();
                                    cartRepository.save(cart);
                                    log.info("Cleared {} items from cart for user {} after Khalti Completed order {}", size, uid, orderId);
                                }
                            });
                        }
                    } catch (Exception ce) {
                        log.warn("Failed to clear cart after Khalti Completed pidx {} order {}", pidx, orderId, ce);
                    }
                } else if ("User canceled".equalsIgnoreCase(status) || "Expired".equalsIgnoreCase(status)) {
                    payment.setStatus(PaymentStatus.EXPIRED);
                    payment.getOrder().setPaymentStatus(OrderPaymentStatus.EXPIRED);
                    log.info("Khalti {} for pidx {} order {}", status, pidx, orderId);
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
                log.error("Khalti lookup 401 Unauthorized for pidx {} – invalid secret. Set correct live_secret_key.", pidx);
                throw new IllegalStateException("Khalti authentication failed (401) – invalid KHALTI_SECRET_KEY", e);
            }
            log.error("Khalti lookup failed for pidx {}", pidx, e);
            throw new IllegalStateException("Lookup failed: " + e.getMessage(), e);
        }
    }
}
