package com.javaias.authorizationservice.repository;

import com.javaias.authorizationservice.domain.Customer;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, String> {

    @Modifying
    @Query("""
            UPDATE customers
            SET consumed_amount = consumed_amount + :amount,
                available_amount = available_amount - :amount
            WHERE customer_id = :customerId
              AND state = true
              AND available_amount >= :amount
            """)
    Mono<Integer> tryConsume(@Param("customerId") String customerId, @Param("amount") Long amount);

    @Modifying
    @Query("""
            UPDATE customers
            SET daily_limit = :dailyLimit,
                available_amount = :dailyLimit - consumed_amount
            WHERE customer_id = :customerId
            """)
    Mono<Integer> updateDailyLimit(@Param("customerId") String customerId, @Param("dailyLimit") Long dailyLimit);
}
