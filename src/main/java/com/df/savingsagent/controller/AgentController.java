package com.df.savingsagent.controller;

import com.df.savingsagent.agent.BankingAgentService;
import com.df.savingsagent.dto.AgentChatRequest;
import com.df.savingsagent.dto.AgentChatResponse;
import com.df.savingsagent.dto.ToolTraceDto;
import com.df.savingsagent.security.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {
    private final AuthService authService;
    private final BankingAgentService bankingAgentService;

    public AgentController(AuthService authService, BankingAgentService bankingAgentService) {
        this.authService = authService;
        this.bankingAgentService = bankingAgentService;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestHeader("Authorization") String authorization,
                                  @Valid @RequestBody AgentChatRequest request) {
        return bankingAgentService.chat(authService.authenticate(authorization), request.conversationId(), request.message());
    }

    @PostMapping("/conversations/{conversationId}/confirm")
    public AgentChatResponse confirm(@RequestHeader("Authorization") String authorization,
                                     @PathVariable String conversationId) {
        return bankingAgentService.confirm(authService.authenticate(authorization), conversationId);
    }

    @PostMapping("/conversations/{conversationId}/reject")
    public AgentChatResponse reject(@RequestHeader("Authorization") String authorization,
                                    @PathVariable String conversationId) {
        authService.authenticate(authorization);
        return bankingAgentService.reject(conversationId);
    }

    @GetMapping("/conversations/{conversationId}/trace")
    public List<ToolTraceDto> trace(@PathVariable String conversationId) {
        return bankingAgentService.lastTrace(conversationId);
    }
}
