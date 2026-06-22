package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Listings", description = "API quản lý tin đăng BĐS (Seller)")
public class ListingController {
    private final ListingServiceInterface listingService;

    // ─────────────────────────────────────────────────────
    // POST /api/v1/listings  — multipart/form-data
    // Seller: Tạo bài đăng + upload ảnh trong 1 lần gọi
    // ─────────────────────────────────────────────────────
    @PostMapping(value = "/listings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_Seller')")
    @Operation(summary = "Seller: Tạo bài đăng mới + upload ảnh trong 1 request (multipart/form-data)")
    public ResponseEntity<ApiResponse> createListing(
            @RequestPart("data") @Valid CreateListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return listingService.createListing(request, images);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/listings
    // ─────────────────────────────────────────────────────
    @GetMapping("/listings")
    @Operation(summary = "Lấy danh sách tin đăng")
    public ResponseEntity<ApiResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMarketListings(page, size);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/listings/{id}
    // ─────────────────────────────────────────────────────
    @GetMapping("/listings/{id}")
    @Operation(summary = "Chi tiết tin đăng công khai")
    public ResponseEntity<ApiResponse> getListingDetail(@PathVariable("id") Integer listingId) {
        return listingService.getListingDetail(listingId);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/seller/listings
    // ─────────────────────────────────────────────────────
    @GetMapping("/seller/listings")
    @PreAuthorize("hasAnyAuthority('ROLE_Seller')")
    @Operation(summary = "Seller: Xem danh sách tin đăng cá nhân")
    public ResponseEntity<ApiResponse> getMyListings() {
        return listingService.getMyListings();
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/seller/properties
    // ─────────────────────────────────────────────────────
    @GetMapping("/seller/properties")
    @PreAuthorize("hasAnyAuthority('ROLE_Seller')")
    @Operation(summary = "Seller: Xem danh sách tài sản đang sở hữu")
    public ResponseEntity<ApiResponse> getMyProperties() {
        return listingService.getMyProperties();
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/v1/listings/{id}
    // ─────────────────────────────────────────────────────
    @PutMapping("/listings/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_Seller','ROLE_Admin','ROLE_Staff')")
    @Operation(summary = "Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable("id") Integer listingId,
            @RequestBody UpdateListingRequest request) {
        return listingService.updateListing(listingId, request);
    }
}
