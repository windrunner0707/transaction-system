package com.qiqibai.transactionsystem.presentation;

import com.qiqibai.transactionsystem.application.TransactionApplicationService;
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

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionApplicationService transactionApplicationService;

    @GetMapping("/li-qiang")
    public String getLiQiang() {
        return "Object extends Liqiang";
    }

    @PostMapping
    public String createTransaction(@RequestBody @Valid TransactionCreateRequest request) {
        return transactionApplicationService.createTransaction(request);
    }

    @DeleteMapping("/{id}")
    public void deleteTransaction(@PathVariable String id) {
        transactionApplicationService.deleteTransaction(id);
    }

    @PatchMapping("/{id}")
    public void modifyTransaction(@PathVariable String id, @RequestBody @Valid TransactionUpdateRequest request) {
        transactionApplicationService.modifyTransaction(id, request);
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
        transactionApplicationService.markFailed(id, request);
    }

    @PostMapping("/{id}/cancel")
    public void cancel(@PathVariable String id, @RequestBody @Valid TransactionActionRequest request) {
        transactionApplicationService.cancel(id, request);
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
