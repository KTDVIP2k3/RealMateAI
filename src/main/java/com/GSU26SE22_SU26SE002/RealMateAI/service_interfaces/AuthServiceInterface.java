package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.LoginRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

public interface AuthServiceInterface {
    ResponseEntity<ApiResponse> register(RegisterRequest registerRequest, HttpSession session);

    ResponseEntity<ApiResponse> login(LoginRequest loginRequest, HttpSession httpSession);

    ResponseEntity<ApiResponse> forgotPassword(String email, HttpSession httpSession);

    ResponseEntity<ApiResponse> resetPassword(String newPassword, HttpSession httpSession);

    ResponseEntity<ApiResponse> sendOtp(HttpSession httpSession);

    ResponseEntity<ApiResponse> resendOtpUnified(HttpSession httpSession);

    ResponseEntity<ApiResponse> verifyOtp(String otp, HttpSession httpSession);

    ResponseEntity<ApiResponse> verifyLogin(String otp, HttpSession httpSession);

    public ResponseEntity<ApiResponse> activateAccount(String otp, HttpSession httpSession);
}