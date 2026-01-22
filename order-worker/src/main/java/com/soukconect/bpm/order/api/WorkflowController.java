package com.soukconect.bpm.order.api;

import com.soukconect.bpm.common.dto.OrderWorkflowInput;
import com.soukconect.bpm.common.workflow.OrderWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for starting and managing order workflows from the UI.
 * This is the entry point from the frontend - when user clicks "Place Order".
 */
@RestController
@RequestMapping("/api/workflows/orders")
@CrossOrigin(origins = "*")
public class WorkflowController {

    private static final Logger log = LoggerFactory.getLogger(WorkflowController.class);

    private final WorkflowClient workflowClient;

    public WorkflowController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    /**
     * Start an order workflow when user clicks "Place Order" in the UI.
     * 
     * @param request Order details from UI (cart, address, payment)
     * @return Workflow ID and order ID
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startOrderWorkflow(@RequestBody OrderWorkflowInput request) {

        log.info("Starting order workflow for customerId: {}", request.customerId());

        try {
            // Build workflow options
            String workflowId = "order-" + System.currentTimeMillis();
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue(OrderWorkflow.TASK_QUEUE)
                    .setWorkflowId(workflowId)
                    .build();

            // Create workflow stub
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowClient.start(workflow::processOrder, request);

            log.info("Order workflow started: workflowId={}", workflowId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "workflowId", workflowId,
                    "message", "Order workflow started successfully"));

        } catch (Exception e) {
            log.error("Failed to start order workflow", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()));
        }
    }

    /**
     * Send vendor confirmation signal.
     */
    @PostMapping("/{workflowId}/vendor-confirmed")
    public ResponseEntity<Map<String, Object>> vendorConfirmed(
            @PathVariable String workflowId,
            @RequestBody VendorConfirmationRequest request) {

        log.info("Sending vendor confirmation for workflowId: {}, vendorId: {}", workflowId, request.vendorId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            workflow.vendorConfirmed(request.vendorId, request.confirmed, request.prepTimeMinutes, request.notes);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to send vendor confirmation signal", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Send vendor ready signal.
     */
    @PostMapping("/{workflowId}/vendor-ready")
    public ResponseEntity<Map<String, Object>> vendorReady(
            @PathVariable String workflowId,
            @RequestBody Map<String, Long> request) {

        log.info("Sending vendor ready signal for workflowId: {}", workflowId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            workflow.vendorReady(request.get("vendorId"));

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to send vendor ready signal", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Send delivery completed signal.
     */
    @PostMapping("/{workflowId}/delivery-completed")
    public ResponseEntity<Map<String, Object>> deliveryCompleted(
            @PathVariable String workflowId,
            @RequestBody DeliveryCompletedRequest request) {

        log.info("Sending delivery completed signal for workflowId: {}", workflowId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            workflow.deliveryCompleted(request.proofUrl, request.signature);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to send delivery completed signal", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Cancel an order workflow.
     */
    @PostMapping("/{workflowId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String workflowId,
            @RequestBody CancelOrderRequest request) {

        log.info("Sending cancel signal for workflowId: {}", workflowId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            workflow.cancelOrder(request.reason, request.refundRequested);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to send cancel signal", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get workflow status.
     */
    @GetMapping("/{workflowId}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String workflowId) {
        log.info("Querying order status for workflowId: {}", workflowId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            String status = workflow.getStatus();

            return ResponseEntity.ok(Map.of(
                    "workflowId", workflowId,
                    "status", status));
        } catch (Exception e) {
            log.error("Failed to query order status", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get workflow timeline.
     */
    @GetMapping("/{workflowId}/timeline")
    public ResponseEntity<Map<String, Object>> getOrderTimeline(@PathVariable String workflowId) {
        log.info("Querying order timeline for workflowId: {}", workflowId);

        try {
            OrderWorkflow workflow = workflowClient.newWorkflowStub(OrderWorkflow.class, workflowId);
            var timeline = workflow.getTimeline();

            return ResponseEntity.ok(Map.of(
                    "workflowId", workflowId,
                    "timeline", timeline));
        } catch (Exception e) {
            log.error("Failed to query order timeline", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============== DTOs ==============

    public record VendorConfirmationRequest(
            Long vendorId,
            boolean confirmed,
            Integer prepTimeMinutes,
            String notes) {
    }

    public record DeliveryCompletedRequest(
            String proofUrl,
            String signature) {
    }

    public record CancelOrderRequest(
            String reason,
            boolean refundRequested) {
    }
}
