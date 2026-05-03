package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> inMemoryDb = new HashMap<>();

    @Override
    public synchronized Optional<Transaction> findById(String id) {
        return Optional.ofNullable(inMemoryDb.get(id))
                .map(Transaction::copy);
    }

    @Override
    public synchronized Optional<Transaction> findBySourceId(String sourceId) {
        return inMemoryDb.values().stream()
                .filter(it -> Objects.equals(it.getSourceId(), sourceId))
                .map(Transaction::copy)
                .findFirst();
    }

    @Override
    public synchronized String save(Transaction transaction) {
        Transaction existingTransaction = inMemoryDb.get(transaction.getId());
        if (Objects.nonNull(existingTransaction) && existingTransaction.getVersion() != transaction.getVersion()) {
            throw new BizException(ErrorCode.CONCURRENT_TRANSACTION_MODIFICATION.getErrorMsg());
        }

        Transaction transactionToSave = transaction.copy();
        if (Objects.nonNull(existingTransaction)) {
            transactionToSave.setVersion(transaction.getVersion() + 1);
        }
        inMemoryDb.put(transaction.getId(), transactionToSave);
        transaction.setVersion(transactionToSave.getVersion());
        return transaction.getId();
    }

    @Override
    public synchronized void delete(String id) {
        inMemoryDb.remove(id);
    }

    @Override
    public synchronized List<Transaction> findAll() {
        return inMemoryDb.values().stream()
                .map(Transaction::copy)
                .toList();
    }

}
