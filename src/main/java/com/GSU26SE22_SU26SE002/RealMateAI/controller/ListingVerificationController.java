package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingVerificationServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/staff/listings")
@RequiredArgsConstructor
@Tag(name = "Listing Verification", description = "Staff/Admin duyệt tin đăng (nội dung + ảnh)")
public class ListingVerificationController {

    private final ListingVerificationServiceInterface verificationService;

    // ─────────────────────────────────────────────────────
    // GET /api/v1/staff/listings/pending
    // Hàng đợi chờ duyệt — đã JOIN FETCH sẵn property.images
    // ─────────────────────────────────────────────────────
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    @Operation(summary = "Staff/Admin: Hàng đợi bài đăng chờ duyệt (kèm sẵn ảnh + nội dung)")
    public ResponseEntity<ApiResponse> getPendingQueue() {
        return verificationService.getPendingQueue();
    }

    // ─────────────────────────────────────────────────────
    // POST /api/v1/staff/listings/{id}/verify
    // Duyệt 1 bài đăng — APPROVED yêu cầu Property phải có ảnh
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    @Operation(summary = "Staff/Admin: Duyệt bài đăng (xét đồng thời nội dung và ảnh)")
    public ResponseEntity<ApiResponse> verifyListing(
            @PathVariable("id") Integer listingId,
            @Valid @RequestBody VerifyListingRequest request) {

        return verificationService.verifyListing(listingId, request);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/staff/listings/{id}/verifications
    // Lịch sử duyệt — kể cả các lần REJECTED trước đó
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/verifications")
    @PreAuthorize("hasAnyRole('Staff','Admin','Seller')")
    @Operation(summary = "Lịch sử duyệt của 1 bài đăng")
    public ResponseEntity<ApiResponse> getVerificationHistory(@PathVariable("id") Integer listingId) {
        return verificationService.getVerificationHistory(listingId);
    }
}
