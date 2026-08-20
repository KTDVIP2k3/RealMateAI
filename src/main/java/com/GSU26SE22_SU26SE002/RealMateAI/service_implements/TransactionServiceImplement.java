package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MyTransactionResponseDto;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.TransactionDetailDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.TransactionSummaryDto;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.TransactionServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
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
                        .body(ApiResponse.fail(
                                "UNAUTHORIZED",
                                "Người dùng chưa đăng nhập"
                        ));
            }

            BigDecimal totalDeposit = countTotalDeposit(currentAccount);
            BigDecimal totalSpent = countTotalSpent(currentAccount);

            List<Transaction> allTransactions = currentAccount.getTransactions();

            if (allTransactions == null) {
                allTransactions = Collections.emptyList();
            }

            List<Transaction> sortedTransactions = allTransactions.stream()
                    .sorted(Comparator.comparing(
                            Transaction::getTransactionDate,
                            Comparator.nullsLast(Comparator.reverseOrder())
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
                        org.springframework.data.domain.PageRequest.of(
                                effectivePage,
                                effectiveSize
                        );

                content = sortedTransactions.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .map(this::convertToMap)
                        .collect(Collectors.toList());

                org.springframework.data.domain.Page<Map<String, Object>> transactionPage =
                        new org.springframework.data.domain.PageImpl<>(
                                content,
                                pageable,
                                totalElements
                        );

                effectivePage = transactionPage.getNumber();
                effectiveSize = transactionPage.getSize();
                totalPages = transactionPage.getTotalPages();
                isLast = transactionPage.isLast();
            }

            MyTransactionResponseDto responseDto = MyTransactionResponseDto.builder()
                    .totalDeposit(totalDeposit)
                    .totalSpent(totalSpent)
                    .content(content)
                    .page(effectivePage)
                    .size(effectiveSize)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .last(isLast)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(
                            responseDto,
                            "Lấy lịch sử giao dịch ví thành công"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(
                            "SERVER_ERROR",
                            "Lỗi hệ thống: " + e.getMessage()
                    ));
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getTransactionsByAdminOrStaff(int page, int size) {
        try {
            List<Transaction> allTransactions = transactionRepository.findAll();
            if (allTransactions == null) {
                allTransactions = Collections.emptyList();
            }

            return processPagination(allTransactions, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getMyTransactionsByType(int page, int size, String type) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<Transaction> allTransactions = currentAccount.getTransactions();
            if (allTransactions == null) {
                allTransactions = Collections.emptyList();
            }

            TransactionTypeEnum targetType = parseTransactionType(type);
            if (targetType != null) {
                allTransactions = allTransactions.stream()
                        .filter(t -> t.getTransactionType() == targetType)
                        .toList();
            }

            return processPagination(allTransactions, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getTransactionsByAdminOrStaffByType(int page, int size, String type) {
        try {
            List<Transaction> allTransactions = transactionRepository.findAll();
            if (allTransactions == null) {
                allTransactions = Collections.emptyList();
            }

            TransactionTypeEnum targetType = parseTransactionType(type);
            if (targetType != null) {
                allTransactions = allTransactions.stream()
                        .filter(t -> t.getTransactionType() == targetType)
                        .toList();
            }

            return processPagination(allTransactions, page, size);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getTransactionDetailById(Integer transactionId) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("NOT_FOUND", "Không tìm thấy thông tin chi tiết giao dịch này"));
            }

            TransactionDetailDTO detailDTO = convertToDetailDTO(transactionOpt.get());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(detailDTO, "Lấy chi tiết giao dịch thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private TransactionTypeEnum parseTransactionType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return null;
        }
        try {
            return TransactionTypeEnum.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ResponseEntity<ApiResponse> processPagination(List<Transaction> list, int page, int size) {
        List<Transaction> sortedTransactions = list.stream()
                .sorted(Comparator.comparing(
                        Transaction::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", effectivePage);
        result.put("size", effectiveSize);
        result.put("totalElements", totalElements);
        result.put("totalPages", totalPages);
        result.put("last", isLast);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(result, "Lấy lịch sử giao dịch ví thành công"));
    }

    private Map<String, Object> convertToMap(Transaction t) {
        Map<String, Object> item = new HashMap<>();
        item.put("transactionId", t.getTransactionId());
        item.put("totalAmount", t.getTotalAmount() != null ? t.getTotalAmount() : 0L);
        item.put("transactionType", t.getTransactionType() != null ? t.getTransactionType().name() : "UNKNOWN");
        item.put("transactionStatus", t.getTransactionStatus() != null ? t.getTransactionStatus() : "PENDING");
        item.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        item.put("contentDescription", t.getContentDescription() != null ? t.getContentDescription() : "");
        item.put("checkoutUrl", t.getCheckoutUrl() != null ? t.getCheckoutUrl() : "");
        return item;
    }

    private BigDecimal countTotalSpent(Account account) {
        List<Transaction> transactionList = account.getTransactions();

        BigDecimal totalSpent = BigDecimal.ZERO;

        if (account.getRole() == RoleEnum.Seller) {
            for (Transaction transaction : transactionList) {
                if (transaction.getTotalAmount() != null) {
                    if (TransactionTypeEnum.WALLET_WITHDRAWAL.equals(transaction.getTransactionType())
                            || TransactionTypeEnum.POSTING_PACKAGE_PAYMENT.equals(transaction.getTransactionType())
                            || TransactionTypeEnum.POSTING_PACKAGE_RENEWAL.equals(transaction.getTransactionType())) {

                        totalSpent = totalSpent.add(transaction.getTotalAmount());
                    }
                }
            }
        }

        if (account.getRole() == RoleEnum.Investor) {
            for (Transaction transaction : transactionList) {
                if (transaction.getTotalAmount() != null) {
                    if (TransactionTypeEnum.WALLET_WITHDRAWAL.equals(transaction.getTransactionType())
                            || TransactionTypeEnum.MEMBERSHIP_PAYMENT.equals(transaction.getTransactionType())
                            || TransactionTypeEnum.MEMBERSHIP_RENEWAL.equals(transaction.getTransactionType())) {

                        totalSpent = totalSpent.add(transaction.getTotalAmount());
                    }
                }
            }
        }

        return totalSpent;
    }

    private BigDecimal countTotalDeposit(Account account){
        List<Transaction> transactionList = account.getTransactions();

        BigDecimal totalDeposit = BigDecimal.ZERO;

        for(Transaction transaction : transactionList){
            if(transaction.getTransactionType().equals(TransactionTypeEnum.WALLET_DEPOSIT)){
                totalDeposit = totalDeposit.add(transaction.getTotalAmount());
            }
        }

        return totalDeposit;
    }


private TransactionDetailDTO convertToDetailDTO(Transaction t) {
    TransactionDetailDTO dto = new TransactionDetailDTO();
    if (t.getWallet() != null && t.getWallet().getAccount() != null) {
        dto.setFullName(t.getWallet().getAccount().getFull_name());
        dto.setPhone(t.getWallet().getAccount().getPhone());
    }
    dto.setTransactionType(t.getTransactionType());
    dto.setTransactionDate(t.getTransactionDate() != null ? t.getTransactionDate() : t.getCreatedAt());
    dto.setTotalAmount(t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO);
    dto.setContentDescription(t.getContentDescription() != null ? t.getContentDescription() : "");
    dto.setTransactionStatus(t.getTransactionStatus() != null ? t.getTransactionStatus() : "PENDING");
    return dto;
}
}