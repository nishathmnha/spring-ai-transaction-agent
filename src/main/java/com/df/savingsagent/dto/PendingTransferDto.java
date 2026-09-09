package com.df.savingsagent.dto;

import java.math.BigDecimal;

public record PendingTransferDto(
        String sourceAccount,
        String sourceAccountAlias,
        String destinationAccount,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        BigDecimal availableBalance,
        BigDecimal balanceAfterTransfer,
        String uuid
) {
}
