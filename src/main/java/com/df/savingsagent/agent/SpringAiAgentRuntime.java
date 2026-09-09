package com.df.savingsagent.agent;

import com.df.savingsagent.config.BankingProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "banking.agent.use-scripted-runtime", havingValue = "false", matchIfMissing = true)
public class SpringAiAgentRuntime implements AgentRuntime {
    private final ChatClient chatClient;

    public SpringAiAgentRuntime(ChatModel chatModel, TransactionTools transactionTools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(AgentSystemPrompt.SYSTEM)
                .defaultTools(transactionTools)
                .build();
    }

    @Override
    public String run(String conversationId, String userMessage, String context) {
        return chatClient.prompt()
                .user("""
                        Conversation ID: %s
                        Server context:
                        %s

                        User message:
                        %s
                        """.formatted(conversationId, context, userMessage))
                .call()
                .content();
    }
}
