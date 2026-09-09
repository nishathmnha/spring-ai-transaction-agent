package com.df.savingsagent.security;

import com.df.savingsagent.config.BankingProperties;
import com.df.savingsagent.exception.BankingException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final BankingProperties properties;

    public AuthService(BankingProperties properties) {
        this.properties = properties;
    }

    public AuthContext authenticate(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BankingException("UNAUTHORIZED", "A bearer token is required.", HttpStatus.UNAUTHORIZED);
        }
        String token = authorizationHeader.substring("Bearer ".length());
        if (!properties.demoToken().equals(token)) {
            throw new BankingException("UNAUTHORIZED", "The supplied bearer token is not valid for this demo.", HttpStatus.UNAUTHORIZED);
        }
        return new AuthContext(properties.demoCustomerId());
    }
}
