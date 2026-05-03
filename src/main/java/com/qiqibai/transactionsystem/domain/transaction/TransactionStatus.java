package com.qiqibai.transactionsystem.domain.transaction;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    CANCELED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(TransactionStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PENDING, EnumSet.of(PROCESSING, CANCELED));
        ALLOWED_TRANSITIONS.put(PROCESSING, EnumSet.of(SUCCEEDED, FAILED, CANCELED));
        ALLOWED_TRANSITIONS.put(SUCCEEDED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(FAILED, EnumSet.noneOf(TransactionStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELED, EnumSet.noneOf(TransactionStatus.class));
    }

    public boolean canTransitTo(TransactionStatus targetStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(TransactionStatus.class))
                .contains(targetStatus);
    }
}
