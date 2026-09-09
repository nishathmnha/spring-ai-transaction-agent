package com.df.savingsagent.agent;

import com.df.savingsagent.dto.BalanceInquiryResponse;
import com.df.savingsagent.dto.BeneficiaryInquiryResponse;
import com.df.savingsagent.dto.ExecuteTransferRequest;
import com.df.savingsagent.dto.FundTransferResult;
import com.df.savingsagent.dto.TransferValidationRequest;
import com.df.savingsagent.dto.TransferValidationResult;
import com.df.savingsagent.security.AuthContextHolder;
import com.df.savingsagent.service.AccountService;
import com.df.savingsagent.service.BeneficiaryService;
import com.df.savingsagent.service.ConversationStateService;
import com.df.savingsagent.service.FundTransferService;
import com.df.savingsagent.service.TransferValidationService;
import java.math.BigDecimal;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class TransactionTools {
    private final AccountService accountService;
    private final BeneficiaryService beneficiaryService;
    private final TransferValidationService transferValidationService;
    private final FundTransferService fundTransferService;
    private final ConversationStateService conversationStateService;
    private final ToolTraceService toolTraceService;

    public TransactionTools(AccountService accountService, BeneficiaryService beneficiaryService,
                            TransferValidationService transferValidationService, FundTransferService fundTransferService,
                            ConversationStateService conversationStateService, ToolTraceService toolTraceService) {
        this.accountService = accountService;
        this.beneficiaryService = beneficiaryService;
        this.transferValidationService = transferValidationService;
        this.fundTransferService = fundTransferService;
        this.conversationStateService = conversationStateService;
        this.toolTraceService = toolTraceService;
    }

    @Tool(description = "Retrieve the authenticated user's savings source accounts including aliases, account numbers, statuses, currencies, and available balances. Read-only.")
    public BalanceInquiryResponse sourceAccountBalanceInquiry() {
        return toolTraceService.record("sourceAccountBalanceInquiry", "read authenticated user's source accounts", () -> {
            BalanceInquiryResponse response = accountService.sourceAccounts(AuthContextHolder.get().customerId());
            response.accounts().stream().findFirst()
                    .ifPresent(account -> conversationStateService.rememberSourceAccount(AgentContextHolder.conversationId(), account.accountNumber()));
            return response;
        });
    }

    @Tool(description = "Retrieve saved DFP P2P beneficiaries only. Returns beneficiary name, account number, bank, branch, currency, customer status, and destination account status. Read-only.")
    public BeneficiaryInquiryResponse savedBeneficiaryInquiry() {
        return toolTraceService.record("savedBeneficiaryInquiry", "read saved beneficiaries for DFP", () ->
                beneficiaryService.savedBeneficiaries("DFP"));
    }

    @Tool(description = "Validate a proposed saved-beneficiary P2P transfer. Enforces positive amount, active accounts, saved beneficiary, currency, sufficient balance, and LKR 50.00 limit. Creates server pending transfer only when valid. Does not move money.")
    public TransferValidationResult validateTransferAmount(
            @ToolParam(description = "Source account alias or account number, for example salary account") String sourceAccount,
            @ToolParam(description = "Saved beneficiary display name, for example Varuni") String beneficiaryName,
            @ToolParam(description = "Destination beneficiary account number if known") String destinationAccount,
            @ToolParam(description = "Requested amount") BigDecimal amount,
            @ToolParam(description = "Currency, normally LKR") String currency,
            @ToolParam(description = "Optional narration") String narration) {
        return toolTraceService.record("validateTransferAmount", "validate amount=%s currency=%s source=%s beneficiary=%s".formatted(amount, currency, sourceAccount, beneficiaryName), () ->
                transferValidationService.validateAndCreatePending(AuthContextHolder.get().customerId(),
                        new TransferValidationRequest(AgentContextHolder.conversationId(), sourceAccount, sourceAccount,
                                destinationAccount, beneficiaryName, amount, currency, "6995", "Dialog Finance PLC", narration)));
    }

    @Tool(description = "Execute a previously validated and server-confirmed saved-beneficiary transfer. Use only after server confirmation. The tool independently rejects missing, expired, unconfirmed, or mismatched pending transfers and is idempotent by UUID.")
    public FundTransferResult executeFundTransfer(
            @ToolParam(description = "Server-generated pending transfer UUID/idempotency key") String uuid,
            @ToolParam(description = "Confirmed source account number") String sourceAccount,
            @ToolParam(description = "Confirmed destination account number") String destinationAccount,
            @ToolParam(description = "Confirmed beneficiary name") String beneficiaryName,
            @ToolParam(description = "Confirmed amount") BigDecimal amount,
            @ToolParam(description = "Confirmed currency") String currency,
            @ToolParam(description = "Receiver bank code") String receiverBank,
            @ToolParam(description = "Receiver branch code, if any") String receiverBranch,
            @ToolParam(description = "Narration, if any") String narration) {
        return toolTraceService.record("executeFundTransfer", "execute uuid=%s amount=%s source=masked destination=masked".formatted(uuid, amount), () ->
                fundTransferService.executeFromAgent(AuthContextHolder.get().customerId(),
                        new ExecuteTransferRequest(AgentContextHolder.conversationId(), uuid, sourceAccount, destinationAccount,
                                beneficiaryName, amount, currency, receiverBank, receiverBranch, narration)));
    }

    public ConversationStateService.PendingTransfer currentPendingTransfer() {
        return conversationStateService.requirePending(AgentContextHolder.conversationId());
    }
}
