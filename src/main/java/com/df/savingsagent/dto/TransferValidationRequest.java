package com.df.savingsagent.dto;

import java.math.BigDecimal;

public record TransferValidationRequest(
        String conversationId,
        String sourceAccount,
        String sourceAccountAlias,
        String destinationAccount,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        String receiverBankCode,
        String receiverBankName,
        String narration
) {
}
