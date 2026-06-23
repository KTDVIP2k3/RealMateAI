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
@Tag(name = "Listing Verification", description = "Duyệt tin đăng - chỉ giữ trạng thái hiện tại")
public class ListingVerificationController {

    private final ListingVerificationServiceInterface verificationService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    @Operation(summary = "Hàng đợi chờ duyệt")
    public ResponseEntity<ApiResponse> getPendingQueue() {
        return verificationService.getPendingQueue();
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    @Operation(summary = "Duyệt bài đăng (APPROVED / REJECTED)")
    public ResponseEntity<ApiResponse> verifyListing(
            @PathVariable("id") Integer listingId,
            @Valid @RequestBody VerifyListingRequest request) {
        return verificationService.verifyListing(listingId, request);
    }

    @GetMapping("/{id}/verifications")
    @PreAuthorize("hasAnyRole('Staff','Admin','Seller')")
    @Operation(summary = "Lấy TRẠNG THÁI DUYỆT HIỆN TẠI của bài đăng")
    public ResponseEntity<ApiResponse> getVerificationStatus(@PathVariable("id") Integer listingId) {
        return verificationService.getVerificationStatus(listingId);
    }
}