package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("ListingServiceImplement — Listing Management")
class ListingServiceImplementTest {

    @Mock private ListingRepository listingRepository;
    @Mock private ListingVerificationRepository listingVerificationRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ListingImageRepository listingImageRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private PropertyTypeRepository propertyTypeRepository;
    @Mock private PropertyConditionRepository propertyConditionRepository;
    @Mock private InvestorRepository investorRepository;
    @Mock private FavoriteListingRepository favoriteListingRepository;
    @Mock private WardRepository wardRepository;
    @Mock private MediaAssetRepository mediaAssetRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private SearchHistoryRepository searchHistoryRepository;
    @Mock private ActiveLogRepository activeLogRepository;
    @Mock private ListingMapper listingMapper;
    @Mock private NotificationService notificationService;
    @Mock private UserEventTrackingService userEventTrackingService;
    @Mock private AuthenUntil authenUntil;
    @Mock private Client geminiClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ListingServiceImplement listingService;

    private Account sellerAccount;
    private Seller seller;
    private Listing sampleListing;

    @BeforeEach
    void setUp() {
        sellerAccount = new Account();
        sellerAccount.setAccountId(1);
        sellerAccount.setRole(RoleEnum.Seller);

        seller = Seller.builder().sellerId(10).account(sellerAccount).isActive(true).build();

        sampleListing = Listing.builder()
                .listingId(100)
                .title("Nha pho quan 1")
                .price(5_000_000_000L)
                .isActive(true)
                .priority(4)
                .seller(seller)
                .build();
    }

    // ── 41. View My Listings ──────────────────────────────────────────
    @Nested
    @DisplayName("41. getMyListings")
    class GetMyListingsTests {
        @Test
        @DisplayName("Trả về danh sách tin đăng của Seller hiện tại")
        void getMyListings_returnsSellerOwnListings() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            Page<Listing> page = new PageImpl<>(List.of(sampleListing));
            when(listingRepository.findBySellerId(eq(10), any())).thenReturn(page);
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(any(Listing.class), anyBoolean(), any()))
                    .thenReturn(ListingSummaryResponse.builder().listingId(100).build());

