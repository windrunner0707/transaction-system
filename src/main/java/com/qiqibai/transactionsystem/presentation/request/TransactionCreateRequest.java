package com.qiqibai.transactionsystem.presentation.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateRequest {

    @NotNull(message = "amount should not be null")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    private String description;

    private String sourceId;

}
