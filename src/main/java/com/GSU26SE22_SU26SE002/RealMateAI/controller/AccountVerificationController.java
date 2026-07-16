package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountVerificationServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
public class AccountVerificationController {

    @Autowired
    private AccountVerificationServiceInterface accountVerificationServiceInterface;

    @GetMapping("/admin/account-verifications")
    @Operation(summary = "Admin/Staff lấy danh sách tất cả các yêu cầu xác thực tài khoản")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    public ResponseEntity<ApiResponse> getAccountVerificationByStaffOrAdmin() {
        return accountVerificationServiceInterface.getAccountVerificationByStaffOrAdmin();
    }

    @GetMapping("/admin/account-verifications/{id}")
    @Operation(summary = "Admin/Staff lấy thông tin chi tiết của một yêu cầu xác thực theo ID")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    public ResponseEntity<ApiResponse> getAccountVerificationByIdByStaffOrAdmin(@PathVariable Integer id) {
        return accountVerificationServiceInterface.getAccountVerificationByIdByStaffOrAdmin(id);
    }

    @PatchMapping("/admin/account-verifications/{id}/approve")
    @Operation(summary = "Admin/Staff phê duyệt (Approve) yêu cầu xác thực tài khoản")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    public ResponseEntity<ApiResponse> approveAccountVerification(@PathVariable Integer id) {
        return accountVerificationServiceInterface.approveAccountVerification(id);
    }

    @PatchMapping("/admin/account-verifications/{id}/reject")
    @Operation(summary = "Admin/Staff từ chối (Reject) yêu cầu xác thực tài khoản kèm lý do")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    public ResponseEntity<ApiResponse> rejectAccountVerification(
            @PathVariable Integer id,
            @RequestParam String reason) {
        return accountVerificationServiceInterface.rejectAccountVerification(id, reason);
    }

    @GetMapping("/account-verifications/me")
    @Operation(summary = "Seller lấy danh sách các yêu cầu xác thực tài khoản của cá nhân mình")
    @PreAuthorize("hasAnyRole('Seller')")
    public ResponseEntity<ApiResponse> getAccountVerificationForUser() {
        return accountVerificationServiceInterface.getAccountVerificationForUser();
    }

    @GetMapping("/account-verifications/me/{id}")
    @PreAuthorize("hasAnyRole('Seller')")
    @Operation(summary = "Seller xem thông tin chi tiết một yêu cầu xác thực tài khoản của cá nhân mình")
    public ResponseEntity<ApiResponse> getAccountVerificationDetailForUser(@PathVariable Integer id) {
        return accountVerificationServiceInterface.getAccountVerificationDetailForUser(id);
    }

    @PostMapping(value = "/account-verifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Seller')")
    @Operation(summary = "Seller tạo mới yêu cầu xác thực tài khoản (Hỗ trợ tải lên hình ảnh dạng file)")
    public ResponseEntity<ApiResponse> createAccountVerification(
            @ModelAttribute AccountVerificationRequest request) {
        return accountVerificationServiceInterface.createAccountVerification(request);
    }

    @PutMapping(value = "/account-verifications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Seller')")
    @Operation(summary = "Seller cập nhật lại yêu cầu xác thực tài khoản (Hỗ trợ tải lên hình ảnh dạng file)")
    public ResponseEntity<ApiResponse> updateAccountVerification(
            @ModelAttribute AccountVerificationUpdateRequest request) {
        return accountVerificationServiceInterface.updateAccountVerification(request);
    }
}