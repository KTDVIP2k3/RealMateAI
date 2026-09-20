package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AuthServiceInterface;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthServiceInterface authServiceInterface;

    @PostMapping(value = "register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> register(@jakarta.validation.Valid @ModelAttribute RegisterRequest registerRequest, HttpSession httpSession){
        return authServiceInterface.register(registerRequest, httpSession);
    }

    @PostMapping(value = "login")
    public ResponseEntity<ApiResponse> login(@jakarta.validation.Valid @RequestBody LoginRequest loginRequest, HttpSession httpSession){
        return authServiceInterface.login(loginRequest, httpSession);
    }

    @PostMapping(value = "send-otp")
    public ResponseEntity<ApiResponse> sendOtp(HttpSession httpSession, @jakarta.validation.Valid @RequestBody SendOtpRequest sendOtpRequest) {
        return authServiceInterface.resendOtpUnified(httpSession, sendOtpRequest);
    }


    @PostMapping(value = "verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@jakarta.validation.Valid @RequestBody OtpRequest otpRequest, HttpSession httpSession) {
        return authServiceInterface.verifyOtp(otpRequest, httpSession);
    }

    @PostMapping(value = "forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@jakarta.validation.Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpSession httpSession){
        return authServiceInterface.forgotPassword(forgotPasswordRequest, httpSession);
    }

    @PutMapping(value = "new-password")
    public ResponseEntity<ApiResponse> newPassword(@jakarta.validation.Valid @RequestBody NewPasswordRequest newPasswordRequest, HttpSession httpSession){
        return authServiceInterface.newPassword(newPasswordRequest, httpSession);
    }

    @PutMapping(value = "reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@jakarta.validation.Valid @RequestBody ResetPasswordRequest resetPasswordRequest, HttpSession session){
        return authServiceInterface.resetPassword(resetPasswordRequest, session);
    }
//
//    @PostMapping(value = "verify-login")
//    public ResponseEntity<ApiResponse> verifyLogin(@RequestParam String otp, HttpSession httpSession) {
//        return authServiceInterface.verifyLogin(otp, httpSession);
//    }

//    @PostMapping(value = "activate-account")
//    public ResponseEntity<ApiResponse> activateAccount(@RequestBody OtpRequest otpRequest, HttpSession httpSession) {
//        return authServiceInterface.activateAccount(otpRequest, httpSession);
//    }
}