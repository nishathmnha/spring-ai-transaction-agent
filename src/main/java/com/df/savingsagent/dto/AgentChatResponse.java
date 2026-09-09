package com.df.savingsagent.dto;

import java.util.List;

public record AgentChatResponse(
        String conversationId,
        String status,
        String message,
        boolean confirmationRequired,
        PendingTransferDto transfer,
        FundTransferResult transaction,
        List<ToolTraceDto> toolTrace
) {
}
