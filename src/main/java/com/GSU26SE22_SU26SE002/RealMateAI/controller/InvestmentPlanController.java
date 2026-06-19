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

    @GetMapping("/version/inputs/{profileId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfilesByInvest(Integer profileId){
        return investmentPlanServiceInterface.getListViewsByProfileId(profileId);
    }

    @GetMapping("/input/{versionId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getProfileVersionDetailById(versionId);
    }

    @GetMapping("/result/{versionId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByVersionId(versionId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

    @PutMapping("/{versionId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> updateInvestmentPlan(
            @PathVariable Integer versionId,
            @RequestBody UpdateInvestmentPlanRequest request) {
        return investmentPlanServiceInterface.updateExistingInvestmentPlan(versionId, request);
    }

    @DeleteMapping("/{versionId}")
    @PreAuthorize("hasAnyRole('Investor')")
    public ResponseEntity<ApiResponse> deleteInvestmentProfileVersion(@PathVariable Integer versionId){
        return investmentPlanServiceInterface.deleteInvestmentPlanVersion(versionId);
    }

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('Investor')")
//    public ResponseEntity<ApiResponse> saveInvestmentPlan(@RequestBody SaveInvestmentPlanRequest request) {
//        return investmentPlanServiceInterface.savePlanToDatabaseDirectly(request);
//    }
}