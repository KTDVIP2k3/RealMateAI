package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AddFavoriteRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho FavoriteListingServiceImplement — Functions 59-61
 * (3.4 Favorites Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FavoriteListingServiceImplement — Favorites Management")
class FavoriteListingServiceImplementTest {

    @Mock private FavoriteListingRepository favoriteListingRepository;
    @Mock private ActiveLogRepository activeLogRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private InvestorRepository investorRepository;
    @Mock private AuthenUntil authenUntil;
    @Mock private UserEventTrackingService userEventTrackingService;

    @InjectMocks
    private FavoriteListingServiceImplement favoriteService;

    private Account investorAccount;
    private Investor investor;

    @BeforeEach
    void setUp() {
        investorAccount = new Account();
        investorAccount.setAccountId(1);
        investor = Investor.builder().investorId(5).account(investorAccount).build();
    }

    // ── 59. View Favourite Listing ────────────────────────────────────────
    @Nested
    @DisplayName("59. getMyFavorites")
    class GetMyFavoritesTests {
        @Test
        @DisplayName("Trả về danh sách yêu thích của Investor hiện tại")
        void getMyFavorites_returnsList() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            Listing listing = Listing.builder().listingId(100).seller(Seller.builder().sellerId(10).build()).build();
            FavoriteListing fav = FavoriteListing.builder().favoriteListingId(1).investor(investor).listing(listing).build();
            when(favoriteListingRepository.findByInvestorIdWithDetails(5)).thenReturn(List.of(fav));
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = favoriteService.getMyFavorites();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 60. Like Favourite Listing ────────────────────────────────────────
    @Nested
    @DisplayName("60. addFavorite")
    class AddFavoriteTests {
        @Test
        @DisplayName("Thêm yêu thích thành công khi listing đang active và chưa lưu trước đó")
        void addFavorite_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            Listing listing = Listing.builder().listingId(100).isActive(true).build();
            when(listingRepository.findActiveById(100)).thenReturn(Optional.of(listing));
            when(favoriteListingRepository.existsByInvestor_InvestorIdAndListing_ListingId(5, 100))
                    .thenReturn(false);
            when(favoriteListingRepository.save(any(FavoriteListing.class)))
                    .thenReturn(FavoriteListing.builder().favoriteListingId(1).build());
            AddFavoriteRequest req = new AddFavoriteRequest();
            req.setListingId(100);

            ResponseEntity<ApiResponse> response = favoriteService.addFavorite(req);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 404 khi listing không tồn tại hoặc chưa được duyệt")
        void addFavorite_listingNotActive_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            when(listingRepository.findActiveById(999)).thenReturn(Optional.empty());
            AddFavoriteRequest req = new AddFavoriteRequest();
            req.setListingId(999);

            ResponseEntity<ApiResponse> response = favoriteService.addFavorite(req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 403 khi người dùng không phải Investor")
        void addFavorite_notInvestor_returnsForbidden() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.empty());
            AddFavoriteRequest req = new AddFavoriteRequest();
            req.setListingId(100);

            ResponseEntity<ApiResponse> response = favoriteService.addFavorite(req);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // ── 61. Delete Favourite Listing ──────────────────────────────────────
    @Nested
    @DisplayName("61. removeFavorite")
    class RemoveFavoriteTests {
        @Test
        @DisplayName("Xoá thành công mục yêu thích thuộc đúng Investor")
        void removeFavorite_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            FavoriteListing fav = FavoriteListing.builder().favoriteListingId(1).investor(investor).build();
            when(favoriteListingRepository.findByFavoriteListingIdAndInvestor_InvestorId(1, 5))
                    .thenReturn(Optional.of(fav));

            ResponseEntity<ApiResponse> response = favoriteService.removeFavorite(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(favoriteListingRepository).delete(fav);
        }

        @Test
        @DisplayName("Trả 404 khi mục yêu thích không tồn tại hoặc không thuộc Investor hiện tại")
        void removeFavorite_notFoundOrNotOwned_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(investor));
            when(favoriteListingRepository.findByFavoriteListingIdAndInvestor_InvestorId(999, 5))
                    .thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = favoriteService.removeFavorite(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(favoriteListingRepository, never()).delete(any());
        }
    }
}
