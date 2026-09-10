package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestmentFuturePlanServiceInterface {

    ResponseEntity<ApiResponse> generateAndSaveFuturePlan(GenerateFuturePlanRequest request);

    ResponseEntity<ApiResponse> getFuturePlanDetail(Integer futurePlanId);


    ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(Integer sourceVersionId);
}
