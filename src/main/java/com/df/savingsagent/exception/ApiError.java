package com.df.savingsagent.exception;

import java.time.Instant;

public record ApiError(String code, String message, String correlationId, Instant timestamp) {
}
