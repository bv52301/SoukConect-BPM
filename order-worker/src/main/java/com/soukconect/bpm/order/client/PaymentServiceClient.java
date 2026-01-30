package com.soukconect.bpm.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for Payment Service DB operations.
 * Handles creating, updating, and querying payment records.
 * 
 * Base URL: http://localhost:8085/api/v1/payments
 */
@Component
public class PaymentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentServiceClient(
            RestTemplate restTemplate,
            @Value("${services.payment.url:http://localhost:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Create a new payment record with PENDING status.
     * Returns existing payment if idempotency key matches.
     *
     * @return PaymentInfo with payment ID and status
     */
    public PaymentInfo createPayment(CreatePaymentRequest request) {
        log.info("Creating payment record: orderId={}, amount={}, method={}",
                request.orderId(), request.amount(), request.paymentMethod());

        String url = baseUrl + "/api/v1/payments";

        Map<String, Object> body = new HashMap<>();
        body.put("orderId", request.orderId());
        body.put("customerId", request.customerId());
        if (request.vendorId() != null) {
            body.put("vendorId", request.vendorId());
        }
        body.put("amount", request.amount());
        body.put("currency", request.currency() != null ? request.currency() : "MAD");
        body.put("paymentMethod", request.paymentMethod());
        body.put("paymentGateway", request.gateway());
        body.put("idempotencyKey", request.idempotencyKey());
        body.put("description", request.description());
        body.put("payerEmail", request.customerEmail());
        body.put("payerName", request.customerName());
        body.put("payerPhone", request.customerPhone());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

            if (response != null && response.get("id") != null) {
                Long paymentId = ((Number) response.get("id")).longValue();
                String status = (String) response.get("status");
                String idempKey = (String) response.get("idempotencyKey");

                log.info("Payment record created/found: paymentId={}, status={}", paymentId, status);

                return new PaymentInfo(
                        paymentId,
                        status,
                        idempKey,
                        (String) response.get("gatewayPaymentId"),
                        null, // errorCode
                        null // errorMessage
                );
            }

            throw new RuntimeException("Failed to create payment: no ID returned");
        } catch (Exception e) {
            log.error("Failed to create payment record: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    /**
     * Get payment by ID.
     */
    public PaymentInfo getPayment(Long paymentId) {
        log.debug("Getting payment: {}", paymentId);
        String url = baseUrl + "/api/v1/payments/" + paymentId;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null) {
                return new PaymentInfo(
                        ((Number) response.get("id")).longValue(),
                        (String) response.get("status"),
                        (String) response.get("idempotencyKey"),
                        (String) response.get("gatewayPaymentId"),
                        null,
                        null);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get payment: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Mark payment as abandoned after exhausting all retries.
     * User can still try again with a different card/method (new Payment record).
     */
    public void giveup(Long paymentId, String reason) {
        log.info("Giving up payment: paymentId={}, reason={}", paymentId, reason);

        String url = baseUrl + "/api/v1/payments/" + paymentId + "/giveup";

        Map<String, String> body = new HashMap<>();
        if (reason != null) {
            body.put("reason", reason);
        }

        try {
            restTemplate.postForObject(url, body, Map.class);
            log.info("Payment marked as abandoned: paymentId={}", paymentId);
        } catch (Exception e) {
            log.error("Failed to mark payment as abandoned: {}", e.getMessage());
            throw new RuntimeException("Failed to give up payment: " + e.getMessage(), e);
        }
    }

    // ==================== DTOs ====================

    /**
     * Request to create a payment record.
     */
    public record CreatePaymentRequest(
            Long orderId,
            Long customerId,
            Long vendorId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String gateway,
            String idempotencyKey,
            String description,
            String customerEmail,
            String customerName,
            String customerPhone) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long orderId;
            private Long customerId;
            private Long vendorId;
            private BigDecimal amount;
            private String currency = "MAD";
            private String paymentMethod = "CARD";
            private String gateway = "STRIPE";
            private String idempotencyKey;
            private String description;
            private String customerEmail;
            private String customerName;
            private String customerPhone;

            public Builder orderId(Long orderId) {
                this.orderId = orderId;
                return this;
            }

            public Builder customerId(Long customerId) {
                this.customerId = customerId;
                return this;
            }

            public Builder vendorId(Long vendorId) {
                this.vendorId = vendorId;
                return this;
            }

            public Builder amount(BigDecimal amount) {
                this.amount = amount;
                return this;
            }

            public Builder currency(String currency) {
                this.currency = currency;
                return this;
            }

            public Builder paymentMethod(String paymentMethod) {
                this.paymentMethod = paymentMethod;
                return this;
            }

            public Builder gateway(String gateway) {
                this.gateway = gateway;
                return this;
            }

            public Builder idempotencyKey(String idempotencyKey) {
                this.idempotencyKey = idempotencyKey;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder customerEmail(String email) {
                this.customerEmail = email;
                return this;
            }

            public Builder customerName(String name) {
                this.customerName = name;
                return this;
            }

            public Builder customerPhone(String phone) {
                this.customerPhone = phone;
                return this;
            }

            public CreatePaymentRequest build() {
                return new CreatePaymentRequest(
                        orderId, customerId, vendorId, amount, currency,
                        paymentMethod, gateway, idempotencyKey, description,
                        customerEmail, customerName, customerPhone);
            }
        }
    }

    /**
     * Simplified payment info from API.
     */
    public record PaymentInfo(
            Long id,
            String status,
            String idempotencyKey,
            String gatewayPaymentId,
            String errorCode,
            String errorMessage) {
        public boolean isPending() {
            return "PENDING".equals(status);
        }

        public boolean isCompleted() {
            return "COMPLETED".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }

        public boolean isAbandoned() {
            return "ABANDONED".equals(status);
        }
    }
}
