package com.soukconect.bpm.common.workflow;

import com.soukconect.bpm.common.dto.OrderWorkflowInput;
import com.soukconect.bpm.common.dto.OrderWorkflowOutput;
import com.soukconect.bpm.common.dto.TimelineEvent;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OrderWorkflow orchestrates the complete order lifecycle from placement to delivery.
 *
 * State Machine:
 * CREATED → VALIDATING → PAYMENT_PROCESSING → INVENTORY_RESERVED →
 * AWAITING_VENDOR_CONFIRMATION → VENDOR_PREPARING → READY_FOR_PICKUP →
 * DELIVERY_ASSIGNED → OUT_FOR_DELIVERY → DELIVERED → COMPLETED
 *
 * Compensation (Saga) on failure reverses completed steps.
 */
@WorkflowInterface
public interface OrderWorkflow {

    String TASK_QUEUE = "order-queue";

    /**
     * Main workflow method that processes an order through its lifecycle.
     *
     * @param input The order workflow input
     * @return The workflow output with final status
     */
    @WorkflowMethod
    OrderWorkflowOutput processOrder(OrderWorkflowInput input);

    // ============== SIGNALS ==============

    /**
     * Signal: Vendor accepts/rejects the order.
     */
    @SignalMethod
    void vendorConfirmed(Long vendorId, boolean confirmed, Integer prepTimeMinutes, String notes);

    /**
     * Signal: Vendor marks order as ready for pickup.
     */
    @SignalMethod
    void vendorReady(Long vendorId);

    /**
     * Signal: Delivery partner picked up the order.
     */
    @SignalMethod
    void deliveryPickedUp(Long partnerId, LocalDateTime timestamp);

    /**
     * Signal: Real-time delivery status update.
     */
    @SignalMethod
    void deliveryUpdate(String status, Double lat, Double lng, LocalDateTime eta);

    /**
     * Signal: Delivery completed with proof.
     */
    @SignalMethod
    void deliveryCompleted(String proofUrl, String signature);

    /**
     * Signal: Customer requests order cancellation.
     */
    @SignalMethod
    void cancelOrder(String reason, boolean refundRequested);

    // ============== QUERIES ==============

    /**
     * Query: Get current workflow status.
     */
    @QueryMethod
    String getStatus();

    /**
     * Query: Get full event timeline.
     */
    @QueryMethod
    List<TimelineEvent> getTimeline();

    /**
     * Query: Get estimated delivery time.
     */
    @QueryMethod
    LocalDateTime getETA();
}
