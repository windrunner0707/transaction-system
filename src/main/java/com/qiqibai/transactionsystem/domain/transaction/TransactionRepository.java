package com.qiqibai.transactionsystem.domain.transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Optional<Transaction> findById(String id);

    Optional<Transaction> findBySourceId(String sourceId);

    String save(Transaction transaction);

    void delete(String id);

    List<Transaction> findAll();

}
