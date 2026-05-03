package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.TransactionEventLog;
import com.qiqibai.transactionsystem.domain.transaction.TransactionEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryTransactionEventLog implements TransactionEventLog {

    private final List<TransactionEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void record(TransactionEvent event) {
        events.add(event);
    }

    @Override
    public List<TransactionEvent> findByTransactionId(String transactionId) {
        return events.stream()
                .filter(e -> transactionId.equals(e.transactionId()))
                .toList();
    }

}
