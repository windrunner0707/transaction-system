package com.qiqibai.transactionsystem.presentation.response;

import com.qiqibai.transactionsystem.domain.transaction.TransactionEvent;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;

import java.time.LocalDateTime;

public record TransactionEventResponse(
        String id,
        String transactionId,
        String eventType,
        TransactionStatus previousStatus,
        TransactionStatus newStatus,
        String reason,
        String referenceId,
        LocalDateTime occurredAt
) {
    public static TransactionEventResponse fromDomain(TransactionEvent event) {
        return new TransactionEventResponse(
                event.id(),
                event.transactionId(),
                event.eventType(),
                event.previousStatus(),
                event.newStatus(),
                event.reason(),
                event.referenceId(),
                event.occurredAt()
        );
    }
}
