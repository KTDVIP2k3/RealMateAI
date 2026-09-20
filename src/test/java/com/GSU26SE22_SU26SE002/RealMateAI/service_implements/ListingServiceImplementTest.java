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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.*;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.*;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Expanded unit tests for ListingServiceImplement.
 *
 * Test-data convention used for the report:
 * - N (Normal): valid input and valid business state.
 * - A (Abnormal): empty value, non-existing ID, invalid quantity/state,
 *   wrong role, or dependency failure.
 * - Null parameter test cases are intentionally excluded.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListingServiceImplement - Expanded Unit Tests")
class ListingServiceImplementTest_Expanded {

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
    @Mock private PostingPackageOrderServiceInterface postingPackageOrderServiceInterface;

    @Spy
    @InjectMocks
    private ListingServiceImplement listingService;

    private Account sellerAccount;
    private Account investorAccount;
    private Seller seller;
    private Seller otherSeller;
    private Property property;
    private Listing listing;

    @BeforeEach
    void setUp() {
        sellerAccount = new Account();
        sellerAccount.setAccountId(1);
        sellerAccount.setRole(RoleEnum.Seller);

        investorAccount = new Account();
        investorAccount.setAccountId(2);
        investorAccount.setRole(RoleEnum.Investor);

        seller = Seller.builder()
                .sellerId(10)
                .account(sellerAccount)
                .isActive(true)
                .build();

        Account otherAccount = new Account();
        otherAccount.setAccountId(3);
        otherAccount.setRole(RoleEnum.Seller);
        otherSeller = Seller.builder().sellerId(20).account(otherAccount).isActive(true).build();

        property = Property.builder()
                .propertyId(100)
                .seller(seller)
                .title("Nhà phố Quận 1")
                .price(5_000_000_000L)
                .area(80D)
                .build();

        listing = Listing.builder()
                .listingId(1000)
                .title("Bán nhà phố Quận 1")
                .price(5_000_000_000L)
                .isActive(true)
                .priority(4)
                .status(SellerListingStatusEnum.ACTIVE)
                .seller(seller)
                .property(property)
                .build();

        // createListing() calls self.persistNewListingCore() to preserve @Transactional.
        ReflectionTestUtils.setField(listingService, "self", listingService);
    }

    private void mockCurrentSeller() {
        when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
        when(sellerRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(seller));
    }

    private ListingDetailResponse detailResponse() {
        return ListingDetailResponse.builder().listingId(1000).build();
    }

    private ListingSummaryResponse summaryResponse() {
        return ListingSummaryResponse.builder().listingId(1000).build();
    }

    @Nested
    @DisplayName("Create Listing")
    class CreateListingTests {

        @ParameterizedTest(name = "A - duration={0} must return 400")
        @CsvSource({"0", "-1", "-10"})
        void invalidDuration_returns400(int duration) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPostingPackageId(1);
            request.setDuration(duration);
            request.setTotalAmount(new BigDecimal("100000"));

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(propertyRepository, listingRepository);
        }

        @Test
        void missingReuseExistingProperty_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setReuseExistingProperty(null);

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - non-positive existingPropertyId={0} returns 400")
        @CsvSource({"0", "-1"})
        void nonPositiveExistingPropertyId_returns400(int existingPropertyId) {
            mockCurrentSeller();
            CreateListingRequest request = new CreateListingRequest();
            request.setReuseExistingProperty(true);
            request.setExistingPropertyId(existingPropertyId);
            request.setTitle("Bán nhà phố");
            request.setDescription("Nhà phố vị trí đẹp");
            request.setPrice(5_000_000_000L);

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(propertyRepository, listingRepository);
        }

        @ParameterizedTest(name = "A - blank listing string field {0} returns 400")
        @CsvSource({"title", "description"})
        void blankRequiredListingString_returns400(String field) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();

            if ("title".equals(field)) request.setTitle("   ");
            if ("description".equals(field)) request.setDescription("   ");

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - blank optional string field {0} returns 400")
        @CsvSource({
                "contactPerson", "contactPersonPhone", "contactEmail", "viewingDate",
                "propDescription", "propPostalCode", "propDirection", "propLegalStatus",
                "propAddressParticular", "propProjectName", "propFurniture"
        })
        void blankOptionalStringWhenProvided_returns400(String field) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();

