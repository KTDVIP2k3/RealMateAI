package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PayOSWebhookRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WalletServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet")
public class WalletController {

    private final WalletServiceInterface walletService;

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Người investor hoặc seller nạp tiền vào ví - Frontend gọi trực tiếp để lấy link thanh toán")
    public ResponseEntity<ApiResponse> deposit(
            @RequestParam("amount") Long amount,
            @RequestParam(value = "returnUrl", required = false) String customReturnUrl,
            @RequestParam(value = "cancelUrl", required = false) String customCancelUrl) {
        return walletService.initiateDeposit(amount, customReturnUrl, customCancelUrl);
    }

    @GetMapping("/deposit/success")
    @Operation(summary = "[KHÔNG CẦN CALL] Trình duyệt tự động chuyển hướng về khi thanh toán thành công")
    public void depositSuccess(
            @RequestParam("orderCode") String orderCode,
            @RequestParam("status") String status,
            HttpServletResponse response) throws IOException {
        String transactionStatus = "PAID".equalsIgnoreCase(status) ? "SUCCESS" : status;
        walletService.handlePayOSWebhook(orderCode, transactionStatus);

        String redirectUrl = walletService.resolveRedirectUrl(orderCode, "success");
        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/deposit/cancel")
    @Operation(summary = "[KHÔNG CẦN CALL] Trình duyệt tự động chuyển hướng về khi bấm hủy thanh toán")
    public void depositCancel(
            @RequestParam("orderCode") String orderCode,
            HttpServletResponse response) throws IOException {
        walletService.handlePayOSWebhook(orderCode, "CANCELLED");

        String redirectUrl = walletService.resolveRedirectUrl(orderCode, "cancel");
        response.sendRedirect(redirectUrl);
    }

    @PostMapping("/deposit/webhook")
    @Operation(summary = "[KHÔNG CẦN CALL] Hệ thống PayOS tự động gọi ngầm để đồng bộ dữ liệu giao dịch")
    public ResponseEntity<ApiResponse> webhook(@RequestBody PayOSWebhookRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", "Dữ liệu webhook trống"));
            }

            String orderCode = String.valueOf(data.get("orderCode"));
            String status = request.getDesc();
            if ("success".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(String.valueOf(data.get("status")))) {
                status = "SUCCESS";
            }

            return walletService.handlePayOSWebhook(orderCode, status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("PARSE_ERROR", "Không thể phân tích dữ liệu Webhook"));
        }
    }

    @PostMapping("/wallet-withdrawals")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Người investor hoặc seller yêu cầu rút tiền")
    public ResponseEntity<ApiResponse> withdraw(
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("bankName") String bankName,
            @RequestParam("bankAccountNumber") String bankAccountNumber,
            @RequestParam("note") String note) {
        return walletService.requestWithdrawal(amount, bankName, bankAccountNumber, note);
    }

    @PostMapping("/wallet-withdrawals/{withdrawalId}/review")
    @PreAuthorize("hasAnyRole('Staff', 'Admin')")
    @Operation(summary = "[FE CALL] Staff, Admin duyệt đơn rút tiền")
    public ResponseEntity<ApiResponse> reviewWithdraw(
            @PathVariable("withdrawalId") Integer withdrawalId,
            @RequestParam("status") String status,
            @RequestParam(value = "reason", required = false) String reason) {
        return walletService.reviewWithdrawRequest(withdrawalId, status, reason);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Lấy thông tin số dư Ví hiện tại của tôi")
    public ResponseEntity<ApiResponse> getMyWallet() {
        return walletService.getMyWallet();
    }
}