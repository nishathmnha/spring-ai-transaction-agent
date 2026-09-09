package com.df.savingsagent.repository;

import com.df.savingsagent.domain.Customer;
import com.df.savingsagent.domain.CustomerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByCustomerTypeAndSavedBeneficiaryTrue(CustomerType customerType);
}
