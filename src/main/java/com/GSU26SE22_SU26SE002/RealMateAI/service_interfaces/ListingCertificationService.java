package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.ReviewCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

/** "Tích xanh" — xác thực Listing bằng giấy tờ pháp lý tài sản, Staff duyệt. */
public interface ListingCertificationService {

    /** Seller nộp giấy tờ cho 1 Listing của chính mình. */
    ResponseEntity<ApiResponse> submitCertificationRequest(Integer listingId, SubmitCertificationRequest request);

    /** Seller xem danh sách yêu cầu tích xanh của mình. */
    ResponseEntity<ApiResponse> getMyCertificationRequests();

    /** Seller xem chi tiết 1 yêu cầu của mình. */
    ResponseEntity<ApiResponse> getMyCertificationRequestDetail(Integer id);

    /** Staff/Admin xem hàng đợi yêu cầu đang chờ duyệt. */
    ResponseEntity<ApiResponse> getPendingCertificationQueue();

    /** Staff/Admin xem chi tiết 1 yêu cầu bất kỳ. */

    ResponseEntity<ApiResponse> getCertificationRequestDetail(Integer id);

    /** Staff/Admin duyệt hoặc từ chối — APPROVED sẽ bật Listing.isVerified = true. */
    ResponseEntity<ApiResponse> reviewCertificationRequest(Integer id, ReviewCertificationRequest request);
}
