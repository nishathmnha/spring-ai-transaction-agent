package com.df.savingsagent.service;

import com.df.savingsagent.config.BankingProperties;
import com.df.savingsagent.dto.FundTransferResult;
import com.df.savingsagent.dto.PendingTransferDto;
import com.df.savingsagent.dto.TransferValidationResult;
import com.df.savingsagent.exception.BankingException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ConversationStateService {
    private final BankingProperties properties;
    private final Map<String, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();
    private final Map<String, String> lastSourceAccountByConversation = new ConcurrentHashMap<>();
    private final Map<String, FundTransferResult> completedByConversation = new ConcurrentHashMap<>();

    public ConversationStateService(BankingProperties properties) {
        this.properties = properties;
    }

    public void rememberSourceAccount(String conversationId, String sourceAccount) {
        if (conversationId != null && sourceAccount != null) {
            lastSourceAccountByConversation.put(conversationId, sourceAccount);
        }
    }

    public Optional<String> lastSourceAccount(String conversationId) {
        return Optional.ofNullable(lastSourceAccountByConversation.get(conversationId));
    }

    public PendingTransfer createOrReplace(String conversationId, TransferValidationResult validation) {
        PendingTransfer transfer = new PendingTransfer(
                conversationId,
                validation.sourceAccount(),
                validation.sourceAccountAlias(),
                validation.sourceAccountName(),
                validation.destinationAccount(),
                validation.beneficiaryName(),
                validation.amount(),
                validation.currency(),
                validation.availableBalance(),
                validation.remainingBalance(),
                validation.uuid(),
                validation,
                false,
                false,
                Instant.now());
        pendingTransfers.put(conversationId, transfer);
        rememberSourceAccount(conversationId, validation.sourceAccount());
        return transfer;
    }

    public PendingTransfer confirm(String conversationId) {
        PendingTransfer pending = requirePending(conversationId);
        if (pending.rejected()) {
            throw new BankingException("TRANSFER_REJECTED", "The pending transfer has been rejected.", HttpStatus.CONFLICT);
        }
        if (isExpired(pending)) {
            pendingTransfers.remove(conversationId);
            throw new BankingException("CONFIRMATION_EXPIRED", "The transfer confirmation has expired.", HttpStatus.GONE);
        }
        PendingTransfer confirmed = pending.withConfirmed(true);
        pendingTransfers.put(conversationId, confirmed);
        return confirmed;
    }

    public void reject(String conversationId) {
        pendingTransfers.remove(conversationId);
    }

    public PendingTransfer requireConfirmedMatching(String conversationId, String uuid, String sourceAccount,
                                                    String destinationAccount, String beneficiaryName,
                                                    BigDecimal amount, String currency) {
        PendingTransfer pending = requirePending(conversationId);
        if (isExpired(pending)) {
            pendingTransfers.remove(conversationId);
            throw new BankingException("CONFIRMATION_EXPIRED", "The transfer confirmation has expired.", HttpStatus.GONE);
        }
        if (!pending.confirmed()) {
            throw new BankingException("CONFIRMATION_REQUIRED", "Server-side confirmation is required before execution.", HttpStatus.CONFLICT);
        }
        boolean matches = Objects.equals(pending.uuid(), uuid)
                && Objects.equals(pending.sourceAccount(), sourceAccount)
                && Objects.equals(pending.destinationAccount(), destinationAccount)
                && equalsIgnoreCase(pending.beneficiaryName(), beneficiaryName)
                && pending.amount().compareTo(amount) == 0
                && equalsIgnoreCase(pending.currency(), currency)
                && pending.validationResult().valid();
        if (!matches) {
            throw new BankingException("CONFIRMED_TRANSFER_MISMATCH", "The transfer details no longer match the confirmed transfer.", HttpStatus.CONFLICT);
        }
        return pending;
    }

    public Optional<PendingTransfer> pending(String conversationId) {
        PendingTransfer pending = pendingTransfers.get(conversationId);
        if (pending == null) {
            return Optional.empty();
        }
        if (isExpired(pending)) {
            pendingTransfers.remove(conversationId);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public void markCompleted(String conversationId, FundTransferResult result) {
        pendingTransfers.remove(conversationId);
        completedByConversation.put(conversationId, result);
    }

    public Optional<FundTransferResult> completed(String conversationId) {
        return Optional.ofNullable(completedByConversation.remove(conversationId));
    }

    public PendingTransfer requirePending(String conversationId) {
        return pending(conversationId)
                .orElseThrow(() -> new BankingException("PENDING_TRANSFER_NOT_FOUND", "No pending transfer exists for this conversation.", HttpStatus.NOT_FOUND));
    }

    public PendingTransferDto toDto(PendingTransfer pending) {
        return new PendingTransferDto(
                pending.sourceAccount(),
                pending.sourceAccountAlias(),
                pending.destinationAccount(),
                pending.beneficiaryName(),
                pending.amount(),
                pending.currency(),
                pending.availableBalance(),
                pending.balanceAfterTransfer(),
                pending.uuid());
    }

    private boolean isExpired(PendingTransfer pending) {
        return Duration.between(pending.createdAt(), Instant.now()).getSeconds() > properties.pendingTransferTtlSeconds();
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    public record PendingTransfer(
            String conversationId,
            String sourceAccount,
            String sourceAccountAlias,
            String sourceAccountName,
            String destinationAccount,
            String beneficiaryName,
            BigDecimal amount,
            String currency,
            BigDecimal availableBalance,
            BigDecimal balanceAfterTransfer,
            String uuid,
            TransferValidationResult validationResult,
            boolean confirmed,
            boolean rejected,
            Instant createdAt
    ) {
        public PendingTransfer withConfirmed(boolean confirmed) {
            return new PendingTransfer(conversationId, sourceAccount, sourceAccountAlias, sourceAccountName, destinationAccount,
                    beneficiaryName, amount, currency, availableBalance, balanceAfterTransfer, uuid, validationResult,
                    confirmed, rejected, createdAt);
        }
    }
}
