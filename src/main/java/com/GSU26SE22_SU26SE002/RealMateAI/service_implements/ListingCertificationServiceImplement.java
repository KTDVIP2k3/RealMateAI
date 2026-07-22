package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingCertificationRequestRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyImageRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.ReviewCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitCertificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.CertificationRequestResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingCertificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Tích xanh" — Seller nộp giấy tờ pháp lý tài sản cho 1 Listing CỤ THỂ,
 * Staff duyệt. Xem giải thích thiết kế đầy đủ ở Listing#isVerified,
 * ListingCertificationRequest, PropertyImage.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingCertificationServiceImplement implements ListingCertificationService {

    private final ListingCertificationRequestRepository certificationRequestRepository;
    private final ListingRepository listingRepository;
    private final PropertyImageRepository propertyImageRepository;
    private final CloudinaryMediaServiceImplement cloudinaryMediaService;
    private final AuthenUntil authenUntil;

    // ════════════════════════════════════════════════════════════════════════
    // POST /seller/listings/{id}/certification
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> submitCertificationRequest(Integer listingId, SubmitCertificationRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId()).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            if (Boolean.TRUE.equals(listing.getIsVerified())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bài đăng này đã được tích xanh trước đó"));
            }
            if (certificationRequestRepository.existsByListing_ListingIdAndStatus(listingId, CertificationStatusEnum.PENDING)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bài đăng này đang có 1 yêu cầu tích xanh chờ duyệt"));
            }

            List<MultipartFile> documents = request.getDocuments();
            if (documents == null || documents.isEmpty() || documents.stream().allMatch(MultipartFile::isEmpty)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Cần ít nhất 1 giấy tờ (sổ đỏ/hợp đồng...) để yêu cầu tích xanh"));
            }

            LocalDateTime now = LocalDateTime.now();
            ListingCertificationRequest certRequest = ListingCertificationRequest.builder()
                    .listing(listing)
                    .seller(seller)
                    .status(CertificationStatusEnum.PENDING)
                    .createdAt(now)
                    .build();
            ListingCertificationRequest savedRequest = certificationRequestRepository.save(certRequest);

            Property property = listing.getProperty();
            int order = 0;
            for (MultipartFile file : documents) {
                if (file == null || file.isEmpty()) continue;
                String url = cloudinaryMediaService.uploadImage(file);
                PropertyImage doc = PropertyImage.builder()
                        .property(property)
                        .certificationRequest(savedRequest)
                        .imageUrl(url)
                        .isMain(order == 0)
                        .displayOrder(order++)
                        .build();
                propertyImageRepository.save(doc);
            }

            listing.setCertificationStatus(CertificationStatusEnum.PENDING);
            listingRepository.save(listing);

            log.info("[ListingCertificationService] sellerId={} nộp yêu cầu tích xanh cho listingId={} (requestId={}, {} tài liệu)",
                    seller.getSellerId(), listingId, savedRequest.getCertificationRequestId(), order);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    toResponse(certificationRequestRepository.findByIdWithDetails(savedRequest.getCertificationRequestId())
                            .orElse(savedRequest)),
                    "Đã gửi yêu cầu tích xanh, đang chờ Staff duyệt"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingCertificationService] submitCertificationRequest lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyCertificationRequests() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            List<CertificationRequestResponse> result = certificationRequestRepository
                    .findBySellerIdWithDetails(seller.getSellerId())
                    .stream().map(this::toResponse).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách yêu cầu tích xanh của bạn"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingCertificationService] getMyCertificationRequests lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyCertificationRequestDetail(Integer id) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            ListingCertificationRequest req = certificationRequestRepository
                    .findByIdAndSellerIdWithDetails(id, seller.getSellerId()).orElse(null);
            if (req == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại hoặc không thuộc sở hữu của bạn"));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(req), "Chi tiết yêu cầu tích xanh"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingCertificationService] getMyCertificationRequestDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getPendingCertificationQueue() {
        try {
            List<CertificationRequestResponse> result = certificationRequestRepository
                    .findByStatusWithDetails(CertificationStatusEnum.PENDING)
                    .stream().map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(result, "Hàng đợi yêu cầu tích xanh chờ duyệt"));
        } catch (Exception e) {
            log.error("[ListingCertificationService] getPendingCertificationQueue lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getCertificationRequestDetail(Integer id) {
        try {
            ListingCertificationRequest req = certificationRequestRepository.findByIdWithDetails(id).orElse(null);
            if (req == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại: id=" + id));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(req), "Chi tiết yêu cầu tích xanh"));
        } catch (Exception e) {
            log.error("[ListingCertificationService] getCertificationRequestDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATCH /staff/certification-requests/{id}/review
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> reviewCertificationRequest(Integer id, ReviewCertificationRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }

            if (request.getDecision() == null || request.getDecision() == CertificationStatusEnum.PENDING) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "decision phải là APPROVED hoặc REJECTED"));
            }
            if (request.getDecision() == CertificationStatusEnum.REJECTED
                    && (request.getReviewerNote() == null || request.getReviewerNote().isBlank())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Từ chối phải kèm lý do (reviewerNote)"));
            }

            ListingCertificationRequest certRequest = certificationRequestRepository.findByIdWithDetails(id).orElse(null);
            if (certRequest == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại: id=" + id));
            }
            if (certRequest.getStatus() != CertificationStatusEnum.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Yêu cầu này đã được xử lý trước đó (status=" + certRequest.getStatus() + ")"));
            }

            certRequest.setStatus(request.getDecision());
            certRequest.setReviewerNote(request.getReviewerNote());
            certRequest.setReviewedBy(currentUser);
            certRequest.setReviewedAt(LocalDateTime.now());
            certificationRequestRepository.save(certRequest);

            Listing listing = certRequest.getListing();
            listing.setCertificationStatus(request.getDecision());
            if (request.getDecision() == CertificationStatusEnum.APPROVED) {
                listing.setIsVerified(true);
            }
            listingRepository.save(listing);

            log.info("[ListingCertificationService] Staff accountId={} {} yêu cầu tích xanh id={} (listingId={})",
                    currentUser.getAccountId(), request.getDecision(), id, listing.getListingId());

            String msg = request.getDecision() == CertificationStatusEnum.APPROVED
                    ? "Duyệt tích xanh thành công — bài đăng đã hiển thị badge xác thực"
                    : "Đã từ chối yêu cầu tích xanh";

            return ResponseEntity.ok(ApiResponse.success(toResponse(certRequest), msg));

        } catch (Exception e) {
            log.error("[ListingCertificationService] reviewCertificationRequest lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Seller getCurrentSellerOrThrow(Account currentUser) {
        if (currentUser == null) throw new RuntimeException("Unauthorized");
        if (currentUser.getRole() == null || !"Seller".equals(currentUser.getRole().name())) {
            throw new RuntimeException("Forbidden: Chỉ tài khoản Seller mới được thực hiện chức năng này");
        }
        if (currentUser.getSeller() == null) throw new RuntimeException("Seller profile không tồn tại");
        return currentUser.getSeller();
    }

    private ResponseEntity<ApiResponse> handleAuthException(RuntimeException e) {
        String message = e.getMessage();
        if ("Unauthorized".equals(message)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", message));
        }
        if (message != null && message.startsWith("Forbidden")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden", message));
        }
        log.error("[ListingCertificationService] Lỗi không xác định", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Server_Error", message != null ? message : e.getClass().getSimpleName()));
    }

    private CertificationRequestResponse toResponse(ListingCertificationRequest r) {
        List<String> urls = r.getDocuments() == null ? List.of() : r.getDocuments().stream()
                .sorted(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(PropertyImage::getImageUrl)
                .collect(Collectors.toList());

        return CertificationRequestResponse.builder()
                .certificationRequestId(r.getCertificationRequestId())
                .listingId(r.getListing().getListingId())
                .listingTitle(r.getListing().getTitle())
                .sellerId(r.getSeller().getSellerId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .reviewerNote(r.getReviewerNote())
                .documentUrls(urls)
                .reviewedByName(r.getReviewedBy() != null ? r.getReviewedBy().getFull_name() : null)
                .createdAt(r.getCreatedAt())
                .reviewedAt(r.getReviewedAt())
                .build();
    }
}
