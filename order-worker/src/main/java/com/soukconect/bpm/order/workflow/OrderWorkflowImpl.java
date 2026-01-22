package com.soukconect.bpm.order.workflow;

import com.soukconect.bpm.common.activity.OrderActivities;
import com.soukconect.bpm.common.dto.OrderWorkflowInput;
import com.soukconect.bpm.common.dto.OrderWorkflowOutput;
import com.soukconect.bpm.common.dto.PaymentResult;
import com.soukconect.bpm.common.dto.TimelineEvent;
import com.soukconect.bpm.common.workflow.OrderWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of OrderWorkflow with full state machine from
 * workflow-specifications.md
 *
 * State Machine:
 * CREATED → VALIDATING → PAYMENT_PROCESSING → INVENTORY_RESERVED →
 * AWAITING_VENDOR_CONFIRMATION → VENDOR_PREPARING → READY_FOR_PICKUP →
 * DELIVERY_ASSIGNED → OUT_FOR_DELIVERY → DELIVERED → COMPLETED
 */
public class OrderWorkflowImpl implements OrderWorkflow {

    private static final Logger log = Workflow.getLogger(OrderWorkflowImpl.class);

    // Timeouts from specs
    private static final Duration VENDOR_CONFIRMATION_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration DELIVERY_COMPLETION_TIMEOUT = Duration.ofHours(4);

    // Activity stubs with different timeout/retry configurations per spec

