package com.df.savingsagent.agent;

import com.df.savingsagent.dto.AgentChatResponse;
import com.df.savingsagent.dto.FundTransferResult;
import com.df.savingsagent.dto.ToolTraceDto;
import com.df.savingsagent.exception.BankingException;
import com.df.savingsagent.security.AuthContext;
import com.df.savingsagent.security.AuthContextHolder;
import com.df.savingsagent.service.ConversationStateService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BankingAgentService {
    private final AgentRuntime agentRuntime;
    private final ConversationMemoryService memoryService;
    private final ConversationStateService conversationStateService;
    private final ToolTraceService toolTraceService;
    private final MeterRegistry meterRegistry;
    private final Map<String, List<ToolTraceDto>> lastTraceByConversation = new ConcurrentHashMap<>();

    public BankingAgentService(AgentRuntime agentRuntime, ConversationMemoryService memoryService,
                               ConversationStateService conversationStateService, ToolTraceService toolTraceService,
                               MeterRegistry meterRegistry) {
        this.agentRuntime = agentRuntime;
        this.memoryService = memoryService;
        this.conversationStateService = conversationStateService;
        this.toolTraceService = toolTraceService;
        this.meterRegistry = meterRegistry;
    }

    public AgentChatResponse chat(AuthContext authContext, String conversationId, String message) {
        return runTurn(authContext, conversationId, message);
    }

    public AgentChatResponse confirm(AuthContext authContext, String conversationId) {
        var pending = conversationStateService.confirm(conversationId);
        String message = """
                SERVER-CONFIRMATION-APPROVED.
                The user clicked the server confirmation button for this exact pending transfer:
                uuid=%s sourceAccount=%s destinationAccount=%s beneficiaryName=%s amount=%s currency=%s.
                You may now call executeFundTransfer with exactly these values if all rules are still satisfied.
                """.formatted(pending.uuid(), pending.sourceAccount(), pending.destinationAccount(), pending.beneficiaryName(),
                pending.amount(), pending.currency());
        return runTurn(authContext, conversationId, message);
    }

    public AgentChatResponse reject(String conversationId) {
        conversationStateService.reject(conversationId);
        List<ToolTraceDto> trace = List.of();
        lastTraceByConversation.put(conversationId, trace);
        return new AgentChatResponse(conversationId, "REJECTED",
                "The pending transfer was rejected. No balances were changed.", false, null, null, trace);
    }

    public List<ToolTraceDto> lastTrace(String conversationId) {
        return lastTraceByConversation.getOrDefault(conversationId, List.of());
    }

    private AgentChatResponse runTurn(AuthContext authContext, String conversationId, String message) {
        String agentRequestId = UUID.randomUUID().toString();
        Instant start = Instant.now();
        MDC.put("conversationId", conversationId);
        MDC.put("agentRequestId", agentRequestId);
        AuthContextHolder.set(authContext);
        AgentContextHolder.setConversationId(conversationId);
        toolTraceService.startTurn();
        try {
            String context = buildContext(conversationId);
            String modelText = agentRuntime.run(conversationId, message, context);
            memoryService.append(conversationId, message.startsWith("SERVER-CONFIRMATION") ? "server" : "user", message);
            memoryService.append(conversationId, "assistant", modelText);
            List<ToolTraceDto> trace = toolTraceService.endTurn();
            lastTraceByConversation.put(conversationId, trace);
            meterRegistry.timer("banking.agent.turn.duration").record(Duration.between(start, Instant.now()));
            return shapeResponse(conversationId, modelText, trace);
        } catch (BankingException ex) {
            List<ToolTraceDto> trace = toolTraceService.endTurn();
            lastTraceByConversation.put(conversationId, trace);
            return new AgentChatResponse(conversationId, "FAILED", ex.getMessage(), false, null, null, trace);
        } catch (RuntimeException ex) {
            List<ToolTraceDto> trace = toolTraceService.endTurn();
            lastTraceByConversation.put(conversationId, trace);
            return new AgentChatResponse(conversationId, "FAILED",
                    "The assistant could not complete the request safely. Please try again with a new request.",
                    false, null, null, trace);
        } finally {
            AuthContextHolder.clear();
            AgentContextHolder.clear();
            MDC.clear();
        }
    }

    private AgentChatResponse shapeResponse(String conversationId, String modelText, List<ToolTraceDto> trace) {
        FundTransferResult completed = conversationStateService.completed(conversationId).orElse(null);
        if (completed != null) {
            return new AgentChatResponse(conversationId, "COMPLETED", modelText, false, null, completed, trace);
        }
        var pending = conversationStateService.pending(conversationId).orElse(null);
        if (pending != null && !pending.confirmed()) {
            return new AgentChatResponse(conversationId, "AWAITING_CONFIRMATION",
                    "The transfer passed validation. Please confirm whether you want to continue.",
                    true, conversationStateService.toDto(pending), null, trace);
        }
        if (modelText.endsWith("?") || modelText.toLowerCase().startsWith("please provide")) {
            return new AgentChatResponse(conversationId, "NEEDS_CLARIFICATION", modelText, false, null, null, trace);
        }
        if (trace.stream().anyMatch(t -> "validateTransferAmount".equals(t.tool()))
                && trace.stream().noneMatch(t -> "executeFundTransfer".equals(t.tool()))) {
            HttpStatus ignored = HttpStatus.OK;
            return new AgentChatResponse(conversationId, "VALIDATION_FAILED", modelText, false, null, null, trace);
        }
        return new AgentChatResponse(conversationId, "ANSWERED", modelText, false, null, null, trace);
    }

    private String buildContext(String conversationId) {
        String history = String.join("\n", memoryService.messages(conversationId));
        String pending = conversationStateService.pending(conversationId)
                .map(p -> "Pending transfer: uuid=%s source=%s destination=%s beneficiary=%s amount=%s currency=%s confirmed=%s"
                        .formatted(p.uuid(), p.sourceAccount(), p.destinationAccount(), p.beneficiaryName(), p.amount(), p.currency(), p.confirmed()))
                .orElse("Pending transfer: none");
        return history + "\n" + pending;
    }
}
