package com.GSU26SE22_SU26SE002.RealMateAI.controller;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.ReviewCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingCertificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Listing Certification (Tích xanh)")
@RestController
@RequiredArgsConstructor
public class ListingCertificationController {

    private final ListingCertificationService listingCertificationService;

    // ── Seller ───────────────────────────────────────────────────────────────

    @PostMapping(value = "/seller/listings/{id}/certification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Gửi yêu cầu tích xanh cho 1 tin đăng (kèm giấy tờ pháp lý)")
    public ResponseEntity<ApiResponse> submitCertification(
            @PathVariable("id") Integer listingId,
            @ModelAttribute SubmitCertificationRequest request) {
        return listingCertificationService.submitCertificationRequest(listingId, request);
    }

    @GetMapping("/seller/certification-requests")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Danh sách yêu cầu tích xanh của mình")
    public ResponseEntity<ApiResponse> getMyCertificationRequests() {
        return listingCertificationService.getMyCertificationRequests();
    }

    @GetMapping("/seller/certification-requests/{id}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Chi tiết 1 yêu cầu tích xanh của mình")
    public ResponseEntity<ApiResponse> getMyCertificationRequestDetail(@PathVariable Integer id) {
        return listingCertificationService.getMyCertificationRequestDetail(id);
    }

    // ── Staff/Admin ──────────────────────────────────────────────────────────

    @GetMapping("/staff/certification-requests/pending")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Hàng đợi yêu cầu tích xanh chờ duyệt")
    public ResponseEntity<ApiResponse> getPendingQueue() {
        return listingCertificationService.getPendingCertificationQueue();
    }

    @GetMapping("/staff/certification-requests/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Chi tiết 1 yêu cầu tích xanh bất kỳ")
    public ResponseEntity<ApiResponse> getDetail(@PathVariable Integer id) {
        return listingCertificationService.getCertificationRequestDetail(id);
    }

    @PatchMapping("/staff/certification-requests/{id}/review")
    @PreAuthorize("hasAnyRole('Admin', 'Staff')")
    @Operation(summary = "Staff/Admin: Duyệt (APPROVED) hoặc từ chối (REJECTED) yêu cầu tích xanh")
    public ResponseEntity<ApiResponse> review(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewCertificationRequest request) {
        return listingCertificationService.reviewCertificationRequest(id, request);
    }
}
