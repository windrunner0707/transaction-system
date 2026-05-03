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
    CONCURRENT_TRANSACTION_MODIFICATION("Error-004", "The transaction was modified concurrently.", HttpStatus.CONFLICT);

    private final String errorCode;
    private final String errorMsg;
    private final HttpStatus httpStatus;

}
