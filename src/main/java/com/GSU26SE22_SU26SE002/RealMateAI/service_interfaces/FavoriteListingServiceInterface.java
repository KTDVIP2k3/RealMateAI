package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.AddFavoriteRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface FavoriteListingServiceInterface {
    /** Thêm bài đăng vào danh sách yêu thích của user hiện tại */
    ResponseEntity<ApiResponse> addFavorite(AddFavoriteRequest request);

    /** Lấy danh sách yêu thích của user hiện tại */
    ResponseEntity<ApiResponse> getMyFavorites();

    /** Bỏ 1 bài đăng khỏi danh sách yêu thích (favoriteListingId) */
    ResponseEntity<ApiResponse> removeFavorite(Integer favoriteListingId);
}
