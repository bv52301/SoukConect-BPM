package com.soukconect.bpm.general.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Workflow for sending batch or scheduled notifications.
 */
@WorkflowInterface
public interface NotificationWorkflow {

    @WorkflowMethod
    void sendNotification(String recipientType, Long recipientId, String channel, String title, String message);
}
