package com.soukconect.bpm.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DTO matching the OrderResponse from order-service.
 * Used for communication between BPM workflows and the Order API.
 */
public record OrderDto(
        Long id,
        Long customerId,
        Long addressId,
        BigDecimal totalAmount,
        String status,
        String paymentMethod,
        LocalDate requestedDeliveryDate,
        String deliveryFlexibility,
        LocalTime deliverySlotStart,
        LocalTime deliverySlotEnd,
        String notes,
        List<OrderItemDto> items
) {
    /**
     * Order status enum matching Order.OrderStatus from common-domain.
     */
    public enum Status {
        PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    /**
     * Payment method enum matching Order.PaymentMethod from common-domain.
     */
    public enum PaymentMethod {
        CASH, CARD, WALLET, BANK_TRANSFER, PAYNOW, OTHERS
    }
}
