package com.df.savingsagent.dto;

import java.math.BigDecimal;

public record AccountBalanceDto(
        String accountNumber,
        String accountName,
        String accountAlias,
        String currency,
        BigDecimal availableBalance,
        String status
) {
}
