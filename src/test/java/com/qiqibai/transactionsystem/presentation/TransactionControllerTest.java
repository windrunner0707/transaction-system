package com.qiqibai.transactionsystem.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiqibai.transactionsystem.application.TransactionApplicationService;
import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.exception.GlobalExceptionHandler;
import com.qiqibai.transactionsystem.presentation.request.TransactionActionRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionCreateRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionUpdateRequest;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerTest {

    private final TransactionApplicationService transactionApplicationService = mock(TransactionApplicationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionApplicationService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateTransaction() throws Exception {
        when(transactionApplicationService.createTransaction(any(CreateTransactionCommand.class))).thenReturn("tx-1");

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 100.50,
                                  "description": "new transaction",
                                  "sourceId": "source-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("tx-1"));

        verify(transactionApplicationService).createTransaction(any(CreateTransactionCommand.class));
    }

    @Test
    void shouldRejectInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "missing amount"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").value("amount should not be null"));
    }

    @Test
    void shouldRejectNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": -10,
                                  "description": "negative amount"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").value("amount must be positive"));
    }

    @Test
    void shouldDeleteTransaction() throws Exception {
        mockMvc.perform(delete("/transactions/tx-1"))
                .andExpect(status().isOk());

        verify(transactionApplicationService).deleteTransaction("tx-1");
    }

    @Test
    void shouldModifyTransaction() throws Exception {
        mockMvc.perform(patch("/transactions/tx-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransactionUpdateRequest(BigDecimal.valueOf(42), "updated"))))
                .andExpect(status().isOk());

        verify(transactionApplicationService).modifyTransaction(eq("tx-1"), any(UpdateTransactionCommand.class));
    }

    @Test
    void shouldStartProcessingTransaction() throws Exception {
        mockMvc.perform(post("/transactions/tx-1/processing"))
                .andExpect(status().isOk());

        verify(transactionApplicationService).startProcessing("tx-1");
    }

    @Test
    void shouldMarkTransactionSucceeded() throws Exception {
        mockMvc.perform(post("/transactions/tx-1/success"))
                .andExpect(status().isOk());

        verify(transactionApplicationService).markSucceeded("tx-1");
    }

    @Test
    void shouldMarkTransactionFailed() throws Exception {
        mockMvc.perform(post("/transactions/tx-1/failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionActionRequest("declined"))))
                .andExpect(status().isOk());

        verify(transactionApplicationService).markFailed(eq("tx-1"), any(TransactionActionCommand.class));
    }

    @Test
    void shouldCancelTransaction() throws Exception {
        mockMvc.perform(post("/transactions/tx-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransactionActionRequest("duplicate"))))
                .andExpect(status().isOk());

        verify(transactionApplicationService).cancel(eq("tx-1"), any(TransactionActionCommand.class));
    }

    @Test
    void shouldRejectBlankActionReason() throws Exception {
        mockMvc.perform(post("/transactions/tx-1/failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("reason should not be blank"));
    }

    @Test
    void shouldReturnTransactionById() throws Exception {
        when(transactionApplicationService.getTransactionById("tx-1")).thenReturn(TransactionQueryResponse.builder()
                .id("tx-1")
                .amount(BigDecimal.valueOf(99))
                .description("stored")
                .sourceId("src-1")
                .status(TransactionStatus.PROCESSING)
                .statusReason("review")
                .version(2L)
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2024, 1, 1, 10, 5))
                .build());

        mockMvc.perform(get("/transactions/tx-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tx-1"))
                .andExpect(jsonPath("$.amount").value(99))
                .andExpect(jsonPath("$.sourceId").value("src-1"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.statusReason").value("review"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void shouldReturnPagedTransactions() throws Exception {
        when(transactionApplicationService.getAllTransactionsByPage(any())).thenReturn(new PageImpl<>(
                List.of(
                        TransactionQueryResponse.builder().id("tx-1").amount(BigDecimal.ONE).status(TransactionStatus.PENDING).build(),
                        TransactionQueryResponse.builder().id("tx-2").amount(BigDecimal.TEN).status(TransactionStatus.SUCCEEDED).build()
                ),
                PageRequest.of(1, 2),
                5
        ));

        mockMvc.perform(get("/transactions?page=1&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("tx-1"))
                .andExpect(jsonPath("$.content[1].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void shouldTranslateBizExceptionToBadRequest() throws Exception {
        when(transactionApplicationService.getTransactionById("missing"))
                .thenThrow(new BizException(ErrorCode.NO_TRANSACTION_FOUND));

        mockMvc.perform(get("/transactions/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No transaction found."))
                .andExpect(jsonPath("$.code").value("Error-001"));
    }
}
