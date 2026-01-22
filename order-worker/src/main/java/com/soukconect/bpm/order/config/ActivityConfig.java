package com.soukconect.bpm.order.config;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for activity timeouts and retries.
 * Loaded from application.yml and can be controlled per environment.
 */
@Configuration
@ConfigurationProperties(prefix = "workflow.activities")
@Data
public class ActivityConfig {

    private ActivitySettings standard = new ActivitySettings(30, 3);
    private ActivitySettings payment = new ActivitySettings(60, 3);
    private ActivitySettings tracking = new ActivitySettings(300, 3);
    private ActivitySettings notification = new ActivitySettings(30, 5);

    @Data
    public static class ActivitySettings {
        private int timeoutSeconds;
        private int maxAttempts;

        public ActivitySettings() {
        }

        public ActivitySettings(int timeoutSeconds, int maxAttempts) {
            this.timeoutSeconds = timeoutSeconds;
            this.maxAttempts = maxAttempts;
        }

        public ActivityOptions toActivityOptions() {
            return ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(timeoutSeconds))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(maxAttempts)
                            .build())
                    .build();
        }
    }

    public ActivityOptions getStandardOptions() {
        return standard.toActivityOptions();
    }

    public ActivityOptions getPaymentOptions() {
        return payment.toActivityOptions();
    }

    public ActivityOptions getTrackingOptions() {
        return tracking.toActivityOptions();
    }

    public ActivityOptions getNotificationOptions() {
        return notification.toActivityOptions();
    }
}
