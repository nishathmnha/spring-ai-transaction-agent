package com.df.savingsagent.util;

import com.df.savingsagent.config.BankingProperties;
import org.springframework.stereotype.Component;

@Component
public class AccountMasker {
    private final BankingProperties properties;

    public AccountMasker(BankingProperties properties) {
        this.properties = properties;
    }

    public String mask(String accountNumber) {
        if (accountNumber == null || properties.localDebugFullAccountNumbers()) {
            return accountNumber;
        }
        if (accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
