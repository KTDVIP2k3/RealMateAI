package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ProvinceServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/province")
public class ProvinceController {
    @Autowired
    private ProvinceServiceInterface provinceServiceInterface;

    @GetMapping
    public ResponseEntity<ApiResponse> getProvinces(){
        return provinceServiceInterface.getProvinces();
    }
}
