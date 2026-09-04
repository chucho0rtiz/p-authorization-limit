package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class CustomerInactiveException extends BusinessException {
    public CustomerInactiveException(String customerId) {
        super(HttpStatus.CONFLICT, "CUSTOMER_INACTIVE", "Customer is inactive: " + customerId);
    }
}
