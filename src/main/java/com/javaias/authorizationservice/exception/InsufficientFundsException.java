package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {
    public InsufficientFundsException(String customerId) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient available amount for customer: " + customerId);
    }
}
