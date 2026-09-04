package com.javaias.authorizationservice.service;

import com.javaias.authorizationservice.domain.Authorization;
import com.javaias.authorizationservice.domain.Customer;
import com.javaias.authorizationservice.dto.AuthorizationRequest;
import com.javaias.authorizationservice.exception.CustomerInactiveException;
import com.javaias.authorizationservice.exception.CustomerNotFoundException;
import com.javaias.authorizationservice.exception.InsufficientFundsException;
import com.javaias.authorizationservice.exception.InvalidAmountException;
import com.javaias.authorizationservice.exception.TransactionConflictException;
import com.javaias.authorizationservice.repository.AuthorizationRepository;
import com.javaias.authorizationservice.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    void authorize_deberiaAprobar_cuandoHaySaldoSuficiente() {
        AuthorizationRequest request = new AuthorizationRequest("TX-001", "CUS-001", 150000L);
        Authorization saved = new Authorization("TX-001", "CUS-001", 150000L);

        when(authorizationRepository.findByTransactionId("TX-001")).thenReturn(Mono.empty());
        when(customerRepository.tryConsume("CUS-001", 150000L)).thenReturn(Mono.just(1));
        when(authorizationRepository.save(any(Authorization.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(authorizationService.authorize(request))
                .assertNext(response -> {
                    assertThat(response.getTransactionId()).isEqualTo("TX-001");
                    assertThat(response.getCustomerId()).isEqualTo("CUS-001");
                    assertThat(response.getAmount()).isEqualTo(150000L);
                    assertThat(response.getStatus()).isEqualTo("APPROVED");
                })
                .verifyComplete();
    }

    @Test
    void authorize_deberiaRechazarPorFondosInsuficientes_cuandoElUpdateNoAfectaFilas() {
        AuthorizationRequest request = new AuthorizationRequest("TX-002", "CUS-002", 999999L);
        Customer customer = new Customer("CUS-002", 100000L, 90000L, 10000L, false, true);

        when(authorizationRepository.findByTransactionId("TX-002")).thenReturn(Mono.empty());
        when(customerRepository.tryConsume("CUS-002", 999999L)).thenReturn(Mono.just(0));
        when(customerRepository.findById("CUS-002")).thenReturn(Mono.just(customer));

        StepVerifier.create(authorizationService.authorize(request))
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void authorize_deberiaRechazarPorClienteInexistente_cuandoNoExisteEnElRepositorio() {
        AuthorizationRequest request = new AuthorizationRequest("TX-003", "CUS-DOES-NOT-EXIST", 1000L);

        when(authorizationRepository.findByTransactionId("TX-003")).thenReturn(Mono.empty());
        when(customerRepository.tryConsume("CUS-DOES-NOT-EXIST", 1000L)).thenReturn(Mono.just(0));
        when(customerRepository.findById("CUS-DOES-NOT-EXIST")).thenReturn(Mono.empty());

        StepVerifier.create(authorizationService.authorize(request))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    @Test
    void authorize_deberiaRechazarPorClienteInactivo_cuandoStateEsFalse() {
        AuthorizationRequest request = new AuthorizationRequest("TX-004", "CUS-INACTIVE", 1000L);
        Customer inactiveCustomer = new Customer("CUS-INACTIVE", 500000L, 0L, 500000L, false, false);

        when(authorizationRepository.findByTransactionId("TX-004")).thenReturn(Mono.empty());
        when(customerRepository.tryConsume("CUS-INACTIVE", 1000L)).thenReturn(Mono.just(0));
        when(customerRepository.findById("CUS-INACTIVE")).thenReturn(Mono.just(inactiveCustomer));

        StepVerifier.create(authorizationService.authorize(request))
                .expectError(CustomerInactiveException.class)
                .verify();
    }

    @Test
    void authorize_deberiaRechazarPorMontoInvalido_sinTocarLosRepositorios() {
        AuthorizationRequest request = new AuthorizationRequest("TX-005", "CUS-001", -100L);

        StepVerifier.create(authorizationService.authorize(request))
                .expectError(InvalidAmountException.class)
                .verify();

        verify(authorizationRepository, never()).findByTransactionId(anyString());
        verify(customerRepository, never()).tryConsume(anyString(), anyLong());
    }

    @Test
    void authorize_deberiaSerIdempotente_cuandoSeRepiteLaMismaTransaccion() {
        AuthorizationRequest request = new AuthorizationRequest("TX-006", "CUS-001", 150000L);
        Authorization existing = new Authorization("TX-006", "CUS-001", 150000L);

        when(authorizationRepository.findByTransactionId("TX-006")).thenReturn(Mono.just(existing));

        StepVerifier.create(authorizationService.authorize(request))
                .assertNext(response -> assertThat(response.getStatus()).isEqualTo("APPROVED"))
                .verifyComplete();

        verify(customerRepository, never()).tryConsume(anyString(), anyLong());
    }

    @Test
    void authorize_deberiaRechazarPorConflicto_cuandoElMismoIdTraeDatosDistintos() {
        AuthorizationRequest request = new AuthorizationRequest("TX-007", "CUS-001", 999999L);
        Authorization existing = new Authorization("TX-007", "CUS-001", 150000L);

        when(authorizationRepository.findByTransactionId("TX-007")).thenReturn(Mono.just(existing));

        StepVerifier.create(authorizationService.authorize(request))
                .expectError(TransactionConflictException.class)
                .verify();
    }
}
