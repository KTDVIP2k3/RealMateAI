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
import java.util.List;
import java.util.stream.Collectors;

/**
 * ListingVerificationServiceImplement
 *
 * Xử lý nghiệp vụ duyệt tin của Staff/Admin:
 *   GET  /api/v1/staff/listings/pending          — hàng đợi chờ duyệt
 *   POST /api/v1/staff/listings/{id}/verify       — duyệt (APPROVED/REJECTED)
 *   GET  /api/v1/staff/listings/{id}/verifications — lịch sử duyệt
 *
 * ĐIỂM QUAN TRỌNG NHẤT (giải quyết yêu cầu "duyệt cần duyệt cả ảnh và nội dung"):
 *   - getPendingQueue() trả về Listing kèm property.images NGAY TRONG response,
 *     Staff thấy đủ nội dung + ảnh trong CÙNG một lần gọi API, không phải gọi
 *     thêm API riêng để xem ảnh trước khi quyết định duyệt.
 *   - verifyListing(APPROVED) bị CHẶN ở tầng service nếu Property liên kết
 *     chưa có ảnh nào (propertyImages rỗng) — không thể duyệt một bài đăng
 *     "trắng ảnh", bất kể nội dung text có tốt đến đâu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingVerificationServiceImplement implements ListingVerificationServiceInterface {

    private final ListingVerificationRepository listingVerificationRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;
    private final AuthenUntil authenUntil;

    // ════════════════════════════════════════════════════
    //  GET /staff/listings/pending
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getPendingQueue() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            List<ListingVerificationResponse> queue = listingVerificationRepository
                    .findPendingQueue(ListingStatusEnum.PENDING)
                    .stream()
                    .map(this::toVerificationResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(queue, "Hàng đợi chờ duyệt"));

        } catch (Exception e) {
            log.error("[ListingVerificationService] getPendingQueue lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  POST /staff/listings/{id}/verify
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> verifyListing(Integer listingId, VerifyListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            if (!roleName.equals("Staff") && !roleName.equals("Admin")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Chỉ Staff/Admin được quyền duyệt tin"));
            }

            Listing listing = listingRepository.findByIdWithDetails(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));
            }

            if (request.getDecision() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Quyết định duyệt không được để trống"));
            }

            // ── REJECTED bắt buộc có lý do để Seller biết sửa gì ──
            if (request.getDecision() == ListingStatusEnum.REJECTED
                    && (request.getReviewerNote() == null || request.getReviewerNote().isBlank())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Từ chối duyệt phải kèm lý do (reviewerNote)"));
            }

            // ════════════════════════════════════════════════
            // ĐIỂM CỐT LÕI: APPROVED bắt buộc Property phải có ảnh.
            // Đây chính là chỗ "duyệt cả ảnh và nội dung" được enforce —
            // không phải chỉ là quy ước UI, mà là rule chặn ở Service,
            // không thể bypass dù gọi API trực tiếp.
            // ════════════════════════════════════════════════
            Property property = listing.getProperty();
            if (request.getDecision() == ListingStatusEnum.APPROVED) {
                boolean hasImage = property != null
                        && property.getPropertyImages() != null
                        && !property.getPropertyImages().isEmpty();
                if (!hasImage) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request",
                                    "Không thể duyệt: tài sản chưa có ảnh thực tế. " +
                                            "Yêu cầu Seller bổ sung ảnh trước khi duyệt lại."));
                }
            }

            // ── Ghi nhận lượt duyệt mới (lưu lịch sử, không overwrite bản cũ) ──
            ListingVerification verification = ListingVerification.builder()
                    .listing(listing)
                    .account(currentUser)
                    .status(request.getDecision())
                    .reviewerNote(request.getReviewerNote())
                    .verifiedAt(LocalDateTime.now())
                    .build();
            ListingVerification saved = listingVerificationRepository.save(verification);

            // ── Đồng bộ trạng thái hiển thị của Listing ──
            listing.setIsActive(request.getDecision() == ListingStatusEnum.APPROVED);
            listing.setUpdatedAt(LocalDateTime.now());
            listingRepository.save(listing);

            log.info("[ListingVerificationService] accountId={} đã duyệt listingId={} → {}",
                    currentUser.getAccountId(), listingId, request.getDecision());

            String message = request.getDecision() == ListingStatusEnum.APPROVED
                    ? "Đã duyệt bài đăng — hiện hiển thị trên Chợ BĐS"
                    : "Đã từ chối bài đăng";

            return ResponseEntity.ok(ApiResponse.success(toVerificationResponse(saved), message));

        } catch (Exception e) {
            log.error("[ListingVerificationService] verifyListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  GET /staff/listings/{id}/verifications
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getVerificationHistory(Integer listingId) {
        try {
            List<ListingVerificationResponse> history = listingVerificationRepository
                    .findByListing_ListingIdOrderByListingVerificationIdDesc(listingId)
                    .stream()
                    .map(this::toVerificationResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(history, "Lịch sử duyệt bài đăng"));

        } catch (Exception e) {
            log.error("[ListingVerificationService] getVerificationHistory lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  Mapper
    // ════════════════════════════════════════════════════

    private ListingVerificationResponse toVerificationResponse(ListingVerification lv) {
        Listing l = lv.getListing();
        Account reviewer = lv.getAccount();

        return ListingVerificationResponse.builder()
                .listingVerificationId(lv.getListingVerificationId())
                .status(lv.getStatus())
                .reviewerNote(lv.getReviewerNote())
                .verifiedAt(lv.getVerifiedAt())
                .reviewerAccountId(reviewer != null ? reviewer.getAccountId() : null)
                .reviewerName(reviewer != null ? reviewer.getFull_name() : null)
                .listing(l != null ? listingMapper.toListingDetail(l, l.getProperty()) : null)
                .build();
    }
}
