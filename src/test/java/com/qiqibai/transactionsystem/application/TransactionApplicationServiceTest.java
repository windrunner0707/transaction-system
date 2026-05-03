package com.qiqibai.transactionsystem.application;

import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.SucceedTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.domain.transaction.TransactionType;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionApplicationServiceTest {

    private TransactionApplicationService transactionService;
    private TransactionRepository transactionRepository;
    private TransactionCache transactionCache;
    private TransactionEventLog transactionEventLog;


    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        transactionCache = mock(TransactionCache.class);
        transactionEventLog = mock(TransactionEventLog.class);
        transactionService = new TransactionApplicationService(transactionRepository, transactionCache, transactionEventLog);
    }

    @Test
    void testCreateTransaction() {
        // Arrange
        CreateTransactionCommand command = new CreateTransactionCommand(
                BigDecimal.valueOf(100.0), "USD", "Test transaction", "Test sourceId",
                TransactionType.PAYMENT, "payer-1", "payee-1");

        when(transactionRepository.findBySourceId("Test sourceId")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenReturn("id1");

        // Act
        String transactionId = transactionService.createTransaction(command);

        // Assert
        assertNotNull(transactionId);
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionArgumentCaptor.capture());
        verify(transactionRepository).findBySourceId("Test sourceId");
        assertEquals(TransactionStatus.PENDING, transactionArgumentCaptor.getValue().getStatus());
        assertEquals("Test sourceId", transactionArgumentCaptor.getValue().getSourceId());
        assertEquals("USD", transactionArgumentCaptor.getValue().getCurrency());
        assertEquals(TransactionType.PAYMENT, transactionArgumentCaptor.getValue().getType());
    }

    @Test
    void testCreateTransaction_amountExceedsLimit() {
        CreateTransactionCommand command = new CreateTransactionCommand(
                Transaction.MAX_AMOUNT.add(BigDecimal.ONE), "USD", "Too big", "src-1",
                TransactionType.PAYMENT, null, null);

        BizException exception = assertThrows(BizException.class,
                () -> transactionService.createTransaction(command));

        assertEquals(ErrorCode.AMOUNT_EXCEEDS_LIMIT.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testCreateTransaction_duplicatedTransaction() {
        // Arrange
        CreateTransactionCommand command = new CreateTransactionCommand(
                BigDecimal.valueOf(100.0), "USD", "Test transaction", "Test sourceId",
                TransactionType.PAYMENT, null, null);

        Transaction transaction = Transaction.builder()
                .sourceId("Test sourceId")
                .build();

        when(transactionRepository.findBySourceId("Test sourceId")).thenReturn(Optional.of(transaction));

        // Act
        BizException exception = assertThrows(BizException.class, () -> transactionService.createTransaction(command));

        // Assert
        assertEquals(ErrorCode.DUPLICATED_TRANSACTION.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, times(1)).findBySourceId("Test sourceId");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction_softDeletes() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.SUCCEEDED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertTrue(captor.getValue().isDeleted());
        verify(transactionCache, times(1)).invalidate(transactionId);
    }

    @Test
    void testDeleteTransaction_rejectsNonTerminal() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        BizException exception = assertThrows(BizException.class,
                () -> transactionService.deleteTransaction(transactionId));

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testModifyTransaction() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();
        UpdateTransactionCommand command = new UpdateTransactionCommand(BigDecimal.valueOf(200.0), "Updated Description");
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .amount(BigDecimal.valueOf(100.0))
                .description("Old Description")
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction.getId());

        // Act
        transactionService.modifyTransaction(transactionId, command);

        // Assert
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(captor.capture());
        verify(transactionCache, times(1)).invalidate(transactionId);
        assertEquals(command.amount(), captor.getValue().getAmount());
        assertEquals(command.description(), captor.getValue().getDescription());
    }

    @Test
    void testModifyTransaction_invalidState() {
        String transactionId = UUID.randomUUID().toString();
        UpdateTransactionCommand command = new UpdateTransactionCommand(BigDecimal.valueOf(200.0), "Updated Description");
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        BizException exception = assertThrows(BizException.class,
                () -> transactionService.modifyTransaction(transactionId, command));

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testGetTransactionById_fromCache() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .amount(BigDecimal.valueOf(100.0))
                .description("Cached Transaction")
                .build();

        when(transactionCache.get(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        TransactionQueryResponse response = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(response);
        assertEquals(transactionId, response.getId());
        assertEquals(TransactionStatus.PENDING, response.getStatus());
        verify(transactionCache, times(1)).get(transactionId);
        verify(transactionRepository, never()).findById(transactionId);
    }

    @Test
    void testGetTransactionById_fromDatabase() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .amount(BigDecimal.valueOf(100.0))
                .description("DB Transaction")
                .build();

        when(transactionCache.get(transactionId)).thenReturn(Optional.empty());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        TransactionQueryResponse response = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(response);
        assertEquals(transactionId, response.getId());
        assertEquals(TransactionStatus.PENDING, response.getStatus());
        verify(transactionCache, times(1)).get(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionCache, times(1)).put(transactionId, transaction);
    }

    @Test
    void testGetTransactionById_notFound() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();

        when(transactionCache.get(transactionId)).thenReturn(Optional.empty());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        BizException exception = assertThrows(BizException.class,
                () -> transactionService.getTransactionById(transactionId));
        assertEquals(ErrorCode.NO_TRANSACTION_FOUND.getErrorMsg(), exception.getMessage());
        verify(transactionCache, times(1)).get(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void testGetAllTransactionsByPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 3);
        List<Transaction> pageItems = List.of(
                Transaction.builder().id("tx-1").amount(BigDecimal.valueOf(100.0)).build(),
                Transaction.builder().id("tx-2").amount(BigDecimal.valueOf(200.0)).build()
        );
        when(transactionRepository.findAll(pageable)).thenReturn(new PageImpl<>(pageItems, pageable, 5));

        // Act
        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable, null);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals("tx-1", result.getContent().get(0).getId());
        verify(transactionRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetAllTransactionsByPage_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 3);
        List<Transaction> pageItems = List.of(
                Transaction.builder().id("tx-1").status(TransactionStatus.PENDING).build()
        );
        when(transactionRepository.findAll(pageable, TransactionStatus.PENDING))
                .thenReturn(new PageImpl<>(pageItems, pageable, 1));

        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable, TransactionStatus.PENDING);

        assertEquals(1, result.getContent().size());
        verify(transactionRepository, times(1)).findAll(pageable, TransactionStatus.PENDING);
    }

    @Test
    void testGetAllTransactionsByPage_empty() {
        // Arrange
        Pageable pageable = PageRequest.of(5, 3);
        when(transactionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 10));

        // Act
        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable, null);

        // Assert
        assertEquals(0, result.getContent().size());
        assertEquals(10, result.getTotalElements());
        verify(transactionRepository, times(1)).findAll(pageable);
    }

    @Test
    void testStartProcessing() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.startProcessing(transactionId);

        assertEquals(TransactionStatus.PROCESSING, transaction.getStatus());
        verify(transactionRepository).save(transaction);
        verify(transactionCache).invalidate(transactionId);
    }

    @Test
    void testMarkSucceeded() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.markSucceeded(transactionId, new SucceedTransactionCommand("GW-001"));

        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
        assertEquals("GW-001", transaction.getReferenceId());
        assertNull(transaction.getStatusReason());
    }

    @Test
    void testMarkSucceeded_withoutReferenceId() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.markSucceeded(transactionId, new SucceedTransactionCommand(null));

        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
        assertNull(transaction.getReferenceId());
    }

    @Test
    void testMarkFailed() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.markFailed(transactionId, new TransactionActionCommand("bank rejected"));

        assertEquals(TransactionStatus.FAILED, transaction.getStatus());
        assertEquals("bank rejected", transaction.getStatusReason());
        verify(transactionCache).invalidate(transactionId);
    }

    @Test
    void testCancel() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PENDING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.cancel(transactionId, new TransactionActionCommand("user canceled"));

        assertEquals(TransactionStatus.CANCELED, transaction.getStatus());
        assertEquals("user canceled", transaction.getStatusReason());
    }

    @Test
    void testRetryTransaction() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.FAILED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionId);

        transactionService.retryTransaction(transactionId);

        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        verify(transactionRepository).save(transaction);
        verify(transactionCache).invalidate(transactionId);
    }

    @Test
    void testInvalidTransition() {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.SUCCEEDED)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        BizException exception = assertThrows(BizException.class,
                () -> transactionService.startProcessing(transactionId));

        assertEquals(ErrorCode.INVALID_TRANSACTION_STATUS_TRANSITION.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

}

