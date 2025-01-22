package com.qiqibai.transactionsystem.presentation.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCreateRequest {

    @NotNull(message = "amount should not be null")
    private BigDecimal amount;

    private String description;

}
