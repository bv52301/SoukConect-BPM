package com.soukconect.bpm.order.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Use Apache HttpClient 5 for PATCH support
        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory = new org.springframework.http.client.HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(factory);
    }

    @Bean
    public WorkflowClient workflowClient() {
        WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
        return WorkflowClient.newInstance(serviceStubs);
    }
}
