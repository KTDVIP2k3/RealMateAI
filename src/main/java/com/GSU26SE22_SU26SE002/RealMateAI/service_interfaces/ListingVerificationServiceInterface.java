package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

/**
 * ListingVerificationServiceInterface
 *
 * Quy trình duyệt tin của Staff/Admin. Mỗi lượt duyệt xem xét ĐỒNG THỜI
 * nội dung Listing/Property VÀ ảnh property_image — không tách riêng
 * "duyệt ảnh" và "duyệt nội dung" thành 2 bước khác nhau.
 */
public interface ListingVerificationServiceInterface {
    /** Staff/Admin: lấy hàng đợi các Listing đang chờ duyệt (PENDING) */
    ResponseEntity<ApiResponse> getPendingQueue();

    /**
     * Staff/Admin: duyệt 1 Listing — APPROVED hoặc REJECTED.
     * APPROVED chỉ được phép khi Property liên kết đã có ít nhất 1 ảnh.
     */
    ResponseEntity<ApiResponse> verifyListing(Integer listingId, VerifyListingRequest request);

    /** Xem lịch sử các lượt duyệt của 1 Listing (kể cả các lần REJECTED trước) */
    ResponseEntity<ApiResponse> getVerificationHistory(Integer listingId);
}
