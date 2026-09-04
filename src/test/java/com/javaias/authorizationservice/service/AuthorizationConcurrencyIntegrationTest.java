package com.javaias.authorizationservice.service;

import com.javaias.authorizationservice.domain.Customer;
import com.javaias.authorizationservice.dto.AuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthorizationConcurrencyIntegrationTest {

    private static final String TEST_CUSTOMER_ID = "CUS-CONCURRENCY-TEST";
    private static final long STARTING_AVAILABLE_AMOUNT = 100000L;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private DatabaseClient databaseClient;

    @BeforeEach
    void resetCustomerBalance() {
        databaseClient.sql("UPDATE customers SET consumed_amount = 0, available_amount = " + STARTING_AVAILABLE_AMOUNT
                        + " WHERE customer_id = :id")
                .bind("id", TEST_CUSTOMER_ID)
                .fetch()
                .rowsUpdated()
                .block();
    }

    @Test
    void authorize_noDebeSuperarElSaldoDisponible_cuandoLleganDosSolicitudesConcurrentes() {
        String suffix = String.valueOf(System.currentTimeMillis());
        AuthorizationRequest requestA = new AuthorizationRequest("TX-RACE-A-" + suffix, TEST_CUSTOMER_ID, 70000L);
        AuthorizationRequest requestB = new AuthorizationRequest("TX-RACE-B-" + suffix, TEST_CUSTOMER_ID, 60000L);

        Mono<Boolean> outcomeA = authorizationService.authorize(requestA)
                .map(response -> true)
                .onErrorReturn(false);

        Mono<Boolean> outcomeB = authorizationService.authorize(requestB)
                .map(response -> true)
                .onErrorReturn(false);

        Tuple2<Boolean, Boolean> results = Mono.zip(outcomeA, outcomeB).block();

        boolean approvedA = results.getT1();
        boolean approvedB = results.getT2();

        assertThat(approvedA || approvedB)
                .as("al menos una de las dos solicitudes debe aprobarse")
                .isTrue();
        assertThat(approvedA && approvedB)
                .as("no pueden aprobarse ambas: $70.000 + $60.000 > $100.000 disponibles")
                .isFalse();

        Customer finalState = databaseClient.sql("SELECT * FROM customers WHERE customer_id = :id")
                .bind("id", TEST_CUSTOMER_ID)
                .map((row, metadata) -> new Customer(
                        row.get("customer_id", String.class),
                        row.get("daily_limit", Long.class),
                        row.get("consumed_amount", Long.class),
                        row.get("available_amount", Long.class),
                        row.get("role_admin", Boolean.class),
                        row.get("state", Boolean.class)))
                .one()
                .block();

        assertThat(finalState.getAvailableAmount())
                .as("el saldo disponible nunca debe quedar negativo")
                .isGreaterThanOrEqualTo(0L);
        assertThat(finalState.getConsumedAmount())
                .as("el consumo final debe ser exactamente uno de los dos montos, nunca la suma de ambos")
                .isIn(70000L, 60000L);
    }
}
