package com.soukconect.bpm.general.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NotificationActivitiesImpl implements NotificationActivities {

    private static final Logger log = LoggerFactory.getLogger(NotificationActivitiesImpl.class);

    private final RestTemplate restTemplate;
    private final String customerServiceUrl;
    private final String vendorServiceUrl;

    public NotificationActivitiesImpl(
            RestTemplate restTemplate,
            @Value("${services.customer.url:http://localhost:8084}") String customerServiceUrl,
            @Value("${services.vendor.url:http://localhost:8083}") String vendorServiceUrl) {
        this.restTemplate = restTemplate;
        this.customerServiceUrl = customerServiceUrl;
        this.vendorServiceUrl = vendorServiceUrl;
    }

    @Override
    public void sendEmail(String recipientType, Long recipientId, String subject, String body) {
        log.info("Sending email to {} {}: {}", recipientType, recipientId, subject);

        String baseUrl = getBaseUrl(recipientType);
        String url = baseUrl + "/" + getRecipientPath(recipientType) + "/" + recipientId + "/email";

        Map<String, String> request = Map.of(
                "subject", subject,
                "body", body
        );

        try {
            restTemplate.postForObject(url, request, Void.class);
            log.info("Email sent successfully");
        } catch (Exception e) {
            log.warn("Failed to send email: {}", e.getMessage());
        }
    }

    @Override
    public void sendSms(String recipientType, Long recipientId, String message) {
        log.info("Sending SMS to {} {}", recipientType, recipientId);

        String baseUrl = getBaseUrl(recipientType);
        String url = baseUrl + "/" + getRecipientPath(recipientType) + "/" + recipientId + "/sms";

        Map<String, String> request = Map.of("message", message);

        try {
            restTemplate.postForObject(url, request, Void.class);
            log.info("SMS sent successfully");
        } catch (Exception e) {
            log.warn("Failed to send SMS: {}", e.getMessage());
        }
    }

    @Override
    public void sendPushNotification(String recipientType, Long recipientId, String title, String body) {
        log.info("Sending push notification to {} {}: {}", recipientType, recipientId, title);

        String baseUrl = getBaseUrl(recipientType);
        String url = baseUrl + "/" + getRecipientPath(recipientType) + "/" + recipientId + "/push";

        Map<String, String> request = Map.of(
                "title", title,
                "body", body
        );

        try {
            restTemplate.postForObject(url, request, Void.class);
            log.info("Push notification sent successfully");
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }

    private String getBaseUrl(String recipientType) {
        return "VENDOR".equalsIgnoreCase(recipientType) ? vendorServiceUrl : customerServiceUrl;
    }

    private String getRecipientPath(String recipientType) {
        return "VENDOR".equalsIgnoreCase(recipientType) ? "vendors" : "customers";
    }
}
