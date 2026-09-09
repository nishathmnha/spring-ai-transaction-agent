package com.df.savingsagent.dto;

import java.time.Instant;

public record ToolTraceDto(
        String tool,
        Instant startTime,
        long durationMs,
        String status,
        String summary,
        String errorCode
) {
}
