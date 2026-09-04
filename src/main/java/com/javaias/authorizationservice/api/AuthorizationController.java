package com.javaias.authorizationservice.api;

import com.javaias.authorizationservice.dto.AuthorizationRequest;
import com.javaias.authorizationservice.dto.AuthorizationResponse;
import com.javaias.authorizationservice.service.AuthorizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public Mono<AuthorizationResponse> authorize(@RequestBody AuthorizationRequest request) {
        return authorizationService.authorize(request);
    }

    @GetMapping("/{transactionId}")
    public Mono<AuthorizationResponse> getAuthorization(@PathVariable String transactionId) {
        return authorizationService.getAuthorization(transactionId);
    }
}
