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
//@RequestMapping("/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites")
public class FavoriteListingController {

    private final FavoriteListingServiceInterface favoriteListingService;

    @PostMapping("/investor/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> addFavorite(@Valid @RequestBody AddFavoriteRequest request) {
        return favoriteListingService.addFavorite(request);
    }

    @GetMapping(" /investor/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> getMyFavorites() {
        return favoriteListingService.getMyFavorites();
    }

    @DeleteMapping("/investor/favorites/{listingId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> removeFavorite(@PathVariable("listingId") Integer listingId) {
        return favoriteListingService.removeFavorite(listingId);
    }
}