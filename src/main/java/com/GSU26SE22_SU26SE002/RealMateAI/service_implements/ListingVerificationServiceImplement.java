package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.VerifyListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingVerificationResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingVerificationServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingVerificationServiceImplement implements ListingVerificationServiceInterface {

    private final ListingVerificationRepository listingVerificationRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final AuthenUntil authenUntil;

    private Account getCurrentStaffOrAdmin() {
        Account currentUser = authenUntil.getCurrentUSer();
        if (currentUser == null) throw new RuntimeException("Unauthorized");

        String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";
        if (!"Staff".equals(role) && !"Admin".equals(role)) {
            throw new RuntimeException("Forbidden: Chỉ Staff hoặc Admin mới được thực hiện");
        }
        return currentUser;
    }

    // GET Pending Queue
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getPendingQueue() {
        try {
            getCurrentStaffOrAdmin();
            var queue = listingVerificationRepository.findPendingQueue(ListingStatusEnum.PENDING)
                    .stream()
                    .map(this::toVerificationResponse)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(queue, "Hàng đợi chờ duyệt"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("getPendingQueue error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // POST Verify - UPSERT (chỉ 1 record)
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> verifyListing(Integer listingId, VerifyListingRequest request) {
        try {
            Account currentUser = getCurrentStaffOrAdmin();

            Listing listing = listingRepository.findByIdWithDetails(listingId)
                    .orElseThrow(() -> new RuntimeException("Listing not found"));

            // Validation
            if (request.getDecision() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Decision không được để trống"));
            }

            if (request.getDecision() == ListingStatusEnum.REJECTED &&
                    (request.getReviewerNote() == null || request.getReviewerNote().isBlank())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Từ chối phải có lý do"));
            }

            if (request.getDecision() == ListingStatusEnum.APPROVED) {
                Property p = listing.getProperty();
                boolean hasImage = p != null && p.getPropertyImages() != null && !p.getPropertyImages().isEmpty();
                if (!hasImage) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.fail("Bad_Request", "Phải có ít nhất 1 ảnh thực tế"));
                }
            }

            // UPSERT verification
            ListingVerification verification = listingVerificationRepository
                    .findByListing_ListingId(listingId)
                    .orElseGet(() -> {
                        ListingVerification newVer = ListingVerification.builder()
                                .listing(listing)
                                .build();
                        listing.setListingVerification(newVer); // bidirectional
                        return newVer;
                    });

            verification.setAccount(currentUser);
            verification.setStatus(request.getDecision());
            verification.setReviewerNote(request.getReviewerNote());
            verification.setVerifiedAt(LocalDateTime.now());

            listingVerificationRepository.save(verification);

            // Update listing status
            listing.setIsActive(request.getDecision() == ListingStatusEnum.APPROVED);
            listing.setUpdatedAt(LocalDateTime.now());
            listingRepository.save(listing);

            String msg = request.getDecision() == ListingStatusEnum.APPROVED ? "Duyệt thành công" : "Từ chối duyệt";

            return ResponseEntity.ok(ApiResponse.success(toVerificationResponse(verification), msg));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("verifyListing error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // GET Current Status (thay vì history)
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getVerificationStatus(Integer listingId) {
        try {
            ListingVerification ver = listingVerificationRepository
                    .findByListing_ListingId(listingId)
                    .orElse(null);

            if (ver == null) {
                return ResponseEntity.ok(ApiResponse.success(null, "Bài đăng chưa được duyệt lần nào"));
            }

            return ResponseEntity.ok(ApiResponse.success(toVerificationResponse(ver), "Trạng thái duyệt hiện tại"));
        } catch (Exception e) {
            log.error("getVerificationStatus error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse> handleAuthException(RuntimeException e) {
        if (e.getMessage().contains("Unauthorized")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));
        }
        if (e.getMessage().contains("Forbidden")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Forbidden", e.getMessage()));
        }
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Server_Error", e.getMessage()));
    }

    private ListingVerificationResponse toVerificationResponse(ListingVerification lv) {
        return ListingVerificationResponse.builder()
                .listingVerificationId(lv.getListingVerificationId())
                .status(lv.getStatus())
                .reviewerNote(lv.getReviewerNote())
                .verifiedAt(lv.getVerifiedAt())
                .reviewerAccountId(lv.getAccount() != null ? lv.getAccount().getAccountId() : null)
                .reviewerName(lv.getAccount() != null ? lv.getAccount().getFull_name() : null)
                .listing(lv.getListing() != null ?
                        listingMapper.toListingDetail(lv.getListing(), lv.getListing().getProperty()) : null)
                .build();
    }
}