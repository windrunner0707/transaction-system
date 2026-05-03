package com.qiqibai.transactionsystem.domain.transaction;

import java.util.List;

public interface TransactionEventLog {

    void record(TransactionEvent event);

    List<TransactionEvent> findByTransactionId(String transactionId);

}
