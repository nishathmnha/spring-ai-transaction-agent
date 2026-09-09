package com.df.savingsagent.service;

import com.df.savingsagent.domain.AccountStatus;
import com.df.savingsagent.domain.Customer;
import com.df.savingsagent.domain.SavingsAccount;
import com.df.savingsagent.dto.AccountBalanceDto;
import com.df.savingsagent.dto.BalanceInquiryResponse;
import com.df.savingsagent.exception.BankingException;
import com.df.savingsagent.repository.CustomerRepository;
import com.df.savingsagent.repository.SavingsAccountRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final CustomerRepository customerRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public AccountService(CustomerRepository customerRepository, SavingsAccountRepository savingsAccountRepository) {
        this.customerRepository = customerRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    public BalanceInquiryResponse sourceAccounts(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BankingException("CUSTOMER_NOT_FOUND", "The authenticated customer was not found.", HttpStatus.NOT_FOUND));
        if (customer.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("CUSTOMER_UNAVAILABLE", "The authenticated customer is not active.", HttpStatus.CONFLICT);
        }
        List<AccountBalanceDto> accounts = savingsAccountRepository.findByCustomerCustomerId(customerId).stream()
                .map(account -> new AccountBalanceDto(
                        account.getAccountNumber(),
                        account.getAccountName(),
                        account.getAccountAlias(),
                        account.getCurrency(),
                        account.getAvailableBalance(),
                        account.getStatus().name()))
                .toList();
        return new BalanceInquiryResponse(customer.getCustomerId(), accounts);
    }

    public SavingsAccount requireOwnedAccount(String customerId, String sourceAccount, String alias) {
        List<SavingsAccount> accounts = savingsAccountRepository.findByCustomerCustomerId(customerId);
        String wanted = firstPresent(sourceAccount, alias);
        if (wanted == null || wanted.isBlank()) {
            throw new BankingException("SOURCE_ACCOUNT_REQUIRED", "Please specify the source account.", HttpStatus.BAD_REQUEST);
        }
        return accounts.stream()
                .filter(account -> matches(account, wanted))
                .findFirst()
                .orElseThrow(() -> new BankingException("SOURCE_ACCOUNT_NOT_FOUND", "I could not find that source account for this customer.", HttpStatus.NOT_FOUND));
    }

    private boolean matches(SavingsAccount account, String value) {
        return account.getAccountNumber().equalsIgnoreCase(value)
                || account.getAccountAlias().equalsIgnoreCase(value)
                || account.getAccountName().equalsIgnoreCase(value);
    }

    private String firstPresent(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
