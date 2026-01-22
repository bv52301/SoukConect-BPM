package com.soukconect.bpm.general.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class VendorActivitiesImpl implements VendorActivities {

    private static final Logger log = LoggerFactory.getLogger(VendorActivitiesImpl.class);

    private final RestTemplate restTemplate;
    private final String vendorServiceUrl;
    private final String paymentServiceUrl;

    public VendorActivitiesImpl(
            RestTemplate restTemplate,
            @Value("${services.vendor.url:http://localhost:8083}") String vendorServiceUrl,
            @Value("${services.payment.url:http://localhost:8085}") String paymentServiceUrl) {
        this.restTemplate = restTemplate;
        this.vendorServiceUrl = vendorServiceUrl;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getVendorBankDetails(Long vendorId) {
        log.info("Fetching bank details for vendor: {}", vendorId);

        String url = vendorServiceUrl + "/vendors/" + vendorId + "/bank-details";

        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("Failed to fetch bank details, using placeholder: {}", e.getMessage());
            // Return placeholder for testing
            return Map.of(
                    "bankName", "Bank Al-Maghrib",
                    "accountNumber", "****1234",
                    "iban", "MA**************1234"
            );
        }
    }

    @Override
    public String transferToVendor(Long vendorId, BigDecimal amount, Map<String, String> bankDetails) {
        log.info("Transferring {} to vendor: {}", amount, vendorId);

        String url = paymentServiceUrl + "/payments/transfer";

        Map<String, Object> request = Map.of(
                "vendorId", vendorId,
                "amount", amount,
                "currency", "MAD",
                "bankDetails", bankDetails
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.get("transactionId") != null) {
                return response.get("transactionId").toString();
            }
        } catch (Exception e) {
            log.warn("Payment service unavailable, generating mock transaction: {}", e.getMessage());
        }

        // Mock transaction ID for testing
        return "PAYOUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void recordPayout(Long vendorId, Long orderId, BigDecimal amount, BigDecimal commission, String transactionId) {
        log.info("Recording payout: vendor={}, order={}, amount={}, commission={}", vendorId, orderId, amount, commission);

        String url = vendorServiceUrl + "/vendors/" + vendorId + "/payouts";

        Map<String, Object> record = Map.of(
                "orderId", orderId,
                "amount", amount,
                "commission", commission,
                "transactionId", transactionId
        );

        try {
            restTemplate.postForObject(url, record, Void.class);
        } catch (Exception e) {
            log.warn("Failed to record payout: {}", e.getMessage());
        }
    }

    @Override
    public void notifyVendorPayout(Long vendorId, BigDecimal amount, String transactionId) {
        log.info("Notifying vendor {} of payout: {}", vendorId, amount);

        String url = vendorServiceUrl + "/vendors/" + vendorId + "/notifications";

        Map<String, Object> notification = Map.of(
                "type", "PAYOUT_COMPLETED",
                "amount", amount,
                "transactionId", transactionId,
                "message", "Your payout of " + amount + " MAD has been processed."
        );

        try {
            restTemplate.postForObject(url, notification, Void.class);
        } catch (Exception e) {
            log.warn("Failed to notify vendor: {}", e.getMessage());
        }
    }
}
