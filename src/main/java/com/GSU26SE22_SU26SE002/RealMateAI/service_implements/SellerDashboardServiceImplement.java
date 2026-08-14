package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Seller;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ActiveLogRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.FavoriteListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.SellerDashboardKpiDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.SellerTopListingDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SellerDashboardServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class SellerDashboardServiceImplement implements SellerDashboardServiceInterface {

    private final AuthenUntil authenUntil;
    private final SellerRepository sellerRepository;
    private final ListingRepository listingRepository;
    private final ActiveLogRepository activeLogRepository;
    private final FavoriteListingRepository favoriteListingRepository;

    private Seller getCurrentSeller() {
        Account currentUser = authenUntil.getCurrentUSer();
        return sellerRepository.findByAccount_AccountId(currentUser.getAccountId())
                .orElseThrow(() -> new RuntimeException("Seller profile không tồn tại"));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getSellerDashboardKpi() {
        try {
            Seller seller = getCurrentSeller();

            // Lấy TOÀN BỘ listing của Seller (không phân trang) — 1 Seller
            // thường không có số lượng tin quá lớn nên gom hết vào bộ nhớ để
            // đếm 1 lần là hợp lý, tránh nhiều round-trip DB.
            List<Listing> listings = listingRepository.findBySellerId(seller.getSellerId(), Pageable.unpaged()).getContent();

            long totalListings = listings.size();
            long activeListings = listings.stream()
                    .filter(l -> l.getStatus() == SellerListingStatusEnum.ACTIVE).count();
            long hiddenListings = listings.stream()
                    .filter(l -> l.getStatus() == SellerListingStatusEnum.HIDDEN).count();
            long pendingApproval = listings.stream()
                    .filter(l -> {
                        ListingVerification lv = l.getListingVerification();
                        return lv != null && lv.getStatus() == ListingStatusEnum.PENDING;
                    }).count();

            List<Integer> listingIds = listings.stream().map(Listing::getListingId).toList();
            long totalViews = listingIds.isEmpty() ? 0
                    : activeLogRepository.countByListingIdInAndEventType(listingIds, UserEventTypeEnum.VIEW);
            long totalContacts = listingIds.isEmpty() ? 0
                    : activeLogRepository.countByListingIdInAndEventType(listingIds, UserEventTypeEnum.CONTACT);
            long totalSaved = listingIds.isEmpty() ? 0
                    : favoriteListingRepository.countByListing_ListingIdIn(listingIds);

            SellerDashboardKpiDTO dto = SellerDashboardKpiDTO.builder()
                    .totalListings(totalListings)
                    .activeListings(activeListings)
                    .hiddenListings(hiddenListings)
                    .pendingApproval(pendingApproval)
                    .totalViews(totalViews)
                    .totalSaved(totalSaved)
                    .totalContacts(totalContacts)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dto, "Seller dashboard KPI"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", e.getMessage()));
        } catch (Exception e) {
            log.error("[SellerDashboardService] getSellerDashboardKpi lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getTopListings(int limit) {
        try {
            Seller seller = getCurrentSeller();
            int effectiveLimit = limit > 0 ? limit : 5;

            List<Listing> listings = listingRepository.findBySellerId(seller.getSellerId(), Pageable.unpaged()).getContent();
            List<Integer> listingIds = listings.stream().map(Listing::getListingId).toList();

            Map<Integer, Long> viewCountByListing = new HashMap<>();
            Map<Integer, Long> saveCountByListing = new HashMap<>();

            if (!listingIds.isEmpty()) {
                activeLogRepository.countGroupedByListingId(listingIds, UserEventTypeEnum.VIEW)
                        .forEach(p -> viewCountByListing.put(p.getListingId(), p.getViewCount()));
                favoriteListingRepository.countGroupedByListingId(listingIds)
                        .forEach(row -> saveCountByListing.put((Integer) row[0], (Long) row[1]));
            }

            List<SellerTopListingDTO> topListings = listings.stream()
                    .map(l -> SellerTopListingDTO.builder()
                            .listingId(l.getListingId())
                            .title(l.getTitle())
                            .price(l.getPrice())
                            .viewCount(viewCountByListing.getOrDefault(l.getListingId(), 0L))
                            .saveCount(saveCountByListing.getOrDefault(l.getListingId(), 0L))
                            .status(l.getStatus() != null ? l.getStatus().name() : null)
                            .build())
                    // Sắp xếp theo viewCount giảm dần — đúng ý nghĩa "Top BĐS được xem nhiều nhất"
                    .sorted(Comparator.comparingLong(SellerTopListingDTO::getViewCount).reversed())
                    .limit(effectiveLimit)
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(topListings, "Top " + effectiveLimit + " BĐS được xem/lưu nhiều nhất"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", e.getMessage()));
        } catch (Exception e) {
            log.error("[SellerDashboardService] getTopListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
