package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PayOSWebhookRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WalletServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @Operation(summary = "[FE CALL] Người investor hoặc seller  nạp tiền vào ví - Frontend gọi trực tiếp để lấy link thanh toán")
    public ResponseEntity<ApiResponse> deposit(@RequestParam("amount") Long amount) {
        return walletService.initiateDeposit(amount);
    }

    @GetMapping("/deposit/success")
    @Operation(summary = "[KHÔNG CẦN CALL] Trình duyệt tự động chuyển hướng về khi thanh toán thành công")
    public ResponseEntity<ApiResponse> depositSuccess(
            @RequestParam("orderCode") String orderCode,
            @RequestParam("status") String status) {
        String transactionStatus = "PAID".equalsIgnoreCase(status) ? "SUCCESS" : status;
        return walletService.handlePayOSWebhook(orderCode, transactionStatus);
    }

    @GetMapping("/deposit/cancel")
    @Operation(summary = "[KHÔNG CẦN CALL] Trình duyệt tự động chuyển hướng về khi bấm hủy thanh toán")
    public ResponseEntity<ApiResponse> depositCancel(
            @RequestParam("orderCode") String orderCode) {
        return walletService.handlePayOSWebhook(orderCode, "CANCELLED");
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

    @PostMapping("/withdraw")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Người investor hoặc seller yêu cầu rút tiền - Frontend gọi khi người investor hoặc seller muốn rút tiền về ngân hàng")
    public ResponseEntity<ApiResponse> withdraw(
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("bankName") String bankName,
            @RequestParam("bankAccountNumber") String bankAccountNumber) {
        return walletService.requestWithdrawal(amount, bankName, bankAccountNumber);
    }

    @PostMapping("/withdraw/review")
    @PreAuthorize("hasAnyRole('Staff', 'Admin')")
    @Operation(summary = "[FE CALL] Staff, Admin duyệt đơn rút tiền - Frontend quản trị gọi khi phê duyệt hoặc từ chối")
    public ResponseEntity<ApiResponse> reviewWithdraw(
            @RequestParam("withdrawalId") Integer withdrawalId,
            @RequestParam("status") String status,
            @RequestParam(value = "note", required = false) String note) {
        return walletService.reviewWithdrawRequest(withdrawalId, status, note);
    }
}