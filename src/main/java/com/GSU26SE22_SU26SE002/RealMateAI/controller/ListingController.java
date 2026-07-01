package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithExistingPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithNewPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingStatusRequest;
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
    // POST /listings/existing  — Đăng lại tài sản ĐÃ CÓ SẴN
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/existing",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Đăng tin với tài sản ĐÃ CÓ SẴN")
    public ResponseEntity<ApiResponse> createListingWithExistingProperty(
            @Valid @RequestBody CreateListingWithExistingPropertyRequest request) {
        return listingService.createListingWithExistingProperty(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings/new  — Tạo tài sản MỚI + đăng tin (ĐÃ SỬA ĐỂ HỖ TRỢ SWAGGER)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(
            summary = "Seller: Tạo tài sản MỚI + đăng tin + upload ảnh (multipart/form-data)"
    )
    public ResponseEntity<ApiResponse> createListingWithNewProperty(

            @RequestPart(value = "request", required = true) String requestJson,
            @RequestParam(value = "images", required = true) List<MultipartFile> images) {

        try {
            log.info("[ListingController] Nhận request JSON: {}", requestJson);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            CreateListingWithNewPropertyRequest request =
                    objectMapper.readValue(requestJson, CreateListingWithNewPropertyRequest.class);

            log.info("[ListingController] Parse thành công - propPropertyTypeId={}, propPropertyConditionId={}",
                    request.getPropPropertyTypeId(), request.getPropPropertyConditionId());

            return listingService.createListingWithNewProperty(request, images);

        } catch (JsonProcessingException e) {
            log.error("[ListingController] Lỗi parse JSON request", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Bad_Request", "JSON request không hợp lệ: " + e.getMessage()));
        } catch (Exception e) {
            log.error("[ListingController] Lỗi không xác định", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /listings
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings")
    @Operation(summary = "Lấy danh sách tin đăng công khai")
    public ResponseEntity<ApiResponse> getListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMarketListings(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /listings/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings/{id}")
    @Operation(summary = "Chi tiết tin đăng công khai")
    public ResponseEntity<ApiResponse> getListingDetail(
            @PathVariable("id") Integer listingId) {
        return listingService.getListingDetail(listingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/listings
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/listings")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tin đăng cá nhân")
    public ResponseEntity<ApiResponse> getMyListings() {
        return listingService.getMyListings();
    }
    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/listings/{id}  — NEW
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/listings/{id}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem chi tiết 1 tin đăng của mình (kể cả chưa duyệt)")
    public ResponseEntity<ApiResponse> getMyListingDetail(
            @PathVariable("id") Integer listingId) {
        return listingService.getMyListingDetail(listingId);
    }


    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /seller/listings/{id}  — NEW (soft delete)
    // ─────────────────────────────────────────────────────────────────────────
    @DeleteMapping("/seller/listings/{id}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xoá mềm tin đăng (đặt isActive = false)")
    public ResponseEntity<ApiResponse> deleteMyListing(
            @PathVariable("id") Integer listingId) {
        return listingService.softDeleteListing(listingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /seller/listings/{id}  — NEW: Seller tự đổi trạng thái hiển thị
    // (PAUSE/RESUME). RESUME chỉ hợp lệ khi bài đăng đã được Staff APPROVED.
    // ─────────────────────────────────────────────────────────────────────────
    @PatchMapping("/seller/listings/{id}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Đổi trạng thái hiển thị tin đăng (PAUSE/RESUME)")
    public ResponseEntity<ApiResponse> updateMyListingStatus(
            @PathVariable("id") Integer listingId,
            @Valid @RequestBody UpdateListingStatusRequest request) {
        return listingService.updateListingStatus(listingId, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /seller/properties
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/properties")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Xem danh sách tài sản đang sở hữu")
    public ResponseEntity<ApiResponse> getMyProperties() {
        return listingService.getMyProperties();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /listings/{id}
    // ─────────────────────────────────────────────────────────────────────────
    @PutMapping("/listings/{id}")
    @PreAuthorize("hasAnyRole('Seller', 'Admin', 'Staff')")
    @Operation(summary = "Seller/Admin: Chỉnh sửa nội dung tin đăng và thông số BĐS")
    public ResponseEntity<ApiResponse> updateListing(
            @PathVariable("id") Integer listingId,
            @RequestBody UpdateListingRequest request) {
        return listingService.updateListing(listingId, request);
    }
}