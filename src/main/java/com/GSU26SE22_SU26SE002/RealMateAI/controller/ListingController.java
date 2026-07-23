package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingServiceInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Listings", description = "API quản lý tin đăng BĐS (Seller)")
public class ListingController {

    private final ListingServiceInterface listingService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings  — API TẠO TIN ĐĂNG DUY NHẤT (gộp 2 luồng cũ)
    //
    // Body: application/json thuần — KHÔNG multipart. Ảnh KHÔNG upload trực
    // tiếp ở API này. Seller phải upload TRƯỚC qua
    //   POST /media/upload/multiple?entityType=ACCOUNT&entityId={accountId}
    // lấy về publicId của từng ảnh, rồi truyền vào field "draftImagePublicIds"
    // của request này — BE chỉ "nhận nuôi" (re-parent) ảnh đã có sẵn trên
    // Cloudinary sang Listing vừa tạo, lưu URL vào bảng listing_image.
    //
    // "reuseExistingProperty":
    //   - true  → đăng lại tài sản ĐÃ CÓ SẴN (cần "existingPropertyId"). Nếu
    //             không gửi ảnh mới, hệ thống tự copy lại ảnh từ 1 Listing
    //             khác cùng Property (nếu có).
    //   - false → tạo tài sản MỚI từ các field "prop*" (bắt buộc), đồng thời
    //             bắt buộc phải có ít nhất 1 ảnh trong "draftImagePublicIds".
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Tạo tin đăng (API duy nhất — set reuseExistingProperty=true/false; ảnh upload trước qua POST /media/upload/multiple)")
    public ResponseEntity<ApiResponse> createListing(
            @Valid @RequestBody CreateListingRequest request) {
        return listingService.createListing(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /listings
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings")
    @Operation(summary = "Lấy danh sách tin đăng công khai (mặc định 10/trang)")
    public ResponseEntity<ApiResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMarketListings(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /listings/{listingId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings/{listingId}")
    @Operation(summary = "Chi tiết tin đăng công khai")
    public ResponseEntity<ApiResponse> getListingDetail(
            @PathVariable("listingId") Integer listingId) {
        return listingService.getListingDetail(listingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/listings — mặc định page=0, size=10
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tin đăng cá nhân (mặc định 10/trang)")
    public ResponseEntity<ApiResponse> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMyListings(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/listings/{listingId}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/listings/{listingId}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem chi tiết 1 tin đăng của mình (kể cả chưa duyệt)")
    public ResponseEntity<ApiResponse> getMyListingDetail(
            @PathVariable("listingId") Integer listingId) {
        return listingService.getMyListingDetail(listingId);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /seller/listings/{listingId}  — Xoá mềm VĨNH VIỄN (status = DELETED)
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/seller/listings/{listingId}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xoá tin đăng vĩnh viễn (status = DELETED — không thể sửa/mở lại)")
    public ResponseEntity<ApiResponse> deleteMyListing(
            @PathVariable("listingId") Integer listingId) {
        return listingService.softDeleteListing(listingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /seller/listings/{listingId}  — Seller tự đổi trạng thái hiển thị
    // bằng cách gửi "status" ĐÍCH (ACTIVE/HIDDEN/DELETED). Chuyển sang ACTIVE
    // chỉ hợp lệ khi bài đăng đã được Staff APPROVED.
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/seller/listings/{listingId}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Đổi trạng thái hiển thị tin đăng (status: ACTIVE / HIDDEN / DELETED)")
    public ResponseEntity<ApiResponse> updateMyListingStatus(
            @PathVariable("listingId") Integer listingId,
            @Valid @RequestBody UpdateListingStatusRequest request) {
        return listingService.updateListingStatus(listingId, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/properties — mặc định page=0, size=10
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/properties")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tài sản đang sở hữu (mặc định 10/trang)")
    public ResponseEntity<ApiResponse> getMyProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMyProperties(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /listings/{listingId}
    //
    // Luồng ảnh: giống luồng ① (existing property) — KHÔNG multipart. Nếu
    // muốn bổ sung thêm ảnh, upload trước qua POST /media/upload/multiple
    // (entityType=ACCOUNT, entityId=accountId của chính Seller), lấy publicId
    // trả về đưa vào "draftImagePublicIds" — ảnh mới sẽ được NỐI THÊM vào bộ
    // ảnh hiện có của Listing (không xoá ảnh cũ).
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/listings/{listingId}")
    @PreAuthorize("hasAnyRole('Seller', 'Admin', 'Staff')")
    @Operation(summary = "Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS (ảnh mới — nếu có — upload trước qua POST /media/upload/multiple)")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable("listingId") Integer listingId,
            @RequestBody UpdateListingRequest request) {
        return listingService.updateListing(listingId, request);
    }
    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings/generate-content — Seller: AI sinh tiêu đề + mô tả
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/generate-content",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: AI (Gemini) sinh tiêu đề + mô tả bài đăng dựa trên thông số tài sản")
    public ResponseEntity<ApiResponse> generateListingContent(
            @Valid @RequestBody GenerateListingContentRequest request) {
        return listingService.generateListingContent(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings/price-suggestion — Seller: AI đề xuất khoảng giá bán
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/price-suggestion",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Đề xuất khoảng giá bán dựa trên tin đăng tương đồng + AI")
    public ResponseEntity<ApiResponse> suggestListingPrice(
            @Valid @RequestBody PriceSuggestionRequest request) {
        return listingService.suggestListingPrice(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings/search — Tìm kiếm nâng cao tin đăng công khai
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/search",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Tìm kiếm nâng cao tin đăng công khai (từ khoá, vị trí, khoảng giá, diện tích, số phòng...)")
    public ResponseEntity<ApiResponse> searchListings(
            @RequestBody(required = false) ListingSearchRequest request) {
        return listingService.searchListings(request != null ? request : new ListingSearchRequest());
    }
}
