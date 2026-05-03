package com.qiqibai.transactionsystem.domain.transaction;

import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    public static final BigDecimal MAX_AMOUNT = BigDecimal.valueOf(1_000_000);
    public static final int MAX_RETRIES = 3;

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private BigDecimal amount;
    private String currency;
    private String description;
    private String sourceId;
    private TransactionType type;
    private String payerId;
    private String payeeId;
    private String referenceId;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String statusReason;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private int attemptCount = 0;

    @Builder.Default
    private long version = 0L;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void modify(BigDecimal amount, String description) {
        ensureMutable();
        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException(ErrorCode.INVALID_AMOUNT);
            }
            if (amount.compareTo(MAX_AMOUNT) > 0) {
                throw new BizException(ErrorCode.AMOUNT_EXCEEDS_LIMIT);
            }
        }
        this.amount = amount;
        this.description = description;
        touch();
    }

    public void startProcessing() {
        transitTo(TransactionStatus.PROCESSING, null);
        this.attemptCount++;
    }

    public void markSucceeded(String referenceId) {
        transitTo(TransactionStatus.SUCCEEDED, null);
        this.referenceId = referenceId;
    }

    public void markFailed(String reason) {
        transitTo(TransactionStatus.FAILED, reason);
    }

    public void cancel(String reason) {
        transitTo(TransactionStatus.CANCELED, reason);
    }

    public void retry() {
        if (this.status != TransactionStatus.FAILED) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION);
        }
        if (this.attemptCount >= MAX_RETRIES) {
            throw new BizException(ErrorCode.MAX_RETRIES_EXCEEDED);
        }
        transitTo(TransactionStatus.PENDING, null);
    }

    public void archive() {
        if (!status.isTerminal()) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION);
        }
        this.deleted = true;
        touch();
    }

    public Transaction copy() {
        return this.toBuilder().build();
    }

    private void ensureMutable() {
        if (status != TransactionStatus.PENDING) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION);
        }
    }

    private void transitTo(TransactionStatus targetStatus, String reason) {
        if (!status.canTransitTo(targetStatus)) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION);
        }
        this.status = targetStatus;
        this.statusReason = reason;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}

