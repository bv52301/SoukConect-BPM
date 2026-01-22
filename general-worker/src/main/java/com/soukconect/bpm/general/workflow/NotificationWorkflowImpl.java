package com.soukconect.bpm.general.workflow;

import com.soukconect.bpm.general.activity.NotificationActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class NotificationWorkflowImpl implements NotificationWorkflow {

    private static final Logger log = Workflow.getLogger(NotificationWorkflowImpl.class);

    private final NotificationActivities activities = Workflow.newActivityStub(
            NotificationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumAttempts(3)
                            .build())
                    .build()
    );

    @Override
    public void sendNotification(String recipientType, Long recipientId, String channel, String title, String message) {
        log.info("Sending notification: type={}, id={}, channel={}", recipientType, recipientId, channel);

        switch (channel.toUpperCase()) {
            case "EMAIL":
                activities.sendEmail(recipientType, recipientId, title, message);
                break;
            case "SMS":
                activities.sendSms(recipientType, recipientId, message);
                break;
            case "PUSH":
                activities.sendPushNotification(recipientType, recipientId, title, message);
                break;
            default:
                // Send to all channels
                activities.sendEmail(recipientType, recipientId, title, message);
                activities.sendPushNotification(recipientType, recipientId, title, message);
        }

        log.info("Notification sent successfully");
    }
}
