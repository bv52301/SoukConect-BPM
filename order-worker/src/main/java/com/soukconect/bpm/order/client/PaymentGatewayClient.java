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
 * Client for Payment Gateway API.
 * Calls the gateway endpoints which process payments via Stripe/CMI
 * and automatically update the DB with results.
 * 
 * Base URL: http://localhost:8085/api (then adds /v1/gateway/{gateway}/...)
 */
@Component
public class PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentGatewayClient(
            RestTemplate restTemplate,
            @Value("${services.payment.url:http://localhost:8085}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Charge the customer's payment method via gateway.
     * This endpoint automatically updates the Payment DB record with the result.
     *
     * @return GatewayResult with success/failure and transaction details
     */
    public GatewayResult charge(ChargeRequest request) {
        log.info("Processing charge: paymentId={}, gateway={}, amount={}",
                request.paymentId(), request.gateway(), request.amount());

        String url = baseUrl + "/v1/gateway/" + request.gateway() + "/charge";

        Map<String, Object> body = new HashMap<>();
        body.put("paymentId", request.paymentId());
        body.put("idempotencyKey", request.idempotencyKey());
        body.put("amount", request.amount());
        body.put("currency", request.currency() != null ? request.currency() : "MAD");
        body.put("paymentToken", request.paymentToken());
        body.put("gatewayCustomerId", request.gatewayCustomerId());
        body.put("orderId", request.orderId());
        body.put("description", request.description());
        body.put("customerEmail", request.customerEmail());
        body.put("customerName", request.customerName());
        body.put("customerPhone", request.customerPhone());
        body.put("require3DS", request.require3DS());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

            return parseGatewayResponse(response);

        } catch (Exception e) {
            log.error("Gateway charge failed: {}", e.getMessage());
            return GatewayResult.failure("GATEWAY_ERROR", e.getMessage());
        }
    }

    /**
     * Refund a previous payment via gateway.
     * This endpoint automatically updates the Payment DB record.
     */
    public GatewayResult refund(RefundRequest request) {
        log.info("Processing refund: paymentId={}, gateway={}, amount={}",
                request.paymentId(), request.gateway(), request.amount());

        String url = baseUrl + "/v1/gateway/" + request.gateway() + "/refund";

        Map<String, Object> body = new HashMap<>();
        body.put("paymentId", request.paymentId());
        body.put("gatewayPaymentId", request.gatewayPaymentId());
        body.put("amount", request.amount());
        body.put("reason", request.reason());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

            return parseGatewayResponse(response);

        } catch (Exception e) {
            log.error("Gateway refund failed: {}", e.getMessage());
            return GatewayResult.failure("GATEWAY_ERROR", e.getMessage());
        }
    }

    /**
     * Check gateway status for a payment (read-only, no DB update).
     */
    public GatewayResult getStatus(String gateway, String gatewayPaymentId) {
        log.debug("Getting status: gateway={}, gatewayPaymentId={}", gateway, gatewayPaymentId);

        String url = baseUrl + "/v1/gateway/" + gateway + "/status/" + gatewayPaymentId;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            return parseGatewayResponse(response);

        } catch (Exception e) {
            log.error("Get status failed: {}", e.getMessage());
            return GatewayResult.failure("GATEWAY_ERROR", e.getMessage());
        }
    }

    private GatewayResult parseGatewayResponse(Map<String, Object> response) {
        if (response == null) {
            return GatewayResult.failure("NO_RESPONSE", "No response from gateway");
        }

        Boolean success = (Boolean) response.get("success");
        String status = (String) response.get("status");
        String gatewayPaymentId = (String) response.get("gatewayPaymentId");
        String errorCode = (String) response.get("errorCode");
        String errorMessage = (String) response.get("errorMessage");
        String authUrl = (String) response.get("authUrl");

        log.info("Gateway response: success={}, status={}, gatewayPaymentId={}",
                success, status, gatewayPaymentId);

        return new GatewayResult(
                Boolean.TRUE.equals(success),
                status,
                gatewayPaymentId,
                errorCode,
                errorMessage,
                authUrl);
    }

    // ==================== DTOs ====================

    /**
     * Request to charge a payment via gateway.
     */
    public record ChargeRequest(
            Long paymentId,
            String gateway,
            String idempotencyKey,
            BigDecimal amount,
            String currency,
            String paymentToken,
            String gatewayCustomerId,
            Long orderId,
            String description,
            String customerEmail,
            String customerName,
            String customerPhone,
            boolean require3DS) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long paymentId;
            private String gateway = "STRIPE";
            private String idempotencyKey;
            private BigDecimal amount;
            private String currency = "MAD";
            private String paymentToken;
            private String gatewayCustomerId;
            private Long orderId;
            private String description;
            private String customerEmail;
            private String customerName;
            private String customerPhone;
            private boolean require3DS = false;

            public Builder paymentId(Long paymentId) {
                this.paymentId = paymentId;
                return this;
            }

            public Builder gateway(String gateway) {
                this.gateway = gateway;
                return this;
            }

            public Builder idempotencyKey(String key) {
                this.idempotencyKey = key;
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

            public Builder paymentToken(String token) {
                this.paymentToken = token;
                return this;
            }

            public Builder gatewayCustomerId(String id) {
                this.gatewayCustomerId = id;
                return this;
            }

            public Builder orderId(Long orderId) {
                this.orderId = orderId;
                return this;
            }

            public Builder description(String desc) {
                this.description = desc;
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

            public Builder require3DS(boolean require) {
                this.require3DS = require;
                return this;
            }

            public ChargeRequest build() {
                return new ChargeRequest(
                        paymentId, gateway, idempotencyKey, amount, currency,
                        paymentToken, gatewayCustomerId, orderId, description,
                        customerEmail, customerName, customerPhone, require3DS);
            }
        }
    }

    /**
     * Request to refund a payment via gateway.
     */
    public record RefundRequest(
            Long paymentId,
            String gateway,
            String gatewayPaymentId,
            BigDecimal amount,
            String reason) {
    }

    /**
     * Result from gateway operation.
     */
    public record GatewayResult(
            boolean success,
            String status,
            String gatewayPaymentId,
            String errorCode,
            String errorMessage,
            String authUrl) {
        public static GatewayResult failure(String errorCode, String errorMessage) {
            return new GatewayResult(false, "FAILED", null, errorCode, errorMessage, null);
        }

        public boolean isSucceeded() {
            return success && "SUCCEEDED".equals(status);
        }

        public boolean requiresAction() {
            return "REQUIRES_ACTION".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }

        public boolean isRetryable() {
            // Most gateway errors are retryable except for specific cases
            if (errorCode == null)
                return true;
            return switch (errorCode) {
                case "card_declined", "processing_error", "timeout" -> true;
                case "invalid_card", "expired_card", "stolen_card", "fraud_detected" -> false;
                default -> true;
            };
        }
    }
}
