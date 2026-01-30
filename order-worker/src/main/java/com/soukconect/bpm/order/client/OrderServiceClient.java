package com.soukconect.bpm.order.client;

import com.soukconect.bpm.common.dto.OrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * REST client for order-service.
 */
@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OrderServiceClient(
            RestTemplate restTemplate,
            @Value("${services.order.url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public OrderDto getOrder(Long orderId) {
        String url = baseUrl + "/orders/" + orderId;
        log.debug("GET {}", url);
        return restTemplate.getForObject(url, OrderDto.class);
    }

    public void updateStatus(Long orderId, String status) {
        String url = baseUrl + "/orders/" + orderId + "/status";
        log.debug("PATCH {} status={}", url, status);

        // Using Map to send status update
        Map<String, String> request = Map.of("status", status);
        restTemplate.patchForObject(url, request, Void.class);
    }

    public OrderDto createOrder(com.soukconect.bpm.common.dto.CreateOrderRequest request) {
        String url = baseUrl + "/orders";
        log.debug("POST {}", url);
        return restTemplate.postForObject(url, request, OrderDto.class);
    }
}
