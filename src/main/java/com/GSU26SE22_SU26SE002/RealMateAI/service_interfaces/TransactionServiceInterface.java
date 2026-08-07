package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface TransactionServiceInterface {
    ResponseEntity<ApiResponse> getMyTransactions(int page, int size);
    ResponseEntity<ApiResponse> getTransactionsByAdminOrStaff(int page, int size);
    ResponseEntity<ApiResponse> getTransactionDetailById(Integer transactionId);
    ResponseEntity<ApiResponse> getMyTransactionsByType(int page, int size, String type);
    ResponseEntity<ApiResponse> getTransactionsByAdminOrStaffByType(int page, int size, String type);
}