package com.soukconect.bpm.order.activity;

import com.soukconect.bpm.common.activity.OrderActivities;
import com.soukconect.bpm.common.dto.OrderWorkflowInput;
import com.soukconect.bpm.common.dto.PaymentResult;
import com.soukconect.bpm.order.client.OrderServiceClient;
import com.soukconect.bpm.order.client.ProductServiceClient;
import com.soukconect.bpm.order.client.VendorServiceClient;
import com.soukconect.bpm.order.client.CustomerServiceClient;
import com.soukconect.bpm.order.client.PaymentGatewayClient;
import io.temporal.activity.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementation of OrderActivities that calls existing microservices via REST.
 */
@Component
public class OrderActivitiesImpl implements OrderActivities {

    private static final Logger log = LoggerFactory.getLogger(OrderActivitiesImpl.class);

    private final OrderServiceClient orderServiceClient;
    private final ProductServiceClient productServiceClient;
    private final VendorServiceClient vendorServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final PaymentGatewayClient paymentGatewayClient;

    public OrderActivitiesImpl(
            OrderServiceClient orderServiceClient,
            ProductServiceClient productServiceClient,
            VendorServiceClient vendorServiceClient,
            CustomerServiceClient customerServiceClient,
            PaymentGatewayClient paymentGatewayClient) {
        this.orderServiceClient = orderServiceClient;
        this.productServiceClient = productServiceClient;
        this.vendorServiceClient = vendorServiceClient;
        this.customerServiceClient = customerServiceClient;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    // ============== VALIDATION ==============

    @Override
    public void validateOrder(OrderWorkflowInput input) {
        log.info("Validating order: {}", input.orderId());

        // Validate order exists
        var order = orderServiceClient.getOrder(input.orderId());
        if (order == null) {
            throw Activity.wrap(new IllegalArgumentException("Order not found: " + input.orderId()));
        }

        // Validate customer
        if (input.customerId() == null) {
            throw Activity.wrap(new IllegalArgumentException("Customer ID is required"));
        }

        // Validate vendors
        if (input.vendorIds() == null || input.vendorIds().isEmpty()) {
            throw Activity.wrap(new IllegalArgumentException("At least one vendor is required"));
        }

        // Validate delivery address
        if (input.address() == null) {
            throw Activity.wrap(new IllegalArgumentException("Delivery address is required"));
        }

        log.info("Order validated successfully: {}", input.orderId());
    }

    // ============== PAYMENT ==============

    @Override
    public PaymentResult processPayment(OrderWorkflowInput input) {
        log.info("Processing payment for order: {}, amount: {}", input.orderId(), input.totalAmount());

        try {
            String transactionId = paymentGatewayClient.charge(
                    input.customerId(),
                    input.totalAmount(),
                    input.paymentMethod(),
                    "Order #" + input.orderId()
            );

            log.info("Payment successful for order: {}", input.orderId());
            return PaymentResult.success(transactionId);

        } catch (Exception e) {
            log.error("Payment failed for order: {}", input.orderId(), e);
            return PaymentResult.failure("PAYMENT_ERROR", e.getMessage());
        }
    }

    @Override
    public void refundPayment(Long orderId, String transactionId) {
        log.info("Refunding payment for order: {}, transactionId: {}", orderId, transactionId);

        try {
            var order = orderServiceClient.getOrder(orderId);
            if (order != null) {
                paymentGatewayClient.refund(transactionId, order.totalAmount());
            }
            log.info("Refund successful for order: {}", orderId);
        } catch (Exception e) {
            log.error("Refund failed for order: {}", orderId, e);
            throw Activity.wrap(e);
        }
    }

    // ============== INVENTORY ==============

    @Override
    public void reserveInventory(OrderWorkflowInput input) {
        log.info("Reserving inventory for order: {}", input.orderId());

        var order = orderServiceClient.getOrder(input.orderId());
        if (order != null && order.items() != null) {
            for (var item : order.items()) {
                productServiceClient.reserveStock(item.productId(), item.quantity());
            }
        }

        log.info("Inventory reserved for order: {}", input.orderId());
    }

    @Override
    public void releaseInventory(OrderWorkflowInput input) {
        log.info("Releasing inventory for order: {}", input.orderId());

        try {
            var order = orderServiceClient.getOrder(input.orderId());
            if (order != null && order.items() != null) {
                for (var item : order.items()) {
                    try {
                        productServiceClient.releaseStock(item.productId(), item.quantity());
                    } catch (Exception e) {
                        log.warn("Failed to release stock for product {}: {}", item.productId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to release inventory for order {}: {}", input.orderId(), e.getMessage());
        }
    }

    // ============== VENDOR ==============

    @Override
    public void notifyVendors(OrderWorkflowInput input) {
        log.info("Notifying vendors for order: {}", input.orderId());

        var order = orderServiceClient.getOrder(input.orderId());

        for (Long vendorId : input.vendorIds()) {
            try {
                vendorServiceClient.notifyNewOrder(vendorId, order);
                log.info("Vendor {} notified for order: {}", vendorId, input.orderId());
            } catch (Exception e) {
                log.warn("Failed to notify vendor {}: {}", vendorId, e.getMessage());
            }
        }
    }

    @Override
    public void notifyVendorCancellation(Long orderId, Long vendorId, String reason) {
        log.info("Notifying vendor {} of cancellation for order: {}", vendorId, orderId);

        try {
            vendorServiceClient.notifyCancellation(vendorId, orderId, reason);
        } catch (Exception e) {
            log.warn("Failed to notify vendor {} of cancellation: {}", vendorId, e.getMessage());
        }
    }

    // ============== DELIVERY ==============

    @Override
    public Long assignDeliveryPartner(OrderWorkflowInput input) {
        log.info("Assigning delivery partner for order: {}", input.orderId());

        // TODO: Integrate with delivery partner service
        // For now, return a mock partner ID
        Long partnerId = 1000L + (input.orderId() % 100);
        log.info("Delivery partner {} assigned for order: {}", partnerId, input.orderId());
        return partnerId;
    }

    @Override
    public void cancelDeliveryAssignment(Long orderId, Long partnerId) {
        log.info("Cancelling delivery assignment for order: {}, partner: {}", orderId, partnerId);
        // TODO: Integrate with delivery partner service
    }

    @Override
    public String trackDelivery(Long orderId, Long partnerId) {
        log.info("Tracking delivery for order: {}, partner: {}", orderId, partnerId);
        // TODO: Integrate with delivery tracking service
        return "IN_TRANSIT";
    }

    @Override
    public void captureDeliveryProof(Long orderId, String proofUrl, String signature) {
        log.info("Capturing delivery proof for order: {}, proof: {}", orderId, proofUrl);
        // TODO: Store delivery proof in order service
        orderServiceClient.updateStatus(orderId, "DELIVERED");
    }

    // ============== NOTIFICATIONS ==============

    @Override
    public void sendDeliveryNotification(Long orderId, Long customerId, String status, String message) {
        log.info("Sending delivery notification to customer {}: {}", customerId, status);

        try {
            customerServiceClient.sendNotification(customerId, "Order #" + orderId + " - " + status, message);
        } catch (Exception e) {
            log.warn("Failed to send notification to customer {}: {}", customerId, e.getMessage());
        }
    }

    @Override
    public void triggerReviewRequest(Long orderId, Long customerId) {
        log.info("Triggering review request for order: {}, customer: {}", orderId, customerId);

        try {
            customerServiceClient.sendNotification(customerId,
                    "Rate your order",
                    "How was your experience with order #" + orderId + "? Leave a review!");
        } catch (Exception e) {
            log.warn("Failed to trigger review request: {}", e.getMessage());
        }
    }

    // ============== STATUS ==============

    @Override
    public void updateOrderStatus(Long orderId, String status) {
        log.info("Updating order status: {} -> {}", orderId, status);
        orderServiceClient.updateStatus(orderId, status);
    }
}
