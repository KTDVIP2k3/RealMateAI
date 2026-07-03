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

import java.util.HashMap;
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
    public ResponseEntity<ApiResponse> getMyTransactions() {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            List<Transaction> transactions = currentAccount.getTransactions();

            if (transactions == null) {
                transactions = java.util.Collections.emptyList();
            }

            List<Map<String, Object>> responseList = transactions.stream()
                    .sorted((t1, t2) -> {
                        if (t1.getTransactionDate() == null || t2.getTransactionDate() == null) return 0;
                        return t2.getTransactionDate().compareTo(t1.getTransactionDate());
                    })
                    .map(t -> {
                        Map<String, Object> item = new java.util.HashMap<>();
                        item.put("transactionId", t.getTransactionId());
                        item.put("totalAmount", t.getTotalAmount() != null ? t.getTotalAmount() : 0L);
                        item.put("transactionType", t.getTransactionType() != null ? t.getTransactionType().name() : "UNKNOWN");
                        item.put("transactionStatus", t.getTransactionStatus() != null ? t.getTransactionStatus() : "PENDING");
                        item.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
                        item.put("contentDescription", t.getContentDescription() != null ? t.getContentDescription() : "");
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(responseList, "Lấy lịch sử giao dịch ví thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}