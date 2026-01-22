package com.soukconect.bpm.common.dto;

/**
 * Result of a payment processing activity.
 */
public record PaymentResult(
        boolean success,
        String transactionId,
        String errorCode,
        String errorMessage
) {
    public static PaymentResult success(String transactionId) {
        return new PaymentResult(true, transactionId, null, null);
    }

    public static PaymentResult failure(String errorCode, String errorMessage) {
        return new PaymentResult(false, null, errorCode, errorMessage);
    }
}
