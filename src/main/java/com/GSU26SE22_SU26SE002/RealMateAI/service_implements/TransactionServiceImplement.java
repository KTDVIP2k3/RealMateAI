package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.TransactionServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImplement implements TransactionServiceInterface {

    private final TransactionRepository transactionRepository;
    private final AuthenUntil authenUntil;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getMyTransactions(int page, int size) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<Transaction> allTransactions = currentAccount.getTransactions();
            if (allTransactions == null) {
                allTransactions = java.util.Collections.emptyList();
            }

            List<Transaction> sortedTransactions = allTransactions.stream()
                    .sorted(java.util.Comparator.comparing(
                            Transaction::getTransactionDate,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<Map<String, Object>> content;
            int effectivePage = 0;
            int effectiveSize = sortedTransactions.size();
            int totalElements = sortedTransactions.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                content = sortedTransactions.stream()
                        .map(this::convertToMap)
                        .collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                org.springframework.data.domain.Pageable pageable =
                        org.springframework.data.domain.PageRequest.of(effectivePage, effectiveSize);

                content = sortedTransactions.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .map(this::convertToMap)
                        .collect(Collectors.toList());

                org.springframework.data.domain.Page<Map<String, Object>> transactionPage =
                        new org.springframework.data.domain.PageImpl<>(content, pageable, totalElements);

                effectivePage = transactionPage.getNumber();
                effectiveSize = transactionPage.getSize();
                totalPages = transactionPage.getTotalPages();
                isLast = transactionPage.isLast();
            }

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("content", content);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(result, "Lấy lịch sử giao dịch ví thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getTransactionsByAdminOrStaff(int page, int size) {
        try {


            List<Transaction> allTransactions = transactionRepository.findAll();
            if (allTransactions == null) {
                allTransactions = java.util.Collections.emptyList();
            }

            List<Transaction> sortedTransactions = allTransactions.stream()
                    .sorted(java.util.Comparator.comparing(
                            Transaction::getTransactionDate,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<Map<String, Object>> content;
            int effectivePage = 0;
            int effectiveSize = sortedTransactions.size();
            int totalElements = sortedTransactions.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                content = sortedTransactions.stream()
                        .map(this::convertToMap)
                        .collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                org.springframework.data.domain.Pageable pageable =
                        org.springframework.data.domain.PageRequest.of(effectivePage, effectiveSize);

                content = sortedTransactions.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .map(this::convertToMap)
                        .collect(Collectors.toList());

                org.springframework.data.domain.Page<Map<String, Object>> transactionPage =
                        new org.springframework.data.domain.PageImpl<>(content, pageable, totalElements);

                effectivePage = transactionPage.getNumber();
                effectiveSize = transactionPage.getSize();
                totalPages = transactionPage.getTotalPages();
                isLast = transactionPage.isLast();
            }

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("content", content);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(result, "Lấy lịch sử giao dịch ví thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }


    private Map<String, Object> convertToMap(Transaction t) {
        Map<String, Object> item = new java.util.HashMap<>();
        item.put("transactionId", t.getTransactionId());
        item.put("totalAmount", t.getTotalAmount() != null ? t.getTotalAmount() : 0L);
        item.put("transactionType", t.getTransactionType() != null ? t.getTransactionType().name() : "UNKNOWN");
        item.put("transactionStatus", t.getTransactionStatus() != null ? t.getTransactionStatus() : "PENDING");
        item.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        item.put("contentDescription", t.getContentDescription() != null ? t.getContentDescription() : "");
        item.put("checkoutUrl", t.getCheckoutUrl() != null ? t.getCheckoutUrl() : "");
        return item;
    }


}