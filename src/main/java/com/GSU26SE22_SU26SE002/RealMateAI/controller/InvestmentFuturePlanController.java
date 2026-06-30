package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentFuturePlanServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Future Plan = một InvestmentProfileVersion mới được sinh ra từ feedback thực tế.
 * Do đó controller chỉ cần 2 endpoint, tận dụng triệt để hạ tầng version-control
 * đã có sẵn ở InvestmentPlanController (không cần endpoint riêng cho list/delete/rename
 * vì các API đó của /investment-plans/** đã hoạt động trên InvestmentProfileVersion rồi,
 * dùng được luôn cho version FUTURE_PLAN).
 */
@RestController
@RequestMapping("/investment-plans/future")
@Tag(name = "Investment Future Plan", description = "Investor: Tạo phiên bản kế hoạch đầu tư tương lai từ phản hồi thực tế trên property đã chọn")
public class InvestmentFuturePlanController {

    @Autowired
    private InvestmentFuturePlanServiceInterface investmentFuturePlanService;

    /**
     * FE gửi: sourceVersionId + danh sách property investor đã chọn kèm mục đích sử dụng,
     * dòng tiền thực tế, giá mua thực tế.
     * BE: tính lợi nhuận -> gọi AI sinh kịch bản -> tạo InvestmentProfileVersion mới
     * (version="FUTURE_PLAN") -> lưu toàn bộ trong 1 transaction -> trả kết quả để FE hiển
     * thị màn hình So sánh.
     */
    @PostMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gửi feedback property đã chọn -> Tạo phiên bản kế hoạch tương lai mới (kèm tính lợi nhuận thực tế)")
    public ResponseEntity<ApiResponse> generateFuturePlan(@RequestBody GenerateFuturePlanRequest request) {
        return investmentFuturePlanService.generateAndSaveFuturePlan(request);
    }

    /**
     * Lấy chi tiết 1 version FUTURE_PLAN theo ID.
     * Để xem TOÀN BỘ lịch sử version (cả AI_GENERATED lẫn FUTURE_PLAN) thì dùng endpoint
     * có sẵn GET /investment-plans/{profileId}/versions — không cần endpoint riêng.
     */
    @GetMapping("/{versionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết phiên bản kế hoạch tương lai (màn hình So sánh)")
    public ResponseEntity<ApiResponse> getFuturePlanDetail(@PathVariable Integer versionId) {
        return investmentFuturePlanService.getFuturePlanDetail(versionId);
    }
}
