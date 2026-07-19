package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CompleteValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RejectValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PropertyValuationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Property Valuation Request (Định giá thủ công)")
@RestController
@RequiredArgsConstructor
public class PropertyValuationRequestController {

    private final PropertyValuationRequestService propertyValuationRequestService;

    // ── Seller ───────────────────────────────────────────────────────────────

    @PostMapping("/seller/valuation-requests")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Gửi yêu cầu định giá cho 1 tài sản của mình")
    public ResponseEntity<ApiResponse> submit(@Valid @RequestBody SubmitValuationRequest request) {
        return propertyValuationRequestService.submitValuationRequest(request);
    }

    @GetMapping("/seller/valuation-requests")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Danh sách yêu cầu định giá của mình")
    public ResponseEntity<ApiResponse> getMyRequests() {
        return propertyValuationRequestService.getMyValuationRequests();
    }

    @GetMapping("/seller/valuation-requests/{id}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Chi tiết 1 yêu cầu định giá của mình")
    public ResponseEntity<ApiResponse> getMyRequestDetail(@PathVariable Integer id) {
        return propertyValuationRequestService.getMyValuationRequestDetail(id);
    }

    // ── Staff/Admin ──────────────────────────────────────────────────────────

    @GetMapping("/staff/valuation-requests/pending")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Hàng đợi yêu cầu định giá chờ xử lý")
    public ResponseEntity<ApiResponse> getPendingQueue() {
        return propertyValuationRequestService.getPendingValuationQueue();
    }

    @GetMapping("/staff/valuation-requests/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Chi tiết 1 yêu cầu định giá bất kỳ")
    public ResponseEntity<ApiResponse> getDetail(@PathVariable Integer id) {
        return propertyValuationRequestService.getValuationRequestDetail(id);
    }

    @PatchMapping("/staff/valuation-requests/{id}/complete")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Hoàn tất — đưa ra mức giá đề xuất cho Seller")
    public ResponseEntity<ApiResponse> complete(
            @PathVariable Integer id,
            @Valid @RequestBody CompleteValuationRequest request) {
        return propertyValuationRequestService.completeValuationRequest(id, request);
    }

    @PatchMapping("/staff/valuation-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Từ chối xử lý yêu cầu định giá (kèm lý do)")
    public ResponseEntity<ApiResponse> reject(
            @PathVariable Integer id,
            @Valid @RequestBody RejectValuationRequest request) {
        return propertyValuationRequestService.rejectValuationRequest(id, request);
    }
}
