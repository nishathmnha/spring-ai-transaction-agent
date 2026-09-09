package com.df.savingsagent.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "banking")
public record BankingProperties(
        String demoToken,
        String demoCustomerId,
        BigDecimal transferLimit,
        long pendingTransferTtlSeconds,
        boolean localDebugFullAccountNumbers,
        Agent agent
) {
    public record Agent(boolean useScriptedRuntime, String modelName) {
    }
}
