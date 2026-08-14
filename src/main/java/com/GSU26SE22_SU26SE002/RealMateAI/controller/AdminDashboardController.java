package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AdminDashboardServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/admin")
@Tag(name = "Admin_Dashboard", description = "Các API Dashboard giám sát toàn hệ thống dành cho Admin")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardServiceInterface adminDashboardServiceInterface;

    @GetMapping("/system-kpis")
    @Operation(summary = "API 3.1: Chỉ số KPI Hệ thống & Người dùng",
            description = "Tổng số tài khoản phân theo RoleEnum (Investor, Seller, Staff) và tổng số BĐS trên nền tảng.")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getSystemKpis() {
        return adminDashboardServiceInterface.getSystemKpis();
    }

    @GetMapping("/revenue-analytics")
    @Operation(summary = "API 3.2: Thống kê Doanh thu Nền tảng (Platform Revenue)",
            description = "Doanh thu tổng hợp từ các giao dịch (POSTING_PACKAGE_PAYMENT, MEMBERSHIP_PAYMENT).")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getRevenueAnalytics(
            @RequestParam(value = "timeframe", defaultValue = "this_month") String timeframe) {
        return adminDashboardServiceInterface.getRevenueAnalytics(timeframe);
    }

    @GetMapping("/pending-withdrawals")
    @Operation(summary = "API 3.3: Hàng chờ Duyệt Rút tiền Ví (Pending Withdrawals)",
            description = "Danh sách các đơn rút tiền ví (wallet-withdrawal) chờ Admin duyệt giải ngân.")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getPendingWithdrawals(
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        return adminDashboardServiceInterface.getPendingWithdrawals(limit);
    }
}