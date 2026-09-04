package com.javaias.authorizationservice.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedAdminAccessException extends BusinessException {
    public UnauthorizedAdminAccessException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN_ADMIN_OPERATION", "Caller is not authorized to perform this administrative operation");
    }
}
