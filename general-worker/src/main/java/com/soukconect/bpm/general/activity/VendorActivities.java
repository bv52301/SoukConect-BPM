package com.soukconect.bpm.general.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.Map;

@ActivityInterface
public interface VendorActivities {

    @ActivityMethod
    Map<String, String> getVendorBankDetails(Long vendorId);

    @ActivityMethod
    String transferToVendor(Long vendorId, BigDecimal amount, Map<String, String> bankDetails);

    @ActivityMethod
    void recordPayout(Long vendorId, Long orderId, BigDecimal amount, BigDecimal commission, String transactionId);

    @ActivityMethod
    void notifyVendorPayout(Long vendorId, BigDecimal amount, String transactionId);
}
