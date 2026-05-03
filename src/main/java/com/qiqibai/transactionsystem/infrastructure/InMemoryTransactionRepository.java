package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Stream;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final Map<String, Transaction> inMemoryDb = new HashMap<>();

    @Override
    public synchronized Optional<Transaction> findById(String id) {
        return Optional.ofNullable(inMemoryDb.get(id))
                .filter(t -> !t.isDeleted())
                .map(Transaction::copy);
    }

    @Override
    public synchronized Optional<Transaction> findBySourceId(String sourceId) {
        return inMemoryDb.values().stream()
                .filter(it -> !it.isDeleted())
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
                .filter(t -> !t.isDeleted())
                .map(Transaction::copy)
                .toList();
    }

    @Override
    public synchronized Page<Transaction> findAll(Pageable pageable) {
        return findAll(pageable, null);
    }

    @Override
    public synchronized Page<Transaction> findAll(Pageable pageable, TransactionStatus status) {
        Stream<Transaction> stream = inMemoryDb.values().stream()
                .filter(t -> !t.isDeleted());

        if (status != null) {
            stream = stream.filter(t -> t.getStatus() == status);
        }

        List<Transaction> sorted = stream
                .sorted(buildComparator(pageable.getSort()))
                .map(Transaction::copy)
                .toList();

        int total = sorted.size();
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(sorted.subList(start, end), pageable, total);
    }

    private Comparator<Transaction> buildComparator(Sort sort) {
        if (sort.isSorted()) {
            for (Sort.Order order : sort) {
                if ("updatedAt".equals(order.getProperty())) {
                    Comparator<Transaction> c = Comparator.comparing(Transaction::getUpdatedAt);
                    return order.isAscending() ? c : c.reversed();
                }
                if ("createdAt".equals(order.getProperty())) {
                    Comparator<Transaction> c = Comparator.comparing(Transaction::getCreatedAt);
                    return order.isAscending() ? c : c.reversed();
                }
            }
        }
        // default: ascending by createdAt for deterministic ordering
        return Comparator.comparing(Transaction::getCreatedAt);
    }

}

