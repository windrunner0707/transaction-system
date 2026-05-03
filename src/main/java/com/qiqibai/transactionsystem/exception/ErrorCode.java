package com.qiqibai.transactionsystem.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NO_TRANSACTION_FOUND("Error-001", "No transaction found.", HttpStatus.NOT_FOUND),
    DUPLICATED_TRANSACTION("Error-002", "The transaction is duplicated.", HttpStatus.CONFLICT),
    INVALID_TRANSACTION_STATUS_TRANSITION("Error-003", "The transaction status transition is invalid.", HttpStatus.UNPROCESSABLE_ENTITY),
    CONCURRENT_TRANSACTION_MODIFICATION("Error-004", "The transaction was modified concurrently.", HttpStatus.CONFLICT),
    AMOUNT_EXCEEDS_LIMIT("Error-005", "Transaction amount exceeds the maximum allowed limit.", HttpStatus.UNPROCESSABLE_ENTITY),
    MAX_RETRIES_EXCEEDED("Error-006", "Maximum retry attempts exceeded.", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_AMOUNT("Error-007", "Transaction amount must be positive.", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String errorCode;
    private final String errorMsg;
    private final HttpStatus httpStatus;

}
