package com.javaias.authorizationservice.service;

import com.javaias.authorizationservice.domain.Authorization;
import com.javaias.authorizationservice.domain.Customer;
import com.javaias.authorizationservice.dto.AuthorizationRequest;
import com.javaias.authorizationservice.dto.AuthorizationResponse;
import com.javaias.authorizationservice.exception.AuthorizationNotFoundException;
import com.javaias.authorizationservice.exception.CustomerInactiveException;
import com.javaias.authorizationservice.exception.CustomerNotFoundException;
import com.javaias.authorizationservice.exception.InsufficientFundsException;
import com.javaias.authorizationservice.exception.InvalidAmountException;
import com.javaias.authorizationservice.exception.TransactionConflictException;
import com.javaias.authorizationservice.repository.AuthorizationRepository;
import com.javaias.authorizationservice.repository.CustomerRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthorizationService {

    private final AuthorizationRepository authorizationRepository;
    private final CustomerRepository customerRepository;

    public AuthorizationService(AuthorizationRepository authorizationRepository, CustomerRepository customerRepository) {
        this.authorizationRepository = authorizationRepository;
        this.customerRepository = customerRepository;
    }

    // check if the request has errors
    public Mono<AuthorizationResponse> authorize(AuthorizationRequest request) {
        Mono<AuthorizationResponse> validationError = validate(request);
        if (validationError != null) {
            return validationError;
        }

        return authorizationRepository.findByTransactionId(request.getTransactionId())
                .flatMap(existing -> handleExistingTransaction(existing, request))
                .switchIfEmpty(Mono.defer(() -> processNewAuthorization(request)));
    }

    public Mono<AuthorizationResponse> getAuthorization(String transactionId) {
        return authorizationRepository.findByTransactionId(transactionId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(new AuthorizationNotFoundException(transactionId)));
    }
    // this function validates  the incoming data
    private Mono<AuthorizationResponse> validate(AuthorizationRequest request) {
        if (request.getTransactionId() == null || request.getTransactionId().isBlank()) {
            return Mono.error(new InvalidAmountException("transactionId is required"));
        }
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            return Mono.error(new InvalidAmountException("customerId is required"));
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            return Mono.error(new InvalidAmountException("amount must be greater than zero"));
        }
        return null;
    }

    // this function has a single responsibility, verify the idempotency
    private Mono<AuthorizationResponse> handleExistingTransaction(Authorization existing, AuthorizationRequest request) {
        boolean sameData = existing.getCustomerId().equals(request.getCustomerId())
                && existing.getAmount().equals(request.getAmount());

        if (sameData) {
            return Mono.just(toResponse(existing));
        }
        return Mono.error(new TransactionConflictException(request.getTransactionId()));
    }

    // Saves the record or throws an exception, whether the final response is a success or an error
    private Mono<AuthorizationResponse> processNewAuthorization(AuthorizationRequest request) {
        return customerRepository.tryConsume(request.getCustomerId(), request.getAmount())
                .flatMap(rowsAffected -> rowsAffected > 0
                        ? saveAuthorization(request)
                        : explainRejection(request));
    }

    // save the record
    private Mono<AuthorizationResponse> saveAuthorization(AuthorizationRequest request) {
        Authorization authorization = new Authorization(request.getTransactionId(), request.getCustomerId(), request.getAmount());
        return authorizationRepository.save(authorization)
                .map(this::toResponse)
                .onErrorResume(DuplicateKeyException.class,
                        ex -> authorizationRepository.findByTransactionId(request.getTransactionId())
                                .map(this::toResponse));
    }

    // throw an error
    private Mono<AuthorizationResponse> explainRejection(AuthorizationRequest request) {
        return customerRepository.findById(request.getCustomerId())
                .flatMap(customer -> rejectionFor(customer, request))
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(request.getCustomerId())));
    }

    // verifies whether the user's state is active or not
    private Mono<AuthorizationResponse> rejectionFor(Customer customer, AuthorizationRequest request) {
        if (Boolean.FALSE.equals(customer.getState())) {
            return Mono.error(new CustomerInactiveException(customer.getCustomerId()));
        }
        return Mono.error(new InsufficientFundsException(customer.getCustomerId()));
    }

    private AuthorizationResponse toResponse(Authorization authorization) {
        return new AuthorizationResponse(
                authorization.getTransactionId(),
                authorization.getCustomerId(),
                authorization.getAmount(),
                "APPROVED"
        );
    }
}
