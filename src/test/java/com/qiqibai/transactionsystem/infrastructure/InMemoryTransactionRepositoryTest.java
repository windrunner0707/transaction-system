package com.qiqibai.transactionsystem.infrastructure;

import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        loaded.modify(loaded.getAmount(), "mutated");

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
    void shouldThrowWhenDeletingNonExistentTransaction() {
        BizException exception = assertThrows(BizException.class, () -> repository.delete("non-existent-id"));

        assertEquals(ErrorCode.NO_TRANSACTION_FOUND.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldReturnDetachedCopiesFromFindAll() {
        Transaction transaction = Transaction.builder()
                .amount(BigDecimal.valueOf(15))
                .status(TransactionStatus.PENDING)
                .build();
        repository.save(transaction);

        List<Transaction> retrievedTransactions = repository.findAll();
        retrievedTransactions.getFirst().modify(retrievedTransactions.getFirst().getAmount(), "changed outside repository");

        Transaction reloaded = repository.findById(transaction.getId()).orElseThrow();
        assertEquals(1, retrievedTransactions.size());
        assertNotSame(transaction, retrievedTransactions.getFirst());
        assertEquals(transaction.getId(), retrievedTransactions.getFirst().getId());
        assertEquals(transaction.getDescription(), reloaded.getDescription());
    }

    @Test
    void shouldHideSoftDeletedTransactionsFromFindById() {
        Transaction transaction = Transaction.builder().build();
        repository.save(transaction);

        // Soft-delete via archive (requires terminal state, so cancel first)
        Transaction copy = repository.findById(transaction.getId()).orElseThrow();
        copy.cancel("test");
        repository.save(copy);
        Transaction afterCancel = repository.findById(transaction.getId()).orElseThrow();
        afterCancel.archive();
        repository.save(afterCancel);

        assertTrue(repository.findById(transaction.getId()).isEmpty());
    }

    @Test
    void shouldExcludeSoftDeletedTransactionsFromFindAll() {
        Transaction active = Transaction.builder().build();
        Transaction toDelete = Transaction.builder().build();
        repository.save(active);
        repository.save(toDelete);

        // Soft-delete toDelete
        Transaction copy = repository.findById(toDelete.getId()).orElseThrow();
        copy.cancel("cleanup");
        repository.save(copy);
        Transaction afterCancel = repository.findById(toDelete.getId()).orElseThrow();
        afterCancel.archive();
        repository.save(afterCancel);

        List<Transaction> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals(active.getId(), all.getFirst().getId());
    }

    @Test
    void shouldFilterByStatus() {
        Transaction pending = Transaction.builder().build();
        Transaction processing = Transaction.builder().build();
        repository.save(pending);
        repository.save(processing);

        Transaction processingCopy = repository.findById(processing.getId()).orElseThrow();
        processingCopy.startProcessing();
        repository.save(processingCopy);

        Page<Transaction> result = repository.findAll(PageRequest.of(0, 10), TransactionStatus.PENDING);

        assertEquals(1, result.getTotalElements());
        assertEquals(pending.getId(), result.getContent().getFirst().getId());
    }

    @Test
    void shouldReturnTransactionsInDeterministicOrderByCreatedAt() {
        // Save several transactions; order should be consistent (createdAt ascending)
        for (int i = 0; i < 5; i++) {
            repository.save(Transaction.builder().description("tx-" + i).build());
        }

        Page<Transaction> page1 = repository.findAll(PageRequest.of(0, 5, Sort.by("createdAt")));
        Page<Transaction> page2 = repository.findAll(PageRequest.of(0, 5, Sort.by("createdAt")));

        assertEquals(page1.getContent().stream().map(Transaction::getId).toList(),
                page2.getContent().stream().map(Transaction::getId).toList());
    }
}

