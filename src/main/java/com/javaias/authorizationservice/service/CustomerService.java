package com.javaias.authorizationservice.service;

import com.javaias.authorizationservice.domain.Customer;
import com.javaias.authorizationservice.dto.CustomerLimitResponse;
import com.javaias.authorizationservice.exception.CustomerInactiveException;
import com.javaias.authorizationservice.exception.CustomerNotFoundException;
import com.javaias.authorizationservice.exception.LimitBelowConsumedException;
import com.javaias.authorizationservice.exception.UnauthorizedAdminAccessException;
import com.javaias.authorizationservice.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Mono<CustomerLimitResponse> getLimit(String customerId) {
        return customerRepository.findById(customerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId)))
                .flatMap(customer -> {
                    if (Boolean.FALSE.equals(customer.getState())) {
                        return Mono.error(new CustomerInactiveException(customerId));
                    }
                    return Mono.just(toResponse(customer));
                });
    }

    public Mono<CustomerLimitResponse> updateLimit(String adminCustomerId, String targetCustomerId, Long newDailyLimit) {
        return customerRepository.findById(adminCustomerId)
                .switchIfEmpty(Mono.error(new UnauthorizedAdminAccessException()))
                .flatMap(admin -> {
                    if (Boolean.FALSE.equals(admin.getRoleAdmin())) {
                        return Mono.error(new UnauthorizedAdminAccessException());
                    }
                    return applyLimitUpdate(targetCustomerId, newDailyLimit);
                });
    }

    private Mono<CustomerLimitResponse> applyLimitUpdate(String targetCustomerId, Long newDailyLimit) {
        return customerRepository.findById(targetCustomerId)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(targetCustomerId)))
                .flatMap(target -> {
                    if (newDailyLimit < target.getConsumedAmount()) {
                        return Mono.error(new LimitBelowConsumedException(targetCustomerId));
                    }
                    return customerRepository.updateDailyLimit(targetCustomerId, newDailyLimit)
                            .then(customerRepository.findById(targetCustomerId))
                            .map(this::toResponse);
                });
    }

    private CustomerLimitResponse toResponse(Customer customer) {
        return new CustomerLimitResponse(
                customer.getCustomerId(),
                customer.getDailyLimit(),
                customer.getConsumedAmount(),
                customer.getAvailableAmount()
        );
    }
}
