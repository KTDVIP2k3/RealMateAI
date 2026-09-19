package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho ListingVerificationServiceImplement — Function 51 Moderate
 * Listing (thuộc 3.1 Listing Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ListingVerificationServiceImplement — Moderate Listing")
class ListingVerificationServiceImplementTest {

    @Mock private ListingVerificationRepository listingVerificationRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private ListingMapper listingMapper;
    @Mock private AuthenUntil authenUntil;
    @Mock private PostingPackageOrderRepository postingPackageOrderRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ListingVerificationServiceImplement verificationService;

    private Account staffAccount;
    private Listing pendingListing;

    @BeforeEach
    void setUp() {
        staffAccount = new Account();
        staffAccount.setAccountId(2);
        staffAccount.setRole(RoleEnum.Staff);

        Seller seller = Seller.builder().sellerId(10).account(new Account()).build();
        pendingListing = Listing.builder()
                .listingId(100)
                .status(SellerListingStatusEnum.ACTIVE)
                .seller(seller)
                .isActive(false)
                .build();
    }

    // ── 51. Moderate Listing ────────────────────────────────────────────
    @Nested
    @DisplayName("51. verifyListing")
    class VerifyListingTests {

        @Test
        @DisplayName("Staff duyệt APPROVED thành công")
        void verifyListing_approve_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            when(listingRepository.findByIdWithDetails(100)).thenReturn(Optional.of(pendingListing));
            when(listingVerificationRepository.save(any(ListingVerification.class)))
                    .thenReturn(new ListingVerification());
            when(listingRepository.save(any(Listing.class))).thenReturn(pendingListing);

            VerifyListingRequest req = new VerifyListingRequest();
            req.setDecision(ListingStatusEnum.APPROVED);

            ResponseEntity<ApiResponse> response = verificationService.verifyListing(100, req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Từ chối tin đăng đã bị Seller xoá mềm — trả 409 Conflict")
        void verifyListing_alreadyDeleted_returnsConflict() {
            pendingListing.setStatus(SellerListingStatusEnum.DELETED);
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            when(listingRepository.findByIdWithDetails(100)).thenReturn(Optional.of(pendingListing));

            VerifyListingRequest req = new VerifyListingRequest();
            req.setDecision(ListingStatusEnum.APPROVED);

            ResponseEntity<ApiResponse> response = verificationService.verifyListing(100, req);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            verify(listingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Trả lỗi khi decision để trống (null)")
        void verifyListing_missingDecision_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            when(listingRepository.findByIdWithDetails(100)).thenReturn(Optional.of(pendingListing));

            VerifyListingRequest req = new VerifyListingRequest();
            req.setDecision(null);

            ResponseEntity<ApiResponse> response = verificationService.verifyListing(100, req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Không phải Staff/Admin — bị từ chối quyền")
        void verifyListing_notStaffOrAdmin_forbidden() {
            Account investorAccount = new Account();
            investorAccount.setAccountId(3);
            investorAccount.setRole(RoleEnum.Investor);
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);

            VerifyListingRequest req = new VerifyListingRequest();
            req.setDecision(ListingStatusEnum.APPROVED);

            ResponseEntity<ApiResponse> response = verificationService.verifyListing(100, req);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }
}
