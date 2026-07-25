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

@RestController
@RequestMapping("/investment-plans/future")
@Tag(name = "Investment Future Plan", description = "Investor: Tạo phiên bản kế hoạch đầu tư tương lai từ phản hồi thực tế trên property đã chọn")
public class InvestmentFuturePlanController {

    @Autowired
    private InvestmentFuturePlanServiceInterface investmentFuturePlanService;

    @PostMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gửi feedback property đã chọn -> Tạo phiên bản kế hoạch tương lai mới (chỉ trả về newVersionId — gọi GET /investment-plans/future/{newVersionId} riêng để lấy output đầy đủ)")
    public ResponseEntity<ApiResponse> generateFuturePlan(@RequestBody GenerateFuturePlanRequest request) {
        return investmentFuturePlanService.generateAndSaveFuturePlan(request);
    }

    @GetMapping("/{versionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem đầy đủ output kế hoạch tương lai theo versionId (màn hình So sánh) — nguồn duy nhất cho toàn bộ output")
    public ResponseEntity<ApiResponse> getFuturePlanDetail(@PathVariable("versionId") Integer versionId) {
        return investmentFuturePlanService.getFuturePlanDetail(versionId);
    }
    @GetMapping("/by-source/{sourceVersionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Danh sách TÓM TẮT các kế hoạch tương lai (FUTURE_PLAN) phái sinh từ 1 version gốc (sourceVersionId) — field futureVersionId dùng gọi tiếp GET /investment-plans/future/{futureVersionId}")
    public ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(@PathVariable("sourceVersionId") Integer sourceVersionId) {
        return investmentFuturePlanService.getFutureVersionsBySourceVersionId(sourceVersionId);
    }
}