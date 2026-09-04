package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class LimitBelowConsumedException extends BusinessException {
    public LimitBelowConsumedException(String customerId) {
        super(HttpStatus.BAD_REQUEST, "LIMIT_BELOW_CONSUMED", "New daily limit is below current consumed amount for customer: " + customerId);
    }
}
