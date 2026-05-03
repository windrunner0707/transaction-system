package com.qiqibai.transactionsystem.presentation;

import com.qiqibai.transactionsystem.application.TransactionApplicationService;
import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.SucceedTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.presentation.request.TransactionActionRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionCreateRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionSuccessRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionUpdateRequest;
import com.qiqibai.transactionsystem.presentation.response.TransactionEventResponse;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;

    @PostMapping
    public String createTransaction(@RequestBody @Valid TransactionCreateRequest request) {
        return transactionApplicationService.createTransaction(
                new CreateTransactionCommand(
                        request.getAmount(),
                        request.getCurrency(),
                        request.getDescription(),
                        request.getSourceId(),
                        request.getType(),
                        request.getPayerId(),
                        request.getPayeeId()));
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable String id) {
        transactionApplicationService.deleteTransaction(id);
    }

    @PatchMapping("/{id}")
    public void modifyTransaction(@PathVariable String id, @RequestBody @Valid TransactionUpdateRequest request) {
        transactionApplicationService.modifyTransaction(id,
                new UpdateTransactionCommand(request.getAmount(), request.getDescription()));
    }

    @PostMapping("/{id}/processing")
    public void startProcessing(@PathVariable String id) {
        transactionApplicationService.startProcessing(id);
    }

    @PostMapping("/{id}/success")
    public void markSucceeded(@PathVariable String id,
                              @RequestBody(required = false) TransactionSuccessRequest request) {
        transactionApplicationService.markSucceeded(id,
                new SucceedTransactionCommand(request != null ? request.getReferenceId() : null));
    }

    @PostMapping("/{id}/failure")
    public void markFailed(@PathVariable String id, @RequestBody @Valid TransactionActionRequest request) {
        transactionApplicationService.markFailed(id, new TransactionActionCommand(request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable String id, @RequestBody @Valid TransactionActionRequest request) {
        transactionApplicationService.cancel(id, new TransactionActionCommand(request.getReason()));
    }

    @PostMapping("/{id}/retry")
    public void retryTransaction(@PathVariable String id) {
        transactionApplicationService.retryTransaction(id);
    }

    @GetMapping("/{id}")
    public TransactionQueryResponse getTransactionById(@PathVariable String id) {
        return transactionApplicationService.getTransactionById(id);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionQueryResponse>> getAllTransactionsByPage(
            @PageableDefault() Pageable pageable,
            @RequestParam(required = false) TransactionStatus status) {
        return ResponseEntity.ok(transactionApplicationService.getAllTransactionsByPage(pageable, status));
    }

    @GetMapping("/{id}/history")
    public List<TransactionEventResponse> getTransactionHistory(@PathVariable String id) {
        return transactionApplicationService.getTransactionHistory(id);
    }

}

