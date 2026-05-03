package com.qiqibai.transactionsystem.presentation.response;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.domain.transaction.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionQueryResponse {

    private String id;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String sourceId;
    private TransactionType type;
    private String payerId;
    private String payeeId;
    private String referenceId;
    private TransactionStatus status;
    private String statusReason;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TransactionQueryResponse fromDomain(Transaction transaction) {
        return TransactionQueryResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .sourceId(transaction.getSourceId())
                .type(transaction.getType())
                .payerId(transaction.getPayerId())
                .payeeId(transaction.getPayeeId())
                .referenceId(transaction.getReferenceId())
                .status(transaction.getStatus())
                .statusReason(transaction.getStatusReason())
                .attemptCount(transaction.getAttemptCount())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

}

