package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SellerDashboardServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Seller_Dashboard")
public class SellerDashboardController {

    @Autowired
    private SellerDashboardServiceInterface sellerDashboardServiceInterface;

    @GetMapping("/api/dashboard/seller/package-orders")
    @Operation(description = "Danh sách Đơn hàng Gói tin đăng theo Bài đăng của Seller")
    @PreAuthorize("hasRole('Seller')")
    public ResponseEntity<ApiResponse> getPostingPackageOrders(
            @RequestParam(value = "active_only", defaultValue = "true") Boolean activeOnly,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return sellerDashboardServiceInterface.getPostingPackageOrders(activeOnly, limit);
    }

    @GetMapping("/api/dashboard/seller/wallet-summary")
    @Operation(description = "Tóm tắt Số dư Ví & Biến động Giao dịch gần đây của Seller")
    @PreAuthorize("hasRole('Seller')")
    public ResponseEntity<ApiResponse> getWalletSummary() {
        return sellerDashboardServiceInterface.getWalletSummary();
    }

    // MỚI: theo đúng dashboard_api_specification.md mục 5.2 và 5.3.
    @GetMapping("/api/dashboard/seller/listings-summary")
    @Operation(description = "Thống kê số lượng tin đăng theo 4 trạng thái: ACTIVE/PENDING/REJECTED/EXPIRED")
    @PreAuthorize("hasRole('Seller')")
    public ResponseEntity<ApiResponse> getListingsSummary() {
        return sellerDashboardServiceInterface.getListingsSummary();
    }

    @GetMapping("/api/dashboard/seller/top-listings")
    @Operation(description = "Top tin đăng ACTIVE có lượt xem cao nhất (mặc định limit=5)")
    @PreAuthorize("hasRole('Seller')")
    public ResponseEntity<ApiResponse> getTopListings(
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return sellerDashboardServiceInterface.getTopListings(limit);
    }
}