package com.qiqibai.transactionsystem.domain.transaction;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionEvent(
        String id,
        String transactionId,
        String eventType,
        TransactionStatus previousStatus,
        TransactionStatus newStatus,
        String reason,
        String referenceId,
        LocalDateTime occurredAt
) {
    public static TransactionEvent created(String transactionId) {
        return new TransactionEvent(UUID.randomUUID().toString(), transactionId,
                "CREATED", null, TransactionStatus.PENDING, null, null, LocalDateTime.now());
    }

    public static TransactionEvent statusTransition(String transactionId,
                                                    TransactionStatus from, TransactionStatus to,
                                                    String reason, String referenceId) {
        return new TransactionEvent(UUID.randomUUID().toString(), transactionId,
                "STATUS_TRANSITION", from, to, reason, referenceId, LocalDateTime.now());
    }

    public static TransactionEvent modified(String transactionId, TransactionStatus currentStatus) {
        return new TransactionEvent(UUID.randomUUID().toString(), transactionId,
                "MODIFIED", currentStatus, currentStatus, null, null, LocalDateTime.now());
    }

    public static TransactionEvent archived(String transactionId, TransactionStatus currentStatus) {
        return new TransactionEvent(UUID.randomUUID().toString(), transactionId,
                "ARCHIVED", currentStatus, currentStatus, null, null, LocalDateTime.now());
    }
}
