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
    ResponseEntity<ApiResponse> createListing(CreateListingRequest request, List<MultipartFile> images);

    ResponseEntity<ApiResponse> getMarketListings(int page, int size);

    ResponseEntity<ApiResponse> getListingDetail(Integer listingId);

    ResponseEntity<ApiResponse> getMyListings();

    ResponseEntity<ApiResponse> getMyProperties();

    ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request);
}
