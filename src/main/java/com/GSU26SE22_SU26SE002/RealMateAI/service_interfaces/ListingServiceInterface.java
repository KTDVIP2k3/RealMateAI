package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface ListingServiceInterface {
    /**
     * API tạo tin đăng DUY NHẤT — gộp 2 luồng tạo tin cũ (đăng lại tài sản có
     * sẵn / tạo tài sản mới) thành 1 API. Body: application/json (ảnh KHÔNG
     * upload trực tiếp — phải upload trước qua POST /media/upload/multiple,
     * truyền publicId vào request.draftImagePublicIds).
     * request.reuseExistingProperty quyết định dùng lại tài sản có sẵn
     * (existingPropertyId) hay tạo tài sản mới (các field prop*).
     */
    ResponseEntity<ApiResponse> createListing(CreateListingRequest request);

    ResponseEntity<ApiResponse> getMarketListings(int page, int size);

    ResponseEntity<ApiResponse> getListingDetail(Integer listingId);

    /** Mặc định 10 tin/trang nếu size &lt;= 0 (xem PAGE_SIZE trong ListingServiceImplement). */
    ResponseEntity<ApiResponse> getMyListings(int page, int size);

    /** Mặc định 10 tài sản/trang nếu size &lt;= 0. */
    ResponseEntity<ApiResponse> getMyProperties(int page, int size);

    ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request);

    ResponseEntity<ApiResponse> getMyListingDetail(Integer listingId);

    /**
     * Seller xoá mềm VĨNH VIỄN bài đăng của mình (status → DELETED). Khác
     * chuyển sang HIDDEN: một khi đã xoá thì không thể sửa/mở lại/xem lại
     * trong các API quản lý của Seller nữa. Về bản chất đây là 1 lối gọi tắt
     * của {@link #updateListingStatus} với status = DELETED — giữ lại để
     * tương thích với endpoint DELETE /seller/listings/{id} cũ.
     */
    ResponseEntity<ApiResponse> softDeleteListing(Integer listingId);

    /**
     * Seller tự đổi trạng thái hiển thị bài đăng của mình bằng cách gửi lên
     * status ĐÍCH: HIDDEN (tạm ẩn), DELETED (xoá mềm vĩnh viễn — không thể
     * sửa/mở lại), ACTIVE (mở lại từ HIDDEN). Chuyển sang ACTIVE chỉ được
     * phép khi verification hiện tại = APPROVED.
     */
    ResponseEntity<ApiResponse> updateListingStatus(Integer listingId, UpdateListingStatusRequest request);

    /**
     * Seller: AI (Gemini) sinh tiêu đề + mô tả bài đăng dựa trên thông số tài sản.
     * Không lưu DB — chỉ trả nội dung gợi ý để Seller xem/chỉnh trước khi đăng thật.
     */
    ResponseEntity<ApiResponse> generateListingContent(GenerateListingContentRequest request);

    /**
     * Seller: Đề xuất khoảng giá bán hợp lý dựa trên tin đăng tương đồng đang hoạt
     * động trên thị trường (cùng loại BĐS + cùng phường/xã), kết hợp phân tích của AI.
     */
    ResponseEntity<ApiResponse> suggestListingPrice(PriceSuggestionRequest request);

    /**
     * Tìm kiếm nâng cao tin đăng công khai trên Chợ BĐS theo nhiều tiêu chí kết hợp
     * (từ khoá, loại BĐS, vị trí, khoảng giá, khoảng diện tích, số phòng, hướng...).
     */
    ResponseEntity<ApiResponse> searchListings(ListingSearchRequest request);

    /**
     * MỚI: GET /listings/search/suggestions — Autocomplete Suggestion khi Investor
     * gõ vào ô tìm kiếm (q). Gộp 4 nhóm: Location (Phường/Tỉnh khớp tên), Listing
     * (tin đăng/dự án khớp tên), Property Type (loại BĐS khớp tên), Recent Search
     * (lịch sử tìm kiếm gần đây CỦA CHÍNH người dùng đang đăng nhập — rỗng nếu
     * chưa đăng nhập). q rỗng/null: chỉ trả Recent Search (nếu đã đăng nhập), 3
     * nhóm còn lại rỗng.
     */
    ResponseEntity<ApiResponse> getSearchSuggestions(String q);
}