            switch (field) {
                case "contactPerson" -> request.setContactPerson("   ");
                case "contactPersonPhone" -> request.setContactPersonPhone("   ");
                case "contactEmail" -> request.setContactEmail("   ");
                case "viewingDate" -> request.setViewingDate("   ");
                case "propDescription" -> request.setPropDescription("   ");
                case "propPostalCode" -> request.setPropPostalCode("   ");
                case "propDirection" -> request.setPropDirection("   ");
                case "propLegalStatus" -> request.setPropLegalStatus("   ");
//                case "propAddressParticular" -> request.setPropAddressParticular("   ");
                case "propProjectName" -> request.setPropProjectName("   ");
                case "propFurniture" -> request.setPropFurniture("   ");
            }

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - non-positive numeric field {0}={1} returns 400")
        @CsvSource({
                "price,0", "price,-1",
                "propPrice,0", "propPrice,-1",
                "propArea,0", "propArea,-1",
                "propFloor,0", "propFloor,-1",
                "propBedroom,0", "propBedroom,-1",
                "propBathroom,0", "propBathroom,-1",
                "propPropertyTypeId,0", "propPropertyTypeId,-1",
                "propPropertyConditionId,0", "propPropertyConditionId,-1"
        })
        void nonPositiveCreateNumericField_returns400(String field, long value) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();

