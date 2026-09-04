package com.javaias.authorizationservice.dto;

public class CustomerLimitResponse {

    private final String customerId;
    private final Long dailyLimit;
    private final Long consumedAmount;
    private final Long availableAmount;

    public CustomerLimitResponse(String customerId, Long dailyLimit, Long consumedAmount, Long availableAmount) {
        this.customerId = customerId;
        this.dailyLimit = dailyLimit;
        this.consumedAmount = consumedAmount;
        this.availableAmount = availableAmount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Long getDailyLimit() {
        return dailyLimit;
    }

    public Long getConsumedAmount() {
        return consumedAmount;
    }

    public Long getAvailableAmount() {
        return availableAmount;
    }
}
