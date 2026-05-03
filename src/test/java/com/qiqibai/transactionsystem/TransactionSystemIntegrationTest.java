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
                                {"amount": 250.00, "description": "integration test", "sourceId": "int-src-1"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String id = createResult.getResponse().getContentAsString();

        // Read back
        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.sourceId").value("int-src-1"))
                .andExpect(jsonPath("$.status").value(TransactionStatus.PENDING.name()));

        // Transition to PROCESSING
        mockMvc.perform(post("/transactions/" + id + "/processing"))
                .andExpect(status().isOk());

        // Transition to SUCCEEDED
        mockMvc.perform(post("/transactions/" + id + "/success"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCEEDED.name()));
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
                                {"amount": 10.00, "sourceId": "dup-src"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 10.00, "sourceId": "dup-src"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("Error-002"));
    }

    @Test
    void shouldReturn422ForInvalidStateTransition() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 50.00}
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
    void shouldReturn404WhenDeletingNonExistentTransaction() throws Exception {
        mockMvc.perform(delete("/transactions/ghost-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("Error-001"));
    }

    @Test
    void shouldReturnPagedTransactions() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

}
