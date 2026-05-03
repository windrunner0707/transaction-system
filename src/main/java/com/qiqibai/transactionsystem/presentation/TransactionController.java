package com.qiqibai.transactionsystem.presentation;

import com.qiqibai.transactionsystem.application.TransactionApplicationService;
import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.presentation.request.TransactionActionRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionCreateRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionUpdateRequest;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;

    @PostMapping
    public String createTransaction(@RequestBody @Valid TransactionCreateRequest request) {
        return transactionApplicationService.createTransaction(
                new CreateTransactionCommand(request.getAmount(), request.getDescription(), request.getSourceId()));
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
    public void markSucceeded(@PathVariable String id) {
        transactionApplicationService.markSucceeded(id);
    }

    @PostMapping("/{id}/failure")
    public void markFailed(@PathVariable String id, @RequestBody @Valid TransactionActionRequest request) {
        transactionApplicationService.markFailed(id, new TransactionActionCommand(request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable String id, @RequestBody @Valid TransactionActionRequest request) {
        transactionApplicationService.cancel(id, new TransactionActionCommand(request.getReason()));
    }

    @GetMapping("/{id}")
    public TransactionQueryResponse getTransactionById(@PathVariable String id) {
        return transactionApplicationService.getTransactionById(id);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionQueryResponse>> getAllTransactionsByPage(@PageableDefault() Pageable pageable) {
        return ResponseEntity.ok(transactionApplicationService.getAllTransactionsByPage(pageable));
    }

}
