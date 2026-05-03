package com.qiqibai.transactionsystem.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NO_TRANSACTION_FOUND("Error-001", "Not transaction found."),
    DUPLICATED_TRANSACTION("Error-002", "The transaction is duplicated."),
    INVALID_TRANSACTION_STATUS_TRANSITION("Error-003", "The transaction status transition is invalid."),
    CONCURRENT_TRANSACTION_MODIFICATION("Error-004", "The transaction was modified concurrently.");

    private final String errorCode;
    private final String errorMsg;

}
