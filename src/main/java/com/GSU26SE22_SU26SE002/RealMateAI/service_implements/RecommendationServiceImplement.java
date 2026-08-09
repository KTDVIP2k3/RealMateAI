package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;


import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.RecommendationResult;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.RecommendationResultRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.RecommendationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImplement implements RecommendationService {

    private final RecommendationResultRepository recommendationResultRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getRecommendationsForUser(Integer userId) {
        try {
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "userId không được để trống"));
            }

            List<RecommendationResult> recs = recommendationResultRepository.findByAccountIdOrderByRankAsc(userId);

            if (recs.isEmpty()) {
                // MỚI: cold-start / chưa train — trả về rỗng kèm message rõ ràng
                // thay vì lỗi, để FE có thể fallback sang hiển thị "Tin mới nhất"
                // hoặc "Phổ biến nhất" thay thế.
                return ResponseEntity.ok(ApiResponse.success(
                        List.of(),
                        "Chưa có gợi ý cho user này (có thể do chưa đủ dữ liệu tương tác, hoặc chưa chạy batch train gần đây)"));
            }

            List<Integer> listingIds = recs.stream().map(RecommendationResult::getListingId).toList();
            Map<Integer, Listing> listingById = listingRepository.findAllByListingIdInWithDetails(listingIds)
                    .stream().collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

            List<Map<String, Object>> content = recs.stream()
                    .map(r -> {
                        Listing listing = listingById.get(r.getListingId());
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listingId", r.getListingId());
                        item.put("score", r.getScore());
                        item.put("rank", r.getRank());
                        // Listing có thể đã bị ẩn/xoá sau khi batch job chạy —
                        // trả null thay vì lỗi để không vỡ cả danh sách gợi ý.
                        item.put("listing", listing != null ? listingMapper.toListingSummary(listing, false) : null);
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
            log.error("[RecommendationService] getRecommendationsForUser lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
