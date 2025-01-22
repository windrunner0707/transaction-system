package com.qiqibai.transactionsystem.presentation.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionUpdateRequest {

    private BigDecimal amount;
    private String description;

}
