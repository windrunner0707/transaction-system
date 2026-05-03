package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldIncrementVersionWhenUpdatingExistingTransaction() {
        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.TEN)
                .description("initial")
                .build();
        repository.save(transaction);

        Transaction storedCopy = repository.findById(transaction.getId()).orElseThrow();
        storedCopy.modify(BigDecimal.ONE, "updated");

        repository.save(storedCopy);

        Transaction reloaded = repository.findById(transaction.getId()).orElseThrow();
        assertEquals(1L, storedCopy.getVersion());
        assertEquals(1L, reloaded.getVersion());
        assertEquals(BigDecimal.ONE, reloaded.getAmount());
        assertEquals("updated", reloaded.getDescription());
    }

    @Test
    void shouldFindTransactionBySourceIdUsingDetachedCopy() {
        Transaction transaction = Transaction.builder()
                .sourceId("source-1")
                .description("saved")
                .build();
        repository.save(transaction);

        Transaction loaded = repository.findBySourceId("source-1").orElseThrow();
        loaded.setDescription("mutated");

        Transaction reloaded = repository.findById(transaction.getId()).orElseThrow();
        assertEquals(transaction.getId(), loaded.getId());
        assertNotSame(transaction, loaded);
        assertEquals("saved", reloaded.getDescription());
    }

    @Test
    void shouldDeleteTransactions() {
        Transaction transaction = Transaction.builder().build();
        repository.save(transaction);

        repository.delete(transaction.getId());

        assertTrue(repository.findById(transaction.getId()).isEmpty());
    }

    @Test
    void shouldReturnDetachedCopiesFromFindAll() {
        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(15))
                .status(TransactionStatus.PENDING)
                .build();
        repository.save(transaction);

        List<Transaction> transactions = repository.findAll();
        transactions.getFirst().setDescription("changed outside repository");

        Transaction reloaded = repository.findById(transaction.getId()).orElseThrow();
        assertEquals(1, transactions.size());
        assertFalse(transactions.getFirst() == transaction);
        assertEquals(transaction.getId(), transactions.getFirst().getId());
        assertEquals(transaction.getDescription(), reloaded.getDescription());
    }
}
