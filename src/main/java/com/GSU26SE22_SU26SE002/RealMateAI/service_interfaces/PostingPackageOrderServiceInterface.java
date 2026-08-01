package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageOrder;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PaymentAttemptResult;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;


public interface PostingPackageOrderServiceInterface {
    ResponseEntity<ApiResponse> getPostingPackageOrders(int page, int size);
    ResponseEntity<ApiResponse> payPostingPackage(PostingPackageOrderRequest postingPackageOrderRequest);
    ResponseEntity<ApiResponse> renewPostingPackage(Integer postingPackageOrderId);

    PaymentAttemptResult attemptAutoPaymentForNewListing(Integer listingId, Integer postingPackageId,
                                                         Integer duration, BigDecimal totalAmount);

    /**
     * MỚI: Thanh toán LẠI cho 1 order đang status=FAILED (ví null/không đủ tiền
     * lúc đầu) — KHÔNG tạo order mới, chỉ update lại order này. Thành công thì
     * status -> SUCCESS, Listing chuyển WAITING_PAYMENT -> PENDING.
     */
    ResponseEntity<ApiResponse> retryPayPostingPackage(Integer postingPackageOrderId);
}
