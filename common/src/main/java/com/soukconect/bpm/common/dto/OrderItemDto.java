package com.soukconect.bpm.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO matching OrderItemResponse from order-service.
 */
public record OrderItemDto(
        Long id,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        LocalDate requestedDeliveryDate,
        String deliveryFlexibility,
        LocalTime deliverySlotStart,
        LocalTime deliverySlotEnd
) {}
