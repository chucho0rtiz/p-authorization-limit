package com.javaias.authorizationservice.repository;

import com.javaias.authorizationservice.domain.Authorization;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface AuthorizationRepository extends ReactiveCrudRepository<Authorization, Long> {

    Mono<Authorization> findByTransactionId(String transactionId);
}
