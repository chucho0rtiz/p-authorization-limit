package com.javaias.authorizationservice.api;

import com.javaias.authorizationservice.dto.CustomerLimitResponse;
import com.javaias.authorizationservice.dto.UpdateLimitRequest;
import com.javaias.authorizationservice.service.CustomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{customerId}/limit")
    public Mono<CustomerLimitResponse> getLimit(@PathVariable String customerId) {
        return customerService.getLimit(customerId);
    }

    @PatchMapping("/{customerId}/limit")
    public Mono<CustomerLimitResponse> updateLimit(@PathVariable String customerId,
                                                    @RequestHeader("X-Admin-Customer-Id") String adminCustomerId,
                                                    @RequestBody UpdateLimitRequest request) {
        return customerService.updateLimit(adminCustomerId, customerId, request.getDailyLimit());
    }
}
