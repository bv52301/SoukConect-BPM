package com.soukconect.bpm.common.dto;

import java.time.LocalDateTime;

/**
 * Event in the order workflow timeline.
 */
public record TimelineEvent(
        String event,
        String status,
        LocalDateTime timestamp,
        String details
) {
    public static TimelineEvent completed(String event, LocalDateTime timestamp) {
        return new TimelineEvent(event, "COMPLETED", timestamp, null);
    }

    public static TimelineEvent inProgress(String event, LocalDateTime timestamp) {
        return new TimelineEvent(event, "IN_PROGRESS", timestamp, null);
    }

    public static TimelineEvent failed(String event, LocalDateTime timestamp, String details) {
        return new TimelineEvent(event, "FAILED", timestamp, details);
    }
}
