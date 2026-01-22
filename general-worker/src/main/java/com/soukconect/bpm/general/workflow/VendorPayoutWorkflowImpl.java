package com.soukconect.bpm.general.workflow;

import com.soukconect.bpm.general.activity.VendorActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;

public class VendorPayoutWorkflowImpl implements VendorPayoutWorkflow {

    private static final Logger log = Workflow.getLogger(VendorPayoutWorkflowImpl.class);
    private static final BigDecimal PLATFORM_COMMISSION_RATE = new BigDecimal("0.15"); // 15%

    private final VendorActivities activities = Workflow.newActivityStub(
            VendorActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(60))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setMaximumAttempts(3)
                            .build())
                    .build()
    );

    private String status = "PENDING";

    @Override
    public void processPayout(Long vendorId, Long orderId, BigDecimal orderAmount) {
        log.info("Processing payout for vendor: {}, order: {}, amount: {}", vendorId, orderId, orderAmount);

        try {
            // Calculate commission and vendor payout
            status = "CALCULATING";
            BigDecimal commission = orderAmount.multiply(PLATFORM_COMMISSION_RATE);
            BigDecimal vendorPayout = orderAmount.subtract(commission);

            log.info("Order amount: {}, Commission: {}, Vendor payout: {}", orderAmount, commission, vendorPayout);

            // Get vendor bank details
            status = "FETCHING_BANK_DETAILS";
            var bankDetails = activities.getVendorBankDetails(vendorId);

            // Process payout to vendor
            status = "PROCESSING_PAYOUT";
            String transactionId = activities.transferToVendor(vendorId, vendorPayout, bankDetails);

            // Record the payout
            status = "RECORDING";
            activities.recordPayout(vendorId, orderId, vendorPayout, commission, transactionId);

            // Notify vendor
            status = "NOTIFYING";
            activities.notifyVendorPayout(vendorId, vendorPayout, transactionId);

            status = "COMPLETED";
            log.info("Payout completed for vendor: {}, transactionId: {}", vendorId, transactionId);

        } catch (Exception e) {
            status = "FAILED";
            log.error("Payout failed for vendor: {}", vendorId, e);
            throw Workflow.wrap(e);
        }
    }

    @Override
    public String getStatus() {
        return status;
    }
}
