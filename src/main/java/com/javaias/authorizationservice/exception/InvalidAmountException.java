package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidAmountException extends BusinessException {
    public InvalidAmountException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", message);
    }
}
