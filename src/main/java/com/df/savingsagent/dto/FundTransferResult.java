package com.df.savingsagent.dto;

import java.math.BigDecimal;

public record FundTransferResult(
        String transactionId,
        String uuid,
        String sourceAccount,
        String destinationAccount,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        BigDecimal sourceBalanceAfter,
        BigDecimal destinationBalanceAfter,
        String status,
        String failureReason
) {
}
