package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investment-plan")
public class InvestmentPlanController {

    @Autowired
    private InvestmentPlanServiceInterface investmentPlanServiceInterface;

    @GetMapping("/profiles/me")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfilesByInvest(){
        return investmentPlanServiceInterface.getListProfileByInvestor();
    }

    @GetMapping("/profile/detail/{profileId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable Integer profileId) {
        return investmentPlanServiceInterface.getProfileDetailById(profileId);
    }

    @GetMapping("/detail/{profileId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable Integer profileId) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByProfileId(profileId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('Investor')")
//    public ResponseEntity<ApiResponse> saveInvestmentPlan(@RequestBody SaveInvestmentPlanRequest request) {
//        return investmentPlanServiceInterface.savePlanToDatabaseDirectly(request);
//    }
}