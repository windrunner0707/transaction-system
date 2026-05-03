package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryTransactionRepositoryTest {

    private final InMemoryTransactionRepository repository = new InMemoryTransactionRepository();

    @Test
    void shouldReturnDetachedCopies() {
        Transaction transaction = Transaction.builder().build();
        repository.save(transaction);

        Optional<Transaction> loaded = repository.findById(transaction.getId());

        assertEquals(transaction.getId(), loaded.orElseThrow().getId());
        assertNotSame(transaction, loaded.orElseThrow());
    }

    @Test
    void shouldRejectConcurrentModification() {
        Transaction transaction = Transaction.builder().build();
        repository.save(transaction);

        Transaction firstCopy = repository.findById(transaction.getId()).orElseThrow();
        Transaction secondCopy = repository.findById(transaction.getId()).orElseThrow();

        firstCopy.startProcessing();
        repository.save(firstCopy);

        secondCopy.cancel("duplicate");
        BizException exception = assertThrows(BizException.class, () -> repository.save(secondCopy));

        assertEquals(ErrorCode.CONCURRENT_TRANSACTION_MODIFICATION.getErrorMsg(), exception.getMessage());
    }
}
