package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.LoginRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AuthServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthServiceInterface authServiceInterface;

    @PostMapping(value = "register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> register(@ModelAttribute RegisterRequest registerRequest){
        return authServiceInterface.register(registerRequest);
    }

    @PostMapping(value = "login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest loginRequest){
        return authServiceInterface.login(loginRequest);
    }
}
