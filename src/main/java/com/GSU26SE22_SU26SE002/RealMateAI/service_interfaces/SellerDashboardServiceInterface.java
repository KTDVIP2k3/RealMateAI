package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface SellerDashboardServiceInterface {
    ResponseEntity<ApiResponse> getPostingPackageOrders(Boolean activeOnly, Integer limit);
    ResponseEntity<ApiResponse> getWalletSummary();
}
