package com.soukconect.bpm.common.dto;

/**
 * Result of a payment processing activity.
 */
public record PaymentResult(
        boolean success,
        Long paymentId,
        String transactionId,
        String status,
        String errorCode,
        String errorMessage,
        String authUrl,
        boolean retryable) {
    public static PaymentResult success(Long paymentId, String transactionId) {
        return new PaymentResult(true, paymentId, transactionId, "SUCCEEDED", null, null, null, false);
    }

    public static PaymentResult failure(String errorCode, String errorMessage) {
        return new PaymentResult(false, null, null, "FAILED", errorCode, errorMessage, null, isRetryableError(errorCode));
    }

    public static PaymentResult requiresAction(Long paymentId, String transactionId, String authUrl) {
        return new PaymentResult(false, paymentId, transactionId, "REQUIRES_ACTION", null, null, authUrl, false);
    }

    public boolean requiresAction() {
        return "REQUIRES_ACTION".equals(status) && authUrl != null;
    }

    private static boolean isRetryableError(String errorCode) {
        if (errorCode == null)
            return true;
        return switch (errorCode) {
            case "card_declined", "processing_error", "timeout", "GATEWAY_ERROR", "STRIPE_ERROR" -> true;
            case "invalid_card", "expired_card", "stolen_card", "fraud_detected" -> false;
            default -> true;
        };
    }
}
