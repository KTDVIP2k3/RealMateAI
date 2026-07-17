package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyRepository;
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
    private final PropertyRepository propertyRepository;
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

            // Bài đăng đã bị Seller xoá mềm vĩnh viễn (DELETED) — không cho Staff/Admin
            // duyệt nữa (tránh hồi sinh 1 tin mà Seller đã chủ động xoá).
            if (listing.getStatus() == com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.DELETED) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bài đăng này đã bị Seller xoá, không thể duyệt"));
            }

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
                boolean hasImage = listing.getListingImages() != null && !listing.getListingImages().isEmpty();
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

            boolean approved = request.getDecision() == ListingStatusEnum.APPROVED;

            // Update listing status
            // isActive chỉ = true khi ĐƯỢC DUYỆT và Seller đang để trạng thái ACTIVE.
            // Nếu Seller đã chủ động HIDE (ẩn) tin trước khi Staff duyệt lại, tin vẫn
            // giữ nguyên trạng thái ẩn — quyết định duyệt không được phép "hồi sinh"
            // hiển thị công khai ngoài ý muốn của Seller.
            boolean sellerWantsActive = listing.getStatus() == null
                    || listing.getStatus() == com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.ACTIVE;
            listing.setIsActive(approved && sellerWantsActive);
            listing.setUpdatedAt(LocalDateTime.now());
            listingRepository.save(listing);

            // Đồng bộ Property: khi Listing được tạo kèm Property MỚI (property
            // chưa từng được duyệt lần nào — vẫn đang isActive=false/pending),
            // quyết định duyệt của bài đăng này áp dụng luôn cho cả Property
            // (APPROVE cả 2 hoặc REJECT cả 2). Nếu Property đã từng được duyệt
            // trước đó (đang tái sử dụng cho 1 Listing khác — luồng ① đăng lại
            // tài sản đã có), KHÔNG đụng vào trạng thái Property, vì Property
            // đó đã hợp lệ độc lập với kết quả duyệt của bài đăng hiện tại.
            Property property = listing.getProperty();
            boolean propertyStillPending = property != null && !Boolean.TRUE.equals(property.getIsActive());
            if (propertyStillPending) {
                property.setIsActive(approved);
                property.setUpdatedAt(LocalDateTime.now());
                propertyRepository.save(property);
                log.info("[ListingVerification] Đồng bộ Property propertyId={} theo quyết định listingId={}: isActive={}",
                        property.getPropertyId(), listingId, approved);
            }

            String msg = approved ? "Duyệt thành công" : "Từ chối duyệt";

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