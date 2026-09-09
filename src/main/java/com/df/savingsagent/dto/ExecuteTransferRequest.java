package com.df.savingsagent.dto;

import java.math.BigDecimal;

public record ExecuteTransferRequest(
        String conversationId,
        String uuid,
        String sourceAccount,
        String destinationAccount,
        String beneficiaryName,
        BigDecimal amount,
        String currency,
        String receiverBank,
        String receiverBranch,
        String narration
) {
}
