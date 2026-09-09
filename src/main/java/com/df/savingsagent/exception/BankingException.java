package com.df.savingsagent.exception;

import org.springframework.http.HttpStatus;

public class BankingException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BankingException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
