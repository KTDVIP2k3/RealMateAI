package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;


import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.RecommendationResult;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.FavoriteListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.RecommendationResultRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.RecommendationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImplement implements RecommendationService {

    private final RecommendationResultRepository recommendationResultRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final AuthenUntil authenUntil;
    private final InvestorRepository investorRepository;
    private final FavoriteListingRepository favoriteListingRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getRecommendationsForCurrentUser() {
        try {
            // SỬA: lấy accountId từ TOKEN đăng nhập, không nhận qua tham số
            // nữa — API này giờ CHỈ trả gợi ý của CHÍNH người đang gọi.
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Vui lòng đăng nhập để xem gợi ý dành cho bạn"));
            }
            Integer userId = currentUser.getAccountId();

            List<RecommendationResult> recs = recommendationResultRepository.findByAccountIdOrderByRankAsc(userId);

            if (recs.isEmpty()) {
                // MỚI: cold-start / chưa train — trả về rỗng kèm message rõ ràng
                // thay vì lỗi, để FE có thể fallback sang hiển thị "Tin mới nhất"
                // hoặc "Phổ biến nhất" thay thế.
                return ResponseEntity.ok(ApiResponse.success(
                        List.of(),
                        "Chưa có gợi ý cho bạn (có thể do chưa đủ dữ liệu tương tác, hoặc chưa chạy batch train gần đây)"));
            }

            List<Integer> listingIds = recs.stream().map(RecommendationResult::getListingId).toList();
            Map<Integer, Listing> listingById = listingRepository.findAllByListingIdInWithDetails(listingIds)
                    .stream().collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

            // SỬA: trước đây hardcode isFavorited=false cho mọi listing — giờ
            // tra đúng danh sách yêu thích THẬT của user hiện tại, đồng bộ với
            // cách getMarketListings/getFeaturedListings đang làm.
            Set<Integer> favoritedIds = Collections.emptySet();
            Investor investor = investorRepository.findByAccount_AccountId(userId).orElse(null);
            if (investor != null) {
                favoritedIds = new HashSet<>(
                        favoriteListingRepository.findFavoritedListingIdsByInvestorId(investor.getInvestorId()));
            }
            final Set<Integer> favIds = favoritedIds;

            List<Map<String, Object>> content = recs.stream()
                    .map(r -> {
                        Listing listing = listingById.get(r.getListingId());
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listingId", r.getListingId());
                        item.put("score", r.getScore());
                        item.put("rank", r.getRank());
                        // Listing có thể đã bị ẩn/xoá sau khi batch job chạy —
                        // trả null thay vì lỗi để không vỡ cả danh sách gợi ý.
                        item.put("listing", listing != null
                                ? listingMapper.toListingSummary(listing, favIds.contains(listing.getListingId()))
                                : null);
                        return item;
                    })
                    .filter(m -> m.get("listing") != null)
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("userId", userId);
            result.put("content", content);
            result.put("generatedAt", recs.get(0).getGeneratedAt());

            return ResponseEntity.ok(ApiResponse.success(result, "Gợi ý bất động sản dành cho bạn"));
        } catch (Exception e) {
            log.error("[RecommendationService] getRecommendationsForCurrentUser lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
