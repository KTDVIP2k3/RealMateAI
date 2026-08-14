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
import org.springframework.http.HttpStatus;
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

    @PostMapping(
            value = "/seller/listings",
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
    // MỚI: GET /listings/featured — "Tin nổi bật", xếp theo số lượt xem THẬT
    // (đếm từ ActiveLog). Đặt tên path cụ thể "featured" nên KHÔNG bị nhầm với
    // /listings/{listingId} (Spring ưu tiên khớp path literal trước path
    // variable, đã có tiền lệ với /listings/search cùng tồn tại).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings/featured")
    @Operation(summary = "Tin đăng nổi bật — xếp theo số lượt xem thật (ActiveLog), chỉ tin đã duyệt")
    public ResponseEntity<ApiResponse> getFeaturedListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getFeaturedListings(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MỚI: GET /listings/compare?ids=1,2 — So sánh 2-4 tin đăng, trả đầy đủ
    // chi tiết từng tin để FE tự dựng bảng so sánh. Dùng query param dạng
    // "ids=1,2,3" (1 chuỗi phân tách bởi dấu phẩy) thay vì "ids=1&ids=2" —
    // dễ chia sẻ URL hơn (VD copy link so sánh gửi cho người khác).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings/compare")
    @Operation(summary = "So sánh 2-4 tin đăng — trả đầy đủ chi tiết từng tin để FE tự dựng bảng so sánh")
    public ResponseEntity<ApiResponse> compareListings(
            @RequestParam("ids") String ids) {
        List<Integer> listingIds;
        try {
            listingIds = java.util.Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("Bad_Request", "Tham số ids phải là danh sách số nguyên phân tách bởi dấu phẩy, VD: ids=1,2"));
        }
        return listingService.compareListings(listingIds);
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
    @Operation(summary = "Seller: Xem danh sách tài sản đang sở hữu (mặc định 10/trang, page=0&size=0 -> lấy hết)")
    public ResponseEntity<ApiResponse> getMyProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return listingService.getMyProperties(page, size);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MỚI: GET /seller/properties/{propertyId} — chi tiết 1 tài sản của chính
    // Seller đang đăng nhập (dùng khi cần xem đầy đủ thông tin trước khi chọn
    // "Dùng lại tài sản có sẵn" lúc tạo tin, hoặc để sửa thông tin tài sản).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/seller/properties/{propertyId}")
    @PreAuthorize("hasRole('Seller')")
    @Operation(summary = "Seller: Chi tiết 1 tài sản đang sở hữu")
    public ResponseEntity<ApiResponse> getMyPropertyDetail(
            @PathVariable("propertyId") Integer propertyId) {
        return listingService.getMyPropertyDetail(propertyId);
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
    @PutMapping("/seller/listings/{listingId}")
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
            value = "/seller/listings/generate-content",
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
            value = "/seller/listings/price-suggestion",
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

    // ─────────────────────────────────────────────────────────────────────────
    // MỚI: GET /listings/search — bản query-string của API tìm kiếm ở trên, để
    // FE dựng được URL chia sẻ/back-forward được (VD /listings/search?q=vin&province=79)
    // mà không cần gửi body JSON. KHÔNG viết lại logic lọc/JOIN/phân trang lần 2 —
    // chỉ map query param sang đúng ListingSearchRequest rồi tái sử dụng lại
    // ListingServiceInterface#searchListings() đã có, tránh lệch hành vi giữa 2 API.
    // bedrooms/bathrooms là số lượng TỐI THIỂU (tương đương minBedroom/minBathroom
    // của bản POST), propertyType là propertyTypeId (số).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/listings/search")
    @Operation(summary = "Tìm kiếm nâng cao tin đăng công khai bằng query string (q, propertyType, minPrice, maxPrice, minArea, maxArea, bedrooms, bathrooms, province, ward, sellerId, minLat, maxLat, minLong, maxLong)")
    public ResponseEntity<ApiResponse> searchListingsByQuery(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer propertyType,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Integer bathrooms,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) Integer sellerId,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLong,
            @RequestParam(required = false) Double maxLong) {

        ListingSearchRequest request = new ListingSearchRequest();
        request.setKeyword(q);
        request.setPage(page);
        request.setSize(size);
        request.setPropertyTypeId(propertyType);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinArea(minArea);
        request.setMaxArea(maxArea);
        request.setMinBedroom(bedrooms);
        request.setMinBathroom(bathrooms);
        request.setProvinceCode(province);
        request.setWardCode(ward);
        request.setSellerId(sellerId);
        request.setMinLat(minLat);
        request.setMaxLat(maxLat);
        request.setMinLong(minLong);
        request.setMaxLong(maxLong);

        return listingService.searchListings(request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MỚI: GET /listings/search/suggestions — Autocomplete Suggestion cho ô tìm
    // kiếm. VD q="vin" -> gợi ý gộp 4 nhóm: Location / Listing / Property Type /
    // Recent Search (nhóm cuối chỉ có khi đã đăng nhập).
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/investor/listings/search/suggestions")
    @Operation(summary = "Autocomplete Suggestion cho ô tìm kiếm (Location / Listing / Property Type / Recent Search)")
    public ResponseEntity<ApiResponse> getSearchSuggestions(
            @RequestParam(required = false) String q) {
        return listingService.getSearchSuggestions(q);
    }
}
