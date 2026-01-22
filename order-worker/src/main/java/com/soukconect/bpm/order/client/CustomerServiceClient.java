package com.soukconect.bpm.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for customer-service.
 */
@Component
public class CustomerServiceClient {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CustomerServiceClient(
            RestTemplate restTemplate,
            @Value("${services.customer.url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void sendNotification(Long customerId, String title, String message) {
        String url = baseUrl + "/customers/" + customerId + "/notifications";
        log.debug("POST {}", url);

        Map<String, String> notification = Map.of(
                "title", title,
                "message", message
        );

        try {
            restTemplate.postForObject(url, notification, Void.class);
            log.info("Customer {} notified: {}", customerId, title);
        } catch (Exception e) {
            log.warn("Failed to notify customer {}: {}", customerId, e.getMessage());
        }
    }
}
