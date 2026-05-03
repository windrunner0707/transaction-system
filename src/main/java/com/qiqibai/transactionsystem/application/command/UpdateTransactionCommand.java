package com.qiqibai.transactionsystem.application.command;

import java.math.BigDecimal;

public record UpdateTransactionCommand(BigDecimal amount, String description) {
}
