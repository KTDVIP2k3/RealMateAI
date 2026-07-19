package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyValuation;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Seller;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyValuationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CompleteValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RejectValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SubmitValuationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyValuationResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PropertyValuationRequestService;
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
 * Định giá bất động sản THỦ CÔNG (Seller gửi yêu cầu → Staff kiểm tra thông
 * tin → đưa ra mức giá đề xuất). Dùng lại entity PropertyValuation có sẵn
 * trong schema (trước đó chưa có repository/service/controller nào) — xem
 * giải thích đầy đủ ở PropertyValuation.
 *
 * KHÁC với /ai/property-valuation (định giá TỰ ĐỘNG bằng XGBoost, tức thì,
 * không cần Staff can thiệp) — 2 tính năng độc lập, Seller có thể dùng cả 2.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PropertyValuationRequestServiceImplement implements PropertyValuationRequestService {

    private final PropertyValuationRepository propertyValuationRepository;
    private final PropertyRepository propertyRepository;
    private final AuthenUntil authenUntil;

    // ════════════════════════════════════════════════════════════════════════
    // POST /seller/valuation-requests
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> submitValuationRequest(SubmitValuationRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            Property property = propertyRepository.findByIdWithDetails(request.getPropertyId()).orElse(null);
            if (property == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Tài sản không tồn tại: id=" + request.getPropertyId()));
            }
            if (property.getSeller() == null || !property.getSeller().getSellerId().equals(seller.getSellerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài sản này không thuộc sở hữu của bạn"));
            }
            if (propertyValuationRepository.existsByProperty_PropertyIdAndPropertyValuationStatus(
                    property.getPropertyId(), PropertyValuationStatusEnum.PENDING)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Tài sản này đang có 1 yêu cầu định giá chờ xử lý"));
            }

            LocalDateTime now = LocalDateTime.now();
            PropertyValuation valuation = PropertyValuation.builder()
                    .property(property)
                    .sellerNote(request.getSellerNote())
                    .propertyValuationStatus(PropertyValuationStatusEnum.PENDING)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            PropertyValuation saved = propertyValuationRepository.save(valuation);

            log.info("[PropertyValuationRequestService] sellerId={} gửi yêu cầu định giá propertyId={} (requestId={})",
                    seller.getSellerId(), property.getPropertyId(), saved.getPropertyValuationId());

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                    toResponse(saved), "Đã gửi yêu cầu định giá, đang chờ Staff xử lý"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] submitValuationRequest lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyValuationRequests() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            List<PropertyValuationResponse> result = propertyValuationRepository
                    .findBySellerIdWithDetails(seller.getSellerId())
                    .stream().map(this::toResponse).collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách yêu cầu định giá của bạn"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] getMyValuationRequests lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyValuationRequestDetail(Integer id) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSellerOrThrow(currentUser);

            PropertyValuation valuation = propertyValuationRepository
                    .findByIdAndSellerIdWithDetails(id, seller.getSellerId()).orElse(null);
            if (valuation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại hoặc không thuộc sở hữu của bạn"));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(valuation), "Chi tiết yêu cầu định giá"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] getMyValuationRequestDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getPendingValuationQueue() {
        try {
            List<PropertyValuationResponse> result = propertyValuationRepository
                    .findByStatusWithDetails(PropertyValuationStatusEnum.PENDING)
                    .stream().map(this::toResponse).collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(result, "Hàng đợi yêu cầu định giá chờ xử lý"));
        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] getPendingValuationQueue lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getValuationRequestDetail(Integer id) {
        try {
            PropertyValuation valuation = propertyValuationRepository.findByIdWithDetails(id).orElse(null);
            if (valuation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại: id=" + id));
            }
            return ResponseEntity.ok(ApiResponse.success(toResponse(valuation), "Chi tiết yêu cầu định giá"));
        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] getValuationRequestDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATCH /staff/valuation-requests/{id}/complete
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> completeValuationRequest(Integer id, CompleteValuationRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }

            PropertyValuation valuation = propertyValuationRepository.findByIdWithDetails(id).orElse(null);
            if (valuation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại: id=" + id));
            }
            if (valuation.getPropertyValuationStatus() != PropertyValuationStatusEnum.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("Conflict",
                        "Yêu cầu này đã được xử lý trước đó (status=" + valuation.getPropertyValuationStatus() + ")"));
            }

            valuation.setTotalValue(request.getTotalValue());
            valuation.setReason(request.getReason());
            valuation.setMarketUnitPrice(request.getMarketUnitPrice());
            valuation.setLocationK(request.getLocationK());
            valuation.setGfa(request.getGfa());
            valuation.setConstructionNewPrice(request.getConstructionNewPrice());
            valuation.setRemainingQuantity(request.getRemainingQuantity());
            valuation.setLandPrice(request.getLandPrice());
            valuation.setConstructionCost(request.getConstructionCost());
            valuation.setPropertyValuationStatus(PropertyValuationStatusEnum.COMPLETED);
            valuation.setAccount(currentUser);
            valuation.setReviewedAt(LocalDateTime.now());
            valuation.setUpdatedAt(LocalDateTime.now());
            propertyValuationRepository.save(valuation);

            log.info("[PropertyValuationRequestService] Staff accountId={} hoàn tất định giá requestId={}, totalValue={}",
                    currentUser.getAccountId(), id, request.getTotalValue());

            return ResponseEntity.ok(ApiResponse.success(toResponse(valuation), "Đã đưa ra mức giá đề xuất cho Seller"));

        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] completeValuationRequest lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> rejectValuationRequest(Integer id, RejectValuationRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "Cần đăng nhập"));
            }

            PropertyValuation valuation = propertyValuationRepository.findByIdWithDetails(id).orElse(null);
            if (valuation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Yêu cầu không tồn tại: id=" + id));
            }
            if (valuation.getPropertyValuationStatus() != PropertyValuationStatusEnum.PENDING) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail("Conflict",
                        "Yêu cầu này đã được xử lý trước đó (status=" + valuation.getPropertyValuationStatus() + ")"));
            }

            valuation.setReason(request.getReason());
            valuation.setPropertyValuationStatus(PropertyValuationStatusEnum.FAILED);
            valuation.setAccount(currentUser);
            valuation.setReviewedAt(LocalDateTime.now());
            valuation.setUpdatedAt(LocalDateTime.now());
            propertyValuationRepository.save(valuation);

            return ResponseEntity.ok(ApiResponse.success(toResponse(valuation), "Đã từ chối yêu cầu định giá"));

        } catch (Exception e) {
            log.error("[PropertyValuationRequestService] rejectValuationRequest lỗi", e);
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
        if ("Unauthorized".equals(e.getMessage())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", e.getMessage()));
        }
        if (e.getMessage() != null && e.getMessage().startsWith("Forbidden")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", e.getMessage()));
    }

    private PropertyValuationResponse toResponse(PropertyValuation v) {
        return PropertyValuationResponse.builder()
                .propertyValuationId(v.getPropertyValuationId())
                .propertyId(v.getProperty().getPropertyId())
                .propertyTitle(v.getProperty().getTitle())
                .status(v.getPropertyValuationStatus() != null ? v.getPropertyValuationStatus().name() : null)
                .sellerNote(v.getSellerNote())
                .totalValue(v.getTotalValue())
                .reason(v.getReason())
                .marketUnitPrice(v.getMarketUnitPrice())
                .locationK(v.getLocationK())
                .gfa(v.getGfa())
                .constructionNewPrice(v.getConstructionNewPrice())
                .remainingQuantity(v.getRemainingQuantity())
                .landPrice(v.getLandPrice())
                .constructionCost(v.getConstructionCost())
                .reviewedByName(v.getAccount() != null ? v.getAccount().getFull_name() : null)
                .createdAt(v.getCreatedAt())
                .reviewedAt(v.getReviewedAt())
                .build();
    }
}
