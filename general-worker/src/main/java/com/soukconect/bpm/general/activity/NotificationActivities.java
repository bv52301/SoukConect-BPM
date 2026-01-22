package com.soukconect.bpm.general.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotificationActivities {

    @ActivityMethod
    void sendEmail(String recipientType, Long recipientId, String subject, String body);

    @ActivityMethod
    void sendSms(String recipientType, Long recipientId, String message);

    @ActivityMethod
    void sendPushNotification(String recipientType, Long recipientId, String title, String body);
}
