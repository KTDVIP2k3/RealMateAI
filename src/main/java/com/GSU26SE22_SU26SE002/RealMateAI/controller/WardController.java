package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WardServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wards")
public class WardController {
    @Autowired
    private WardServiceInterface wardServiceInterface;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllWardByProvince(@RequestParam(name = "provinceCode") String provinceCode){
        return wardServiceInterface.getWardListByProvince(provinceCode);
    }
}