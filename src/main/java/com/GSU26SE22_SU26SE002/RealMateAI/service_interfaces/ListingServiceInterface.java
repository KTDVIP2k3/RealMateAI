package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListingServiceInterface {

    /** Seller: tạo bài đăng mới — khởi tạo Property + Listing + Location */
    ResponseEntity<ApiResponse> createListing(CreateListingRequest request);

    /** Seller: upload ảnh minh hoạ cho bài đăng (Cloudinary) */
    ResponseEntity<ApiResponse> uploadListingImages(Integer listingId, List<MultipartFile> files, Integer mainImageIndex);

    /** Public: Chợ BĐS — danh sách bài đăng đã duyệt, có phân trang */
    ResponseEntity<ApiResponse> getMarketListings(int page, int size);

    /** Public: Chi tiết 1 bài đăng công khai */
    ResponseEntity<ApiResponse> getListingDetail(Integer listingId);

    /** Seller: danh sách bài đăng cá nhân (cả chưa duyệt) */
    ResponseEntity<ApiResponse> getMyListings();

    /** Seller/Admin: chỉnh sửa nội dung bài đăng + thông số BĐS */
    ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request);
}
