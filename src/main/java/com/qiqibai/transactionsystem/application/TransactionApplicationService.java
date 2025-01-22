package com.qiqibai.transactionsystem.application;

import com.google.common.cache.Cache;
import com.qiqibai.transactionsystem.domain.transaction.Transaction;
import com.qiqibai.transactionsystem.domain.transaction.TransactionRepository;
import com.qiqibai.transactionsystem.exception.BizException;
import com.qiqibai.transactionsystem.exception.ErrorCode;
import com.qiqibai.transactionsystem.presentation.request.TransactionCreateRequest;
import com.qiqibai.transactionsystem.presentation.request.TransactionUpdateRequest;
import com.qiqibai.transactionsystem.presentation.response.TransactionQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionApplicationService {

    private final TransactionRepository transactionRepository;
    private final Cache<String, Object> transactionCache;

    public String createTransaction(TransactionCreateRequest request) {
        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .description(request.getDescription())
                .build();
        transactionRepository.save(transaction);
        return transaction.getId();
    }

    public void deleteTransaction(String id) {
        transactionRepository.delete(id);
    }

    public void modifyTransaction(String id, TransactionUpdateRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND.getErrorMsg()));
        transaction.modify(request.getAmount(), request.getDescription());
        transactionRepository.save(transaction);
    }

    public TransactionQueryResponse getTransactionById(String id) {
        Transaction transaction = (Transaction) transactionCache.getIfPresent(id);
        if (Objects.isNull(transaction)) {
            log.info("Cannot find transaction {} from cache, try to find in db", id);
            transaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new BizException(ErrorCode.NO_TRANSACTION_FOUND.getErrorMsg()));
            transactionCache.put(id, transaction);
        }
        return TransactionQueryResponse.fromDomain(transaction);
    }

    public Page<TransactionQueryResponse> getAllTransactionsByPage(Pageable pageable) {
        List<TransactionQueryResponse> allTransactions = transactionRepository.findAll()
                .stream()
                .map(TransactionQueryResponse::fromDomain)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTransactions.size());

        if (start >= allTransactions.size()) {
            return new PageImpl<>(List.of(), pageable, allTransactions.size());
        }

        List<TransactionQueryResponse> pageContent = allTransactions.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allTransactions.size());
    }

}
