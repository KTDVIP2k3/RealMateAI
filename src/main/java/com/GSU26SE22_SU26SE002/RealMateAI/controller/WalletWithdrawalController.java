package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WalletWithDrawlServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class WalletWithdrawalController {

    @Autowired
    private WalletWithDrawlServiceInterface walletWithDrawlServiceInterface;

    @GetMapping("/admin/wallet-withdrawals")
    @Operation(summary = "Admin lấy danh sách tất cả các yêu cầu rút tiền")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getWalletWithdrawalByAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletWithDrawlServiceInterface.getWalletWithdrawalByAdmin(page, size);
    }

    @GetMapping("/wallet-withdrawals/me")
    @Operation(summary = "Investor hoặc Seller xem lịch sử rút tiền của chính mình")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    public ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSeller(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return walletWithDrawlServiceInterface.getWalletWithdrawalByInvestorOrSeller(page, size);
    }

    @GetMapping("/admin/wallet-withdrawals/status")
    @Operation(summary = "Admin lấy danh sách các yêu cầu rút tiền kèm filter status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getWalletWithdrawalByAdminWithStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String status) {
        return walletWithDrawlServiceInterface.getWalletWithdrawalByAdminStatus(page, size, status);
    }

    @GetMapping("/wallet-withdrawals/me/status")
    @Operation(summary = "Investor hoặc Seller xem lịch sử rút tiền của chính mình kèm filter status")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    public ResponseEntity<ApiResponse> getWalletWithdrawalByInvestorOrSellerWithStatus(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam String status) {
        return walletWithDrawlServiceInterface.getWalletWithdrawalByInvestorOrSellerByStatus(page, size, status);
    }

    @GetMapping("/wallet-withdrawals/{walletWithdrawalId}")
    @Operation(summary = "Xem chi tiết thông tin một lệnh rút tiền theo ID")
    @PreAuthorize("hasAnyRole('Admin', 'Investor', 'Seller')")
    public ResponseEntity<ApiResponse> getWalletWithdrawalDetailById(
            @PathVariable Integer walletWithdrawalId) {
        return walletWithDrawlServiceInterface.getWalletWithdrawalDetailById(walletWithdrawalId);
    }
}