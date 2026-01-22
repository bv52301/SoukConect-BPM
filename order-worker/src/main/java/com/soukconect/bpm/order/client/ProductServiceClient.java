package com.soukconect.bpm.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for product-service.
 */
@Component
public class ProductServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ProductServiceClient(
            RestTemplate restTemplate,
            @Value("${services.product.url:http://localhost:8082}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public boolean isProductAvailable(Long productId, Integer quantity) {
        String url = baseUrl + "/products/" + productId + "/availability?quantity=" + quantity;
        log.debug("GET {}", url);

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("available"));
        } catch (Exception e) {
            log.warn("Failed to check availability for product {}: {}", productId, e.getMessage());
            return false;
        }
    }

    public void reserveStock(Long productId, Integer quantity) {
        String url = baseUrl + "/products/" + productId + "/reserve";
        log.debug("POST {} quantity={}", url, quantity);

        Map<String, Integer> request = Map.of("quantity", quantity);
        restTemplate.postForObject(url, request, Void.class);
    }

    public void releaseStock(Long productId, Integer quantity) {
        String url = baseUrl + "/products/" + productId + "/release";
        log.debug("POST {} quantity={}", url, quantity);

        Map<String, Integer> request = Map.of("quantity", quantity);
        restTemplate.postForObject(url, request, Void.class);
    }

    public Long getVendorId(Long productId) {
        String url = baseUrl + "/products/" + productId;
        log.debug("GET {}", url);

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.get("vendorId") != null) {
            return ((Number) response.get("vendorId")).longValue();
        }
        return null;
    }
}
