package com.df.savingsagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(@NotBlank String conversationId, @NotBlank String message) {
}
