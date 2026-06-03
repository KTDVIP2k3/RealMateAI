package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.LoginRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

public interface AuthServiceInterface {
    ResponseEntity<ApiResponse> register(RegisterRequest registerRequest);

    ResponseEntity<ApiResponse> login(LoginRequest loginRequest);
}
