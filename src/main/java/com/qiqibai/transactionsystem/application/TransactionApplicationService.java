package com.qiqibai.transactionsystem.application;

import com.qiqibai.transactionsystem.application.command.CreateTransactionCommand;
import com.qiqibai.transactionsystem.application.command.SucceedTransactionCommand;
import com.qiqibai.transactionsystem.application.command.TransactionActionCommand;
import com.qiqibai.transactionsystem.application.command.UpdateTransactionCommand;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionEvent;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.domain.transaction.TransactionStatus;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.presentation.response.TransactionEventResponse;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepository transactionRepository;
    private final TransactionCache transactionCache;
    private final TransactionEventLog transactionEventLog;

    public String createTransaction(CreateTransactionCommand command) {
        if (command.amount() != null && command.amount().compareTo(Transaction.MAX_AMOUNT) > 0) {
            throw new BizException(ErrorCode.AMOUNT_EXCEEDS_LIMIT);
        }
        if (Objects.nonNull(command.sourceId())
                && transactionRepository.findBySourceId(command.sourceId()).isPresent()) {
            log.error("The transaction already exists, sourceId {}", command.sourceId());
            throw new BizException(ErrorCode.DUPLICATED_TRANSACTION);
        }
        Transaction transaction = Transaction.builder()
                .amount(command.amount())
                .currency(command.currency())
                .description(command.description())
                .sourceId(command.sourceId())
                .type(command.type())
                .payerId(command.payerId())
                .payeeId(command.payeeId())
                .build();
        transactionRepository.save(transaction);
        transactionEventLog.record(TransactionEvent.created(transaction.getId()));
        return transaction.getId();
    }

    public void deleteTransaction(String id) {
        TransactionStatus previousStatus = updateTransaction(id, Transaction::archive);
        transactionEventLog.record(TransactionEvent.archived(id, previousStatus));
    }

    public void modifyTransaction(String id, UpdateTransactionCommand command) {
        TransactionStatus currentStatus = updateTransaction(id,
                transaction -> transaction.modify(command.amount(), command.description()));
        transactionEventLog.record(TransactionEvent.modified(id, currentStatus));
    }

    public void startProcessing(String id) {
        TransactionStatus previousStatus = updateTransaction(id, Transaction::startProcessing);
        transactionEventLog.record(TransactionEvent.statusTransition(
                id, previousStatus, TransactionStatus.PROCESSING, null, null));
    }

    public void markSucceeded(String id, SucceedTransactionCommand command) {
        String referenceId = command != null ? command.referenceId() : null;
        TransactionStatus previousStatus = updateTransaction(id,
                transaction -> transaction.markSucceeded(referenceId));
        transactionEventLog.record(TransactionEvent.statusTransition(
                id, previousStatus, TransactionStatus.SUCCEEDED, null, referenceId));
    }

    public void markFailed(String id, TransactionActionCommand command) {
        TransactionStatus previousStatus = updateTransaction(id,
                transaction -> transaction.markFailed(command.reason()));
        transactionEventLog.record(TransactionEvent.statusTransition(
                id, previousStatus, TransactionStatus.FAILED, command.reason(), null));
    }

    public void cancel(String id, TransactionActionCommand command) {
        TransactionStatus previousStatus = updateTransaction(id,
                transaction -> transaction.cancel(command.reason()));
        transactionEventLog.record(TransactionEvent.statusTransition(
                id, previousStatus, TransactionStatus.CANCELED, command.reason(), null));
    }

    public void retryTransaction(String id) {
        TransactionStatus previousStatus = updateTransaction(id, Transaction::retry);
        transactionEventLog.record(TransactionEvent.statusTransition(
                id, previousStatus, TransactionStatus.PENDING, null, null));
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

    public Page<TransactionQueryResponse> getAllTransactionsByPage(Pageable pageable, TransactionStatus status) {
        if (status != null) {
            return transactionRepository.findAll(pageable, status)
                    .map(TransactionQueryResponse::fromDomain);
        }
        return transactionRepository.findAll(pageable)
                .map(TransactionQueryResponse::fromDomain);
    }

    public List<TransactionEventResponse> getTransactionHistory(String id) {
        transactionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND));
        return transactionEventLog.findByTransactionId(id).stream()
                .map(TransactionEventResponse::fromDomain)
                .toList();
    }

    private TransactionStatus updateTransaction(String id, java.util.function.Consumer<Transaction> updater) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND));
        TransactionStatus previousStatus = transaction.getStatus();
        updater.accept(transaction);
        transactionRepository.save(transaction);
        transactionCache.invalidate(id);
        return previousStatus;
    }

}

