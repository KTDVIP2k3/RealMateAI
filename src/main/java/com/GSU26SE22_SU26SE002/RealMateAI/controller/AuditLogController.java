package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Audit Log")
@RestController
@RequiredArgsConstructor
public class AuditLogController {

    private final UserEventTrackingService userEventTrackingService;

    @GetMapping("/listings/{listingId}/views")
    @Operation(summary = "Số lượt xem của 1 tin đăng (public)")
    public ResponseEntity<ApiResponse> getViewCount(@PathVariable("listingId") Integer listingId) {
        return userEventTrackingService.getViewCount(listingId);
    }

    // SỬA: thêm alias "/account/viewed-listings" — path cũ "/investor/viewed-listings"
    // GÂY HIỂU LẦM là chỉ dành cho Investor, trong khi @PreAuthorize thực tế
    // chỉ yêu cầu "isAuthenticated()" (không giới hạn role) và service dùng
    // chung currentUser.getAccountId() — API này ĐÃ hoạt động đúng cho CẢ
    // Investor lẫn Seller từ trước, chỉ là tên đường dẫn không phản ánh đúng.
    // Giữ nguyên path cũ để không phá vỡ FE đang gọi sẵn, thêm path mới cho
    // rõ nghĩa hơn — cả 2 cùng trỏ về đúng 1 hàm.
    @GetMapping({ "/account/viewed-listings"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách tin đăng tôi đã xem gần đây — dùng chung cho MỌI role đã đăng nhập (Investor, Seller...), mặc định 10/trang, mới nhất trước")
    public ResponseEntity<ApiResponse> getMyViewedListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userEventTrackingService.getMyViewedListings(page, size);
    }

    // MỚI: FE tự báo lên khi user CLICK vào 1 kết quả tìm kiếm, SHARE tin đăng,
    // hoặc bấm xem/CONTACT số điện thoại người bán — các hành động này xảy ra
    // hoàn toàn phía client, không có endpoint nghiệp vụ backend nào để hook
    // trực tiếp như VIEW/SAVE/SEARCH (đã tự động ghi nhận sẵn).
    @PostMapping("/events/track")
    @Operation(summary = "FE báo lên sự kiện CLICK/SHARE/CONTACT — dữ liệu đầu vào cho Recommendation System")
    public ResponseEntity<ApiResponse> trackClientEvent(
            @RequestParam UserEventTypeEnum eventType,
            @RequestParam Integer listingId) {
        return userEventTrackingService.trackClientEvent(eventType, listingId);
    }
}
