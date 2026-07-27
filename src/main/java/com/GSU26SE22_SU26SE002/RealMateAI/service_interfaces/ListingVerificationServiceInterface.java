package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;


public interface ListingVerificationServiceInterface {

    ResponseEntity<ApiResponse> getPendingQueue();

    ResponseEntity<ApiResponse> verifyListing(Integer listingId, VerifyListingRequest request);

    /** Lấy trạng thái duyệt hiện tại của 1 Listing */
    ResponseEntity<ApiResponse> getVerificationStatus(Integer listingId);

    /**
     * Gọi từ PostingPackageOrderServiceImplement#payPostingPackage() ngay khi
     * thanh toán gói dịch vụ đăng tin thành công — chuyển WAITING_PAYMENT ->
     * PENDING trên chính Listing (đúng luồng: chờ thanh toán -> thanh toán
     * thành công = chờ duyệt -> Staff duyệt -> public = bắt đầu tính ngày gói).
     * Đặt ở đây (không phải bên PostingPackageOrder) vì đây là nơi DUY NHẤT xử
     * lý mọi thay đổi trạng thái vòng đời của ListingVerification.
     *
     * @return true nếu Listing đã APPROVED sẵn từ trước (mua thêm gói cho tin
     *         đang sống — không đổi trạng  , PostingPackageOrderServiceImplement
     *         sẽ tự kích hoạt gói ngay); false nếu còn phải chờ Staff duyệt.
     */
    boolean transitionToPendingOnPayment(Listing listing);
}