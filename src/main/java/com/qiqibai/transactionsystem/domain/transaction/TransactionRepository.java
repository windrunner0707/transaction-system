package com.qiqibai.transactionsystem.domain.transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Optional<Transaction> findById(String id);

    void save(Transaction transaction);

    void delete(String id);

    List<Transaction> findAll();

}
