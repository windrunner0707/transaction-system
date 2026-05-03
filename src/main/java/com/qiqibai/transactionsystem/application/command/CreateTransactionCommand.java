package com.qiqibai.transactionsystem.application.command;

import com.qiqibai.transactionsystem.domain.transaction.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionCommand(
        BigDecimal amount,
        String currency,
        String description,
        String sourceId,
        TransactionType type,
        String payerId,
        String payeeId
) {
}
