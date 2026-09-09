package com.df.savingsagent.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransferValidationResult(
        boolean valid,
        BigDecimal amount,
        String currency,
        String sourceAccount,
        String sourceAccountAlias,
        String sourceAccountName,
        String destinationAccount,
        String beneficiaryName,
        BigDecimal availableBalance,
        BigDecimal remainingBalance,
        BigDecimal limit,
        String uuid,
        List<ViolationDto> violations
) {
}
