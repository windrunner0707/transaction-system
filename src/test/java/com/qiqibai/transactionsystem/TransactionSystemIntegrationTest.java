package com.qiqibai.transactionsystem;

import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateRetrieveAndTransitionTransaction() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 250.00,
                                  "currency": "USD",
                                  "description": "integration test",
                                  "sourceId": "int-src-1",
                                  "type": "PAYMENT",
                                  "payerId": "payer-A",
                                  "payeeId": "payee-B"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        // Read back
        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.sourceId").value("int-src-1"))
                .andExpect(jsonPath("$.type").value("PAYMENT"))
                .andExpect(jsonPath("$.payerId").value("payer-A"))
                .andExpect(jsonPath("$.payeeId").value("payee-B"))
                .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.name()))
                .andExpect(jsonPath("$.attemptCount").value(0))
                .andExpect(jsonPath("$.version").doesNotExist());

        // Transition to PROCESSING
        mockMvc.perform(post("/transactions/" + id + "/processing"))
                .andExpect(status().isOk());

        // Verify attemptCount incremented
        mockMvc.perform(get("/transactions/" + id))
                .andExpect(jsonPath("$.attemptCount").value(1));

        // Transition to SUCCEEDED with referenceId
        mockMvc.perform(post("/transactions/" + id + "/success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"referenceId": "GW-INTTEST-001"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.referenceId").value("GW-INTTEST-001"));
    }

    @Test
    void shouldReturn404ForNonExistentTransaction() throws Exception {
        mockMvc.perform(get("/transactions/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Error-001"))
                .andExpect(jsonPath("$.message").value("No transaction found."));
    }

    @Test
    void shouldReturn409ForDuplicateSourceId() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00, "currency": "EUR", "sourceId": "dup-src-int", "type": "TRANSFER"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00, "currency": "EUR", "sourceId": "dup-src-int", "type": "TRANSFER"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Error-002"));
    }

    @Test
    void shouldReturn422ForInvalidStateTransition() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50.00, "currency": "USD", "sourceId": "src-state-test", "type": "PAYMENT"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        // Cannot go directly PENDING -> SUCCEEDED
        mockMvc.perform(post("/transactions/" + id + "/success"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("Error-003"));
    }

    @Test
    void shouldReturn422WhenDeletingNonTerminalTransaction() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 20.00, "currency": "GBP", "sourceId": "src-delete-test", "type": "REFUND"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        // Cannot soft-delete a PENDING transaction
        mockMvc.perform(delete("/transactions/" + id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("Error-003"));
    }

    @Test
    void shouldSoftDeleteTerminalTransaction() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 30.00, "currency": "USD", "sourceId": "src-softdel", "type": "WITHDRAWAL"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        // Bring to terminal state (cancel)
        mockMvc.perform(post("/transactions/" + id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "test cleanup"}
                                """))
                .andExpect(status().isOk());

        // Soft delete
        mockMvc.perform(delete("/transactions/" + id))
                .andExpect(status().isOk());

        // Transaction should no longer be found
        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentTransaction() throws Exception {
        mockMvc.perform(delete("/transactions/ghost-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Error-001"));
    }

    @Test
    void shouldRetryFailedTransaction() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 75.00, "currency": "USD", "sourceId": "src-retry", "type": "PAYMENT"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        mockMvc.perform(post("/transactions/" + id + "/processing")).andExpect(status().isOk());
        mockMvc.perform(post("/transactions/" + id + "/failure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "timeout"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transactions/" + id + "/retry"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attemptCount").value(1));
    }

    @Test
    void shouldReturn422WhenMaxRetriesExceeded() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00, "currency": "USD", "sourceId": "src-max-retry", "type": "PAYMENT"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/transactions/" + id + "/processing")).andExpect(status().isOk());
            mockMvc.perform(post("/transactions/" + id + "/failure")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"reason": "error"}
                                    """))
                    .andExpect(status().isOk());
            if (i < 2) {
                mockMvc.perform(post("/transactions/" + id + "/retry")).andExpect(status().isOk());
            }
        }

        mockMvc.perform(post("/transactions/" + id + "/retry"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("Error-006"));
    }

    @Test
    void shouldReturn422WhenAmountExceedsLimit() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 9999999.99, "currency": "USD", "sourceId": "src-big", "type": "PAYMENT"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("Error-005"));
    }

    @Test
    void shouldReturnTransactionHistory() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00, "currency": "USD", "sourceId": "src-history", "type": "DEPOSIT"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        mockMvc.perform(get("/transactions/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$[0].newStatus").value("PENDING"));
    }

    @Test
    void shouldReturnPagedTransactions() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldFilterTransactionsByStatus() throws Exception {
        // Create a PENDING transaction
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 5.00, "currency": "USD", "sourceId": "src-filter", "type": "PAYMENT"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

}

