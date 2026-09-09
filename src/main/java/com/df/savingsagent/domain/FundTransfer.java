package com.df.savingsagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fund_transfer")
public class FundTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "uuid", nullable = false, unique = true, length = 64)
    private String uuid;

    @Column(name = "source_account", nullable = false, length = 32)
    private String sourceAccount;

    @Column(name = "destination_account", nullable = false, length = 32)
    private String destinationAccount;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_bank", nullable = false, length = 16)
    private String receiverBank;

    @Column(name = "receiver_branch", length = 16)
    private String receiverBranch;

    @Column(name = "narration")
    private String narration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FundTransferStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected FundTransfer() {
    }

    public FundTransfer(String uuid, String sourceAccount, String destinationAccount, BigDecimal amount,
                        String currency, String receiverName, String receiverBank, String receiverBranch,
                        String narration, FundTransferStatus status, String failureReason, Instant createdAt,
                        Instant completedAt) {
        this.uuid = uuid;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.receiverName = receiverName;
        this.receiverBank = receiverBank;
        this.receiverBranch = receiverBranch;
        this.narration = narration;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSourceAccount() {
        return sourceAccount;
    }

    public String getDestinationAccount() {
        return destinationAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverBank() {
        return receiverBank;
    }

    public String getReceiverBranch() {
        return receiverBranch;
    }

    public String getNarration() {
        return narration;
    }

    public FundTransferStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
