package com.soukconect.bpm.order.client;

import com.soukconect.bpm.common.dto.OrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for vendor-service.
 */
@Component
public class VendorServiceClient {

    private static final Logger log = LoggerFactory.getLogger(VendorServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public VendorServiceClient(
            RestTemplate restTemplate,
            @Value("${services.vendor.url:http://localhost:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void notifyNewOrder(Long vendorId, OrderDto order) {
        String url = baseUrl + "/vendors/" + vendorId + "/notifications";
        log.debug("POST {}", url);

        Map<String, Object> notification = Map.of(
                "type", "NEW_ORDER",
                "orderId", order.id(),
                "totalAmount", order.totalAmount(),
                "itemCount", order.items() != null ? order.items().size() : 0
        );

        try {
            restTemplate.postForObject(url, notification, Void.class);
            log.info("Vendor {} notified of new order {}", vendorId, order.id());
        } catch (Exception e) {
            log.warn("Failed to notify vendor {}: {}", vendorId, e.getMessage());
        }
    }

    public void notifyCancellation(Long vendorId, Long orderId, String reason) {
        String url = baseUrl + "/vendors/" + vendorId + "/notifications";
        log.debug("POST {} (cancellation)", url);

        Map<String, Object> notification = Map.of(
                "type", "ORDER_CANCELLED",
                "orderId", orderId,
                "reason", reason
        );

        try {
            restTemplate.postForObject(url, notification, Void.class);
            log.info("Vendor {} notified of order {} cancellation", vendorId, orderId);
        } catch (Exception e) {
            log.warn("Failed to notify vendor {} of cancellation: {}", vendorId, e.getMessage());
        }
    }
}
