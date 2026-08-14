package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface StaffDashboardServiceInterface {
    ResponseEntity<ApiResponse> getDashBoardKpi();
    ResponseEntity<ApiResponse> getPendingListing(int page, int size);
    ResponseEntity<ApiResponse> getPendingAccountVerifications(int page, int size);
    ResponseEntity<ApiResponse> getPendingPropertyValuations(int page, int size);;
}
