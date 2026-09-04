package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends BusinessException {
    public CustomerNotFoundException(String customerId) {
        super(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found: " + customerId);
    }
}
