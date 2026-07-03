package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.TransactionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction")
public class TransactionController {

    private final TransactionServiceInterface transactionService;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('Investor', 'Seller')")
    @Operation(summary = "[FE CALL] Lấy danh sách toàn bộ lịch sử giao dịch của tôi")
    public ResponseEntity<ApiResponse> getMyTransactions() {
        return transactionService.getMyTransactions();
    }
}