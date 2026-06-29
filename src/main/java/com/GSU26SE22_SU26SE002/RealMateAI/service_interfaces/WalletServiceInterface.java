package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import java.math.BigDecimal;

public interface WalletServiceInterface {
    ResponseEntity<ApiResponse> initiateDeposit(Long amount);
    ResponseEntity<ApiResponse> handlePayOSWebhook(String orderCode, String status);
    ResponseEntity<ApiResponse> requestWithdrawal(BigDecimal amount, String bankName, String bankAccountNumber);
    ResponseEntity<ApiResponse> reviewWithdrawRequest(Integer withdrawalId, String status, String note);
}