package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface WalletWithDrawlServiceInterface {
    ResponseEntity<ApiResponse> getWalletWithdrawalByAdmin(int page, int size);
    ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSeller(int page, int size);
    ResponseEntity<ApiResponse> getWalletWithdrawalByAdminStatus(int page, int size, String status);
    ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSellerByStatus(int page, int size, String status);
    ResponseEntity<ApiResponse> getWalletWithdrawalDetailById(Integer walletWithdrawalId);
}