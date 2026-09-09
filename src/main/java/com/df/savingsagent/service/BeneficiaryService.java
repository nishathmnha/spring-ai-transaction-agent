package com.df.savingsagent.service;

import com.df.savingsagent.domain.Customer;
import com.df.savingsagent.domain.CustomerType;
import com.df.savingsagent.domain.SavingsAccount;
import com.df.savingsagent.dto.BeneficiaryDto;
import com.df.savingsagent.dto.BeneficiaryInquiryResponse;
import com.df.savingsagent.exception.BankingException;
import com.df.savingsagent.repository.CustomerRepository;
import com.df.savingsagent.repository.SavingsAccountRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BeneficiaryService {
    private final CustomerRepository customerRepository;
    private final SavingsAccountRepository savingsAccountRepository;

    public BeneficiaryService(CustomerRepository customerRepository, SavingsAccountRepository savingsAccountRepository) {
        this.customerRepository = customerRepository;
        this.savingsAccountRepository = savingsAccountRepository;
    }

    public BeneficiaryInquiryResponse savedBeneficiaries(String transferType) {
        List<BeneficiaryDto> beneficiaries = customerRepository
                .findByCustomerTypeAndSavedBeneficiaryTrue(CustomerType.BENEFICIARY)
                .stream()
                .flatMap(customer -> savingsAccountRepository.findByCustomerCustomerId(customer.getCustomerId()).stream()
                        .map(account -> toDto(customer, account)))
                .toList();
        return new BeneficiaryInquiryResponse(transferType, beneficiaries);
    }

    public BeneficiaryDto requireSavedBeneficiary(String nameOrAccount, String destinationAccount) {
        List<BeneficiaryDto> matches = savedBeneficiaries("DFP").beneficiaries().stream()
                .filter(beneficiary -> matches(beneficiary, nameOrAccount, destinationAccount))
                .toList();
        if (matches.isEmpty()) {
            throw new BankingException("BENEFICIARY_NOT_FOUND", "The requested beneficiary is not in the saved beneficiary list.", HttpStatus.NOT_FOUND);
        }
        if (matches.size() > 1) {
            throw new BankingException("AMBIGUOUS_BENEFICIARY", "More than one saved beneficiary matched. Please provide the account number.", HttpStatus.CONFLICT);
        }
        return matches.getFirst();
    }

    private boolean matches(BeneficiaryDto beneficiary, String nameOrAccount, String destinationAccount) {
        if (destinationAccount != null && !destinationAccount.isBlank() && beneficiary.accountNumber().equalsIgnoreCase(destinationAccount)) {
            return true;
        }
        if (nameOrAccount == null || nameOrAccount.isBlank()) {
            return false;
        }
        String requested = nameOrAccount.trim().toLowerCase();
        return beneficiary.beneficiaryName().toLowerCase().contains(requested)
                || beneficiary.accountNumber().equalsIgnoreCase(nameOrAccount);
    }

    private BeneficiaryDto toDto(Customer customer, SavingsAccount account) {
        return new BeneficiaryDto(
                customer.getCustomerId(),
                customer.getFullName(),
                account.getAccountNumber(),
                customer.getBankCode(),
                customer.getBankName(),
                account.getBranchCode(),
                account.getCurrency(),
                customer.getStatus().name(),
                account.getStatus().name(),
                customer.isSavedBeneficiary());
    }
}
