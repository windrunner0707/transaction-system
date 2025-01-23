package com.qiqibai.transactionsystem.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NO_TRANSACTION_FOUND("Error-001", "Not transaction found."),
    DUPLICATED_TRANSACTION("Error-002", "The transaction is duplicated.");

    private final String errorCode;
    private final String errorMsg;

}
