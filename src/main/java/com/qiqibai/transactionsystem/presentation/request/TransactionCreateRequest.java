package com.qiqibai.transactionsystem.presentation.request;

import com.qiqibai.transactionsystem.domain.transaction.TransactionType;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "currency should not be blank")
    private String currency;

    private String description;

    @NotBlank(message = "sourceId should not be blank")
    private String sourceId;

    @NotNull(message = "type should not be null")
    private TransactionType type;

    private String payerId;

    private String payeeId;

}

