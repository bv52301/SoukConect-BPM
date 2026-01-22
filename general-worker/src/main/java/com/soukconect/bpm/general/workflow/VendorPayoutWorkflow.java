package com.soukconect.bpm.general.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;

/**
 * Workflow for processing vendor payouts.
 * Calculates commission, processes payment to vendor.
 */
@WorkflowInterface
public interface VendorPayoutWorkflow {

    @WorkflowMethod
    void processPayout(Long vendorId, Long orderId, BigDecimal orderAmount);

    @QueryMethod
    String getStatus();
}
