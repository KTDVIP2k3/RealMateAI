package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface SellerDashboardServiceInterface {

    /** GET /api/v1/dashboard/seller/kpis — API 1.1. */
    ResponseEntity<ApiResponse> getSellerDashboardKpi();

    /** GET /api/v1/dashboard/seller/top-listings?limit=5 — API 1.4. */
    ResponseEntity<ApiResponse> getTopListings(int limit);
}
