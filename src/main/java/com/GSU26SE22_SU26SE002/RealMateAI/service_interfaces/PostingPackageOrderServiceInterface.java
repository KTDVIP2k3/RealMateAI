package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageOrder;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PostingPackageOrderServiceInterface {
    ResponseEntity<ApiResponse> getPostingPackageOrders(int page, int size);
    ResponseEntity<ApiResponse> payPostingPackage(PostingPackageOrderRequest postingPackageOrderRequest);
    ResponseEntity<ApiResponse> renewPostingPackage(Integer postingPackageOrderId);
}
