package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.FavoriteListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AddFavoriteRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.FavoriteListingResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingSummaryResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.FavoriteListingServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteListingServiceImplement  implements FavoriteListingServiceInterface {

    private final FavoriteListingRepository favoriteListingRepository;
    private final ListingRepository listingRepository;
    private final InvestorRepository investorRepository;
    private final AuthenUntil authenUntil;

    // ════════════════════════════════════════════════════
    //  POST /favorites
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> addFavorite(AddFavoriteRequest request) {
        try {
            Investor investor = getCurrentInvestorOrNull();
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Chỉ Investor mới có thể sử dụng danh sách yêu thích"));
            }

            Integer listingId = request.getListingId();

            // 1. Listing phải tồn tại và đang active (đã duyệt, hiển thị trên Chợ BĐS)
            Listing listing = listingRepository.findActiveById(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại hoặc chưa được duyệt"));
            }

            // 2. Chống duplicate
            boolean alreadyFavorited = favoriteListingRepository
                    .existsByInvestor_InvestorIdAndListing_ListingId(investor.getInvestorId(), listingId);
            if (alreadyFavorited) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bạn đã yêu thích bài đăng này rồi"));
            }

            // 3. Lưu
            FavoriteListing favorite = FavoriteListing.builder()
                    .investor(investor)
                    .listing(listing)
                    .build();

            FavoriteListing saved = favoriteListingRepository.save(favorite);
            log.info("[FavoriteService] investorId={} đã thêm listingId={} vào yêu thích (favoriteId={})",
                    investor.getInvestorId(), listingId, saved.getFavoriteListingId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            Map.of("favoriteListingId", saved.getFavoriteListingId()),
                            "Đã thêm vào danh sách yêu thích"));

        } catch (Exception e) {
            log.error("[FavoriteService] addFavorite lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  GET /favorites
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyFavorites() {
        try {
            Investor investor = getCurrentInvestorOrNull();
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Chỉ Investor mới có thể sử dụng danh sách yêu thích"));
            }

            List<FavoriteListingResponse> favorites = favoriteListingRepository
                    .findByInvestorIdWithDetails(investor.getInvestorId())
                    .stream()
                    .map(this::toFavoriteResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(favorites, "Danh sách yêu thích"));

        } catch (Exception e) {
            log.error("[FavoriteService] getMyFavorites lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  DELETE /favorites/{id}
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> removeFavorite(Integer favoriteListingId) {
        try {
            Investor investor = getCurrentInvestorOrNull();
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Chỉ Investor mới có thể sử dụng danh sách yêu thích"));
            }

            FavoriteListing favorite = favoriteListingRepository
                    .findByFavoriteListingIdAndInvestor_InvestorId(favoriteListingId, investor.getInvestorId())
                    .orElse(null);

            if (favorite == null) {
                // 404 — không lộ thông tin liệu favoriteListingId có tồn tại nhưng thuộc investor khác
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy mục yêu thích hoặc bạn không có quyền xóa"));
            }

            favoriteListingRepository.delete(favorite);
            log.info("[FavoriteService] investorId={} đã xóa favoriteListingId={}",
                    investor.getInvestorId(), favoriteListingId);

            return ResponseEntity.ok(ApiResponse.success(null, "Đã xóa khỏi danh sách yêu thích"));

        } catch (Exception e) {
            log.error("[FavoriteService] removeFavorite lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  Helper
    // ════════════════════════════════════════════════════

    /**
     * Lấy Investor tương ứng với Account hiện tại.
     * Trả về null nếu chưa đăng nhập HOẶC account không phải Investor
     * → Controller/Service trả 401/403 tương ứng.
     */
    private Investor getCurrentInvestorOrNull() {
        Account currentUser = authenUntil.getCurrentUSer();
        if (currentUser == null) {
            return null;
        }
        return investorRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
    }

    // ════════════════════════════════════════════════════
    //  Mapper
    // ════════════════════════════════════════════════════

    private FavoriteListingResponse toFavoriteResponse(FavoriteListing fl) {
        Listing l = fl.getListing();
        Property p = l.getProperty();

        // Thumbnail: ảnh is_main hoặc ảnh đầu tiên trong property_image
        String thumbnail = (p == null || p.getPropertyImages() == null || p.getPropertyImages().isEmpty())
                ? null
                : p.getPropertyImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getPropertyImages().stream()
                        .min(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(PropertyImage::getImageUrl)
                        .orElse(null));

        ListingSummaryResponse summary = ListingSummaryResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .price(l.getPrice())
                .area(p != null ? p.getArea() : null)
                .bedroom(p != null ? p.getBedroom() : null)
                .bathroom(p != null ? p.getBathroom() : null)
                .propertyTypeName(p != null && p.getPropertyType() != null ? p.getPropertyType().getName() : null)
                .thumbnailUrl(thumbnail)
                .isActive(l.getIsActive())
                .createdAt(l.getCreatedAt())
                .isFavorited(true) // hiển nhiên true — đây là danh sách yêu thích
                .build();

        return FavoriteListingResponse.builder()
                .favoriteListingId(fl.getFavoriteListingId())
                .addedAt(fl.getCreatedAt())
                .listing(summary)
                .build();
    }
}
