package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestorSurveyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestorServiceInterface {
    public ResponseEntity<ApiResponse> getInvestorSurvey();

    public ResponseEntity<ApiResponse> createInvestorSurvey(InvestorSurveyRequest investorSurveyRequest);

    public ResponseEntity<ApiResponse> updateInvestorSurvey(Integer investorId, InvestorSurveyRequest investorSurveyRequest);
}
