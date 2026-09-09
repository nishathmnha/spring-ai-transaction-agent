package com.df.savingsagent.service;

import com.df.savingsagent.domain.FundTransfer;
import com.df.savingsagent.domain.FundTransferStatus;
import com.df.savingsagent.domain.SavingsAccount;
import com.df.savingsagent.dto.ExecuteTransferRequest;
import com.df.savingsagent.dto.FundTransferRequest;
import com.df.savingsagent.dto.FundTransferResult;
import com.df.savingsagent.dto.TransferValidationRequest;
import com.df.savingsagent.dto.TransferValidationResult;
import com.df.savingsagent.exception.BankingException;
import com.df.savingsagent.repository.FundTransferRepository;
import com.df.savingsagent.repository.SavingsAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FundTransferService {
    private final FundTransferRepository fundTransferRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final TransferValidationService validationService;
    private final ConversationStateService conversationStateService;

    public FundTransferService(FundTransferRepository fundTransferRepository,
                               SavingsAccountRepository savingsAccountRepository,
                               TransferValidationService validationService,
                               ConversationStateService conversationStateService) {
        this.fundTransferRepository = fundTransferRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.validationService = validationService;
        this.conversationStateService = conversationStateService;
    }

    @Transactional
    public FundTransferResult executeFromAgent(String customerId, ExecuteTransferRequest request) {
        return fundTransferRepository.findByUuid(request.uuid())
                .map(this::toResult)
                .orElseGet(() -> executeNewFromAgent(customerId, request));
    }

    @Transactional
    public FundTransferResult executeFromRest(String customerId, FundTransferRequest request) {
        return fundTransferRepository.findByUuid(request.uuid())
                .map(this::toResult)
                .orElseGet(() -> {
                    SavingsAccount source = savingsAccountRepository.findLockedByAccountNumber("001020020001")
                            .orElseThrow(() -> new BankingException("SOURCE_ACCOUNT_NOT_FOUND", "The demo source account was not found.", HttpStatus.NOT_FOUND));
                    TransferValidationResult validation = validationService.validate(customerId, new TransferValidationRequest(
                            null,
                            source.getAccountNumber(),
                            source.getAccountAlias(),
                            request.accountNumber(),
                            request.receiverName(),
                            request.amount(),
                            "LKR",
                            request.receiverBank(),
                            null,
                            request.narration()), false);
                    if (!validation.valid()) {
                        throw new BankingException("TRANSFER_VALIDATION_FAILED", validation.violations().getFirst().message(), HttpStatus.CONFLICT);
                    }
                    return executeLocked(request.uuid(), source.getAccountNumber(), request.accountNumber(), request.receiverName(),
                            request.amount(), "LKR", request.receiverBank(), request.receiverBranch(), request.narration());
                });
    }

    private FundTransferResult executeNewFromAgent(String customerId, ExecuteTransferRequest request) {
        conversationStateService.requireConfirmedMatching(
                request.conversationId(),
                request.uuid(),
                request.sourceAccount(),
                request.destinationAccount(),
                request.beneficiaryName(),
                request.amount(),
                request.currency());

        TransferValidationResult validation = validationService.validate(customerId, new TransferValidationRequest(
                request.conversationId(),
                request.sourceAccount(),
                null,
                request.destinationAccount(),
                request.beneficiaryName(),
                request.amount(),
                request.currency(),
                request.receiverBank(),
                null,
                request.narration()), false);
        if (!validation.valid()) {
            throw new BankingException("TRANSFER_VALIDATION_FAILED", validation.violations().getFirst().message(), HttpStatus.CONFLICT);
        }
        FundTransferResult result = executeLocked(request.uuid(), request.sourceAccount(), request.destinationAccount(),
                request.beneficiaryName(), request.amount(), request.currency(), request.receiverBank(),
                request.receiverBranch(), request.narration());
        conversationStateService.markCompleted(request.conversationId(), result);
        return result;
    }

    private FundTransferResult executeLocked(String uuid, String sourceAccountNumber, String destinationAccountNumber,
                                             String receiverName, BigDecimal amount, String currency,
                                             String receiverBank, String receiverBranch, String narration) {
        SavingsAccount source = savingsAccountRepository.findLockedByAccountNumber(sourceAccountNumber)
                .orElseThrow(() -> new BankingException("SOURCE_ACCOUNT_NOT_FOUND", "The source account was not found.", HttpStatus.NOT_FOUND));
        SavingsAccount destination = savingsAccountRepository.findLockedByAccountNumber(destinationAccountNumber)
                .orElseThrow(() -> new BankingException("DESTINATION_ACCOUNT_NOT_FOUND", "The destination account was not found.", HttpStatus.NOT_FOUND));
        if (source.getAvailableBalance().compareTo(amount) < 0) {
            throw new BankingException("INSUFFICIENT_BALANCE", "The source account does not have sufficient available balance.", HttpStatus.CONFLICT);
        }

        source.debit(amount);
        destination.credit(amount);
        Instant now = Instant.now();
        FundTransfer transfer = new FundTransfer(uuid, source.getAccountNumber(), destination.getAccountNumber(),
                amount, currency, receiverName, receiverBank, receiverBranch, narration, FundTransferStatus.COMPLETED,
                null, now, now);
        FundTransfer saved = fundTransferRepository.saveAndFlush(transfer);
        return toResult(saved, source.getAvailableBalance(), destination.getAvailableBalance());
    }

    private FundTransferResult toResult(FundTransfer transfer) {
        BigDecimal sourceBalance = savingsAccountRepository.findById(transfer.getSourceAccount())
                .map(SavingsAccount::getAvailableBalance)
                .orElse(null);
        BigDecimal destinationBalance = savingsAccountRepository.findById(transfer.getDestinationAccount())
                .map(SavingsAccount::getAvailableBalance)
                .orElse(null);
        return toResult(transfer, sourceBalance, destinationBalance);
    }

    private FundTransferResult toResult(FundTransfer transfer, BigDecimal sourceBalance, BigDecimal destinationBalance) {
        return new FundTransferResult(
                "TXN-%06d".formatted(transfer.getTransactionId()),
                transfer.getUuid(),
                transfer.getSourceAccount(),
                transfer.getDestinationAccount(),
                transfer.getReceiverName(),
                transfer.getAmount(),
                transfer.getCurrency(),
                sourceBalance,
                destinationBalance,
                transfer.getStatus().name(),
                transfer.getFailureReason());
    }
}
