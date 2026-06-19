package com.GSU26SE22_SU26SE002.RealMateAI.controller;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired
    AccountServiceInterface accountServiceInterface;

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
}
