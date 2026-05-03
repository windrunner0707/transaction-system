package com.qiqibai.transactionsystem.presentation.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpdateRequest {

    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    @Size(max = 500, message = "description must not exceed 500 characters")
    private String description;

}
