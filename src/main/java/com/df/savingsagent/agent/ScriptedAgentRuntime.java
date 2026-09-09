package com.df.savingsagent.agent;

import com.df.savingsagent.dto.TransferValidationResult;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "banking.agent.use-scripted-runtime", havingValue = "true")
public class ScriptedAgentRuntime implements AgentRuntime {
    private static final Pattern AMOUNT = Pattern.compile("(?i)(?:LKR\\s*)?([0-9][0-9,]*(?:\\.\\d{1,2})?)");
    private final TransactionTools tools;

    public ScriptedAgentRuntime(TransactionTools tools) {
        this.tools = tools;
    }

    @Override
    public String run(String conversationId, String userMessage, String context) {
        String normalized = userMessage.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("server-confirmation-approved")) {
            var pending = tools.currentPendingTransfer();
            var result = tools.executeFundTransfer(pending.uuid(), pending.sourceAccount(), pending.destinationAccount(),
                    pending.beneficiaryName(), pending.amount(), pending.currency(), "6995", null, "");
            return "Your transfer of LKR %s to %s was completed successfully.".formatted(result.amount(), result.beneficiaryName());
        }
        if (normalized.contains("available balance mean")) {
            return "Available balance is the money currently available for withdrawal or transfer after holds and restrictions.";
        }
        if (normalized.startsWith("change it")) {
            var pending = tools.currentPendingTransfer();
            BigDecimal amount = extractAmount(userMessage);
            if (amount == null) {
                return "Please provide the new transfer amount.";
            }
            TransferValidationResult validation = tools.validateTransferAmount(pending.sourceAccount(), pending.beneficiaryName(),
                    pending.destinationAccount(), amount, pending.currency(), "");
            if (!validation.valid()) {
                return validation.violations().getFirst().message();
            }
            return "The updated transfer passed validation. Please confirm whether you want to continue.";
        }
        if (normalized.contains("balance")) {
            tools.sourceAccountBalanceInquiry();
            return "Your salary account has an available balance of LKR 1,000.00.";
        }
        if (!normalized.contains("transfer")) {
            return "I can help with balances, saved beneficiaries, and saved-beneficiary P2P transfers.";
        }

        BigDecimal amount = extractAmount(userMessage);
        String sourceAlias = sourceAlias(normalized);
        String beneficiary = beneficiaryName(userMessage);
        boolean contextReference = normalized.contains("that account");
        if (amount == null || (sourceAlias == null && !contextReference)) {
            return "Please provide the transfer amount and source account.";
        }

        tools.sourceAccountBalanceInquiry();
        tools.savedBeneficiaryInquiry();
        TransferValidationResult validation = tools.validateTransferAmount(sourceAlias, beneficiary, null, amount, "LKR", "");
        if (!validation.valid()) {
            return validation.violations().getFirst().message();
        }
        return "The transfer passed validation. Please confirm whether you want to continue.";
    }

    private BigDecimal extractAmount(String message) {
        Matcher matcher = AMOUNT.matcher(message);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(",", "")) : null;
    }

    private String sourceAlias(String normalized) {
        if (normalized.contains("unknown account")) {
            return "unknown account";
        }
        if (normalized.contains("dormant account")) {
            return "dormant account";
        }
        if (normalized.contains("frozen account")) {
            return "frozen account";
        }
        if (normalized.contains("closed account")) {
            return "closed account";
        }
        if (normalized.contains("restricted account")) {
            return "restricted account";
        }
        if (normalized.contains("that account")) {
            return null;
        }
        if (normalized.contains("salary account")) {
            return "salary account";
        }
        return normalized.contains("main account") ? "main account" : null;
    }

    private String beneficiaryName(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("unknown person")) {
            return "Unknown Person";
        }
        if (lower.contains("unsaved person")) {
            return "Unsaved Person";
        }
        if (lower.contains("dormant beneficiary")) {
            return "Dormant Beneficiary";
        }
        if (lower.contains("frozen beneficiary")) {
            return "Frozen Beneficiary";
        }
        if (lower.contains("closed beneficiary")) {
            return "Closed Beneficiary";
        }
        if (lower.contains("restricted beneficiary")) {
            return "Restricted Beneficiary";
        }
        return "Varuni";
    }
}
