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
    public ResponseEntity<ApiResponse> register(@ModelAttribute RegisterRequest registerRequest, HttpSession httpSession){
        return authServiceInterface.register(registerRequest, httpSession);
    }

    @PostMapping(value = "login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest, HttpSession httpSession){
        return authServiceInterface.login(loginRequest, httpSession);
    }

    @PostMapping(value = "send-otp")
    public ResponseEntity<ApiResponse> sendOtp(HttpSession httpSession,@RequestBody SendOtpRequest sendOtpRequest) {
        return authServiceInterface.resendOtpUnified(httpSession, sendOtpRequest);
    }


    @PostMapping(value = "verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody OtpRequest otpRequest, HttpSession httpSession) {
        return authServiceInterface.verifyOtp(otpRequest, httpSession);
    }

    @PostMapping(value = "forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody ForgotPasswordRequest forgotPasswordRequest, HttpSession httpSession){
        return authServiceInterface.forgotPassword(forgotPasswordRequest, httpSession);
    }

    @PostMapping(value = "reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestPart ResetPasswordRequest resetPasswordRequest, HttpSession session){
        return authServiceInterface.resetPassword(resetPasswordRequest, session);
    }
//
//    @PostMapping(value = "verify-login")
//    public ResponseEntity<ApiResponse> verifyLogin(@RequestParam String otp, HttpSession httpSession) {
//        return authServiceInterface.verifyLogin(otp, httpSession);
//    }

    @PostMapping(value = "activate-account")
    public ResponseEntity<ApiResponse> activateAccount(@RequestBody OtpRequest otpRequest, HttpSession httpSession) {
        return authServiceInterface.activateAccount(otpRequest, httpSession);
    }
}