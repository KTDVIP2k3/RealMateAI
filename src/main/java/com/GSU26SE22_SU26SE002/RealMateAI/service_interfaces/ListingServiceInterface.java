package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithExistingPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithNewPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
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
}