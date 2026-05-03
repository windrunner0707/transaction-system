package com.qiqibai.transactionsystem.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NO_TRANSACTION_FOUND("Error-001", "No transaction found."),
    DUPLICATED_TRANSACTION("Error-002", "The transaction is duplicated."),
    INVALID_TRANSACTION_STATUS_TRANSITION("Error-003", "The transaction status transition is invalid."),
    CONCURRENT_TRANSACTION_MODIFICATION("Error-004", "The transaction was modified concurrently."),
    AMOUNT_EXCEEDS_LIMIT("Error-005", "Transaction amount exceeds the maximum allowed limit."),
    MAX_RETRIES_EXCEEDED("Error-006", "Maximum retry attempts exceeded."),
    INVALID_AMOUNT("Error-007", "Transaction amount must be positive.");

    private final String errorCode;
    private final String errorMsg;

}
