package com.df.savingsagent.agent;

import com.df.savingsagent.dto.ToolTraceDto;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ToolTraceService {
    private static final Logger log = LoggerFactory.getLogger(ToolTraceService.class);
    private static final ThreadLocal<List<ToolTraceDto>> TRACES = ThreadLocal.withInitial(ArrayList::new);
    private final MeterRegistry meterRegistry;

    public ToolTraceService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void startTurn() {
        TRACES.set(new ArrayList<>());
    }

    public List<ToolTraceDto> endTurn() {
        List<ToolTraceDto> copy = List.copyOf(TRACES.get());
        TRACES.remove();
        return copy;
    }

    public List<ToolTraceDto> currentTrace() {
        return Collections.unmodifiableList(TRACES.get());
    }

    public <T> T record(String tool, String inputSummary, Supplier<T> supplier) {
        Instant start = Instant.now();
        try {
            T result = supplier.get();
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            TRACES.get().add(new ToolTraceDto(tool, start, durationMs, "SUCCESS", inputSummary, null));
            meterRegistry.timer("banking.agent.tool.duration", "tool", tool, "status", "SUCCESS").record(Duration.ofMillis(durationMs));
            log.info("event=tool_call tool={} status=SUCCESS durationMs={} summary={}", tool, durationMs, inputSummary);
            return result;
        } catch (RuntimeException ex) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            String code = ex.getClass().getSimpleName();
            TRACES.get().add(new ToolTraceDto(tool, start, durationMs, "FAILED", inputSummary, code));
            meterRegistry.timer("banking.agent.tool.duration", "tool", tool, "status", "FAILED").record(Duration.ofMillis(durationMs));
            log.warn("event=tool_call tool={} status=FAILED durationMs={} errorCode={} summary={}",
                    tool, durationMs, code, inputSummary);
            throw ex;
        }
    }
}
