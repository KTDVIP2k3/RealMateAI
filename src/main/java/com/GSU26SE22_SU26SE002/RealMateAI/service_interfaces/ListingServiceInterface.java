package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface ListingServiceInterface {
    /**
     * Luồng ①: Seller đăng lại tài sản ĐÃ CÓ SẴN.
     * Body: application/json
     */
    ResponseEntity<ApiResponse> createListingWithExistingProperty(
            CreateListingWithExistingPropertyRequest request);

    /**
     * Luồng ②: Seller tạo tài sản MỚI + đăng tin + upload ảnh.
     * Body: multipart/form-data
     */
    ResponseEntity<ApiResponse> createListingWithNewProperty(
            CreateListingWithNewPropertyRequest request,
            List<MultipartFile> images);

    ResponseEntity<ApiResponse> getMarketListings(int page, int size);

    ResponseEntity<ApiResponse> getListingDetail(Integer listingId);

    ResponseEntity<ApiResponse> getMyListings();

    ResponseEntity<ApiResponse> getMyProperties();

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
}