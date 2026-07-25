package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface UserEventTrackingService {


    /**
     * Ghi nhận NHANH, không qua HTTP request/response — hook trực tiếp vào
     * luồng nghiệp vụ có sẵn (vd getListingDetail gọi VIEW). KHÔNG throw
     * exception ra ngoài — lỗi ghi log KHÔNG được làm hỏng luồng chính.
     *
     * @param account   có thể null (bỏ qua nếu null — khách ẩn danh không ghi nhận)
     * @param eventType loại sự kiện
     * @param listingId listing liên quan
     */
    void recordSilently(Account account, UserEventTypeEnum eventType, Integer listingId);

    /** GET /listings/{listingId}/views — số lượt xem của 1 listing (public). */
    ResponseEntity<ApiResponse> getViewCount(Integer listingId);

    /** GET /account/viewed-listings — lịch sử các listing user hiện tại đã xem. */
    ResponseEntity<ApiResponse> getMyViewedListings(int page, int size);
}
