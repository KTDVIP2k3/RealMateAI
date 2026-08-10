package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.StaffDashboardServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Staff_Dashboard")
public class StaffDashboardController {
    @Autowired
    private StaffDashboardServiceInterface staffDashboardServiceInterface;

    @GetMapping("/api/v1/dashboard/staff/kpis")
    @Operation(description = "Đếm tổng số đơn đang ở trạng thái PENDING trong toàn hệ thống")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse> getStaffDashBoardKPI(){
        return staffDashboardServiceInterface.getDashBoardKpi();
    }

    @GetMapping("/api/v1/dashboard/staff/pending-listings")
    @Operation(description = "Hàng chờ Duyệt BĐS (Listing Verification Queue)")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse> getPendingListings(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return staffDashboardServiceInterface.getPendingListing(page, size);
    }

    @GetMapping("/api/v1/dashboard/staff/pending-verifications")
    @Operation(description = "Hàng chờ Duyệt Xác thực Tài khoản (Account Verification / KYC Queue)")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse> getPendingAccountVerifications(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return staffDashboardServiceInterface.getPendingAccountVerifications(page, size);
    }

    @GetMapping("/api/v1/dashboard/staff/pending-valuations")
    @Operation(description = "Hàng chờ Yêu cầu Định giá & Chứng nhận BĐS")
    @PreAuthorize("hasRole('Staff')")
    public ResponseEntity<ApiResponse> getPendingPropertyValuations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return staffDashboardServiceInterface.getPendingPropertyValuations(page, size);
    }
}