package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SellerDashboardServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Seller_Dashboard")
@RequiredArgsConstructor
public class SellerDashboardController {

    private final SellerDashboardServiceInterface sellerDashboardServiceInterface;

    @GetMapping("/api/v1/dashboard/seller/kpis")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: KPI tổng quan BĐS & tương tác (tổng số tin theo status, tổng view/save/contact)")
    public ResponseEntity<ApiResponse> getSellerDashboardKpi() {
        return sellerDashboardServiceInterface.getSellerDashboardKpi();
    }

    @GetMapping("/api/v1/dashboard/seller/top-listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Top BĐS được xem/lưu nhiều nhất")
    public ResponseEntity<ApiResponse> getTopListings(@RequestParam(defaultValue = "5") int limit) {
        return sellerDashboardServiceInterface.getTopListings(limit);
    }
}
