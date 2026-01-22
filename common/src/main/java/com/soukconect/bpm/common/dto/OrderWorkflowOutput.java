package com.soukconect.bpm.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Output from OrderWorkflow - matches workflow-specifications.md
 */
public record OrderWorkflowOutput(
        Long orderId,
        String finalStatus,
        LocalDateTime completedAt,
        String deliveryProofUrl,
        BigDecimal finalAmount,
        List<String> issues
) {}
