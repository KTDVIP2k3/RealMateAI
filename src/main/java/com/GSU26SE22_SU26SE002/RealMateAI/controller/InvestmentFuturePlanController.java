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
    @Operation(summary = "Investor: Gửi feedback property đã chọn -> Tạo phiên bản kế hoạch tương lai mới (kèm tính lợi nhuận thực tế)")
    public ResponseEntity<ApiResponse> generateFuturePlan(@RequestBody GenerateFuturePlanRequest request) {
        return investmentFuturePlanService.generateAndSaveFuturePlan(request);
    }

    @GetMapping("/{versionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết phiên bản kế hoạch tương lai (màn hình So sánh)")
    public ResponseEntity<ApiResponse> getFuturePlanDetail(@PathVariable("versionId") Integer versionId) {
        return investmentFuturePlanService.getFuturePlanDetail(versionId);
    }
}