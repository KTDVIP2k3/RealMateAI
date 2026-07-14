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
@RequiredArgsConstructor
@Tag(name = "Listing Verification")
public class ListingVerificationController {

    private final ListingVerificationServiceInterface verificationService;

    @GetMapping("/staff/listings/pending")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    public ResponseEntity<ApiResponse> getPendingQueue() {
        return verificationService.getPendingQueue();
    }

    @PostMapping("/staff/listings/{listingId}/verification")
    @PreAuthorize("hasAnyRole('Staff','Admin')")
    public ResponseEntity<ApiResponse> verifyListing(
            @PathVariable("listingId") Integer listingId,
            @Valid @RequestBody VerifyListingRequest request) {
        return verificationService.verifyListing(listingId, request);
    }

    @GetMapping("/staff/listings/{listingId}/verification")
    @PreAuthorize("hasAnyRole('Staff','Admin','Seller')")
    public ResponseEntity<ApiResponse> getVerificationStatus(@PathVariable("listingId") Integer listingId) {
        return verificationService.getVerificationStatus(listingId);
    }
}