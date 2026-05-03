package com.qiqibai.transactionsystem.application;

import com.qiqibai.transactionsystem.domain.transaction.TransactionEvent;

import java.util.List;

public interface TransactionEventLog {

    void record(TransactionEvent event);

    List<TransactionEvent> findByTransactionId(String transactionId);

}
