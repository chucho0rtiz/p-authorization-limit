package com.javaias.authorizationservice.dto;

public class AuthorizationResponse {

    private final String transactionId;
    private final String customerId;
    private final Long amount;
    private final String status;

    public AuthorizationResponse(String transactionId, String customerId, Long amount, String status) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}
