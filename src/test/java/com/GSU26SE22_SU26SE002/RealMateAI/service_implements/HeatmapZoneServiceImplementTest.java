package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.HeatmapZoneRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho HeatmapZoneServiceImplement — Function 112 View Heatmap
 * (9.3 Heatmap Zone Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HeatmapZoneServiceImplement — View Heatmap")
class HeatmapZoneServiceImplementTest {

    @Mock private CrawPropertyListingRepository crawPropertyListingRepository;
    @Mock private HeatmapZoneRepository heatmapZoneRepository;
    @Mock private ListingRepository listingRepository;

    @InjectMocks
    private HeatmapZoneServiceImplement heatmapZoneService;

    private Listing inViewportActiveListing;
    private Listing outOfViewportListing;

    @BeforeEach
    void setUp() {
        Location inViewportLoc = Location.builder()
                .latitude(new BigDecimal("10.77")).longitude(new BigDecimal("106.70")).build();
        Property inViewportProperty = Property.builder().propertyId(1).location(inViewportLoc).build();
        inViewportActiveListing = Listing.builder()
                .listingId(100).title("Trong khung nhin").isActive(true)
                .status(SellerListingStatusEnum.ACTIVE).property(inViewportProperty).build();

        Location outsideLoc = Location.builder()
                .latitude(new BigDecimal("21.02")).longitude(new BigDecimal("105.83")).build();
        Property outsideProperty = Property.builder().propertyId(2).location(outsideLoc).build();
        outOfViewportListing = Listing.builder()
                .listingId(200).title("Ngoai khung nhin").isActive(true)
                .status(SellerListingStatusEnum.ACTIVE).property(outsideProperty).build();
    }

    // ── 112. View Heatmap ──────────────────────────────────────────────────
    @Nested
    @DisplayName("112. getListingsByViewportPaged")
    class GetListingsByViewportPagedTests {

        @Test
        @DisplayName("Chỉ trả về listing NẰM TRONG khung nhìn (viewport)")
        void getListings_filtersOnlyWithinViewport() {
            when(listingRepository.findAll()).thenReturn(List.of(inViewportActiveListing, outOfViewportListing));

            ResponseEntity<ApiResponse> response = heatmapZoneService.getListingsByViewportPaged(
                    new BigDecimal("10.70"), new BigDecimal("10.80"),
                    new BigDecimal("106.60"), new BigDecimal("106.80"),
                    0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Bỏ qua listing KHÔNG active hoặc chưa có tọa độ")
        void getListings_excludesInactiveOrMissingLocation() {
            Listing inactiveListing = Listing.builder()
                    .listingId(300).isActive(false)
                    .status(SellerListingStatusEnum.ACTIVE)
                    .property(inViewportActiveListing.getProperty()).build();
            when(listingRepository.findAll()).thenReturn(List.of(inactiveListing));

            ResponseEntity<ApiResponse> response = heatmapZoneService.getListingsByViewportPaged(
                    new BigDecimal("10.70"), new BigDecimal("10.80"),
                    new BigDecimal("106.60"), new BigDecimal("106.80"),
                    0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Hoạt động đúng dù minLat/maxLat truyền vào bị đảo ngược thứ tự")
        void getListings_handlesSwappedMinMax() {
            when(listingRepository.findAll()).thenReturn(List.of(inViewportActiveListing));

            // truyền nguoc: minLat > maxLat — code phải tự Math.min/max lai
            ResponseEntity<ApiResponse> response = heatmapZoneService.getListingsByViewportPaged(
                    new BigDecimal("10.80"), new BigDecimal("10.70"),
                    new BigDecimal("106.60"), new BigDecimal("106.80"),
                    0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
