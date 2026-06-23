package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * Chỉ giữ trạng thái duyệt HIỆN TẠI (không lưu lịch sử)
 */
public interface ListingVerificationServiceInterface {

    ResponseEntity<ApiResponse> getPendingQueue();

    ResponseEntity<ApiResponse> verifyListing(Integer listingId, VerifyListingRequest request);

    /** Lấy trạng thái duyệt hiện tại của 1 Listing */
    ResponseEntity<ApiResponse> getVerificationStatus(Integer listingId);
}