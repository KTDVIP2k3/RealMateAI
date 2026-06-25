package com.GSU26SE22_SU26SE002.RealMateAI.controller;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminCreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminUpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AdminAccountServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired
    AccountServiceInterface accountServiceInterface;
    @Autowired
    AdminAccountServiceInterface adminAccountServiceInterface;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getAccountProfile() {
        return accountServiceInterface.getAccountProfile();
    }


    @PostMapping(value = "/staff", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Admin')")
    public ResponseEntity<ApiResponse> createAccountStaff(@ModelAttribute CreateAccountRequest createAccountRequest) {
        return accountServiceInterface.createAccountByAdmin(createAccountRequest);
    }

    @PostMapping(value = "/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> createAccountAdmin(@ModelAttribute CreateAccountRequest createAccountRequest) {
        return accountServiceInterface.createAccountAdmin(createAccountRequest);
    }

    // ===================== ADMIN MANAGEMENT =====================

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Danh sách tài khoản ")
    public ResponseEntity<ApiResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        return adminAccountServiceInterface.getAllAccounts(pageable, role, keyword);
    }

    @GetMapping("/admin/{accountId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Chi tiết tài khoản theo ID")
    public ResponseEntity<ApiResponse> getAccountById(@PathVariable Integer accountId) {
        return adminAccountServiceInterface.getAccountById(accountId);
    }

    @PostMapping(value = "/admin/create/investor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Tạo tài khoản Investor ")
    public ResponseEntity<ApiResponse> adminCreateInvestor(
            @ModelAttribute AdminCreateAccountRequest request) {
        return adminAccountServiceInterface.createInvestorAccount(request);
    }

    @PostMapping(value = "/admin/create/seller", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Tạo tài khoản Seller")
    public ResponseEntity<ApiResponse> adminCreateSeller(
            @ModelAttribute AdminCreateAccountRequest request) {
        return adminAccountServiceInterface.createSellerAccount(request);
    }

    @PutMapping(value = "/admin/{accountId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Cập nhật thông tin tài khoản")
    public ResponseEntity<ApiResponse> adminUpdateAccount(
            @PathVariable Integer accountId,
            @ModelAttribute AdminUpdateAccountRequest request) {
        return adminAccountServiceInterface.updateAccount(accountId, request);
    }

    @PatchMapping("/admin/{accountId}/role")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Đổi role tài khoản")
    public ResponseEntity<ApiResponse> adminChangeRole(
            @PathVariable Integer accountId,
            @RequestParam String role) {
        return adminAccountServiceInterface.changeRole(accountId, role);
    }

    @PatchMapping("/admin/{accountId}/activate")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Kích hoạt tài khoản")
    public ResponseEntity<ApiResponse> adminActivate(@PathVariable Integer accountId) {
        return adminAccountServiceInterface.setAccountStatus(accountId, true);
    }

    @PatchMapping("/admin/{accountId}/deactivate")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "[Admin] Vô hiệu hoá tài khoản")
    public ResponseEntity<ApiResponse> adminDeactivate(@PathVariable Integer accountId) {
        return adminAccountServiceInterface.setAccountStatus(accountId, false);
    }
}
