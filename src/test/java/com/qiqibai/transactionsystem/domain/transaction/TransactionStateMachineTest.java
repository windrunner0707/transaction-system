package com.qiqibai.transactionsystem.domain.transaction;

import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionStateMachineTest {

    @Test
    void shouldAllowPendingToProcessingToSucceeded() {
        Transaction transaction = Transaction.builder().build();

        transaction.startProcessing();
        transaction.markSucceeded(null);

        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
    }

    @Test
    void shouldRejectInvalidTransitionFromPendingToSucceeded() {
        Transaction transaction = Transaction.builder().build();

        BizException exception = assertThrows(BizException.class, () -> transaction.markSucceeded(null));

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldRejectModificationAfterTerminalState() {
        Transaction transaction = Transaction.builder().build();
        transaction.cancel("duplicate request");

        BizException exception = assertThrows(BizException.class,
                () -> transaction.modify(transaction.getAmount(), transaction.getDescription()));

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldRejectModificationWhenAmountExceedsMaximum() {
        Transaction transaction = Transaction.builder().build();

        BizException exception = assertThrows(BizException.class,
                () -> transaction.modify(Transaction.MAX_AMOUNT.add(BigDecimal.ONE), "too big"));

        assertEquals(ErrorCode.AMOUNT_EXCEEDS_LIMIT.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldRejectModificationWhenAmountIsZeroOrNegative() {
        Transaction transaction = Transaction.builder().build();

        BizException exception = assertThrows(BizException.class,
                () -> transaction.modify(BigDecimal.ZERO, "zero amount"));

        assertEquals(ErrorCode.INVALID_AMOUNT.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldAllowRetryFromFailedToPending() {
        Transaction transaction = Transaction.builder().build();
        transaction.startProcessing();
        transaction.markFailed("bank rejected");

        transaction.retry();

        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        assertEquals(1, transaction.getAttemptCount());
    }

    @Test
    void shouldRejectRetryWhenMaxRetriesExceeded() {
        Transaction transaction = Transaction.builder().build();
        for (int i = 0; i < Transaction.MAX_RETRIES; i++) {
            transaction.startProcessing();
            transaction.markFailed("bank rejected");
            if (i < Transaction.MAX_RETRIES - 1) {
                transaction.retry();
            }
        }

        BizException exception = assertThrows(BizException.class, transaction::retry);

        assertEquals(ErrorCode.MAX_RETRIES_EXCEEDED.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldIncrementAttemptCountOnEachProcessing() {
        Transaction transaction = Transaction.builder().build();

        transaction.startProcessing();
        assertEquals(1, transaction.getAttemptCount());

        transaction.markFailed("error");
        transaction.retry();
        transaction.startProcessing();
        assertEquals(2, transaction.getAttemptCount());
    }

    @Test
    void shouldArchiveTerminalTransaction() {
        Transaction transaction = Transaction.builder().build();
        transaction.startProcessing();
        transaction.markSucceeded("GW-123");

        transaction.archive();

        assertTrue(transaction.isDeleted());
    }

    @Test
    void shouldRejectArchivingNonTerminalTransaction() {
        Transaction transaction = Transaction.builder().build();

        BizException exception = assertThrows(BizException.class, transaction::archive);

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
    }

    @Test
    void shouldCaptureReferenceIdOnSuccess() {
        Transaction transaction = Transaction.builder().build();
        transaction.startProcessing();
        transaction.markSucceeded("GW-XYZ");

        assertEquals("GW-XYZ", transaction.getReferenceId());
        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
    }
}

