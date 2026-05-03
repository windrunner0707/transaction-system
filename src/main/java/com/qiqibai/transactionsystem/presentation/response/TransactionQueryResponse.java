package com.qiqibai.transactionsystem.presentation.response;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
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
    private String description;
    private String sourceId;
    private TransactionStatus status;
    private String statusReason;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TransactionQueryResponse fromDomain(Transaction transaction) {
        return TransactionQueryResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .sourceId(transaction.getSourceId())
                .status(transaction.getStatus())
                .statusReason(transaction.getStatusReason())
                .version(transaction.getVersion())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

}
