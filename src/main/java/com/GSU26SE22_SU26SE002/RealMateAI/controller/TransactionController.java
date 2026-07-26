package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.PageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.TransactionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
//@RequestMapping("/transactions")
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
    @Operation(summary = "[FE CALL]Admin, Staff lấy danh sách toàn bộ lịch sử giao dịch của tôi")
    public ResponseEntity<ApiResponse> getTransactionByAdminOrStaff(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                         @RequestParam(name = "size", required = false, defaultValue = "0") int size)
    {
        return transactionService.getTransactionsByAdminOrStaff(page, size);
    }
}