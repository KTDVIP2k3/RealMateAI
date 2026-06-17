package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.AddFavoriteRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.FavoriteListingServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "API quản lý danh sách yêu thích BĐS")
public class FavoriteListingController {

    private final FavoriteListingServiceInterface favoriteListingService;

    // ─────────────────────────────────────────────────────
    // POST /api/v1/favorites
    // Body: { "listingId": 42 }
    // ─────────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Thêm BĐS vào danh sách yêu thích")
    public ResponseEntity<ApiResponse> addFavorite(@Valid @RequestBody AddFavoriteRequest request) {
        return favoriteListingService.addFavorite(request);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/v1/favorites
    // ─────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách tin đăng quan tâm cá nhân")
    public ResponseEntity<ApiResponse> getMyFavorites() {
        return favoriteListingService.getMyFavorites();
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/v1/favorites/{id}
    // {id} = favoriteListingId
    // ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Bỏ BĐS khỏi danh mục yêu thích")
    public ResponseEntity<ApiResponse> removeFavorite(@PathVariable("id") Integer favoriteListingId) {
        return favoriteListingService.removeFavorite(favoriteListingId);
    }
}
