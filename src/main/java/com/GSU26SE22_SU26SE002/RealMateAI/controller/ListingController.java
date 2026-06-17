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
    // POST /api/v1/listings
    // Seller: Form Tạo bài đăng mớ i (khởi tạo Property + Listing)
    // ─────────────────────────────────────────────────────
    @PostMapping("/api/v1/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Tạo bài đăng mới (khởi tạo thực thể tài sản)")
    public ResponseEntity<ApiResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        return listingService.createListing(request);
    }

    // ─────────────────────────────────────────────────────
    // POST /api/v1/listings/{id}/images
    // Seller: Upload ảnh thực tế gắn vào bài đăng (qua Cloudinary)
    // ─────────────────────────────────────────────────────
    @PostMapping(value = "/api/v1/listings/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('Seller','Admin','Staff')")
    @Operation(summary = "Seller: Tải và gán hình ảnh thực tế vào bài đăng")
    public ResponseEntity<ApiResponse> uploadImages(
            @PathVariable("id") Integer listingId,
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "mainImageIndex", required = false, defaultValue = "0") Integer mainImageIndex) {

        return listingService.uploadListingImages(listingId, files, mainImageIndex);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/listings
    // Màn hình Chợ BĐS: danh sách tin đăng đã duyệt, phân trang
    // ─────────────────────────────────────────────────────
    @GetMapping("/api/v1/listings")
    @Operation(summary = " Lấy danh sách tin đăng")
    public ResponseEntity<ApiResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return listingService.getMarketListings(page, size);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/listings/{id}
    // Chi tiết tin đăng công khai: Property & Image
    // ─────────────────────────────────────────────────────
    @GetMapping("/api/v1/listings/{id}")
    @Operation(summary = "Chi tiết tin đăng công khai: liên kết thông số Property & Image")
    public ResponseEntity<ApiResponse> getListingDetail(@PathVariable("id") Integer listingId) {
        return listingService.getListingDetail(listingId);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/seller/listings
    // Seller: Xem danh sách tin đăng cá nhân của mình
    // ─────────────────────────────────────────────────────
    @GetMapping("/api/v1/seller/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tin đăng cá nhân")
    public ResponseEntity<ApiResponse> getMyListings() {
        return listingService.getMyListings();
    }

//     ─────────────────────────────────────────────────────
//     PUT /api/v1/listings/{id}
//     Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS
//     ─────────────────────────────────────────────────────
    @PutMapping("/api/v1/listings/{id}")
    @PreAuthorize("hasAnyRole('Seller','Admin','Staff')")
    @Operation(summary = "Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable("id") Integer listingId,
            @RequestBody UpdateListingRequest request) {

        return listingService.updateListing(listingId, request);
    }
}
