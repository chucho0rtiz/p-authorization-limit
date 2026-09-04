package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class TransactionConflictException extends BusinessException {
    public TransactionConflictException(String transactionId) {
        super(HttpStatus.CONFLICT, "TRANSACTION_CONFLICT", "Transaction id already used with different data: " + transactionId);
    }
}
