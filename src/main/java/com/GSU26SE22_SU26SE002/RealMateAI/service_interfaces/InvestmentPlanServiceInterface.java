package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestmentPlanServiceInterface {
    ResponseEntity<ApiResponse> getListProfileByInvestor();
    public ResponseEntity<ApiResponse> getProfileDetailById(Integer profileId);
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(Integer profileId);
    ResponseEntity<ApiResponse> generateCompleteInvestmentPlan(InvestmentPlanRequest request);
    public ResponseEntity<ApiResponse> updateExistingInvestmentPlan(Integer currentProfileId, UpdateInvestmentPlanRequest request) ;
    ResponseEntity<ApiResponse> savePlanToDatabaseDirectly(SaveInvestmentPlanRequest saveRequest);
}