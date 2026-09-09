package com.df.savingsagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @Column(name = "customer_id", nullable = false, length = 32)
    private String customerId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 32)
    private CustomerType customerType;

    @Column(name = "mobile_number", length = 32)
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "bank_code", length = 16)
    private String bankCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "saved_beneficiary", nullable = false)
    private boolean savedBeneficiary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Customer() {
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public boolean isSavedBeneficiary() {
        return savedBeneficiary;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
