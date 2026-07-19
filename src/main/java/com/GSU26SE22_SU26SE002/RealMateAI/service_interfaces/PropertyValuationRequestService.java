package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.CompleteValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RejectValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PropertyValuationRequestService {

    ResponseEntity<ApiResponse> submitValuationRequest(SubmitValuationRequest request);

    ResponseEntity<ApiResponse> getMyValuationRequests();

    ResponseEntity<ApiResponse> getMyValuationRequestDetail(Integer id);

    ResponseEntity<ApiResponse> getPendingValuationQueue();

    ResponseEntity<ApiResponse> getValuationRequestDetail(Integer id);

    ResponseEntity<ApiResponse> completeValuationRequest(Integer id, CompleteValuationRequest request);

    ResponseEntity<ApiResponse> rejectValuationRequest(Integer id, RejectValuationRequest request);
}
