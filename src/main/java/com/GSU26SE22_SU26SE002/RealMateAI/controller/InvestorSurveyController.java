package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestorSurveyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestorServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investor")
public class InvestorSurveyController {
    @Autowired
    private InvestorServiceInterface investorServiceInterface;

    @GetMapping(value = "/survey/me")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> getInvestorSurvey(){
        return investorServiceInterface.getInvestorSurvey();
    }

    @PostMapping(value = "/survey/me")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> createInvestorSurvey(@RequestBody InvestorSurveyRequest investorSurveyRequest){
        return investorServiceInterface.createInvestorSurvey(investorSurveyRequest);
    }

    @PutMapping(value = "/survey/me")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> updateInvestorSurvey(@RequestBody InvestorSurveyRequest investorSurveyRequest){
        return investorServiceInterface.updateInvestorSurvey(investorSurveyRequest);
    }
}