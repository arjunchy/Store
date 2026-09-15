package com.ecommerce.backend.service.payment;

import com.ecommerce.backend.dto.response.EsewaInitiateResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.entity.Payment;
import com.ecommerce.backend.enums.OrderPaymentStatus;
import com.ecommerce.backend.enums.PaymentMethod;
import com.ecommerce.backend.enums.PaymentStatus;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.PaymentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class EsewaService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartRepository cartRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate restTemplate;

    @Value("${esewa.product-code:EPAYTEST}")
    private String productCode;

    @Value("${esewa.secret-key:8gBm/:&EnhH.1/q}")
    private String secretKey;

    @Value("${esewa.mode:test}")
    private String mode;

    @Value("${esewa.test-gateway-url:https://rc-epay.esewa.com.np/api/epay/main/v2/form}")
    private String testGatewayUrl;

    @Value("${esewa.prod-gateway-url:https://epay.esewa.com.np/api/epay/main/v2/form}")
    private String prodGatewayUrl;

    @Value("${esewa.test-status-url:https://rc-epay.esewa.com.np/api/epay/transaction/status/}")
    private String testStatusUrl;

    @Value("${esewa.prod-status-url:https://epay.esewa.com.np/api/epay/transaction/status/}")
    private String prodStatusUrl;

    @Value("${esewa.success-url:http://localhost:3000/esewa/callback}")
    private String successUrl;

    @Value("${esewa.failure-url:http://localhost:3000/esewa/callback}")
    private String failureUrl;

    @Value("${esewa.timeout-ms:10000}")
    private int timeoutMs;

    public EsewaService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CartRepository cartRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.cartRepository = cartRepository;
    }

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        this.restTemplate = new RestTemplate(factory);
    }

    private String gatewayUrl() {
        return "prod".equalsIgnoreCase(mode) ? prodGatewayUrl : testGatewayUrl;
    }

    private String statusUrl() {
        return "prod".equalsIgnoreCase(mode) ? prodStatusUrl : testStatusUrl;
    }

    private static String fmt(BigDecimal value) {

        if (value == null) {
            return "0";
        }

        BigDecimal scaled = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();

        if (scaled.scale() < 0) {
            scaled = scaled.setScale(0);
        }

        return scaled.toPlainString();
    }

    public static String hmacSha256Base64(String secret, String message) {
        try {

            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder()
                    .encodeToString(raw);

        } catch (Exception e) {

            throw new IllegalStateException("Failed to sign eSewa payload", e);
        }
    }

    @Transactional
    public EsewaInitiateResponse initiate(String orderId, String userId) {

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

        if (paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID).isPresent()) {
            throw new IllegalArgumentException("Order already paid");
        }

        BigDecimal total = order.getTotalAmount();
        if (total == null) {
            throw new IllegalStateException("Order amount not available");
        }

        BigDecimal tax = order.getTax() != null ? order.getTax() : BigDecimal.ZERO;

        BigDecimal delivery = order.getShippingCost() != null ? order.getShippingCost() : BigDecimal.ZERO;

        BigDecimal amount = total.subtract(tax).subtract(delivery);

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = total;
        }

        String totalStr = fmt(total);

        String amountStr = fmt(amount);

        String taxStr = fmt(tax);

        String deliveryStr = fmt(delivery);

        // Fresh transaction uuid per initiation — never the order id.
        String transactionUuid = "TXN-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12);

        String signedFieldNames = "total_amount,transaction_uuid,product_code";

        String message = "total_amount=" + totalStr + ",transaction_uuid=" + transactionUuid + ",product_code=" + productCode;

        String signature = hmacSha256Base64(secretKey, message);

        Map<String, Object> requestPayload = new LinkedHashMap<>();

        requestPayload.put("amount", amountStr);
        requestPayload.put("tax_amount", taxStr);
        requestPayload.put("total_amount", totalStr);
        requestPayload.put("transaction_uuid", transactionUuid);
        requestPayload.put("product_code", productCode);
        requestPayload.put("product_service_charge", "0");
        requestPayload.put("product_delivery_charge", deliveryStr);
        requestPayload.put("success_url", successUrl);
        requestPayload.put("failure_url", failureUrl);
        requestPayload.put("signed_field_names", signedFieldNames);
        requestPayload.put("signature", signature);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(requestPayload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize eSewa request body", e);
        }

        // Each initiation gets a fresh uuid; supersede any previous INITIATED attempt.
        Optional<Payment> existing = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        if (existing.isPresent() && PaymentStatus.INITIATED.equals(existing.get().getStatus())) {
            Payment superseded = existing.get();
            superseded.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(superseded);
            log.info("Superseded previous INITIATED eSewa payment {} for order {}", superseded.getTransactionId(), orderId);
        }

        Payment payment = Payment.builder()
                        .order(order)
                        .amount(total)
                        .method(PaymentMethod.ESEWA)
                        .transactionId(transactionUuid)
                        .paymentUrl(gatewayUrl())
                        .status(PaymentStatus.INITIATED)
                        .requestBody(requestBody)
                        .build();

        paymentRepository.save(payment);

        log.info("eSewa payment initiated for order {} with transaction UUID {}", orderId, transactionUuid);

        return new EsewaInitiateResponse(
                gatewayUrl(),
                productCode,
                transactionUuid,
                totalStr,
                amountStr,
                taxStr,
                "0",
                deliveryStr,
                successUrl,
                failureUrl,
                signedFieldNames,
                signature,
                orderId,
                order.getOrderNumber()
        );
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String data, String transactionUuid, String transactionCode, String totalAmount) {
        return verify(data, transactionUuid, transactionCode, totalAmount, null);
    }

    // transactionId == eSewa transaction_uuid (fresh per initiate, never the order id);
    // transactionCode == eSewa transaction_code (gateway reference).
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> verify(String data, String transactionUuid, String transactionCode, String totalAmount, String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Authentication required");
        }

        String uuid = transactionUuid;

        String code = transactionCode;

        String totalStr = totalAmount;

        String status = null;

        Map<String, Object> decoded = null;

        if (data != null && !data.isBlank()) {

            try {
                String json = new String(Base64.getDecoder().decode(data.trim()), StandardCharsets.UTF_8);
                decoded = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

                uuid = str(decoded.get("transaction_uuid"));
                code = str(decoded.get("transaction_code"));
                status = str(decoded.get("status"));
                totalStr = str(decoded.get("total_amount"));

                String signedFields = str(decoded.get("signed_field_names"));

                String responseSignature = str(decoded.get("signature"));

                if (uuid == null || signedFields == null || responseSignature == null) {
                    throw new IllegalArgumentException("Invalid eSewa response data");
                }

                List<String> parts = new ArrayList<>();

                for (String field : signedFields.split(",")) {
                    String key = field.trim();
                    Object value = decoded.get(key);
                    parts.add(key + "=" + (value != null ? value.toString() : ""));
                }

                String expectedSignature = hmacSha256Base64(secretKey, String.join(",", parts));
                if (!expectedSignature.equals(responseSignature)) {
                    log.warn("eSewa signature mismatch for UUID {}", uuid);
                    throw new IllegalArgumentException("eSewa signature verification failed");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to decode eSewa response", e);
                throw new IllegalArgumentException("Invalid eSewa response data");
            }
        }
        if (uuid == null || uuid.isBlank()) {
            throw new IllegalArgumentException("transaction_uuid is required");
        }

        final String lookupUuid = uuid;

        // Resolve order through the payment; fall back to legacy rows where uuid == orderId.
        Payment payment = paymentRepository.findByTransactionId(lookupUuid).orElse(null);
        Order order = null;
        if (payment != null && payment.getOrder() != null) {
            order = payment.getOrder();
        } else {
            Optional<Order> legacyOrder = orderRepository.findById(lookupUuid);
            if (legacyOrder.isPresent()) {
                order = legacyOrder.get();
                log.info("Resolved legacy eSewa uuid {} as orderId {}", uuid, order.getId());
                if (payment == null) {
                    payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null);
                }
            }
        }
        if (order == null) {
            throw new IllegalArgumentException("Order not found for transaction " + uuid);
        }

        if (order.getUser() != null && !userId.equals(order.getUser().getUserId())) {
            log.warn("eSewa verify denied: user {} does not own order {}", userId, order.getId());
            throw new IllegalArgumentException("Order not found");
        }

        // Reject a signed callback whose amount differs from the order total.
        if (decoded != null) {
            Object decodedTotal = decoded.get("total_amount");
            if (decodedTotal != null && order.getTotalAmount() != null) {
                try {
                    BigDecimal callbackTotal = new BigDecimal(decodedTotal.toString());
                    if (callbackTotal.compareTo(order.getTotalAmount()) != 0) {
                        log.warn("eSewa amount mismatch for uuid {} order {}: callback {} vs order {}",
                                uuid, order.getId(), callbackTotal, order.getTotalAmount());
                        throw new IllegalArgumentException("eSewa amount mismatch: callback total does not match order total");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid eSewa total_amount");
                }
            }
        }

        if (payment != null && decoded != null) {
            try {
                String responseBody = objectMapper.writeValueAsString(decoded);
                payment.setResponseBody(responseBody);
                paymentRepository.save(payment);

            } catch (Exception e) {

                throw new IllegalStateException(
                        "Failed to serialize eSewa verification response body",
                        e
                );
            }
        }

        if (order.getPaymentStatus() == OrderPaymentStatus.PAID) {
            return alreadyPaidResponse(order, payment, decoded);
        }

        if (status != null && !"COMPLETE".equalsIgnoreCase(status)) {
            if (payment != null) {
                payment.setStatus(PaymentStatus.EXPIRED);
                paymentRepository.save(payment);
            }

            order.setPaymentStatus(OrderPaymentStatus.EXPIRED);
            orderRepository.save(order);

            log.info("eSewa {} for order {} - marked EXPIRED", status, order.getId());

            return unifiedResponse(order, uuid, code, status, order.getPaymentStatus().name());
        }

        String checkTotal = totalStr != null ? totalStr : fmt(order.getTotalAmount());

        Map<String, Object> check = statusCheck(productCode, uuid, checkTotal);

        String checkStatus = str(check.get("status"));

        if (!"COMPLETE".equalsIgnoreCase(checkStatus)) {
            log.info("eSewa status check returned {} for order {}", checkStatus, order.getId());

            return unifiedResponse(order, uuid, str(check.get("transaction_code")), checkStatus, order.getPaymentStatus().name());
        }

        Order locked = orderRepository.findByIdForUpdate(order.getId()).orElse(null);

        if (locked != null) {
            order = locked;
            // Re-read the same payment by transactionId, not the latest for the order.
            Payment fresh = paymentRepository.findByTransactionId(lookupUuid).orElse(null);
            if (fresh != null) {
                payment = fresh;
            }

            if (order.getPaymentStatus() == OrderPaymentStatus.PAID || (payment != null && payment.getStatus() == PaymentStatus.PAID)) {
                return alreadyPaidResponse(order, payment, decoded);
            }
        }

        String txnCode = code != null ? code : str(check.get("transaction_code"));

        if (payment != null) {

            payment.setStatus(PaymentStatus.PAID);

            if (txnCode != null && !txnCode.isBlank()) {
                payment.setTransactionCode(txnCode);
            }

            paymentRepository.save(payment);

        } else {
            payment = paymentRepository.save(Payment.builder()
                                    .order(order)
                                    .amount(order.getTotalAmount())
                                    .method(PaymentMethod.ESEWA)
                                    .transactionId(uuid)
                                    .transactionCode(txnCode)
                                    .status(PaymentStatus.PAID)
                                    .build()
                    );
        }

        order.setPaymentStatus(OrderPaymentStatus.PAID);

        if (order.getStatus() == com.ecommerce.backend.enums.OrderStatus.PROCESSING) {
            order.setStatus(com.ecommerce.backend.enums.OrderStatus.CONFIRMED);
        }

        orderRepository.save(order);

        log.info("eSewa payment COMPLETE for order {} - marked PAID", order.getId());

        final String paidOrderId = order.getId();

        try {
            String uid = order.getUser() != null ? order.getUser().getUserId() : null;

            if (uid != null) {
                cartRepository.findByUserId(uid).ifPresent(cart -> {

                    if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
                                int size = cart.getCartItems().size();
                                cart.getCartItems().clear();
                                cartRepository.save(cart);
                                log.info("Cleared {} cart items from user {} after eSewa PAID order {}", size, uid, paidOrderId);
                            }
                        });
            }

        } catch (Exception e) {
            log.warn("Failed to clear cart after eSewa PAID order {}", paidOrderId, e);
        }

        return unifiedResponse(order, uuid, txnCode, "COMPLETE", OrderPaymentStatus.PAID.name());
    }

    private Map<String, Object> unifiedResponse(Order order, String transactionId, String transactionCode, String status, String paymentStatus) {
        Map<String, Object> out = new HashMap<>();
        out.put("orderId", order.getId());
        out.put("orderNumber", order.getOrderNumber());
        out.put("paymentStatus", paymentStatus);
        out.put("status", status);
        out.put("transactionId", transactionId);
        out.put("transactionCode", transactionCode);
        out.put("amountPaid", fmt(order.getTotalAmount()));
        return out;
    }

    private Map<String, Object> alreadyPaidResponse(
            Order order,
            Payment payment,
            Map<String, Object> decoded
    ) {

        log.info("eSewa verify for already-PAID order {} - idempotent success", order.getId());

        String transactionId = payment != null ? payment.getTransactionId() : null;
        String transactionCode = payment != null ? payment.getTransactionCode() : null;
        if (decoded != null) {
            if (transactionId == null) transactionId = str(decoded.get("transaction_uuid"));
            if (transactionCode == null) transactionCode = str(decoded.get("transaction_code"));
        }
        if (transactionCode == null) {
            Optional<Payment> latest =
                    paymentRepository
                            .findFirstByOrderIdOrderByCreatedAtDesc(
                                    order.getId()
                            );
            if (latest.isPresent() && latest.get().getTransactionCode() != null) {
                transactionCode = latest.get().getTransactionCode();
            }
        }

        return unifiedResponse(order, transactionId, transactionCode, "COMPLETE", OrderPaymentStatus.PAID.name());
    }

    private Map<String, Object> statusCheck(
            String productCode,
            String transactionUuid,
            String totalAmount
    ) {
        String url = UriComponentsBuilder
                        .fromUriString(statusUrl())
                        .queryParam("product_code", productCode)
                        .queryParam("transaction_uuid", transactionUuid)
                        .queryParam("total_amount", totalAmount)
                        .build(true)
                        .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            log.info("Checking eSewa payment status for UUID {}", transactionUuid);
            ResponseEntity<Map> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            Map.class
                    );
            if (response.getBody() == null) {
                throw new IllegalStateException("Empty eSewa status response");
            }
            return response.getBody();
        } catch (Exception e) {
            log.error("eSewa status check failed for UUID {}", transactionUuid, e);
            throw new IllegalStateException("eSewa status check failed: " + e.getMessage(), e);
        }
    }

    private static String str(Object value) {
        return value != null ? value.toString() : null;
    }
}