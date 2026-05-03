package com.qiqibai.transactionsystem.application;

import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepository transactionRepository;
    private final TransactionCache transactionCache;

    public String createTransaction(CreateTransactionCommand command) {
        if (Objects.nonNull(command.sourceId())
                && transactionRepository.findBySourceId(command.sourceId()).isPresent()) {
            log.error("The transaction already exists, sourceId {}", command.sourceId());
            throw new BizException(ErrorCode.DUPLICATED_TRANSACTION);
        }
        Transaction transaction = Transaction.builder()
                .amount(command.amount())
                .description(command.description())
                .sourceId(command.sourceId())
                .build();
        transactionRepository.save(transaction);
        return transaction.getId();
    }

    public void deleteTransaction(String id) {
        transactionRepository.delete(id);
        transactionCache.invalidate(id);
    }

    public void modifyTransaction(String id, UpdateTransactionCommand command) {
        updateTransaction(id, transaction -> transaction.modify(command.amount(), command.description()));
    }

    public void startProcessing(String id) {
        updateTransaction(id, Transaction::startProcessing);
    }

    public void markSucceeded(String id) {
        updateTransaction(id, Transaction::markSucceeded);
    }

    public void markFailed(String id, TransactionActionCommand command) {
        updateTransaction(id, transaction -> transaction.markFailed(command.reason()));
    }

    public void cancel(String id, TransactionActionCommand command) {
        updateTransaction(id, transaction -> transaction.cancel(command.reason()));
    }

    public TransactionQueryResponse getTransactionById(String id) {
        return transactionCache.get(id)
                .map(TransactionQueryResponse::fromDomain)
                .orElseGet(() -> {
                    log.info("Cannot find transaction {} in cache, loading from repository", id);
                    Transaction transaction = transactionRepository.findById(id)
                            .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND));
                    transactionCache.put(id, transaction);
                    return TransactionQueryResponse.fromDomain(transaction);
                });
    }

    public Page<TransactionQueryResponse> getAllTransactionsByPage(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(TransactionQueryResponse::fromDomain);
    }

    private void updateTransaction(String id, java.util.function.Consumer<Transaction> updater) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND));
        updater.accept(transaction);
        transactionRepository.save(transaction);
        transactionCache.invalidate(id);
    }

}
