package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingSummaryResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho RecommendationServiceImplement — Function 58 View Listing
 * Recommendations (3.3 Recommendation Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationServiceImplement — View Listing Recommendations")
class RecommendationServiceImplementTest {

    @Mock private RecommendationResultRepository recommendationResultRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private ListingMapper listingMapper;
    @Mock private AuthenUntil authenUntil;
    @Mock private InvestorRepository investorRepository;
    @Mock private ActiveLogRepository activeLogRepository;
    @Mock private FavoriteListingRepository favoriteListingRepository;

    @InjectMocks
    private RecommendationServiceImplement recommendationService;

    private Account investorAccount;

    @BeforeEach
    void setUp() {
        investorAccount = new Account();
        investorAccount.setAccountId(1);
    }

    // ── 58. View Listing Recommendations ─────────────────────────────────
    @Nested
    @DisplayName("58. getRecommendationsForCurrentUser")
    class GetRecommendationsTests {

        @Test
        @DisplayName("Trả 401 khi chưa đăng nhập")
        void getRecommendations_notLoggedIn_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = recommendationService.getRecommendationsForCurrentUser();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng kèm thông báo khi chưa có gợi ý nào")
        void getRecommendations_noResultsYet_returnsEmptyList() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(recommendationResultRepository.findByAccountIdOrderByRankAsc(1))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = recommendationService.getRecommendationsForCurrentUser();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách gợi ý đã tính sẵn từ batch AI, kèm viewCount thật")
        void getRecommendations_hasResults_returnsRankedList() {
            RecommendationResult rec = RecommendationResult.builder()
                    .listingId(100).score(0.95).rank(1).build();
            Listing listing = Listing.builder().listingId(100).seller(Seller.builder().sellerId(10).build()).build();
            Investor investor = Investor.builder().investorId(5).account(investorAccount).build();

            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(recommendationResultRepository.findByAccountIdOrderByRankAsc(1)).thenReturn(List.of(rec));
            when(listingRepository.findAllByListingIdInWithDetails(anyList())).thenReturn(List.of(listing));
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            when(favoriteListingRepository.findFavoritedListingIdsByInvestorId(5)).thenReturn(Collections.emptyList());
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(any(Listing.class), anyBoolean(), any()))
                    .thenReturn(ListingSummaryResponse.builder().listingId(100).build());

            ResponseEntity<ApiResponse> response = recommendationService.getRecommendationsForCurrentUser();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(recommendationResultRepository).findByAccountIdOrderByRankAsc(1);
        }
    }
}
