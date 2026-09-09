package com.df.savingsagent.agent;

import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {
    private final ChatMemory chatMemory;

    public ConversationMemoryService(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    public List<String> messages(String conversationId) {
        return chatMemory.get(conversationId).stream()
                .map(message -> message.getMessageType().name().toLowerCase() + ": " + message.getText())
                .toList();
    }

    public void append(String conversationId, String role, String content) {
        if ("assistant".equalsIgnoreCase(role)) {
            chatMemory.add(conversationId, new AssistantMessage(content));
        } else {
            chatMemory.add(conversationId, new UserMessage(content));
        }
    }
}
