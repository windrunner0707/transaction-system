package com.qiqibai.transactionsystem.domain.transaction;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private BigDecimal amount;
    private String description;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void modify(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }
}
