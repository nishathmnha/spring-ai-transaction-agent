package com.df.savingsagent.service;

import com.df.savingsagent.config.BankingProperties;
import com.df.savingsagent.domain.AccountStatus;
import com.df.savingsagent.domain.SavingsAccount;
import com.df.savingsagent.dto.BeneficiaryDto;
import com.df.savingsagent.dto.TransferValidationRequest;
import com.df.savingsagent.dto.TransferValidationResult;
import com.df.savingsagent.dto.ValidateAmountRequest;
import com.df.savingsagent.dto.ViolationDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransferValidationService {
    private static final String DEFAULT_CURRENCY = "LKR";

    private final BankingProperties properties;
    private final AccountService accountService;
    private final BeneficiaryService beneficiaryService;
    private final ConversationStateService conversationStateService;

    public TransferValidationService(BankingProperties properties, AccountService accountService,
                                     BeneficiaryService beneficiaryService,
                                     ConversationStateService conversationStateService) {
        this.properties = properties;
        this.accountService = accountService;
        this.beneficiaryService = beneficiaryService;
        this.conversationStateService = conversationStateService;
    }

    public TransferValidationResult validateForPostmanContract(String customerId, ValidateAmountRequest request) {
        SavingsAccount source = accountService.requireOwnedAccount(customerId, "001020020001", "salary account");
        BeneficiaryDto beneficiary = beneficiaryService.requireSavedBeneficiary(request.accountNumber(), request.accountNumber());
        return validate(customerId, new TransferValidationRequest(
                null,
                source.getAccountNumber(),
                source.getAccountAlias(),
                request.accountNumber(),
                beneficiary.beneficiaryName(),
                amount(request.amount()),
                DEFAULT_CURRENCY,
                request.receiverBankCode(),
                request.receiverBankName(),
                null), false);
    }

    public TransferValidationResult validateAndCreatePending(String customerId, TransferValidationRequest request) {
        return validate(customerId, request, true);
    }

    public TransferValidationResult validate(String customerId, TransferValidationRequest request, boolean createPending) {
        BigDecimal amount = scale(request.amount());
        String currency = request.currency() == null || request.currency().isBlank() ? DEFAULT_CURRENCY : request.currency().toUpperCase();
        List<ViolationDto> violations = new ArrayList<>();

        SavingsAccount source = null;
        BeneficiaryDto beneficiary = null;
        try {
            String sourceAccount = request.sourceAccount();
            if ((sourceAccount == null || sourceAccount.isBlank()) && request.conversationId() != null) {
                sourceAccount = conversationStateService.lastSourceAccount(request.conversationId()).orElse(null);
            }
            source = accountService.requireOwnedAccount(customerId, sourceAccount, request.sourceAccountAlias());
        } catch (RuntimeException ex) {
            violations.add(new ViolationDto("SOURCE_ACCOUNT_NOT_FOUND", "The requested source account could not be found."));
        }

        try {
            beneficiary = beneficiaryService.requireSavedBeneficiary(request.beneficiaryName(), request.destinationAccount());
        } catch (RuntimeException ex) {
            violations.add(new ViolationDto("BENEFICIARY_NOT_FOUND", "The requested beneficiary is not saved or could not be found."));
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            violations.add(new ViolationDto("INVALID_AMOUNT", "The transfer amount must be greater than zero."));
        }
        if (source != null) {
            validateStatus("SOURCE_ACCOUNT_UNAVAILABLE", "The source account is not active.", source.getStatus(), violations);
            if (!source.getCurrency().equalsIgnoreCase(currency)) {
                violations.add(new ViolationDto("SOURCE_CURRENCY_MISMATCH", "The source account currency does not match the requested transfer currency."));
            }
            if (amount != null && source.getAvailableBalance().compareTo(amount) < 0) {
                violations.add(new ViolationDto("INSUFFICIENT_BALANCE", "The source account does not have sufficient available balance."));
            }
        }
        if (amount != null && amount.compareTo(properties.transferLimit()) > 0) {
            violations.add(new ViolationDto("TRANSACTION_LIMIT_EXCEEDED",
                    "The maximum permitted transfer amount is LKR 50.00. Additional verification would normally be required."));
        }
        if (beneficiary != null) {
            validateStatus("BENEFICIARY_CUSTOMER_UNAVAILABLE", "The beneficiary customer is not active.",
                    AccountStatus.valueOf(beneficiary.customerStatus()), violations);
            validateStatus("DESTINATION_ACCOUNT_UNAVAILABLE", "The destination account is not active.",
                    AccountStatus.valueOf(beneficiary.accountStatus()), violations);
            if (!beneficiary.savedBeneficiary()) {
                violations.add(new ViolationDto("BENEFICIARY_NOT_SAVED", "Only saved beneficiaries may receive P2P transfers."));
            }
            if (!beneficiary.currency().equalsIgnoreCase(currency)) {
                violations.add(new ViolationDto("DESTINATION_CURRENCY_MISMATCH", "The destination account currency does not match the requested transfer currency."));
            }
            if (request.receiverBankCode() != null && !request.receiverBankCode().isBlank()
                    && !beneficiary.bankCode().equalsIgnoreCase(request.receiverBankCode())) {
                violations.add(new ViolationDto("RECEIVER_BANK_MISMATCH", "The receiver bank code does not match the saved beneficiary."));
            }
        }

        boolean valid = violations.isEmpty();
        BigDecimal remaining = source != null && amount != null ? source.getAvailableBalance().subtract(amount) : null;
        TransferValidationResult result = new TransferValidationResult(
                valid,
                amount,
                currency,
                source == null ? request.sourceAccount() : source.getAccountNumber(),
                source == null ? request.sourceAccountAlias() : source.getAccountAlias(),
                source == null ? null : source.getAccountName(),
                beneficiary == null ? request.destinationAccount() : beneficiary.accountNumber(),
                beneficiary == null ? request.beneficiaryName() : beneficiary.beneficiaryName(),
                source == null ? null : source.getAvailableBalance(),
                remaining,
                properties.transferLimit(),
                valid ? UUID.randomUUID().toString() : null,
                List.copyOf(violations));
        if (valid && createPending && request.conversationId() != null) {
            conversationStateService.createOrReplace(request.conversationId(), result);
        }
        return result;
    }

    private void validateStatus(String code, String message, AccountStatus status, List<ViolationDto> violations) {
        if (status != AccountStatus.ACTIVE) {
            violations.add(new ViolationDto(code, message + " Current status is " + status + "."));
        }
    }

    private BigDecimal amount(String raw) {
        try {
            return scale(new BigDecimal(raw.replace(",", "")));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
