package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
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
public class AccountController {

    @Autowired
    AccountServiceInterface accountServiceInterface;

    @Autowired
    AdminAccountServiceInterface adminAccountServiceInterface;

    @GetMapping("/accounts/me")
    public ResponseEntity<ApiResponse> getAccountProfile() {
        return accountServiceInterface.getAccountProfile();
    }

    @GetMapping("/admin/accounts")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        Pageable pageable = PageRequest.of(page, size);
        return adminAccountServiceInterface.getAllAccounts(pageable, role, keyword);
    }

    @GetMapping("/admin/accounts/{accountId}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> getAccountById(@PathVariable Integer accountId) {
        return adminAccountServiceInterface.getAccountById(accountId);
    }

    @PutMapping(value = "/admin/accounts/{accountId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> adminUpdateAccount(
            @PathVariable Integer accountId,
            @ModelAttribute AdminUpdateAccountRequest request) {
        return adminAccountServiceInterface.updateAccount(accountId, request);
    }

//    @PostMapping(value = "/admin/accounts/staff", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasAnyRole('Admin')")
//    public ResponseEntity<ApiResponse> createAccountStaff(@ModelAttribute CreateAccountRequest createAccountRequest) {
//        return accountServiceInterface.createAccountByAdmin(createAccountRequest);
//    }
//
//    @PostMapping(value = "/admin/accounts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('Admin')")
//    public ResponseEntity<ApiResponse> createAccountAdmin(@ModelAttribute CreateAccountRequest createAccountRequest) {
//        return accountServiceInterface.createAccountAdmin(createAccountRequest);
//    }
//
//    @PostMapping(value = "/admin/accounts/sellers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('Admin')")
//    public ResponseEntity<ApiResponse> adminCreateSeller(
//            @ModelAttribute AdminCreateAccountRequest request) {
//        return adminAccountServiceInterface.createSellerAccount(request);
//    }
//
//    @PostMapping(value = "/admin/accounts/investors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('Admin')")
//    public ResponseEntity<ApiResponse> adminCreateInvestor(
//            @ModelAttribute AdminCreateAccountRequest request) {
//        return adminAccountServiceInterface.createInvestorAccount(request);
//    }

    @PostMapping(value = "/admin/accounts/v2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Admin có thể tạo account tuỳ chọn theo role")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> createAccount(
            @ModelAttribute CreateAccountRequestV2 createAccountRequestV2) {
        return accountServiceInterface.createAccount(createAccountRequestV2);
    }

    @PatchMapping("/admin/accounts/{accountId}/role")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> adminChangeRole(
            @PathVariable Integer accountId,
            @RequestParam String role) {
        return adminAccountServiceInterface.changeRole(accountId, role);
    }

    @PatchMapping("/admin/accounts/{accountId}/status")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<ApiResponse> adminChangeStatus(
            @PathVariable Integer accountId,
            @RequestParam boolean isActive) {
        return adminAccountServiceInterface.setAccountStatus(accountId, isActive);
    }

    @PutMapping(value = "/account/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật tài khoản hồ sơ chính mình")
    public ResponseEntity<ApiResponse> updateAccountProfile(@ModelAttribute UpdateAccountRequest updateAccountRequest){
        return accountServiceInterface.updateAccount(updateAccountRequest);
    }
}