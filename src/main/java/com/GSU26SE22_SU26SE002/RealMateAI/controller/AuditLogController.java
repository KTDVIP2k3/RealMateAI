package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/account/viewed-listings")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách tin đăng tôi đã xem (mặc định 10/trang, mới nhất trước)")
    public ResponseEntity<ApiResponse> getMyViewedListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userEventTrackingService.getMyViewedListings(page, size);
    }
}
