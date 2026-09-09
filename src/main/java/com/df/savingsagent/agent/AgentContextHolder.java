package com.df.savingsagent.agent;

public final class AgentContextHolder {
    private static final ThreadLocal<String> CONVERSATION_ID = new ThreadLocal<>();

    private AgentContextHolder() {
    }

    public static void setConversationId(String conversationId) {
        CONVERSATION_ID.set(conversationId);
    }

    public static String conversationId() {
        String conversationId = CONVERSATION_ID.get();
        if (conversationId == null) {
            throw new IllegalStateException("No conversation context is available");
        }
        return conversationId;
    }

    public static void clear() {
        CONVERSATION_ID.remove();
    }
}
