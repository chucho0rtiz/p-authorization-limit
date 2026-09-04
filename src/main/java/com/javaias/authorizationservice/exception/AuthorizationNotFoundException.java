package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class AuthorizationNotFoundException extends BusinessException {
    public AuthorizationNotFoundException(String transactionId) {
        super(HttpStatus.NOT_FOUND, "AUTHORIZATION_NOT_FOUND", "Authorization not found: " + transactionId);
    }
}
