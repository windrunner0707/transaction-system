package com.qiqibai.transactionsystem.application.command;

import java.math.BigDecimal;

public record CreateTransactionCommand(BigDecimal amount, String description, String sourceId) {
}
