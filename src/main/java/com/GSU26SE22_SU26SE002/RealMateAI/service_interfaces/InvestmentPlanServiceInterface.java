package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestmentPlanServiceInterface {
    ResponseEntity<ApiResponse> getListProfileByInvestor();
     ResponseEntity<ApiResponse> getListViewsByProfileId(Integer profileId);
     ResponseEntity<ApiResponse> getProfileVersionDetailById(Integer profileVersionId);
     ResponseEntity<ApiResponse> getInvestmentPlanDetailByVersionId(Integer profileVersionId);
    ResponseEntity<ApiResponse> generateCompleteInvestmentPlan(InvestmentPlanRequest request);
     ResponseEntity<ApiResponse> updateExistingInvestmentPlan(Integer currentProfileId, UpdateInvestmentPlanRequest request) ;
    ResponseEntity<ApiResponse> savePlanToDatabaseDirectly(SaveInvestmentPlanRequest saveRequest);
     ResponseEntity<ApiResponse> deleteInvestmentPlanVersion(Integer versionId);
     ResponseEntity<ApiResponse> deleteInvestmentPlan(Integer profileId);
    ResponseEntity<ApiResponse> updateProfileName(Integer profileId, String newName);
    ResponseEntity<ApiResponse> updateVersionName(Integer versionId, String newName);
}