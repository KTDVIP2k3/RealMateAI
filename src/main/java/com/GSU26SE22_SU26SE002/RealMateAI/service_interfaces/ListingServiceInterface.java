package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListingServiceInterface {

    /**
     * Seller: tạo bài đăng mới.
     * Tạo (hoặc tái sử dụng) Property + tạo Listing + gắn ảnh (re-parent từ
     * draft MediaAsset) — TẤT CẢ trong 1 transaction duy nhất.
     */
    ResponseEntity<ApiResponse> createListing(CreateListingRequest request);

    /** Public: Chợ BĐS — danh sách bài đăng đã duyệt, có phân trang */
    ResponseEntity<ApiResponse> getMarketListings(int page, int size);

    /** Public: Chi tiết 1 bài đăng công khai */
    ResponseEntity<ApiResponse> getListingDetail(Integer listingId);

    /** Seller: danh sách bài đăng cá nhân (cả chưa duyệt) */
    ResponseEntity<ApiResponse> getMyListings();

    /**
     * Seller: danh sách TÀI SẢN (Property) mà Seller hiện tại đang sở hữu.
     * Dùng để FE hiển thị danh sách cho Seller chọn khi muốn "đăng lại"
     * (gửi existingPropertyId trong CreateListingRequest) thay vì tạo mới.
     */
    ResponseEntity<ApiResponse> getMyProperties();

    /** Seller/Admin: chỉnh sửa nội dung bài đăng + thông số BĐS */
    ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request);
}