            switch (field) {
                case "price" -> request.setPrice(value);
                case "propPrice" -> request.setPropPrice(value);
                case "propArea" -> request.setPropArea((double) value);
                case "propFloor" -> request.setPropFloor((int) value);
                case "propBedroom" -> request.setPropBedroom((int) value);
                case "propBathroom" -> request.setPropBathroom((int) value);
                case "propPropertyTypeId" -> request.setPropPropertyTypeId((int) value);
                case "propPropertyConditionId" -> request.setPropPropertyConditionId((int) value);
            }

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - non-positive postingPackageId={0} returns 400")
        @CsvSource({"0", "-1"})
        void nonPositivePostingPackageId_returns400(int postingPackageId) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPostingPackageId(postingPackageId);
            request.setDuration(30);
            request.setTotalAmount(new BigDecimal("100000"));

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - non-positive totalAmount={0} returns 400")
        @CsvSource({"0", "-1", "-100"})
        void nonPositiveTotalAmount_returns400(String totalAmount) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPostingPackageId(1);
            request.setDuration(30);
            request.setTotalAmount(new BigDecimal(totalAmount));

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void blankWardCode_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPropWardCode("   ");

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void blankDraftImagePublicId_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setDraftImagePublicIds(List.of("   "));

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest(name = "A - invalid thumbnailImageIndex={0} returns 400")
        @CsvSource({"-1", "1"})
        void invalidThumbnailImageIndex_returns400(int index) {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setThumbnailImageIndex(index);

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void emptyPropertyTitle_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPropTitle("");

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void blankPropertyTitle_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setPropTitle("   ");

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void nonExistingPropertyType_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            when(propertyTypeRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("A - Non-existing propertyConditionId returns 400")
        void nonExistingPropertyConditionId_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            int nonExistingConditionId = 999;
            request.setPropPropertyConditionId(nonExistingConditionId);
            when(propertyTypeRepository.findById(999)).thenReturn(Optional.of(new PropertyType()));
            when(propertyConditionRepository.findById(nonExistingConditionId))
                    .thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void nonExistingWard_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            when(propertyTypeRepository.findById(999)).thenReturn(Optional.of(new PropertyType()));
            when(wardRepository.findById("WARD-999")).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void emptyDraftImages_returns400() {
            mockCurrentSeller();
            CreateListingRequest request = validNewPropertyRequest();
            request.setDraftImagePublicIds(Collections.emptyList());

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void reuseNonExistingProperty_returns404() {
            mockCurrentSeller();
            CreateListingRequest request = validReuseExistingPropertyRequest();
            request.setExistingPropertyId(999);
            when(propertyRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void reusePropertyOwnedByAnotherSeller_returns403() {
            mockCurrentSeller();
            Property notOwned = Property.builder().propertyId(200).seller(otherSeller).build();
            CreateListingRequest request = validReuseExistingPropertyRequest();
            request.setExistingPropertyId(200);
            when(propertyRepository.findById(200)).thenReturn(Optional.of(notOwned));

            ResponseEntity<ApiResponse> response = listingService.createListing(request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        private CreateListingRequest validNewPropertyRequest() {
            CreateListingRequest request = new CreateListingRequest();
            request.setReuseExistingProperty(false);
            request.setTitle("Bán nhà phố");
            request.setDescription("Nhà phố vị trí đẹp");
            request.setPrice(5_000_000_000L);
            request.setPropTitle("Nhà phố Quận 1");
            request.setPropPrice(5_000_000_000L);
            request.setPropArea(80D);
            request.setPropPropertyTypeId(999);
            request.setPropLatitude(new java.math.BigDecimal("10.7769"));
            request.setPropLongitude(new java.math.BigDecimal("106.7009"));
            request.setPropWardCode("WARD-999");
            request.setDraftImagePublicIds(List.of("draft-image-1"));
            return request;
        }

        private CreateListingRequest validReuseExistingPropertyRequest() {
            CreateListingRequest request = new CreateListingRequest();
            request.setReuseExistingProperty(true);
            request.setTitle("Bán nhà phố");
            request.setDescription("Nhà phố vị trí đẹp");
            request.setPrice(5_000_000_000L);
            return request;
        }
    }

    @Nested
    @DisplayName("getMarketListings")
    class GetMarketListingsTests {

        @Test
        void activeListings_returns200() {
            Page<Listing> idPage = new PageImpl<>(List.of(listing), PageRequest.of(0, 10), 1);
            when(listingRepository.findByIsActiveTrue(any(Pageable.class))).thenReturn(idPage);
            when(listingRepository.findAllByListingIdInWithDetails(List.of(1000))).thenReturn(List.of(listing));
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(activeLogRepository.countGroupedByListingId(anyList(), eq(UserEventTypeEnum.VIEW)))
                    .thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(eq(listing), eq(false), nullable(Long.class)))
                    .thenReturn(summaryResponse());

            ResponseEntity<ApiResponse> response = listingService.getMarketListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            Map<?, ?> data = (Map<?, ?>) response.getBody().getData();
            assertEquals(1L, data.get("totalElements"));
        }

        @ParameterizedTest(name = "A - invalid pagination page={0}, size={1} is normalized")
        @CsvSource({"-1,10", "0,-1", "-1,-5", "0,51"})
        void invalidPagination_isNormalized(int page, int size) {
            when(listingRepository.findByIsActiveTrue(any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 10)));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getMarketListings(page, size);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void repositoryFailure_returns500() {
            when(listingRepository.findByIsActiveTrue(any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database connection timeout"));

            ResponseEntity<ApiResponse> response = listingService.getMarketListings(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getListingDetail")
    class GetListingDetailTests {

        @Test
        void existingActiveListing_returns200AndRecordsView() {
            when(listingRepository.findActiveById(1000)).thenReturn(Optional.of(listing));
            when(authenUntil.getCurrentUSer()).thenReturn(null);
            when(activeLogRepository.countByListingIdAndEventType(1000, UserEventTypeEnum.VIEW)).thenReturn(6L);
            when(listingMapper.toListingDetail(listing, property, 6L)).thenReturn(detailResponse());

            ResponseEntity<ApiResponse> response = listingService.getListingDetail(1000);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(userEventTrackingService).recordSilently(null, UserEventTypeEnum.VIEW, 1000);
        }

        @Test
        void nonExistingListing_returns404() {
            when(listingRepository.findActiveById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.getListingDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verifyNoInteractions(userEventTrackingService);
        }

        @Test
        void repositoryFailure_returns500() {
            when(listingRepository.findActiveById(1000)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = listingService.getListingDetail(1000);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View My Listings")
    class GetMyListingsTests {

        @Test
        void sellerListings_returns200() {
            mockCurrentSeller();
            when(listingRepository.findBySellerId(eq(10), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(listing), PageRequest.of(0, 10), 1));
            when(activeLogRepository.countGroupedByListingId(anyList(), eq(UserEventTypeEnum.VIEW)))
                    .thenReturn(Collections.emptyList());
            when(listingMapper.toListingSummary(eq(listing), eq(false), nullable(Long.class)))
                    .thenReturn(summaryResponse());

            ResponseEntity<ApiResponse> response = listingService.getMyListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void nonSeller_returns403() {
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);

            ResponseEntity<ApiResponse> response = listingService.getMyListings(0, 10);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }
    }

    @Nested
    @DisplayName("View My Properties")
    class GetMyPropertiesTests {

        @Test
        void propertiesExist_returns200() {
            mockCurrentSeller();
            when(propertyRepository.findBySellerIdWithDetails(eq(10), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(property), PageRequest.of(0, 10), 1));
            when(listingRepository.countByProperty_PropertyId(100)).thenReturn(2L);
            when(listingMapper.toPropertyDetail(property, 2)).thenReturn(new PropertyDetailResponse());

            ResponseEntity<ApiResponse> response = listingService.getMyProperties(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

    }

    @Nested
    @DisplayName("View My Property Details")
    class GetMyPropertyDetailTests {

        @Test
        void nonExistingProperty_returns404() {
            mockCurrentSeller();
            when(propertyRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.getMyPropertyDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void propertyOwnedByAnotherSeller_returns403() {
            mockCurrentSeller();
            Property notOwned = Property.builder().propertyId(200).seller(otherSeller).build();
            when(propertyRepository.findByIdWithDetails(200)).thenReturn(Optional.of(notOwned));

            ResponseEntity<ApiResponse> response = listingService.getMyPropertyDetail(200);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        void ownedProperty_returns200() {
            mockCurrentSeller();
            when(propertyRepository.findByIdWithDetails(100)).thenReturn(Optional.of(property));
            when(listingRepository.countByProperty_PropertyId(100)).thenReturn(1L);
            when(listingMapper.toPropertyDetail(property, 1)).thenReturn(new PropertyDetailResponse());

            ResponseEntity<ApiResponse> response = listingService.getMyPropertyDetail(100);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update Listing")
    class UpdateListingTests {

        @ParameterizedTest(name = "A - blank update string field {0} returns 400")
        @CsvSource({
                "title", "description", "contactPerson", "contactPersonPhone",
                "contactEmail", "viewingDate", "propertyTitle", "propertyDescription",
                "direction", "furniture", "postalCode", "wardCode"
        })
        void blankUpdateStringWhenProvided_returns400(String field) {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            UpdateListingRequest request = new UpdateListingRequest();

            switch (field) {
                case "title" -> request.setTitle("   ");
                case "description" -> request.setDescription("   ");
                case "contactPerson" -> request.setContactPerson("   ");
                case "contactPersonPhone" -> request.setContactPersonPhone("   ");
                case "contactEmail" -> request.setContactEmail("   ");
                case "viewingDate" -> request.setViewingDate("   ");
                case "propertyTitle" -> request.setPropertyTitle("   ");
                case "propertyDescription" -> request.setPropertyDescription("   ");
                case "direction" -> request.setDirection("   ");
                case "furniture" -> request.setFurniture("   ");
                case "postalCode" -> request.setPostalCode("   ");
                case "wardCode" -> request.setWardCode("   ");
            }

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }

        @ParameterizedTest(name = "A - non-positive update numeric field {0}={1} returns 400")
        @CsvSource({
                "price,0", "price,-1",
                "propertyPrice,0", "propertyPrice,-1",
                "area,0", "area,-1",
                "floor,0", "floor,-1",
                "bedroom,0", "bedroom,-1",
                "bathroom,0", "bathroom,-1",
                "propertyTypeId,0", "propertyTypeId,-1",
                "propertyConditionId,0", "propertyConditionId,-1",
                "thumbnailListingImageId,0", "thumbnailListingImageId,-1"
        })
        void nonPositiveUpdateNumericField_returns400(String field, long value) {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            UpdateListingRequest request = new UpdateListingRequest();

            switch (field) {
                case "price" -> request.setPrice(value);
                case "propertyPrice" -> request.setPropertyPrice(value);
                case "area" -> request.setArea((double) value);
                case "floor" -> request.setFloor((int) value);
                case "bedroom" -> request.setBedroom((int) value);
                case "bathroom" -> request.setBathroom((int) value);
                case "propertyTypeId" -> request.setPropertyTypeId((int) value);
                case "propertyConditionId" -> request.setPropertyConditionId((int) value);
                case "thumbnailListingImageId" -> request.setThumbnailListingImageId((int) value);
            }

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }

        @Test
        void emptyDraftImageList_returns400() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            UpdateListingRequest request = new UpdateListingRequest();
            request.setDraftImagePublicIds(Collections.emptyList());

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }

        @Test
        void blankDraftImagePublicId_returns400() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            UpdateListingRequest request = new UpdateListingRequest();
            request.setDraftImagePublicIds(List.of("   "));

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }

        @ParameterizedTest(name = "A - invalid update thumbnailImageIndex={0} returns 400")
        @CsvSource({"-1", "1"})
        void invalidUpdateThumbnailImageIndex_returns400(int index) {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            UpdateListingRequest request = new UpdateListingRequest();
            request.setDraftImagePublicIds(List.of("draft-image-1"));
            request.setThumbnailImageIndex(index);

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(listingRepository);
        }

        @Test
        void ownerUpdatesTitle_returns200AndRequiresReview() {
            mockCurrentSeller();
            when(listingRepository.findByIdWithDetails(1000)).thenReturn(Optional.of(listing));
            when(listingVerificationRepository.findByListing_ListingId(1000)).thenReturn(Optional.empty());
            when(listingRepository.save(listing)).thenReturn(listing);
            when(propertyRepository.findByIdWithDetails(100)).thenReturn(Optional.of(property));
            when(listingMapper.toListingDetail(any(Listing.class), eq(property))).thenReturn(detailResponse());
            UpdateListingRequest request = new UpdateListingRequest();
            request.setTitle("Tiêu đề mới");

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Tiêu đề mới", listing.getTitle());
            assertFalse(listing.getIsActive());
            verify(listingVerificationRepository).save(any(ListingVerification.class));
        }

        @Test
        void nonExistingListing_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(listingRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response =
                    listingService.updateListing(999, new UpdateListingRequest());

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void listingOwnedByAnotherSeller_returns403() {
            mockCurrentSeller();
            Listing notOwned = Listing.builder()
                    .listingId(2000).seller(otherSeller).status(SellerListingStatusEnum.ACTIVE).build();
            when(listingRepository.findByIdWithDetails(2000)).thenReturn(Optional.of(notOwned));

            ResponseEntity<ApiResponse> response =
                    listingService.updateListing(2000, new UpdateListingRequest());

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        void deletedListing_returns409() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            listing.setStatus(SellerListingStatusEnum.DELETED);
            when(listingRepository.findByIdWithDetails(1000)).thenReturn(Optional.of(listing));

            ResponseEntity<ApiResponse> response =
                    listingService.updateListing(1000, new UpdateListingRequest());

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        void nonExistingThumbnail_returns400() {
            mockCurrentSeller();
            when(listingRepository.findByIdWithDetails(1000)).thenReturn(Optional.of(listing));
            when(listingVerificationRepository.findByListing_ListingId(1000)).thenReturn(Optional.empty());
            when(listingImageRepository.findByListingImageIdAndListing_ListingId(999, 1000))
                    .thenReturn(Optional.empty());
            UpdateListingRequest request = new UpdateListingRequest();
            request.setThumbnailListingImageId(999);

            ResponseEntity<ApiResponse> response = listingService.updateListing(1000, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View My Listing Details")
    class GetMyListingDetailTests {

        @Test
        void ownedListing_returns200() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            when(activeLogRepository.countByListingIdAndEventType(1000, UserEventTypeEnum.VIEW)).thenReturn(5L);
            when(listingMapper.toListingDetail(listing, property, 5L)).thenReturn(detailResponse());

            ResponseEntity<ApiResponse> response = listingService.getMyListingDetail(1000);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void nonExistingOrNotOwned_returns404() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.getMyListingDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Delete Listing")
    class SoftDeleteListingTests {

        @Test
        void ownedListing_isSoftDeleted() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            when(listingRepository.save(listing)).thenReturn(listing);

            ResponseEntity<ApiResponse> response = listingService.softDeleteListing(1000);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(SellerListingStatusEnum.DELETED, listing.getStatus());
            assertFalse(listing.getIsActive());
            assertNotNull(listing.getDeletedAt());
        }

        @Test
        void nonExistingOrNotOwned_returns404() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.softDeleteListing(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            verify(listingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Toggle Listing Visibility Status")
    class UpdateListingStatusTests {

        @Test
        void activeToHidden_returns200() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            when(listingRepository.save(listing)).thenReturn(listing);
            when(listingMapper.toListingDetail(listing, property)).thenReturn(detailResponse());
            UpdateListingStatusRequest request = new UpdateListingStatusRequest();
            request.setStatus(SellerListingStatusEnum.HIDDEN);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(1000, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(SellerListingStatusEnum.HIDDEN, listing.getStatus());
            assertFalse(listing.getIsActive());
        }

        @Test
        void hiddenApprovedToActive_returns200() {
            mockCurrentSeller();
            listing.setStatus(SellerListingStatusEnum.HIDDEN);
            listing.setIsActive(false);
            ListingVerification verification = ListingVerification.builder()
                    .listing(listing).status(ListingStatusEnum.APPROVED).build();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            when(listingVerificationRepository.findByListing_ListingId(1000))
                    .thenReturn(Optional.of(verification));
            when(listingRepository.save(listing)).thenReturn(listing);
            when(listingMapper.toListingDetail(listing, property)).thenReturn(detailResponse());
            UpdateListingStatusRequest request = new UpdateListingStatusRequest();
            request.setStatus(SellerListingStatusEnum.ACTIVE);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(1000, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(listing.getIsActive());
        }

        @Test
        void hiddenPendingToActive_returns409() {
            mockCurrentSeller();
            listing.setStatus(SellerListingStatusEnum.HIDDEN);
            ListingVerification verification = ListingVerification.builder()
                    .listing(listing).status(ListingStatusEnum.PENDING).build();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            when(listingVerificationRepository.findByListing_ListingId(1000))
                    .thenReturn(Optional.of(verification));
            UpdateListingStatusRequest request = new UpdateListingStatusRequest();
            request.setStatus(SellerListingStatusEnum.ACTIVE);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(1000, request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(listingRepository, never()).save(any());
        }

        @Test
        void activeToActive_returns409() {
            mockCurrentSeller();
            when(listingRepository.findByIdAndSellerId(1000, 10)).thenReturn(Optional.of(listing));
            UpdateListingStatusRequest request = new UpdateListingStatusRequest();
            request.setStatus(SellerListingStatusEnum.ACTIVE);

            ResponseEntity<ApiResponse> response = listingService.updateListingStatus(1000, request);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Generate Listing Content via AI")
    class GenerateListingContentTests {

        @Test
        void generateContent_nonExistingPropertyType_returns400() {
            mockCurrentSeller();
            GenerateListingContentRequest request = new GenerateListingContentRequest();
            request.setPropertyTypeId(999);
            when(propertyTypeRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.generateListingContent(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(geminiClient);
        }

    }

    @Nested
    @DisplayName("Get Listing Price Suggestion")
    class SuggestListingPriceTests {

        @Test
        void suggestPrice_nonExistingPropertyType_returns400() {
            mockCurrentSeller();
            PriceSuggestionRequest request = new PriceSuggestionRequest();
            request.setPropertyTypeId(999);
            request.setWardCode("WARD-01");
            when(propertyTypeRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.suggestListingPrice(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(geminiClient);
        }

        @Test
        void suggestPrice_nonExistingWard_returns400() {
            mockCurrentSeller();
            PriceSuggestionRequest request = new PriceSuggestionRequest();
            request.setPropertyTypeId(1);
            request.setWardCode("WARD-999");
            when(propertyTypeRepository.findById(1)).thenReturn(Optional.of(new PropertyType()));
            when(wardRepository.findById("WARD-999")).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = listingService.suggestListingPrice(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            verifyNoInteractions(geminiClient);
        }
    }

    @Nested
    @DisplayName("compareListings")
    class CompareListingsTests {

        @Test
        void emptyList_returns400() {
            ResponseEntity<ApiResponse> response = listingService.compareListings(Collections.emptyList());
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void oneListing_returns400() {
            ResponseEntity<ApiResponse> response = listingService.compareListings(List.of(1000));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void duplicateIdsLeavingOneDistinctListing_returns400() {
            ResponseEntity<ApiResponse> response = listingService.compareListings(List.of(1000, 1000));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void fiveListings_returns400() {
            ResponseEntity<ApiResponse> response =
                    listingService.compareListings(List.of(1, 2, 3, 4, 5));
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void twoExistingListings_returns200() {
            Listing second = Listing.builder().listingId(2000).property(property).seller(seller).build();
            when(listingRepository.findAllByListingIdInWithDetails(List.of(1000, 2000)))
                    .thenReturn(List.of(listing, second));
            when(activeLogRepository.countGroupedByListingId(anyList(), eq(UserEventTypeEnum.VIEW)))
                    .thenReturn(Collections.emptyList());
            when(listingMapper.toListingDetail(any(Listing.class), any(), nullable(Long.class)))
                    .thenReturn(detailResponse());

            ResponseEntity<ApiResponse> response =
                    listingService.compareListings(List.of(1000, 2000));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void oneExistingAndOneNonExisting_returns400() {
            when(listingRepository.findAllByListingIdInWithDetails(List.of(1000, 9999)))
                    .thenReturn(List.of(listing));
            when(activeLogRepository.countGroupedByListingId(anyList(), eq(UserEventTypeEnum.VIEW)))
                    .thenReturn(Collections.emptyList());
            when(listingMapper.toListingDetail(eq(listing), eq(property), nullable(Long.class)))
                    .thenReturn(detailResponse());

            ResponseEntity<ApiResponse> response =
                    listingService.compareListings(List.of(1000, 9999));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getFeaturedListings")
    class GetFeaturedListingsTests {

        @Test
        void emptyFeaturedList_returns200() {
            when(activeLogRepository.findFeaturedListingIds(eq(UserEventTypeEnum.VIEW), any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(0, 10)));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getFeaturedListings(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        void repositoryFailure_returns500() {
            when(activeLogRepository.findFeaturedListingIds(eq(UserEventTypeEnum.VIEW), any(Pageable.class)))
                    .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = listingService.getFeaturedListings(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getSearchSuggestions")
    class GetSearchSuggestionsTests {

        @ParameterizedTest(name = "A - empty/blank query ''{0}'' returns empty public groups")
        @CsvSource(value = {"EMPTY", "BLANK"})
        void emptyOrBlankQuery_returns200(String kind) {
            String query = "EMPTY".equals(kind) ? "" : "   ";
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getSearchSuggestions(query);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verifyNoInteractions(wardRepository, provinceRepository, propertyTypeRepository);
        }

        @Test
        void validQuery_searchesAllPublicSuggestionGroups() {
            when(wardRepository.findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase("nhà", "nhà"))
                    .thenReturn(Collections.emptyList());
            when(provinceRepository.findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase("nhà", "nhà"))
                    .thenReturn(Collections.emptyList());
            when(listingRepository.searchSuggestionsByTitleOrProjectName(eq("nhà"), any(Pageable.class)))
                    .thenReturn(List.of(listing));
            when(propertyTypeRepository.findTop5ByIsActiveTrueAndNameContainingIgnoreCase("nhà"))
                    .thenReturn(Collections.emptyList());
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = listingService.getSearchSuggestions("nhà");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(listingRepository).searchSuggestionsByTitleOrProjectName(eq("nhà"), any(Pageable.class));
        }

        @Test
        void repositoryFailure_returns500() {
            when(wardRepository.findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase("nhà", "nhà"))
                    .thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = listingService.getSearchSuggestions("nhà");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}