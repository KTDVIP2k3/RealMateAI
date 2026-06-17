package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investment-plans")
public class InvestmentPlanController {

    @Autowired
    private InvestmentPlanServiceInterface investmentPlanServiceInterface;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfilesByInvest(){
        return investmentPlanServiceInterface.getListProfileByInvestor();
    }

    @GetMapping("/{id}/input")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable Integer id) {
        return investmentPlanServiceInterface.getProfileDetailById(id);
    }

    @GetMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable Integer id) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByProfileId(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

    @PutMapping("/update/{profileId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> updateInvestmentPlan(
            @PathVariable Integer profileId,
            @RequestBody UpdateInvestmentPlanRequest request) {
        return investmentPlanServiceInterface.updateExistingInvestmentPlan(profileId, request);
    }

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('Investor')")
//    public ResponseEntity<ApiResponse> saveInvestmentPlan(@RequestBody SaveInvestmentPlanRequest request) {
//        return investmentPlanServiceInterface.savePlanToDatabaseDirectly(request);
//    }
}