    // Standard activities: 30s timeout, 3 retries (validateOrder, reserveInventory,
    // captureDeliveryProof)
    private final OrderActivities standardActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    // Payment & delivery assignment: 60s timeout, 3 retries
    private final OrderActivities paymentActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    // Tracking activities: 5 minutes timeout, 3 retries
    private final OrderActivities trackingActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    // Notification activities: 30s timeout, 5 retries
    private final OrderActivities notificationActivities = Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(5))
                            .setMaximumAttempts(5)
                            .build())
                    .build());

    // ============== WORKFLOW STATE ==============
    private OrderWorkflowInput input;
    private String currentStatus = "CREATED";
    private List<TimelineEvent> timeline = new ArrayList<>();
    private LocalDateTime estimatedDeliveryTime;
    private String paymentTransactionId;
    private Long deliveryPartnerId;
    private String deliveryProofUrl;

    // Signal state
    private boolean cancelRequested = false;
    private String cancellationReason;
    private boolean refundRequested = false;
    private boolean vendorConfirmed = false;
    private boolean vendorRejected = false;
    private Integer vendorPrepTime;
    private boolean vendorReady = false;
    private boolean deliveryPickedUp = false;
    private boolean deliveryCompleted = false;
    private String deliverySignature;

    // ============== MAIN WORKFLOW ==============
    @Override
    public OrderWorkflowOutput processOrder(OrderWorkflowInput input) {
        this.input = input;
        log.info("Starting OrderWorkflow for orderId: {}", input.orderId());

        addTimelineEvent("WORKFLOW_STARTED", "COMPLETED");

        Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
        List<String> issues = new ArrayList<>();

        try {
            // ===== STEP 1: VALIDATE =====
            updateStatus("VALIDATING");
            standardActivities.validateOrder(input);
            addTimelineEvent("ORDER_VALIDATED", "COMPLETED");

            checkCancellation();

            // ===== STEP 2: PROCESS PAYMENT =====
            updateStatus("PAYMENT_PROCESSING");
            PaymentResult paymentResult = paymentActivities.processPayment(input);

            if (!paymentResult.success()) {
                updateStatus("PAYMENT_FAILED");
                addTimelineEvent("PAYMENT_FAILED", "FAILED");
                throw new RuntimeException("Payment failed: " + paymentResult.errorMessage());
            }

            paymentTransactionId = paymentResult.transactionId();
            saga.addCompensation(() -> paymentActivities.refundPayment(input.orderId(), paymentTransactionId));
            addTimelineEvent("PAYMENT_PROCESSED", "COMPLETED");

            checkCancellation();

            // ===== STEP 3: RESERVE INVENTORY =====
            updateStatus("INVENTORY_RESERVED");
            standardActivities.reserveInventory(input);
            saga.addCompensation(() -> standardActivities.releaseInventory(input));
            addTimelineEvent("INVENTORY_RESERVED", "COMPLETED");

            checkCancellation();

            // ===== STEP 4: NOTIFY VENDORS =====
            updateStatus("AWAITING_VENDOR_CONFIRMATION");
            notificationActivities.notifyVendors(input);
            for (Long vendorId : input.vendorIds()) {
                saga.addCompensation(
                        () -> notificationActivities.notifyVendorCancellation(input.orderId(), vendorId,
                                "Order cancelled"));
            }
            addTimelineEvent("VENDORS_NOTIFIED", "COMPLETED");

            // Wait for vendor confirmation with timeout
            boolean confirmed = Workflow.await(VENDOR_CONFIRMATION_TIMEOUT,
                    () -> vendorConfirmed || vendorRejected || cancelRequested);

            if (cancelRequested) {
                throw new RuntimeException("Order cancelled: " + cancellationReason);
            }

            if (!confirmed || vendorRejected) {
                updateStatus("CANCELLED");
                addTimelineEvent("VENDOR_TIMEOUT_OR_REJECTED", "FAILED");
                throw new RuntimeException("Vendor did not confirm within timeout or rejected the order");
            }

            // ===== STEP 5: VENDOR PREPARING =====
            updateStatus("VENDOR_PREPARING");
            addTimelineEvent("VENDOR_CONFIRMED", "COMPLETED");

            // Calculate ETA based on vendor prep time
            if (vendorPrepTime != null) {
                estimatedDeliveryTime = Workflow.currentTimeMillis() > 0
                        ? LocalDateTime.now().plusMinutes(vendorPrepTime + 30) // prep + delivery estimate
                        : null;
            }

            // Wait for vendor ready signal
            Workflow.await(() -> vendorReady || cancelRequested);

            if (cancelRequested) {
                throw new RuntimeException("Order cancelled: " + cancellationReason);
            }

            // ===== STEP 6: ASSIGN DELIVERY =====
            updateStatus("READY_FOR_PICKUP");
            addTimelineEvent("ORDER_READY", "COMPLETED");

            deliveryPartnerId = paymentActivities.assignDeliveryPartner(input);
            saga.addCompensation(() -> paymentActivities.cancelDeliveryAssignment(input.orderId(), deliveryPartnerId));

            updateStatus("DELIVERY_ASSIGNED");
            addTimelineEvent("DELIVERY_ASSIGNED", "COMPLETED");

            notificationActivities.sendDeliveryNotification(input.orderId(), input.customerId(),
                    "DELIVERY_ASSIGNED", "A delivery partner has been assigned to your order");

            // Wait for pickup
            Workflow.await(() -> deliveryPickedUp || cancelRequested);

            if (cancelRequested) {
                throw new RuntimeException("Order cancelled: " + cancellationReason);
            }

            // ===== STEP 7: OUT FOR DELIVERY =====
            updateStatus("OUT_FOR_DELIVERY");
            addTimelineEvent("DELIVERY_PICKED_UP", "COMPLETED");
            standardActivities.updateOrderStatus(input.orderId(), "SHIPPED");

            notificationActivities.sendDeliveryNotification(input.orderId(), input.customerId(),
                    "OUT_FOR_DELIVERY", "Your order is on the way!");

            // Wait for delivery completion with timeout
            boolean delivered = Workflow.await(DELIVERY_COMPLETION_TIMEOUT,
                    () -> deliveryCompleted || cancelRequested);

            if (cancelRequested) {
                throw new RuntimeException("Order cancelled: " + cancellationReason);
            }

            if (!delivered) {
                issues.add("Delivery timeout - manual intervention may be required");
                // Don't fail - continue to check status
            }

            // ===== STEP 8: DELIVERED =====
            updateStatus("DELIVERED");
            addTimelineEvent("DELIVERED", "COMPLETED");

            if (deliveryProofUrl != null) {
                standardActivities.captureDeliveryProof(input.orderId(), deliveryProofUrl, deliverySignature);
            }

            standardActivities.updateOrderStatus(input.orderId(), "DELIVERED");

            notificationActivities.sendDeliveryNotification(input.orderId(), input.customerId(),
                    "DELIVERED", "Your order has been delivered. Thank you!");

            // ===== STEP 9: COMPLETE =====
            updateStatus("COMPLETED");
            addTimelineEvent("WORKFLOW_COMPLETED", "COMPLETED");

            // Trigger review request
            notificationActivities.triggerReviewRequest(input.orderId(), input.customerId());

            log.info("OrderWorkflow completed for orderId: {}", input.orderId());

            return new OrderWorkflowOutput(
                    input.orderId(),
                    "COMPLETED",
                    LocalDateTime.now(),
                    deliveryProofUrl,
                    input.totalAmount(),
                    issues);

        } catch (Exception e) {
            log.error("OrderWorkflow failed for orderId: {}, error: {}", input.orderId(), e.getMessage());

            // Execute saga compensation
            saga.compensate();

            // Update status
            String finalStatus = cancelRequested ? "CANCELLED" : "FAILED";
            updateStatus(finalStatus);
            addTimelineEvent("WORKFLOW_" + finalStatus, "FAILED");

            try {
                standardActivities.updateOrderStatus(input.orderId(), finalStatus);
                notificationActivities.sendDeliveryNotification(input.orderId(), input.customerId(),
                        finalStatus,
                        "Your order has been " + finalStatus.toLowerCase() + ". Reason: " + e.getMessage());
            } catch (Exception notifyError) {
                log.warn("Failed to send cancellation notification: {}", notifyError.getMessage());
            }

            return new OrderWorkflowOutput(
                    input.orderId(),
                    finalStatus,
                    LocalDateTime.now(),
                    null,
                    input.totalAmount(),
                    List.of(e.getMessage()));
        }
    }

    // ============== SIGNALS ==============

    @Override
    public void vendorConfirmed(Long vendorId, boolean confirmed, Integer prepTimeMinutes, String notes) {
        log.info("Vendor {} confirmation: {}, prepTime: {}", vendorId, confirmed, prepTimeMinutes);
        if (confirmed) {
            this.vendorConfirmed = true;
            this.vendorPrepTime = prepTimeMinutes;
        } else {
            this.vendorRejected = true;
        }
    }

    @Override
    public void vendorReady(Long vendorId) {
        log.info("Vendor {} marked order as ready", vendorId);
        this.vendorReady = true;
    }

    @Override
    public void deliveryPickedUp(Long partnerId, LocalDateTime timestamp) {
        log.info("Delivery partner {} picked up order at {}", partnerId, timestamp);
        this.deliveryPickedUp = true;
    }

    @Override
    public void deliveryUpdate(String status, Double lat, Double lng, LocalDateTime eta) {
        log.info("Delivery update: status={}, location=({},{}), eta={}", status, lat, lng, eta);
        this.estimatedDeliveryTime = eta;
    }

    @Override
    public void deliveryCompleted(String proofUrl, String signature) {
        log.info("Delivery completed, proof: {}", proofUrl);
        this.deliveryCompleted = true;
        this.deliveryProofUrl = proofUrl;
        this.deliverySignature = signature;
    }

    @Override
    public void cancelOrder(String reason, boolean refundRequested) {
        log.info("Cancel order signal received: {}, refund: {}", reason, refundRequested);
        this.cancelRequested = true;
        this.cancellationReason = reason;
        this.refundRequested = refundRequested;
    }

    // ============== QUERIES ==============

    @Override
    public String getStatus() {
        return currentStatus;
    }

    @Override
    public List<TimelineEvent> getTimeline() {
        return new ArrayList<>(timeline);
    }

    @Override
    public LocalDateTime getETA() {
        return estimatedDeliveryTime;
    }

    // ============== HELPERS ==============

    private void updateStatus(String status) {
        this.currentStatus = status;
        log.info("Order {} status: {}", input != null ? input.orderId() : "?", status);
    }

    private void addTimelineEvent(String event, String status) {
        timeline.add(new TimelineEvent(event, status, LocalDateTime.now(), null));
    }

    private void checkCancellation() {
        if (cancelRequested) {
            throw new RuntimeException("Order cancelled by customer: " + cancellationReason);
        }
    }
}
