package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> inMemoryDb = new HashMap<>();

    @Override
    public Optional<Transaction> findById(String id) {
        return Optional.ofNullable(inMemoryDb.get(id));
    }

    @Override
    public Optional<Transaction> findBySourceId(String sourceId) {
        return inMemoryDb.values().stream()
                .filter(it -> Objects.equals(it.getSourceId(), sourceId))
                .findFirst();
    }

    @Override
    public String save(Transaction transaction) {
        inMemoryDb.put(transaction.getId(), transaction);
        return transaction.getId();
    }

    @Override
    public void delete(String id) {
        inMemoryDb.remove(id);
    }

    @Override
    public List<Transaction> findAll() {
        return inMemoryDb.values().stream().toList();
    }

}
