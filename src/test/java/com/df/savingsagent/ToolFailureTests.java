package com.df.savingsagent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.df.savingsagent.agent.AgentContextHolder;
import com.df.savingsagent.agent.ToolTraceService;
import com.df.savingsagent.agent.TransactionTools;
import com.df.savingsagent.exception.BankingException;
import com.df.savingsagent.security.AuthContext;
import com.df.savingsagent.security.AuthContextHolder;
import com.df.savingsagent.service.AccountService;
import com.df.savingsagent.service.BeneficiaryService;
import com.df.savingsagent.service.ConversationStateService;
import com.df.savingsagent.service.FundTransferService;
import com.df.savingsagent.service.TransferValidationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ToolFailureTests {
    @AfterEach
    void clear() {
        AuthContextHolder.clear();
        AgentContextHolder.clear();
    }

    @Test
    void sourceToolFailureIsSurfacedAndTraced() {
        AccountService accountService = mock(AccountService.class);
        BeneficiaryService beneficiaryService = mock(BeneficiaryService.class);
        TransferValidationService validationService = mock(TransferValidationService.class);
        FundTransferService fundTransferService = mock(FundTransferService.class);
        ConversationStateService stateService = mock(ConversationStateService.class);
        ToolTraceService traceService = new ToolTraceService(new SimpleMeterRegistry());
        TransactionTools tools = new TransactionTools(accountService, beneficiaryService, validationService,
                fundTransferService, stateService, traceService);
        when(accountService.sourceAccounts("CUST001"))
                .thenThrow(new BankingException("REPOSITORY_FAILURE", "Repository failed.", HttpStatus.INTERNAL_SERVER_ERROR));

        AuthContextHolder.set(new AuthContext("CUST001"));
        AgentContextHolder.setConversationId("CONV-FAIL");
        traceService.startTurn();

        assertThatThrownBy(tools::sourceAccountBalanceInquiry)
                .isInstanceOf(BankingException.class)
                .hasMessageContaining("Repository failed");
    }
}