            ResponseEntity<ApiResponse> response = listingService.getMyListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(listingRepository).findBySellerId(eq(10), any());
        }
    }

    // ── 42. View My Listing Details ───────────────────────────────────
    @Nested
    @DisplayName("42. getMyListingDetail")
    class GetMyListingDetailTests {
        @Test
        @DisplayName("Trả 404 khi tin đăng không thuộc sở hữu / không tồn tại")
        void getMyListingDetail_notFound_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.getMyListingDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về chi tiết khi tin đăng thuộc đúng Seller")
        void getMyListingDetail_found_returnsDetail() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(sampleListing));
            when(activeLogRepository.countByListingIdAndEventType(eq(100), any())).thenReturn(5L);
            when(listingMapper.toListingDetail(any(), any(), any()))
                    .thenReturn(ListingDetailResponse.builder().listingId(100).build());

            ResponseEntity<ApiResponse> response = listingService.getMyListingDetail(100);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 45. Delete Listing ─────────────────────────────────────────────

    @Nested
    @DisplayName("45. softDeleteListing")
    class SoftDeleteListingTests {
        @Test
        @DisplayName("Soft-delete thành công tin đăng của Seller")
        void softDeleteListing_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(sampleListing));
            when(listingRepository.save(any(Listing.class))).thenReturn(sampleListing);

            ResponseEntity<ApiResponse> response = listingService.softDeleteListing(100);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(listingRepository).save(any(Listing.class));
        }

        @Test
        @DisplayName("Trả 404 nếu tin đăng không thuộc sở hữu")
        void softDeleteListing_notOwned_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.softDeleteListing(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(listingRepository, never()).save(any());
        }
    }

    // ── 46. Toggle Listing Visibility Status ──────────────────────────
    // SUA: updateMyListingStatus() da doi ten thanh updateListingStatus(), va
    // gio BAT BUOC request.getStatus() != null (tra 400 ngay neu de trong) —
    // truoc day toi de trong UpdateListingStatusRequest() nen se SAI voi logic
    // moi. Sua: set status=HIDDEN hop le (sampleListing dang o ACTIVE mac
    // dinh, chuyen sang HIDDEN khong can qua buoc validate verification nhu
    // khi chuyen ve ACTIVE).
    @Nested
    @DisplayName("46. updateListingStatus")
    class UpdateListingStatusTests {
        @Test
        @DisplayName("Cập nhật trạng thái hiển thị thành công (chuyển sang HIDDEN)")
        void updateListingStatus_toHidden_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(sampleListing));
            when(listingRepository.save(any(Listing.class))).thenReturn(sampleListing);
            when(listingMapper.toListingDetail(any(Listing.class), any()))
                    .thenReturn(ListingDetailResponse.builder().listingId(100).build());
            UpdateListingStatusRequest req = new UpdateListingStatusRequest();
            req.setStatus(com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.HIDDEN);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(100, req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 400 khi status để trống")
        void updateListingStatus_missingStatus_returnsBadRequest() {
            UpdateListingStatusRequest req = new UpdateListingStatusRequest();
            req.setStatus(null);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(100, req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ── 49. View My Properties ─────────────────────────────────────────
    @Nested
    @DisplayName("49. getMyProperties")
    class GetMyPropertiesTests {
        @Test
        @DisplayName("Trả về danh sách tài sản của Seller")
        void getMyProperties_returnsList() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            Property p = Property.builder().propertyId(5).seller(seller).build();
            when(propertyRepository.findBySellerIdWithDetails(eq(10), any())).thenReturn(new PageImpl<>(List.of(p)));
            when(listingRepository.countByProperty_PropertyId(5)).thenReturn(1L);
            when(listingMapper.toPropertyDetail(any(Property.class), anyInt()))
                    .thenReturn(PropertyDetailResponse.builder().propertyId(5).build());

            ResponseEntity<ApiResponse> response = listingService.getMyProperties(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 50. View My Property Details ───────────────────────────────────
    @Nested
    @DisplayName("50. getMyPropertyDetail")
    class GetMyPropertyDetailTests {
        @Test
        @DisplayName("Trả lỗi khi tài sản không thuộc sở hữu Seller hiện tại")
        void getMyPropertyDetail_notOwned_returnsError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            Seller otherSeller = Seller.builder().sellerId(99).build();
            Property p = Property.builder().propertyId(5).seller(otherSeller).build();
            when(propertyRepository.findByIdWithDetails(5)).thenReturn(Optional.of(p));

            ResponseEntity<ApiResponse> response = listingService.getMyPropertyDetail(5);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    // ── 44. Update Listing ──────────────────────────────────────────────
    @Nested
    @DisplayName("44. updateListing")
    class UpdateListingTests {
        @Test
        @DisplayName("Cập nhật thành công title/description/price")
        void updateListing_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(sampleListing));
            when(listingRepository.save(any(Listing.class))).thenReturn(sampleListing);
            when(listingMapper.toListingDetail(any(), any()))
                    .thenReturn(ListingDetailResponse.builder().listingId(100).build());
            UpdateListingRequest req = new UpdateListingRequest();
            req.setTitle("Ten moi");

            ResponseEntity<ApiResponse> response = listingService.updateListing(100, req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(listingRepository).save(any(Listing.class));
        }

        @Test
        @DisplayName("Trả 404 khi tin đăng không tồn tại/không thuộc sở hữu")
        void updateListing_notFound_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());
            UpdateListingRequest req = new UpdateListingRequest();

            ResponseEntity<ApiResponse> response = listingService.updateListing(999, req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 54. View Listing Details (Public) ───────────────────────────────
    @Nested
    @DisplayName("54. getListingDetail (public)")
    class GetListingDetailTests {
        @Test
        @DisplayName("Ghi nhận 1 lượt VIEW rồi trả về chi tiết tin đăng công khai")
        void getListingDetail_recordsViewAndReturnsDetail() {
            when(listingRepository.findById(100)).thenReturn(Optional.of(sampleListing));
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(activeLogRepository.countByListingIdAndEventType(eq(100), any())).thenReturn(6L);
            when(listingMapper.toListingDetail(any(), any(), any()))
                    .thenReturn(ListingDetailResponse.builder().listingId(100).viewCount(6).build());

            ResponseEntity<ApiResponse> response = listingService.getListingDetail(100);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userEventTrackingService).recordSilently(any(), any(), eq(100));
        }
    }

    // ── 53. View Listings (Public) ───────────────────────────────────────
    @Nested
    @DisplayName("53. getMarketListings (public)")
    class GetListingsTests {
        @Test
        @DisplayName("Trả về danh sách tin đăng công khai đang active")
        void getListings_returnsActiveListings() {
            // SUA (fix loi thật): getMarketListings() KHÔNG dùng
            // findAll(spec, pageable) — nó dùng TwoStepPaginationUtil, gọi
            // findByIsActiveTrue(pageable) để lấy Page<Listing> (bước 1, chỉ
            // cần ID), rồi findAllByListingIdInWithDetails(ids) để lấy đủ dữ
            // liệu (bước 2). Mock đúng 2 bước này, không phải findAll(spec,...).
            Page<Listing> idPage = new PageImpl<>(List.of(sampleListing));
            when(listingRepository.findByIsActiveTrue(any(PageRequest.class))).thenReturn(idPage);
            when(listingRepository.findAllByListingIdInWithDetails(anyList())).thenReturn(List.of(sampleListing));
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(any(Listing.class), anyBoolean(), any()))
                    .thenReturn(ListingSummaryResponse.builder().listingId(100).build());

            ResponseEntity<ApiResponse> response = listingService.getMarketListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 55. View Featured Listings ────────────────────────────────────────
    @Nested
    @DisplayName("55. getFeaturedListings")
    class GetFeaturedListingsTests {
        @Test
        @DisplayName("Trả về danh sách tin nổi bật kèm viewCount thật")
        void getFeaturedListings_returnsRankedByView() {
            when(activeLogRepository.findFeaturedListingIds(any(), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getFeaturedListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 56. Compare Listings ───────────────────────────────────────────
    @Nested
    @DisplayName("56. compareListings")
    class CompareListingsTests {
        @Test
        @DisplayName("Trả lỗi khi số lượng tin đem so sánh < 2")
        void compareListings_lessThanTwo_returnsBadRequest() {
            // SUA (fix loi thật): với chỉ 1 listingId, code trả về BAD_REQUEST
            // NGAY (distinctIds.size() < 2) — KHÔNG gọi tới
            // findAllByListingIdInWithDetails(). Mock method này ở đây là
            // "unnecessary stubbing", sẽ bị MockitoExtension (STRICT_STUBS)
            // ném UnnecessaryStubbingException — đã xoá.
            ResponseEntity<ApiResponse> response = listingService.compareListings(List.of(100));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("So sánh thành công khi có đủ 2 tin đăng hợp lệ")
        void compareListings_twoValidListings_success() {
            Listing listing2 = Listing.builder().listingId(200).seller(seller).priority(4).build();
            when(listingRepository.findAllByListingIdInWithDetails(anyList()))
                    .thenReturn(List.of(sampleListing, listing2));
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());
            when(listingMapper.toListingDetail(any(), any(), any()))
                    .thenReturn(ListingDetailResponse.builder().build());

            ResponseEntity<ApiResponse> response = listingService.compareListings(List.of(100, 200));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 43. Create Listing ──────────────────────────────────────────────
    @Nested
    @DisplayName("43. createListing")
    class CreateListingTests {
        @Test
        @DisplayName("Trả 400 khi có postingPackageId nhưng duration <= 0")
        void createListing_invalidDuration_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
            CreateListingRequest req = new CreateListingRequest();
            req.setPostingPackageId(1);
            req.setDuration(0);

            ResponseEntity<ApiResponse> response = listingService.createListing(req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ── 52. Search Listings ───────────────────────────────────────────────
    @Nested
    @DisplayName("52. searchListings")
    class SearchListingsTests {
        @Test
        @DisplayName("Tìm kiếm theo từ khoá, sắp xếp mặc định NEWEST")
        void searchListings_defaultSort_returnsResults() {
            when(listingRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(100)));
            when(listingRepository.findAllByListingIdInWithDetails(anyList()))
                    .thenReturn(List.of(sampleListing));
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(activeLogRepository.countGroupedByListingId(anyList(), any())).thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(any(Listing.class), anyBoolean(), any()))
                    .thenReturn(ListingSummaryResponse.builder().listingId(100).build());
            ListingSearchRequest req = new ListingSearchRequest();
            req.setKeyword("nha");

            ResponseEntity<ApiResponse> response = listingService.searchListings(req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 57. Get Search Suggestions ────────────────────────────────────────
    // SUA (fix loi thật): getSearchSuggestions() đã viết lại HOÀN TOÀN — gộp
    // 4 nhóm gợi ý (Location/Listing/PropertyType/RecentSearch) qua 4 helper
    // method riêng, KHÔNG còn dùng findTop10ByTitleContainingIgnoreCase()/
    // findDistinctWardNamesByKeyword() (2 method này không còn tồn tại trong
    // ListingRepository).
    @Nested
    @DisplayName("57. getSearchSuggestions")
    class GetSearchSuggestionsTests {
        @Test
        @DisplayName("Trả về rỗng cả 4 nhóm khi từ khoá trống và chưa đăng nhập")
        void getSearchSuggestions_emptyKeyword_returnsAllEmpty() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getSearchSuggestions("");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về gợi ý từ cả 3 nhóm Location/Listing/PropertyType khi có từ khoá")
        void getSearchSuggestions_withKeyword_returnsSuggestionsFromAllGroups() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(wardRepository.findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase("nha", "nha"))
                    .thenReturn(Collections.emptyList());
            when(provinceRepository.findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase("nha", "nha"))
                    .thenReturn(Collections.emptyList());
            when(listingRepository.searchSuggestionsByTitleOrProjectName(eq("nha"), any(PageRequest.class)))
                    .thenReturn(List.of(sampleListing));
            when(propertyTypeRepository.findTop5ByIsActiveTrueAndNameContainingIgnoreCase("nha"))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = listingService.getSearchSuggestions("nha");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
