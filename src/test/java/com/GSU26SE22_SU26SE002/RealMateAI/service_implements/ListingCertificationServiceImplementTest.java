package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.ReviewCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitCertificationRequest;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListingCertificationServiceImplement — Listing Certification")
class ListingCertificationServiceImplementTest {

    @Mock private ListingCertificationRequestRepository certificationRequestRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private PropertyImageRepository propertyImageRepository;
    @Mock private NotificationService notificationService;
    @Mock private CloudinaryMediaServiceImplement cloudinaryMediaService;
    @Mock private AuthenUntil authenUntil;

    @InjectMocks
    private ListingCertificationServiceImplement certificationService;

    private Account sellerAccount;
    private Account staffAccount;
    private Seller seller;
    private Listing listing;

    @BeforeEach
    void setUp() {
        seller = Seller.builder().sellerId(10).build();

        sellerAccount = new Account();
        sellerAccount.setAccountId(1);
        sellerAccount.setRole(RoleEnum.Seller);
        sellerAccount.setSeller(seller);

        staffAccount = new Account();
        staffAccount.setAccountId(2);
        staffAccount.setRole(RoleEnum.Staff);

        listing = Listing.builder().listingId(100).seller(seller).isVerified(false).build();
    }

    // ── 64. Send/Create Listing Certification Request ─────────────────────
    @Nested
    @DisplayName("64. submitCertificationRequest")
    class SubmitCertificationRequestTests {
        @Test
        @DisplayName("Trả 409 khi bài đăng đã được tích xanh trước đó")
        void submit_alreadyVerified_returnsConflict() {
            listing.setIsVerified(true);
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(listing));

            SubmitCertificationRequest req = new SubmitCertificationRequest();
            req.setDocuments(List.of(new MockMultipartFile("f", "f.jpg", "image/jpeg", new byte[]{1})));

            ResponseEntity<ApiResponse> response = certificationService.submitCertificationRequest(100, req);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 400 khi không có giấy tờ nào kèm theo")
        void submit_noDocuments_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(listingRepository.findByIdAndSellerId(100, 10)).thenReturn(Optional.of(listing));
            when(certificationRequestRepository.existsByListing_ListingIdAndStatus(100, CertificationStatusEnum.PENDING))
                    .thenReturn(false);

            SubmitCertificationRequest req = new SubmitCertificationRequest();
            req.setDocuments(Collections.emptyList());

            ResponseEntity<ApiResponse> response = certificationService.submitCertificationRequest(100, req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 404 khi bài đăng không tồn tại hoặc không thuộc Seller hiện tại")
        void submit_notOwned_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(listingRepository.findByIdAndSellerId(999, 10)).thenReturn(Optional.empty());

            SubmitCertificationRequest req = new SubmitCertificationRequest();
            ResponseEntity<ApiResponse> response = certificationService.submitCertificationRequest(999, req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 62. View Listing Certifications ─────────────────────────────────────
    @Nested
    @DisplayName("62. getMyCertificationRequests")
    class GetMyCertificationRequestsTests {
        @Test
        @DisplayName("Trả về danh sách yêu cầu tích xanh của Seller hiện tại")
        void getMyCertificationRequests_returnsList() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(certificationRequestRepository.findBySellerIdWithDetails(10))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = certificationService.getMyCertificationRequests();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 63. View Listing Certification Details (Seller) ─────────────────────
    @Nested
    @DisplayName("63. getMyCertificationRequestDetail")
    class GetMyCertificationRequestDetailTests {
        @Test
        @DisplayName("Trả 404 khi yêu cầu không tồn tại")
        void getDetail_notFound_returns404() {
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);
            when(certificationRequestRepository.findByIdAndSellerIdWithDetails(999, 10))
                    .thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = certificationService.getMyCertificationRequestDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 65/66. View Listing Certification Requests / Pending (Staff) ─────────
    @Nested
    @DisplayName("65/66. getPendingCertificationQueue")
    class GetPendingCertificationQueueTests {
        @Test
        @DisplayName("Staff xem hàng đợi các yêu cầu PENDING")
        void getPendingQueue_returnsList() {
            when(certificationRequestRepository.findByStatusWithDetails(CertificationStatusEnum.PENDING))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = certificationService.getPendingCertificationQueue();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 67/68. View Listing Certification Request Details (Staff) ────────────
    @Nested
    @DisplayName("67/68. getCertificationRequestDetail")
    class GetCertificationRequestDetailTests {
        @Test
        @DisplayName("Trả 404 khi yêu cầu không tồn tại")
        void getDetail_notFound_returns404() {
            when(certificationRequestRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = certificationService.getCertificationRequestDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 69. Verify Listing Certification Request ────────────────────────────
    @Nested
    @DisplayName("69. reviewCertificationRequest")
    class ReviewCertificationRequestTests {
        @Test
        @DisplayName("Trả 400 khi từ chối (REJECTED) nhưng không kèm reviewerNote")
        void review_rejectWithoutNote_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            ReviewCertificationRequest req = new ReviewCertificationRequest();
            req.setDecision(CertificationStatusEnum.REJECTED);
            req.setReviewerNote(null);

            ResponseEntity<ApiResponse> response = certificationService.reviewCertificationRequest(1, req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 409 khi yêu cầu đã được xử lý trước đó (không còn PENDING)")
        void review_alreadyProcessed_returnsConflict() {
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            ListingCertificationRequest existing = ListingCertificationRequest.builder()
                    .certificationRequestId(1).status(CertificationStatusEnum.APPROVED).listing(listing).build();
            when(certificationRequestRepository.findByIdWithDetails(1)).thenReturn(Optional.of(existing));

            ReviewCertificationRequest req = new ReviewCertificationRequest();
            req.setDecision(CertificationStatusEnum.APPROVED);

            ResponseEntity<ApiResponse> response = certificationService.reviewCertificationRequest(1, req);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        @DisplayName("Duyệt APPROVED thành công khi yêu cầu đang PENDING")
        void review_approve_success() {
            when(authenUntil.getCurrentUSer()).thenReturn(staffAccount);
            ListingCertificationRequest existing = ListingCertificationRequest.builder()
                    .certificationRequestId(1).status(CertificationStatusEnum.PENDING).listing(listing).seller(seller).build();
            when(certificationRequestRepository.findByIdWithDetails(1)).thenReturn(Optional.of(existing));
            when(certificationRequestRepository.save(any())).thenReturn(existing);
            when(listingRepository.save(any(Listing.class))).thenReturn(listing);

            ReviewCertificationRequest req = new ReviewCertificationRequest();
            req.setDecision(CertificationStatusEnum.APPROVED);

            ResponseEntity<ApiResponse> response = certificationService.reviewCertificationRequest(1, req);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
