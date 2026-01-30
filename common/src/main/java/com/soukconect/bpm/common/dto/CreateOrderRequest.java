package com.soukconect.bpm.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record CreateOrderRequest(
                Long customerId,
                Long addressId,
                BigDecimal totalAmount,
                String paymentMethod,
                String paymentIntentId,
                String paymentGateway,
                String paymentToken,
                LocalDate requestedDeliveryDate,
                String deliveryFlexibility,
                LocalTime deliverySlotStart,
                LocalTime deliverySlotEnd,
                String notes,
                List<OrderItemRequest> items) {
        public record OrderItemRequest(
                        Long productId,
                        Integer quantity,
                        BigDecimal unitPrice,
                        LocalDate requestedDeliveryDate,
                        String deliveryFlexibility,
                        LocalTime deliverySlotStart,
                        LocalTime deliverySlotEnd) {
        }
}
