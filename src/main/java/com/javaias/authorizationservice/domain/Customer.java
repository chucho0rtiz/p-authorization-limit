package com.javaias.authorizationservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("customers")
public class Customer {

    @Id
    private final String customerId;
    private final Long dailyLimit;
    private final Long consumedAmount;
    private final Long availableAmount;
    private final Boolean roleAdmin;
    private final Boolean state;

    public Customer(String customerId, Long dailyLimit, Long consumedAmount, Long availableAmount, Boolean roleAdmin, Boolean state) {
        this.customerId = customerId;
        this.dailyLimit = dailyLimit;
        this.consumedAmount = consumedAmount;
        this.availableAmount = availableAmount;
        this.roleAdmin = roleAdmin;
        this.state = state;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Long getDailyLimit() {
        return dailyLimit;
    }

    public Long getConsumedAmount() {
        return consumedAmount;
    }

    public Long getAvailableAmount() {
        return availableAmount;
    }

    public Boolean getRoleAdmin() {
        return roleAdmin;
    }

    public Boolean getState() {
        return state;
    }
}
