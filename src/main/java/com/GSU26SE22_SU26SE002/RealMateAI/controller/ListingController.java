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
    // POST /listings/existing  — Đăng lại tài sản ĐÃ CÓ SẴN
    //
    // Luồng ảnh: KHÔNG upload trực tiếp ở API này (application/json thuần).
    // Nếu muốn thêm ảnh mới cho property, Seller phải upload TRƯỚC qua
    //   POST /media/upload/multiple?entityType=ACCOUNT&entityId={accountId}
    // lấy về publicId của từng ảnh, rồi truyền vào field "draftImagePublicIds"
    // của request này — BE sẽ tự "nhận nuôi" (re-parent) các ảnh đó, upload
    // thật sự lên Cloudinary đã xảy ra ở bước /media/upload/multiple, ở đây
    // chỉ là đổi chủ sở hữu ảnh (từ tạm ACCOUNT sang chính thức LISTING) và
    // lưu URL Cloudinary vào bảng listing_image. Nếu không gửi ảnh mới, hệ
    // thống tự copy lại ảnh từ 1 Listing khác cùng Property (nếu có).
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/existing",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Đăng tin với tài sản ĐÃ CÓ SẴN (ảnh mới — nếu có — upload trước qua POST /media/upload/multiple)")
    public ResponseEntity<ApiResponse> createListingWithExistingProperty(
            @Valid @RequestBody CreateListingWithExistingPropertyRequest request) {
        return listingService.createListingWithExistingProperty(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /listings/new  — Tạo tài sản MỚI + đăng tin (multipart/form-data)
    //
    // Luồng ảnh: upload TRỰC TIẾP trong CHÍNH request này qua part "images"
    // (bắt buộc ≥ 1 file) — KHÔNG cần gọi /media/upload/multiple trước như
    // luồng ①, vì Property/Listing chưa tồn tại nên chưa có gì để "nhận nuôi"
    // ảnh trước đó. BE tự: (1) upload từng file lên Cloudinary, (2) tạo
    // Property + Listing, (3) lưu URL Cloudinary trả về vào bảng
    // listing_image, gắn đúng listingId vừa tạo. "thumbnailImageIndex" (0-based,
    // ứng với vị trí trong mảng "images") chỉ định ảnh nào là ảnh đại diện
    // (isThumbnail = true, luôn xếp đầu khi GET chi tiết tin đăng).
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping(
            value = "/listings/new",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('Seller')")
    @Operation(
            summary = "Seller: Tạo tài sản MỚI + đăng tin + upload ảnh trực tiếp (multipart/form-data, bắt buộc ≥1 ảnh)"
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
