package com.soukconect.bpm.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Client for payment gateway (Stripe, CMI, etc.).
 * This is a placeholder - implement actual payment gateway integration.
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
     * Charge the customer's payment method.
     *
     * @return Transaction ID on success
     * @throws RuntimeException on payment failure
     */
    public String charge(Long customerId, BigDecimal amount, String paymentMethod, String description) {
        log.info("Processing payment: customerId={}, amount={}, method={}", customerId, amount, paymentMethod);

        // TODO: Implement actual payment gateway integration (Stripe, CMI, etc.)
        // For now, simulate successful payment

        String url = baseUrl + "/payments/charge";

        Map<String, Object> request = Map.of(
                "customerId", customerId,
                "amount", amount,
                "currency", "MAD",
                "paymentMethod", paymentMethod,
                "description", description
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.get("transactionId") != null) {
                return response.get("transactionId").toString();
            }

            // Fallback for testing - generate mock transaction ID
            return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        } catch (Exception e) {
            log.error("Payment charge failed: {}", e.getMessage());
            throw new RuntimeException("Payment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refund a previous transaction.
     */
    public void refund(String transactionId, BigDecimal amount) {
        log.info("Processing refund: transactionId={}, amount={}", transactionId, amount);

        String url = baseUrl + "/payments/refund";

        Map<String, Object> request = Map.of(
                "transactionId", transactionId,
                "amount", amount
        );

        try {
            restTemplate.postForObject(url, request, Void.class);
            log.info("Refund successful: transactionId={}", transactionId);
        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }
}
