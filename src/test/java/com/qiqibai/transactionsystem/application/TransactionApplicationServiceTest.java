package com.qiqibai.transactionsystem.application;

import com.google.common.cache.Cache;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.presentation.request.TransactionActionRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionCreateRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionUpdateRequest;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionApplicationServiceTest {

    private TransactionApplicationService transactionService;
    private TransactionRepository transactionRepository;
    private Cache<String, Object> transactionCache;


    @BeforeEach
    void setUp() {
        transactionRepository = Mockito.mock(TransactionRepository.class);
        transactionCache = mock(Cache.class);
        transactionService = new TransactionApplicationService(transactionRepository, transactionCache);
    }

    @Test
    void testCreateTransaction() {
        // Arrange
        TransactionCreateRequest request = TransactionCreateRequest.builder()
                .amount(BigDecimal.valueOf(100.0))
                .description("Test transaction")
                .sourceId("Test sourceId")
                .build();

        when(transactionRepository.findBySourceId("Test sourceId")).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenReturn("id1");

        // Act
        String transactionId = transactionService.createTransaction(request);

        // Assert
        assertNotNull(transactionId);
        ArgumentCaptor<Transaction> transactionArgumentCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionArgumentCaptor.capture());
        verify(transactionRepository).findBySourceId("Test sourceId");
        assertEquals(TransactionStatus.PENDING, transactionArgumentCaptor.getValue().getStatus());
        assertEquals("Test sourceId", transactionArgumentCaptor.getValue().getSourceId());
    }

    @Test
    void testCreateTransaction_duplicatedTransaction() {
        // Arrange
        TransactionCreateRequest request = TransactionCreateRequest.builder()
                .amount(BigDecimal.valueOf(100.0))
                .description("Test transaction")
                .sourceId("Test sourceId")
                .build();

        Transaction transaction = Transaction.builder()
                .sourceId("Test sourceId")
                .build();

        when(transactionRepository.findBySourceId("Test sourceId")).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any())).thenReturn("id1");

        // Act
        BizException exception = assertThrows(BizException.class, () -> transactionService.createTransaction(request));

        // Assert
        assertEquals(ErrorCode.DUPLICATED_TRANSACTION.getErrorMsg(), exception.getMessage());
        verify(transactionRepository, times(1)).findBySourceId("Test sourceId");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testDeleteTransaction() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();

        doNothing().when(transactionRepository).delete(transactionId);

        // Act
        transactionService.deleteTransaction(transactionId);

        // Assert
        verify(transactionRepository, times(1)).delete(transactionId);
        verify(transactionCache, times(1)).invalidate(transactionId);
    }

    @Test
    void testModifyTransaction() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();
        TransactionUpdateRequest request = new TransactionUpdateRequest(BigDecimal.valueOf(200.0), "Updated Description");
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .amount(BigDecimal.valueOf(100.0))
                .description("Old Description")
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction.getId());

        // Act
        transactionService.modifyTransaction(transactionId, request);

        // Assert
        assertEquals(request.getAmount(), transaction.getAmount());
        assertEquals(request.getDescription(), transaction.getDescription());
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionRepository, times(1)).save(transaction);
        verify(transactionCache, times(1)).invalidate(transactionId);
    }

    @Test
    void testModifyTransaction_invalidState() {
        String transactionId = UUID.randomUUID().toString();
        TransactionUpdateRequest request = new TransactionUpdateRequest(BigDecimal.valueOf(200.0), "Updated Description");
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .status(TransactionStatus.PROCESSING)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        BizException exception = assertThrows(BizException.class,
                () -> transactionService.modifyTransaction(transactionId, request));

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

        when(transactionCache.getIfPresent(transactionId)).thenReturn(transaction);

        // Act
        TransactionQueryResponse response = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(response);
        assertEquals(transactionId, response.getId());
        assertEquals(TransactionStatus.PENDING, response.getStatus());
        verify(transactionCache, times(1)).getIfPresent(transactionId);
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

        when(transactionCache.getIfPresent(transactionId)).thenReturn(null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        TransactionQueryResponse response = transactionService.getTransactionById(transactionId);

        // Assert
        assertNotNull(response);
        assertEquals(transactionId, response.getId());
        assertEquals(TransactionStatus.PENDING, response.getStatus());
        verify(transactionCache, times(1)).getIfPresent(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
        verify(transactionCache, times(1)).put(transactionId, transaction);
    }

    @Test
    void testGetTransactionById_notFound() {
        // Arrange
        String transactionId = UUID.randomUUID().toString();

        when(transactionCache.getIfPresent(transactionId)).thenReturn(null);
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        BizException exception = assertThrows(BizException.class,
                () -> transactionService.getTransactionById(transactionId));
        assertEquals(ErrorCode.NO_TRANSACTION_FOUND.getErrorMsg(), exception.getMessage());
        verify(transactionCache, times(1)).getIfPresent(transactionId);
        verify(transactionRepository, times(1)).findById(transactionId);
    }

    @Test
    void testGetAllTransactionsByPage_normalPagination() {
        // Arrange
        List<Transaction> mockTransactions = IntStream.range(0, 10)
                .mapToObj(i ->
                        Transaction.builder()
                                .id(UUID.randomUUID().toString())
                                .amount(BigDecimal.valueOf(i * 100.0))
                                .description("Description " + i)
                                .build())
                .collect(Collectors.toList());
        when(transactionRepository.findAll()).thenReturn(mockTransactions);

        Pageable pageable = PageRequest.of(1, 3); // Page 1 with 3 items per page

        // Act
        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable);

        // Assert
        assertEquals(3, result.getSize()); // Page size
        assertEquals(10, result.getTotalElements()); // Total number of elements
        assertEquals(BigDecimal.valueOf(300.0), result.getContent().get(0).getAmount()); // Verify content (e.g., amount)
        verify(transactionRepository, times(1)).findAll(); // Ensure repository is called once
    }

    @Test
    void testGetAllTransactionsByPage_lastPage() {
        // Arrange
        List<Transaction> mockTransactions = IntStream.range(0, 10)
                .mapToObj(i -> Transaction.builder()
                        .id(UUID.randomUUID().toString())
                        .amount(BigDecimal.valueOf(i * 100.0))
                        .description("Description " + i)
                        .build())
                .collect(Collectors.toList());
        when(transactionRepository.findAll()).thenReturn(mockTransactions);

        Pageable pageable = PageRequest.of(2, 4); // Page 1 with 4 items per page

        // Act
        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable);

        // Assert
        assertEquals(4, result.getSize()); // Remaining items
        assertEquals(10, result.getTotalElements()); // Total elements
        assertEquals(BigDecimal.valueOf(800.0), result.getContent().get(0).getAmount()); // Verify amount
        verify(transactionRepository, times(1)).findAll();
    }

    @Test
    void testGetAllTransactionsByPage_outOfRange() {
        // Arrange
        List<Transaction> mockTransactions = IntStream.range(0, 10)
                .mapToObj(i -> Transaction.builder()
                        .id(UUID.randomUUID().toString())
                        .amount(BigDecimal.valueOf(i * 100.0))
                        .description("Description " + i)
                        .build())
                .collect(Collectors.toList());
        when(transactionRepository.findAll()).thenReturn(mockTransactions);

        Pageable pageable = PageRequest.of(5, 3); // Out of range (page 5 with 3 items per page)

        // Act
        Page<TransactionQueryResponse> result = transactionService.getAllTransactionsByPage(pageable);

        // Assert
        assertEquals(3, result.getSize()); // No items
        assertEquals(10, result.getTotalElements()); // Total elements
        assertEquals(0, result.getContent().size()); // Empty content
        verify(transactionRepository, times(1)).findAll();
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

        transactionService.markSucceeded(transactionId);

        assertEquals(TransactionStatus.SUCCEEDED, transaction.getStatus());
        assertNull(transaction.getStatusReason());
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

        transactionService.markFailed(transactionId, new TransactionActionRequest("bank rejected"));

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

        transactionService.cancel(transactionId, new TransactionActionRequest("user canceled"));

        assertEquals(TransactionStatus.CANCELED, transaction.getStatus());
        assertEquals("user canceled", transaction.getStatusReason());
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
