package com.soukconect.bpm.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Input for OrderWorkflow - matches workflow-specifications.md
 */
public record OrderWorkflowInput(
                Long orderId,
                Long customerId,
                List<Long> vendorIds,
                BigDecimal totalAmount,
                String paymentMethod,
                String paymentIntentId,
                String paymentGateway,
                String paymentToken,
                DeliveryAddress address,
                LocalDate requestedDeliveryDate,
                TimeSlot deliverySlot,
                String notes) {
        public record DeliveryAddress(
                        Long addressId,
                        String street,
                        String city,
                        String postal,
                        Double lat,
                        Double lng) {
        }

        public record TimeSlot(
                        LocalTime start,
                        LocalTime end) {
        }
}
