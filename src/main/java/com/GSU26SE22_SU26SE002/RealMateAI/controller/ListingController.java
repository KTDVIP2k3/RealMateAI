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
    // Seller: Tạo bài đăng mới — ảnh đi kèm ngay trong request này
    // (xem draftImagePublicIds trong CreateListingRequest)
    // ─────────────────────────────────────────────────────
    @PostMapping("/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Tạo bài đăng mới (Property mới hoặc đăng lại tài sản cũ, kèm ảnh)")
    public ResponseEntity<ApiResponse> createListing(@Valid @RequestBody CreateListingRequest request) {
        return listingService.createListing(request);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/listings
    // Màn hình Chợ BĐS: danh sách tin đăng đã duyệt, phân trang
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
    // Chi tiết tin đăng công khai: Property & Image
    // ─────────────────────────────────────────────────────
    @GetMapping("/listings/{id}")
    @Operation(summary = "Chi tiết tin đăng công khai: liên kết thông số Property & Image")
    public ResponseEntity<ApiResponse> getListingDetail(@PathVariable("id") Integer listingId) {
        return listingService.getListingDetail(listingId);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/seller/listings
    // Seller: Xem danh sách tin đăng cá nhân của mình
    // ─────────────────────────────────────────────────────
    @GetMapping("/seller/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tin đăng cá nhân")
    public ResponseEntity<ApiResponse> getMyListings() {
        return listingService.getMyListings();
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/seller/properties
    // [MỚI] Seller: Xem danh sách TÀI SẢN mình đang sở hữu
    // → dùng existingPropertyId lấy từ đây để "đăng lại" trong POST /listings
    // ─────────────────────────────────────────────────────
    @GetMapping("/seller/properties")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tài sản (Property) đang sở hữu, dùng để đăng lại")
    public ResponseEntity<ApiResponse> getMyProperties() {
        return listingService.getMyProperties();
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/v1/listings/{id}
    // Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS
    // (Sửa bài → reset chờ duyệt lại; có thể bổ sung thêm ảnh qua draftImagePublicIds)
    // ─────────────────────────────────────────────────────
    @PutMapping("/listings/{id}")
    @PreAuthorize("hasAnyRole('Seller','Admin','Staff')")
    @Operation(summary = "Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable("id") Integer listingId,
            @RequestBody UpdateListingRequest request) {

        return listingService.updateListing(listingId, request);
    }
}
