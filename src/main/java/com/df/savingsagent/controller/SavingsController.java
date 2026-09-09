package com.df.savingsagent.controller;

import com.df.savingsagent.dto.BalanceInquiryResponse;
import com.df.savingsagent.dto.BeneficiaryInquiryResponse;
import com.df.savingsagent.dto.FundTransferRequest;
import com.df.savingsagent.dto.FundTransferResult;
import com.df.savingsagent.dto.TransferValidationResult;
import com.df.savingsagent.dto.ValidateAmountRequest;
import com.df.savingsagent.security.AuthService;
import com.df.savingsagent.service.AccountService;
import com.df.savingsagent.service.BeneficiaryService;
import com.df.savingsagent.service.FundTransferService;
import com.df.savingsagent.service.TransferValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {
    private final AuthService authService;
    private final AccountService accountService;
    private final BeneficiaryService beneficiaryService;
    private final TransferValidationService transferValidationService;
    private final FundTransferService fundTransferService;

    public SavingsController(AuthService authService, AccountService accountService, BeneficiaryService beneficiaryService,
                             TransferValidationService transferValidationService, FundTransferService fundTransferService) {
        this.authService = authService;
        this.accountService = accountService;
        this.beneficiaryService = beneficiaryService;
        this.transferValidationService = transferValidationService;
        this.fundTransferService = fundTransferService;
    }

    @GetMapping("/savings/balance")
    public BalanceInquiryResponse balance(@RequestHeader("Authorization") String authorization) {
        return accountService.sourceAccounts(authService.authenticate(authorization).customerId());
    }

    @GetMapping("/beneficiary/get/{transferType}")
    public BeneficiaryInquiryResponse beneficiaries(@RequestHeader("Authorization") String authorization,
                                                   @PathVariable String transferType) {
        authService.authenticate(authorization);
        return beneficiaryService.savedBeneficiaries(transferType);
    }

    @PostMapping("/transfers/validate/amount")
    public TransferValidationResult validateAmount(@RequestHeader("Authorization") String authorization,
                                                   @Valid @RequestBody ValidateAmountRequest request) {
        return transferValidationService.validateForPostmanContract(authService.authenticate(authorization).customerId(), request);
    }

    @PostMapping("/transfer")
    public FundTransferResult transfer(@RequestHeader("Authorization") String authorization,
                                       @Valid @RequestBody FundTransferRequest request) {
        return fundTransferService.executeFromRest(authService.authenticate(authorization).customerId(), request);
    }
}
