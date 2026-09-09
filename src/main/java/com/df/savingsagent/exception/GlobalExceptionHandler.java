package com.df.savingsagent.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BankingException.class)
    ResponseEntity<ApiError> banking(BankingException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("category=banking_failure code={} correlationId={} path={} message={}",
                ex.getCode(), correlationId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiError(ex.getCode(), ex.getMessage(), correlationId, Instant.now()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    ResponseEntity<ApiError> validation(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("category=request_validation correlationId={} path={}", correlationId, request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "The request is missing required or valid fields.", correlationId, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("category=unexpected_failure correlationId={} path={}", correlationId, request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "The request could not be completed.", correlationId, Instant.now()));
    }
}
