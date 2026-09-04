package com.javaias.authorizationservice.dto;

public class AuthorizationRequest {

    private final String transactionId;
    private final String customerId;
    private final Long amount;

    public AuthorizationRequest(String transactionId, String customerId, Long amount) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
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
}
