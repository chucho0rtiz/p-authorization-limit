package com.javaias.authorizationservice.dto;

public class UpdateLimitRequest {

    private final Long dailyLimit;

    public UpdateLimitRequest(Long dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public Long getDailyLimit() {
        return dailyLimit;
    }
}
