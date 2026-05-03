package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
            throw new BizException(ErrorCode.CONCURRENT_TRANSACTION_MODIFICATION);
        }

        Transaction transactionToSave = Objects.nonNull(existingTransaction)
                ? transaction.toBuilder().version(transaction.getVersion() + 1).build()
                : transaction.copy();
        inMemoryDb.put(transaction.getId(), transactionToSave);
        return transaction.getId();
    }

    @Override
    public synchronized void delete(String id) {
        if (!inMemoryDb.containsKey(id)) {
            throw new BizException(ErrorCode.NO_TRANSACTION_FOUND);
        }
        inMemoryDb.remove(id);
    }

    @Override
    public synchronized List<Transaction> findAll() {
        return inMemoryDb.values().stream()
                .map(Transaction::copy)
                .toList();
    }

    @Override
    public synchronized Page<Transaction> findAll(Pageable pageable) {
        List<Transaction> all = inMemoryDb.values().stream()
                .map(Transaction::copy)
                .toList();
        int total = all.size();
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(all.subList(start, end), pageable, total);
    }

}
