package com.qiqibai.transactionsystem.infrastructure;

import com.google.common.cache.Cache;
import com.qiqibai.transactionsystem.application.TransactionCache;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GuavaTransactionCache implements TransactionCache {

    private final Cache<String, Transaction> cache;

    @Override
    public Optional<Transaction> get(String id) {
        return Optional.ofNullable(cache.getIfPresent(id));
    }

    @Override
    public void put(String id, Transaction transaction) {
        cache.put(id, transaction);
    }

    @Override
    public void invalidate(String id) {
        cache.invalidate(id);
    }

}
