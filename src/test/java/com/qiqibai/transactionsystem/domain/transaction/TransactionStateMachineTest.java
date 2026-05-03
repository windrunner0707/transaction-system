package com.qiqibai.transactionsystem.domain.transaction;

import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionStateMachineTest {

    @Test
    void shouldAllowPendingToProcessingToSucceeded() {
        Transaction transaction = Transaction.builder().build();

        transaction.startProcessing();
        transaction.markSucceeded();

        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
    }

    @Test
    void shouldRejectInvalidTransitionFromPendingToSucceeded() {
        Transaction transaction = Transaction.builder().build();

        BizException exception = assertThrows(BizException.class, transaction::markSucceeded);

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
}
