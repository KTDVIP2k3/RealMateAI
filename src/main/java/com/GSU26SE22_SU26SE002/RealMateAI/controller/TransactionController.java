package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.TransactionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Transaction")
public class TransactionController {

    private final TransactionServiceInterface transactionService;

    @GetMapping("/transaction/me")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Lấy danh sách toàn bộ lịch sử giao dịch của tôi")
    public ResponseEntity<ApiResponse> getMyTransactions(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                         @RequestParam(name = "size", required = false, defaultValue = "0") int size)
    {
        return transactionService.getMyTransactions(page, size);
    }

    @GetMapping("admin/transactions")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "[FE CALL] Admin lấy danh sách toàn bộ lịch sử giao dịch của hệ thống")
    public ResponseEntity<ApiResponse> getTransactionByAdminOrStaff(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                                    @RequestParam(name = "size", required = false, defaultValue = "0") int size)
    {
        return transactionService.getTransactionsByAdminOrStaff(page, size);
    }

    @GetMapping("/transaction/me/type")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Lấy lịch sử giao dịch của tôi lọc theo loại giao dịch (TransactionTypeEnum)")
    public ResponseEntity<ApiResponse> getMyTransactionsByType(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                               @RequestParam(name = "size", required = false, defaultValue = "0") int size,
                                                               @RequestParam(name = "type") String type)
    {
        return transactionService.getMyTransactionsByType(page, size, type);
    }

    @GetMapping("admin/transactions/type")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "[FE CALL] Admin lấy danh sách lịch sử giao dịch hệ thống lọc theo loại giao dịch (TransactionTypeEnum)")
    public ResponseEntity<ApiResponse> getTransactionsByAdminOrStaffByType(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                                           @RequestParam(name = "size", required = false, defaultValue = "0") int size,
                                                                           @RequestParam(name = "type") String type)
    {
        return transactionService.getTransactionsByAdminOrStaffByType(page, size, type);
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAnyRole('Admin', 'Staff', 'Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Xem chi tiết thông tin một giao dịch bất kỳ theo ID")
    public ResponseEntity<ApiResponse> getTransactionDetailById(@PathVariable(name = "transactionId") Integer transactionId)
    {
        return transactionService.getTransactionDetailById(transactionId);
    }
}