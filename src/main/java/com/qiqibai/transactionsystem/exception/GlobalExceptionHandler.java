package com.qiqibai.transactionsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Map<String, String>> handleBizException(BizException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("code", ex.getErrorCode().getErrorCode());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(toHttpStatus(ex.getErrorCode())).body(body);
    }

    private HttpStatus toHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case NO_TRANSACTION_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATED_TRANSACTION, CONCURRENT_TRANSACTION_MODIFICATION -> HttpStatus.CONFLICT;
            case INVALID_TRANSACTION_STATUS_TRANSITION, AMOUNT_EXCEEDS_LIMIT,
                    MAX_RETRIES_EXCEEDED, INVALID_AMOUNT -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }

}
