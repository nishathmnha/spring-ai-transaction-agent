package com.df.savingsagent.agent;

public interface AgentRuntime {
    String run(String conversationId, String userMessage, String context);
}
