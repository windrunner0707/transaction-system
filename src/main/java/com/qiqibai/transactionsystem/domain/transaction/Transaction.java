package com.qiqibai.transactionsystem.domain.transaction;

import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private BigDecimal amount;
    private String description;
    private String sourceId;

    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String statusReason;

    @Builder.Default
    private long version = 0L;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void modify(BigDecimal amount, String description) {
        ensureMutable();
        this.amount = amount;
        this.description = description;
        touch();
    }

    public void startProcessing() {
        transitTo(TransactionStatus.PROCESSING, null);
    }

    public void markSucceeded() {
        transitTo(TransactionStatus.SUCCEEDED, null);
    }

    public void markFailed(String reason) {
        transitTo(TransactionStatus.FAILED, reason);
    }

    public void cancel(String reason) {
        transitTo(TransactionStatus.CANCELED, reason);
    }

    public Transaction copy() {
        return this.toBuilder().build();
    }

    private void ensureMutable() {
        if (status != TransactionStatus.PENDING) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg());
        }
    }

    private void transitTo(TransactionStatus targetStatus, String reason) {
        if (!status.canTransitTo(targetStatus)) {
            throw new BizException(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg());
        }
        this.status = targetStatus;
        this.statusReason = reason;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
