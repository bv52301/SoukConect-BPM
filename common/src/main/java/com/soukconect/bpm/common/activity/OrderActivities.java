package com.soukconect.bpm.common.activity;

import com.soukconect.bpm.common.dto.CreateOrderRequest;
import com.soukconect.bpm.common.dto.OrderWorkflowInput;
import com.soukconect.bpm.common.dto.PaymentResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Activities for OrderWorkflow - matches workflow-specifications.md
 */
@ActivityInterface
public interface OrderActivities {

    @ActivityMethod
    Long createOrder(CreateOrderRequest request);

    // ============== VALIDATION ==============

    /**
     * Validate order details, check inventory availability.
     * Timeout: 30s, Retry: 3x
     */
    @ActivityMethod
    void validateOrder(Long orderId);

    // ============== PAYMENT ==============

    /**
     * Process payment - charge the payment method.
     * Timeout: 60s, Retry: 3x
     */
    @ActivityMethod
    PaymentResult processPayment(OrderWorkflowInput input);

    /**
     * Refund payment (compensation).
     */
    @ActivityMethod
    void refundPayment(Long orderId, String transactionId);

    // ============== INVENTORY ==============

    /**
     * Reserve items from vendor inventory.
     * Timeout: 30s, Retry: 3x
     */
    @ActivityMethod
    void reserveInventory(OrderWorkflowInput input);

    /**
     * Release reserved inventory (compensation).
     */
    @ActivityMethod
    void releaseInventory(OrderWorkflowInput input);

    // ============== VENDOR ==============

    /**
     * Send order notification to vendor(s).
     * Timeout: 30s, Retry: 5x
     */
    @ActivityMethod
    void notifyVendors(OrderWorkflowInput input);

    /**
     * Notify vendor of order cancellation (compensation).
     */
    @ActivityMethod
    void notifyVendorCancellation(Long orderId, Long vendorId, String reason);

    // ============== DELIVERY ==============

    /**
     * Find and assign a delivery partner.
     * Timeout: 60s, Retry: 3x
     */
    @ActivityMethod
    Long assignDeliveryPartner(OrderWorkflowInput input);

    /**
     * Cancel delivery assignment (compensation).
     */
    @ActivityMethod
    void cancelDeliveryAssignment(Long orderId, Long partnerId);

    /**
     * Get delivery status updates.
     * Timeout: 5m, Retry: 3x
     */
    @ActivityMethod
    String trackDelivery(Long orderId, Long partnerId);

    /**
     * Store delivery confirmation/proof.
     * Timeout: 30s, Retry: 3x
     */
    @ActivityMethod
    void captureDeliveryProof(Long orderId, String proofUrl, String signature);

    // ============== NOTIFICATIONS ==============

    /**
     * Notify customer of delivery status.
     * Timeout: 30s, Retry: 5x
     */
    @ActivityMethod
    void sendDeliveryNotification(Long orderId, Long customerId, String status, String message);

    /**
     * Trigger review request workflow after delivery.
     * Timeout: 30s, Retry: 5x
     */
    @ActivityMethod
    void triggerReviewRequest(Long orderId, Long customerId);

    // ============== STATUS ==============

    /**
     * Update order status in the database.
     */
    @ActivityMethod
    void updateOrderStatus(Long orderId, String status);
}
