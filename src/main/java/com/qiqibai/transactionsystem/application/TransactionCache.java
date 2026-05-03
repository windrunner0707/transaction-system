package com.qiqibai.transactionsystem.application;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;

import java.util.Optional;

public interface TransactionCache {

    Optional<Transaction> get(String id);

    void put(String id, Transaction transaction);

    void invalidate(String id);

}
