package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.*;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.*;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.TwoStepPaginationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImplement implements ListingServiceInterface {

    private final ListingRepository listingRepository;
    private final ListingVerificationRepository listingVerificationRepository;
    private final PropertyRepository propertyRepository;
    private final LocationRepository locationRepository;
    private final ListingImageRepository listingImageRepository;
    private final SellerRepository sellerRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyConditionRepository propertyConditionRepository;
    private final InvestorRepository investorRepository;
    private final FavoriteListingRepository favoriteListingRepository;
    private final WardRepository wardRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ProvinceRepository provinceRepository;
    // MỚI: dùng cho GET /listings/featured (top view thật từ ActiveLog).
    private final ActiveLogRepository activeLogRepository;
    private final ListingMapper listingMapper;
    private final NotificationService notificationService;
    private final UserEventTrackingService userEventTrackingService;
    private final AuthenUntil authenUntil;
    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final PostingPackageOrderServiceInterface postingPackageOrderServiceInterface;

    // MỚI: self-reference qua Spring proxy (KHÔNG dùng "this." trực tiếp) —
    // bắt buộc để persistNewListingCore() bên dưới thực sự chạy trong 1
    // transaction ĐỘC LẬP, COMMIT XONG trước khi gọi thanh toán tự động (xem
    // javadoc persistNewListingCore để hiểu rõ lý do). Dùng chính class cụ
    // thể (không phải interface) vì persistNewListingCore là hàm NỘI BỘ,
    // không muốn lộ ra ngoài ListingServiceInterface (public API). Field
    // injection + @Lazy để tránh vòng lặp khởi tạo bean (bean tự phụ thuộc
    // chính nó).
    @Autowired
    @Lazy
    private ListingServiceImplement self;
    private final SearchHistoryRepository searchHistoryRepository;

    private static final int PAGE_SIZE = 10;
    private static final int GEMINI_MAX_RETRY = 5;
    private static final long GEMINI_RETRY_DELAY_MS = 8000;
    private static final int MAX_SEARCH_PAGE_SIZE = 50;
    // MỚI: số gợi ý tối đa mỗi nhóm ở GET /listings/search/suggestions.
    private static final int SUGGESTION_LIMIT = 5;
    // MỚI: số dòng lịch sử tìm kiếm tối đa lưu cho mỗi tài khoản.
    private static final int SEARCH_HISTORY_CAP = 20;

    private static class ListingConflictException extends RuntimeException {
        final HttpStatus status;
        ListingConflictException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }
    private Seller getCurrentSeller(Account currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("Unauthorized");
        }
        if (currentUser.getRole() == null || !"Seller".equals(currentUser.getRole().name())) {
            throw new RuntimeException("Forbidden: Chỉ tài khoản Seller mới được thực hiện chức năng này");
        }

        return sellerRepository.findByAccount_AccountId(currentUser.getAccountId())
                .orElseThrow(() -> new RuntimeException("Seller profile không tồn tại"));
    }

    // ════════════════════════════════════════════════════════════════════════
    // API TẠO TIN ĐĂNG DUY NHẤT — gộp Luồng ① (đăng lại tài sản có sẵn) và
    // Luồng ② (tạo tài sản mới) cũ thành 1 API, phân nhánh bằng
    // request.reuseExistingProperty. Ảnh KHÔNG upload trực tiếp trong API này
    // nữa — Seller phải gọi POST /media/upload/multiple TRƯỚC (entityType=
    // ACCOUNT), lấy publicId trả về đưa vào request.draftImagePublicIds; API
    // này chỉ "nhận nuôi" (re-parent) ảnh đã có sẵn trên Cloudinary sang
    // Listing vừa tạo.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public ResponseEntity<ApiResponse> createListing(CreateListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            // MỚI: validate sớm — nếu Seller muốn thanh toán ngay lúc tạo tin
            // (truyền postingPackageId) thì duration/totalAmount là BẮT BUỘC.
            // Chặn ở đây TRƯỚC khi tạo Property/Listing để tránh tạo ra 1
            // Listing "treo" do request thanh toán thiếu field ngay từ đầu.
            if (request.getPostingPackageId() != null) {
                if (request.getDuration() == null || request.getDuration() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request",
                                    "duration phải lớn hơn 0 khi có postingPackageId"));
                }
                if (request.getTotalAmount() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request",
                                    "totalAmount không được để trống khi có postingPackageId"));
                }
            }

            // ════════════════════════════════════════════════════════════════
            // SỬA (fix bug crash thật): gọi qua self-proxy để persistNewListingCore()
            // chạy trong 1 TRANSACTION RIÊNG, COMMIT XONG trước khi tiếp tục — xem
            // javadoc đầy đủ ở persistNewListingCore() bên dưới để hiểu lý do bắt
            // buộc phải tách như vậy (liên quan tới attemptAutoPaymentForNewListing
            // chạy REQUIRES_NEW không thấy được Listing nếu nó chưa thực sự commit).
            // ════════════════════════════════════════════════════════════════
            NewListingCreationResult creationResult = self.persistNewListingCore(request, currentUser, seller);
            Property property = creationResult.property;
            Listing saved = creationResult.listing;
            int reparented = creationResult.reparentedImageCount;

            Property refreshed = propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property);
            Listing refreshedListing = listingRepository.findByIdWithDetails(saved.getListingId()).orElse(saved);

            Object listingDetail = listingMapper.toListingDetail(refreshedListing, refreshed);

            // ════════════════════════════════════════════════════════════════
            // Thanh toán TỰ ĐỘNG ngay khi tạo tin nếu request kèm postingPackageId.
            // Tại đây Listing/Property ĐÃ COMMIT XONG THẬT SỰ (persistNewListingCore
            // ở trên đã return, transaction của nó đã đóng) — nên
            // attemptAutoPaymentForNewListing() (REQUIRES_NEW, chạy transaction
            // KHÁC ở PostingPackageOrderServiceImplement) chắc chắn nhìn thấy được
            // Listing này. Nếu thanh toán thất bại (ví null/không đủ tiền), Listing
            // VẪN tồn tại đúng ở trạng thái WAITING_PAYMENT, KHÔNG bị rollback theo
            // (2 transaction độc lập), VÀ 1 PostingPackageOrder với status=FAILED
            // đã được tạo sẵn (xem PostingPackageOrderServiceImplement#executePayment)
            // — FE dùng data.postingPackageOrderId để gọi
            // POST /seller/posting-package-orders/{id}/retry-pay thanh toán lại.
            // FE dựa vào data.paymentStatus để quyết định điều hướng sang trang
            // thanh toán (paymentStatus=FAILED kèm paymentErrorCode/paymentMessage)
            // hay coi như xong (paymentStatus=SUCCESS, tin đã chuyển WAITING_PAYMENT
            // -> PENDING, chờ Staff duyệt).
            // Không truyền postingPackageId -> GIỮ NGUYÊN hành vi cũ hoàn toàn
            // (data = listingDetail thẳng, không bọc thêm object) để không phá vỡ
            // FE hiện tại chưa cập nhật theo luồng thanh toán mới.
            // ════════════════════════════════════════════════════════════════
            if (request.getPostingPackageId() != null) {
                PaymentAttemptResult paymentResult = postingPackageOrderServiceInterface
                        .attemptAutoPaymentForNewListing(saved.getListingId(), request.getPostingPackageId(),
                                request.getDuration(), request.getTotalAmount());

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("listing", listingDetail);
                data.put("paymentStatus", paymentResult.isSuccess() ? "SUCCESS" : "FAILED");
                data.put("paymentErrorCode", paymentResult.getErrorCode());
                data.put("paymentMessage", paymentResult.getMessage());
                data.put("postingPackageOrderId", paymentResult.getPostingPackageOrderId());

                String suffix = (reparented > 0 ? ", kèm " + reparented + " ảnh" : "");
                String finalMsg = paymentResult.isSuccess()
                        ? "Bài đăng tạo thành công" + suffix + ", đã thanh toán gói dịch vụ đăng tin, đang chờ Staff duyệt"
                        : "Bài đăng tạo thành công" + suffix + ", nhưng thanh toán gói dịch vụ đăng tin THẤT BẠI ("
                        + paymentResult.getMessage() + ") — vui lòng gọi retry-pay với postingPackageOrderId="
                        + paymentResult.getPostingPackageOrderId() + " để thanh toán lại";

                log.info("[ListingService] createListing: thanh toán tự động listingId={}, success={}, errorCode={}, orderId={}",
                        saved.getListingId(), paymentResult.isSuccess(), paymentResult.getErrorCode(), paymentResult.getPostingPackageOrderId());

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, finalMsg));
            }

            String msg = "Bài đăng tạo thành công"
                    + (reparented > 0 ? ", kèm " + reparented + " ảnh" : "")
                    + ", vui lòng thanh toán gói dịch vụ đăng tin để gửi duyệt";

            // Dùng mapper "ForOwner" — KHÔNG trả sellerAvatar/sellerStatus/
            // contactPersonName/linkSocialContactPerson (chỉ dành cho người XEM
            // công khai), THAY VÀO ĐÓ trả thêm wardCode (mã vùng) + email của
            // chính Seller đang tạo tin.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(listingDetail, msg));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] createListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    /** MỚI: kết quả nội bộ của persistNewListingCore() — không phải response HTTP. */
    private static class NewListingCreationResult {
        Property property;
        Listing listing;
        int reparentedImageCount;
    }

    // ════════════════════════════════════════════════════════════════════════
    // MỚI (fix bug crash thật đã gặp khi test): tách phần TẠO + LƯU
    // Property/Listing/Verification/Ảnh ra 1 hàm @Transactional RIÊNG, PHẢI
    // được gọi qua self-proxy (self.persistNewListingCore(...) ở createListing()
    // — KHÔNG được gọi "this.persistNewListingCore(...)" trực tiếp, vì gọi trực
    // tiếp trong cùng object sẽ bị Spring BỎ QUA annotation @Transactional
    // (self-invocation không đi qua AOP proxy) — hàm này sẽ không tạo transaction
    // riêng thật sự nếu gọi sai cách.
    //
    // LÝ DO BẮT BUỘC TÁCH: PostgreSQL (và JPA nói chung) KHÔNG cho 1 transaction
    // B nhìn thấy dữ liệu CHƯA COMMIT của transaction A, kể cả khi A chỉ đang
    // "suspend" (REQUIRES_NEW mở transaction B trên 1 connection RIÊNG hoàn
    // toàn). Trước đây createListing() dùng 1 @Transactional DUY NHẤT bao trọn
    // cả việc tạo Listing LẪN việc gọi thanh toán — khi
    // attemptAutoPaymentForNewListing() (REQUIRES_NEW, ở service khác) cố
    // listingRepository.findById(listingId) để lấy lại Listing vừa tạo, nó
    // KHÔNG THẤY ĐƯỢC (transaction ngoài bao quanh createListing() chưa commit,
    // mới chỉ flush() trong session hiện tại) -> luôn trả "LISTING_NOT_FOUND"
    // dù Listing đã tồn tại thật (đúng bug đã gặp: "success=false,
    // errorCode=LISTING_NOT_FOUND" ngay sau khi vừa tạo listing đó). Nay bắt
    // buộc phải tách để hàm này THỰC SỰ commit xong (transaction đóng lại khi
    // return) trước khi createListing() gọi tiếp bước thanh toán.
    // ════════════════════════════════════════════════════════════════════════
    @Transactional
    public NewListingCreationResult persistNewListingCore(CreateListingRequest request, Account currentUser, Seller seller) {
        boolean reuseExisting = Boolean.TRUE.equals(request.getReuseExistingProperty());

        LocalDateTime now = LocalDateTime.now();
        Property property;
        boolean isNewProperty;

        if (reuseExisting) {
            // ── Nhánh: dùng lại tài sản ĐÃ CÓ SẴN ────────────────────────
            if (request.getExistingPropertyId() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST,
                        "existingPropertyId không được để trống khi reuseExistingProperty=true");
            }

            property = propertyRepository.findById(request.getExistingPropertyId()).orElse(null);
            if (property == null) {
                throw new ListingConflictException(HttpStatus.NOT_FOUND,
                        "Tài sản không tồn tại: id=" + request.getExistingPropertyId());
            }
            if (property.getSeller() == null ||
                    !property.getSeller().getSellerId().equals(seller.getSellerId())) {
                throw new ListingConflictException(HttpStatus.FORBIDDEN, "Tài sản này không thuộc sở hữu của bạn");
            }
            isNewProperty = false;

        } else {
            // ── Nhánh: tạo tài sản MỚI ───────────────────────────────────
            if (request.getPropTitle() == null || request.getPropTitle().isBlank()) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propTitle không được để trống");
            }
            if (request.getPropPrice() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propPrice không được để trống");
            }
            if (request.getPropArea() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propArea không được để trống");
            }
            if (request.getPropPropertyTypeId() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propPropertyTypeId không được để trống");
            }
            if (request.getPropLatitude() == null || request.getPropLongitude() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propLatitude/propLongitude không được để trống");
            }
            if (request.getPropWardCode() == null || request.getPropWardCode().isBlank()) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propWardCode không được để trống");
            }
            // Tài sản mới chưa có ảnh nào để tự động dùng lại → bắt buộc Seller
            // phải upload ảnh trước và truyền publicId vào draftImagePublicIds.
            if (request.getDraftImagePublicIds() == null || request.getDraftImagePublicIds().isEmpty()) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST,
                        "Tạo tài sản mới phải kèm ít nhất 1 ảnh (draftImagePublicIds) — "
                                + "upload trước qua POST /media/upload/multiple");
            }

            PropertyType propertyType = propertyTypeRepository
                    .findById(request.getPropPropertyTypeId()).orElse(null);
            if (propertyType == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST, "Loại bất động sản không hợp lệ");
            }

            PropertyCondition propertyCondition = null;
            Integer conditionId = request.getPropPropertyConditionId();
            if (conditionId != null) {
                if (conditionId <= 0) {
                    throw new ListingConflictException(HttpStatus.BAD_REQUEST, "propPropertyConditionId phải lớn hơn 0");
                }
                propertyCondition = propertyConditionRepository.findById(conditionId).orElse(null);
                if (propertyCondition == null) {
                    throw new ListingConflictException(HttpStatus.BAD_REQUEST,
                            "Tình trạng bất động sản không tồn tại với id = " + conditionId);
                }
            }

            Ward ward = wardRepository.findById(request.getPropWardCode()).orElse(null);
            if (ward == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST,
                        "Mã phường/xã không hợp lệ: " + request.getPropWardCode());
            }

            Location location = Location.builder()
                    .latitude(request.getPropLatitude())
                    .longitude(request.getPropLongitude())
                    .postalCode(request.getPropPostalCode())
                    .ward(ward)
                    .build();
            Location savedLocation = locationRepository.save(location);

            Property newProperty = Property.builder()
                    .seller(seller)
                    .propertyType(propertyType)
                    .propertyCondition(propertyCondition)
                    .location(savedLocation)
                    .title(request.getPropTitle())
                    .description(request.getPropDescription())
                    .price(request.getPropPrice())
                    .area(request.getPropArea())
                    .floor(request.getPropFloor())
                    .bedroom(request.getPropBedroom())
                    .bathroom(request.getPropBathroom())
                    .direction(request.getPropDirection())
                    .legalStatus(request.getPropLegalStatus())
                    .addressParticular(request.getPropAddressParticular())
                    .projectName(request.getPropProjectName())
                    .furniture(request.getPropFurniture())
                    // Property MỚI tạo cùng Listing phải ở trạng thái CHỜ DUYỆT
                    // (isActive=false), chỉ bật lên khi Staff APPROVE bài đăng
                    // (xem ListingVerificationServiceImplement#verifyListing).
                    .isActive(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            property = propertyRepository.save(newProperty);
            propertyRepository.flush();
            isNewProperty = true;
        }

        // ── Tạo Listing (KHÔNG còn fill contactPersonName/linkSocialContactPerson
        //    khi tạo — 2 field này chỉ có thể bổ sung sau qua PUT /listings/{id}) ──
        Listing listing = Listing.builder()
                .property(property)
                .seller(seller)
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .contactPerson(request.getContactPerson())
                .contactPersonPhone(request.getContactPersonPhone())
                .contactEmail(request.getContactEmail())
                .viewingDate(request.getViewingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isActive(false)
                .status(SellerListingStatusEnum.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Listing saved = listingRepository.save(listing);
        listingRepository.flush();
        createPendingVerification(saved);

        // ── Ảnh: re-parent từ draftImagePublicIds (đã upload sẵn qua Media API).
        // Property tái sử dụng mà Seller không gửi ảnh mới → tự động copy lại
        // ảnh từ 1 Listing khác cùng Property (giữ đúng trải nghiệm cũ).
        int reparented;
        if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty()) {
            reparented = reparentUploadedImagesToListing(
                    request.getDraftImagePublicIds(), currentUser, saved, request.getThumbnailImageIndex());
            if (isNewProperty && reparented == 0) {
                throw new ListingConflictException(HttpStatus.CONFLICT,
                        "Không thể gắn ảnh vào bài đăng. Vui lòng kiểm tra lại publicId đã upload.");
            }
        } else {
            reparented = copyImagesFromOtherListingsOfProperty(property, saved);
        }

        log.info("[ListingService] createListing: listingId={}, propertyId={}, sellerId={}, tàiSảnMới={}, ảnh={}",
                saved.getListingId(), property.getPropertyId(), seller.getSellerId(), isNewProperty, reparented);

        notificationService.notify(seller.getAccount(),
                "Bạn vừa đăng tin \"" + saved.getTitle() + "\" thành công, vui lòng thanh toán gói dịch vụ đăng tin để gửi duyệt.",
                NotificationTypeEnum.LISTING);

        NewListingCreationResult result = new NewListingCreationResult();
        result.property = property;
        result.listing = saved;
        result.reparentedImageCount = reparented;
        return result;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Tạo bản ghi ListingVerification ở trạng thái WAITING_PAYMENT ngay
    // khi Listing được tạo — tin đăng KHÔNG vào hàng đợi chờ Staff duyệt
    // (GET /staff/listings/pending chỉ lọc theo PENDING) cho tới khi Seller
    // thanh toán xong gói dịch vụ đăng tin (xem
    // PostingPackageOrderServiceImplement#payPostingPackage — nơi DUY NHẤT
    // chuyển WAITING_PAYMENT -> PENDING).
    // ════════════════════════════════════════════════════════════════════════
    private void createPendingVerification(Listing listing) {
        ListingVerification verification = ListingVerification.builder()
                .listing(listing)
                .status(ListingStatusEnum.WAITING_PAYMENT)
                .build();
        listingVerificationRepository.save(verification);
        // Gán ngược để object Listing trong bộ nhớ phản ánh đúng ngay lập tức
        // (response trả về sau khi tạo cũng hiển thị verificationStatus=WAITING_PAYMENT).
        listing.setListingVerification(verification);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Khi Seller (không phải Admin/Staff) chỉnh sửa 1 bài đăng ĐÃ có
    // kết quả duyệt (APPROVED/REJECTED), đưa verification hiện tại về lại
    // PENDING để bài đăng quay lại hàng đợi chờ Staff duyệt lại.
    // ════════════════════════════════════════════════════════════════════════
    private void resetVerificationToPending(Listing listing) {
        ListingVerification verification = listingVerificationRepository
                .findByListing_ListingId(listing.getListingId())
                .orElse(null);

        if (verification == null) {
            createPendingVerification(listing);
            return;
        }

        if (verification.getStatus() != ListingStatusEnum.PENDING) {
            verification.setStatus(ListingStatusEnum.PENDING);
            verification.setReviewerNote(null);
            verification.setVerifiedAt(null);
            listingVerificationRepository.save(verification);
        }
    }

    // Helper xử lý exception auth
    private ResponseEntity<ApiResponse> handleAuthException(RuntimeException e) {
        // SỬA: ListingConflictException đã tự mang đúng HttpStatus (400/403/404/409...)
        // — phải ưu tiên dùng NÓ trước, nếu không sẽ bị rơi xuống nhánh mặc định
        // INTERNAL_SERVER_ERROR bên dưới (bug có sẵn, lộ ra khi persistNewListingCore()
        // giờ dùng exception thay vì return ResponseEntity trực tiếp cho các lỗi
        // validate — xem javadoc persistNewListingCore).
        if (e instanceof ListingConflictException lce) {
            return ResponseEntity.status(lce.status).body(ApiResponse.fail(lce.status.toString(), e.getMessage()));
        }
        if (e.getMessage().contains("Unauthorized")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));
        }
        if (e.getMessage().contains("Forbidden")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Forbidden", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Server_Error", e.getMessage()));
    }


    // ════════════════════════════════════════════════════════════════════════
    private Pageable resolvePageable(Integer page, Integer size, Sort sort) {
        boolean wantAll = page != null && page == 0 && size != null && size == 0;
        if (wantAll) {
            return Pageable.unpaged(sort);
        }
        int effectivePage = (page == null || page < 0) ? 0 : page;
        int effectiveSize = (size == null || size <= 0) ? PAGE_SIZE : size;
        effectiveSize = Math.min(effectiveSize, MAX_SEARCH_PAGE_SIZE);
        return PageRequest.of(effectivePage, effectiveSize, sort);
    }

    // Các method khác giữ nguyên (public + seller)
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMarketListings(int page, int size) {
        try {
            // SỬA: dùng resolvePageable() dùng chung — page=0 & size=0 tường
            // minh => lấy hết, không phân trang.
            Pageable pageable = resolvePageable(page, size,
                    Sort.by(Sort.Direction.DESC, "priority").and(Sort.by(Sort.Direction.DESC, "createdAt")));

            // Pattern 2-query (xem TwoStepPaginationUtil): query 1 lấy ID đã phân trang
            // CHUẨN ở tầng DB (findByIsActiveTrue không JOIN FETCH collection nào), query 2
            // fetch chi tiết (property/propertyType/location/propertyImages) theo đúng ID đó.
            // Thay cho findAllActiveWithDetails cũ (JOIN FETCH propertyImages + Pageable
            // cùng lúc → Hibernate phải phân trang trong memory, chậm dần khi data lớn).
            Page<Listing> listingPage = TwoStepPaginationUtil.<Integer, Listing>paginate(
                    pageable,
                    p -> listingRepository.findByIsActiveTrue(p).map(Listing::getListingId),
                    listingRepository::findAllByListingIdInWithDetails,
                    Listing::getListingId
            );

            Account currentUser = authenUntil.getCurrentUSer();
            Set<Integer> favoritedIds = Collections.emptySet();
            if (currentUser != null) {
                Investor investor = investorRepository
                        .findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                if (investor != null) {
                    favoritedIds = new HashSet<>(
                            favoriteListingRepository.findFavoritedListingIdsByInvestorId(investor.getInvestorId()));
                }
            }

            final Set<Integer> favIds = favoritedIds;
            List<ListingSummaryResponse> content = listingPage.getContent().stream()
                    .map(l -> listingMapper.toListingSummary(l, favIds.contains(l.getListingId())))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", listingPage.getNumber());
            result.put("size", listingPage.getSize());
            result.put("totalElements", listingPage.getTotalElements());
            result.put("totalPages", listingPage.getTotalPages());
            result.put("last", listingPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách tin đăng trên Chợ BĐS"));
        } catch (Exception e) {
            log.error("[ListingService] getMarketListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getListingDetail(Integer listingId) {
        try {
            Listing listing = listingRepository.findActiveById(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));
            }

            Account viewer = authenUntil.getCurrentUSer();
            userEventTrackingService.recordSilently(viewer, UserEventTypeEnum.VIEW, listingId);

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(listing, listing.getProperty()), "Chi tiết tin đăng"));
        } catch (Exception e) {
            log.error("[ListingService] getListingDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyListings(int page, int size) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            int effectiveSize = size > 0 ? size : PAGE_SIZE;
            Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

            Page<Listing> listingPage = listingRepository.findBySellerId(seller.getSellerId(), pageable);

            List<ListingSummaryResponse> content = listingPage.getContent().stream()
                    .map(l -> listingMapper.toListingSummary(l, false))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", listingPage.getNumber());
            result.put("size", listingPage.getSize());
            result.put("totalElements", listingPage.getTotalElements());
            result.put("totalPages", listingPage.getTotalPages());
            result.put("last", listingPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách tin đăng của bạn"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] getMyListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyProperties(int page, int size) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            int effectiveSize = size > 0 ? size : PAGE_SIZE;
            Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize);

            Page<Property> propertyPage = propertyRepository.findBySellerIdWithDetails(seller.getSellerId(), pageable);

            List<PropertyDetailResponse> content = propertyPage.getContent().stream()
                    .map(p -> {
                        int listingCount = (int) listingRepository.countByProperty_PropertyId(p.getPropertyId());
                        return listingMapper.toPropertyDetail(p, listingCount);
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", propertyPage.getNumber());
            result.put("size", propertyPage.getSize());
            result.put("totalElements", propertyPage.getTotalElements());
            result.put("totalPages", propertyPage.getTotalPages());
            result.put("last", propertyPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Danh sách tài sản bạn đang sở hữu"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] getMyProperties lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));
            }

            Listing listing = listingRepository.findByIdWithDetails(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));
            }

            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            boolean isAdminOrStaff = roleName.equals("Admin") || roleName.equals("Staff");

            // Tin đã bị Seller xoá mềm vĩnh viễn (DELETED) — KHÔNG ai được sửa nữa,
            // kể cả Admin/Staff (đây là bản ghi lịch sử, không còn tái sử dụng được).
            if (listing.getStatus() == SellerListingStatusEnum.DELETED) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bài đăng đã bị xoá, không thể chỉnh sửa"));
            }

            if (!isAdminOrStaff) {
                Seller seller = getCurrentSeller(currentUser);
                boolean isOwner = listing.getSeller() != null &&
                        listing.getSeller().getSellerId().equals(seller.getSellerId());
                if (!isOwner) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Bạn không có quyền sửa bài đăng này"));
                }
                listing.setIsActive(false);
                // Sửa nội dung → cần Staff duyệt lại từ đầu, đưa verification về PENDING
                // để bài đăng xuất hiện lại trong GET /staff/listings/pending.
                resetVerificationToPending(listing);
            }

            // Update fields (giữ nguyên logic cũ của bạn)
            if (request.getTitle() != null) listing.setTitle(request.getTitle());
            if (request.getDescription() != null) listing.setDescription(request.getDescription());
            if (request.getPrice() != null) listing.setPrice(request.getPrice());
            if (request.getContactPerson() != null) listing.setContactPerson(request.getContactPerson());
            if (request.getContactPersonPhone() != null) listing.setContactPersonPhone(request.getContactPersonPhone());
            if (request.getContactEmail() != null) listing.setContactEmail(request.getContactEmail());
            if (request.getViewingDate() != null) listing.setViewingDate(request.getViewingDate());
            if (request.getStartTime() != null) listing.setStartTime(request.getStartTime());
            if (request.getEndTime() != null) listing.setEndTime(request.getEndTime());
            listing.setUpdatedAt(LocalDateTime.now());

            // Update Property fields...
            Property property = listing.getProperty();
            if (property != null) {
                // (Bạn có thể copy phần update property từ code cũ vào đây)
                if (request.getPropertyTitle() != null) property.setTitle(request.getPropertyTitle());
                if (request.getPropertyDescription() != null) property.setDescription(request.getPropertyDescription());
                if (request.getPropertyPrice() != null) property.setPrice(request.getPropertyPrice());
                if (request.getArea() != null) property.setArea(request.getArea());
                if (request.getFloor() != null) property.setFloor(request.getFloor());
                if (request.getBedroom() != null) property.setBedroom(request.getBedroom());
                if (request.getBathroom() != null) property.setBathroom(request.getBathroom());
                if (request.getDirection() != null) property.setDirection(request.getDirection());
                if (request.getFurniture() != null) property.setFurniture(request.getFurniture());

                // Update PropertyType, PropertyCondition, Location... (giữ nguyên code cũ)

                property.setUpdatedAt(LocalDateTime.now());
                propertyRepository.save(property);
            }

            // Re-parent images nếu có — ảnh nay gắn theo LISTING, không còn theo Property
            int reparentedCount = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty()) {
                reparentedCount = reparentUploadedImagesToListing(
                        request.getDraftImagePublicIds(), currentUser, listing, request.getThumbnailImageIndex());
            }

            // Đổi thumbnail sang 1 ảnh ĐÃ CÓ SẴN (không upload gì mới) — xử lý SAU
            // reparent ảnh mới ở trên để field này luôn có tiếng nói cuối cùng nếu
            // Seller gửi cả 2 (xem javadoc UpdateListingRequest#thumbnailListingImageId).
            if (request.getThumbnailListingImageId() != null) {
                ListingImage targetImage = listingImageRepository
                        .findByListingImageIdAndListing_ListingId(request.getThumbnailListingImageId(), listingId)
                        .orElse(null);
                if (targetImage == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.fail("Bad_Request",
                            "thumbnailListingImageId=" + request.getThumbnailListingImageId()
                                    + " không tồn tại hoặc không thuộc bài đăng này"));
                }
                listingImageRepository.clearThumbnailByListingId(listingId);
                targetImage.setIsThumbnail(true);
                listingImageRepository.save(targetImage);
            }

            Listing updated = listingRepository.save(listing);

            if (updated.getSeller() != null && updated.getSeller().getAccount() != null) {
                notificationService.notify(updated.getSeller().getAccount(),
                        "Tin đăng \"" + updated.getTitle() + "\" của bạn vừa được cập nhật, cần Staff duyệt lại.",
                        NotificationTypeEnum.LISTING);
            }

            Property refreshed = property != null
                    ? propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property)
                    : null;
            Listing refreshedListing = listingRepository.findByIdWithDetails(updated.getListingId()).orElse(updated);

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(refreshedListing, refreshed),
                    "Cập nhật bài đăng thành công — cần Staff duyệt lại"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] updateListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Re-parent MediaAsset — gắn ảnh draft (đang tạm ở ACCOUNT) vào
    // đúng LISTING vừa tạo/sửa. Đây là điểm thay đổi cốt lõi: trước đây ảnh
    // được re-parent vào Property (EntityType.PROPERTY), giờ re-parent thẳng
    // vào Listing (EntityType.LISTING) và lưu thành bản ghi ListingImage.
    // ════════════════════════════════════════════════════════════════════════
    private int reparentUploadedImagesToListing(List<String> publicIds,
                                                Account owner,
                                                Listing listing,
                                                Integer thumbnailImageIndex) {
        if (publicIds == null || publicIds.isEmpty()) return 0;

        long existingCount = listingImageRepository.countByListing_ListingId(listing.getListingId());

        // Seller CHỦ ĐỘNG chỉ định ảnh nào làm thumbnail (thumbnailImageIndex != null)
        // → tôn trọng lựa chọn này DÙ listing đã có ảnh từ trước hay chưa (khác
        // hành vi cũ: trước đây chỉ tự động gán thumbnail khi listing CHƯA có ảnh
        // nào, khiến việc thêm ảnh mới vào tin đã có sẵn ảnh KHÔNG THỂ đổi được
        // thumbnail — đây chính là gap đã sửa). Nếu Seller không truyền gì
        // (thumbnailImageIndex == null), giữ hành vi mặc định an toàn cũ: chỉ tự
        // gán ảnh đầu tiên làm thumbnail khi listing đang chưa có ảnh nào.
        boolean callerSpecifiedThumbnail = thumbnailImageIndex != null;
        int targetIdx = callerSpecifiedThumbnail ? thumbnailImageIndex : 0;

        if (callerSpecifiedThumbnail) {
            // Đảm bảo luôn CHỈ 1 thumbnail: bỏ đánh dấu thumbnail của mọi ảnh cũ
            // trước khi gán ảnh mới làm thumbnail.
            listingImageRepository.clearThumbnailByListingId(listing.getListingId());
        }

        int saved = 0;
        Long listingIdL = listing.getListingId().longValue();

        for (int i = 0; i < publicIds.size(); i++) {
            String publicId = publicIds.get(i);

            int claimed = mediaAssetRepository.claimDraftAsset(
                    publicId, owner.getAccountId(), (long) owner.getAccountId(),
                    EntityType.LISTING, listingIdL);

            if (claimed == 0) {
                log.warn("[ListingService] Skip publicId={}", publicId);
                continue;
            }

            MediaAsset asset = mediaAssetRepository.findByPublicId(publicId).orElse(null);
            if (asset == null) continue;

            boolean isThumbnail = callerSpecifiedThumbnail
                    ? (i == targetIdx)
                    : (existingCount == 0 && i == targetIdx);

            ListingImage img = ListingImage.builder()
                    .listing(listing)
                    .imageUrl(asset.getSecureUrl())
                    .isThumbnail(isThumbnail)
                    .displayOrder((int) existingCount + saved)
                    .build();

            listingImageRepository.save(img);
            saved++;
        }
        return saved;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Khi Seller đăng lại 1 Property đã có (Luồng ①) mà KHÔNG upload ảnh
    // mới, tự động COPY bộ ảnh của lần đăng gần nhất (Listing khác) cùng Property
    // sang Listing mới — giữ đúng trải nghiệm cũ (không bắt Seller upload lại ảnh)
    // dù giờ ảnh đã thuộc về Listing thay vì Property.
    // ════════════════════════════════════════════════════════════════════════
    private int copyImagesFromOtherListingsOfProperty(Property property, Listing newListing) {
        List<Listing> others = listingRepository.findOtherListingsOfPropertyWithImages(
                property.getPropertyId(), newListing.getListingId());

        for (Listing other : others) {
            List<ListingImage> sourceImages = other.getListingImages();
            if (sourceImages == null || sourceImages.isEmpty()) continue;

            List<ListingImage> sorted = sourceImages.stream()
                    .sorted(Comparator.comparing(
                            ListingImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            int order = 0;
            for (ListingImage src : sorted) {
                ListingImage copy = ListingImage.builder()
                        .listing(newListing)
                        .imageUrl(src.getImageUrl())
                        .isThumbnail(src.getIsThumbnail())
                        .displayOrder(order++)
                        .build();
                listingImageRepository.save(copy);
            }
            return sorted.size();
        }
        return 0;
    }
    // ════════════════════════════════════════════════════════════════════════
    // GET /seller/listings/{id} — Seller xem chi tiết 1 listing của mình
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyListingDetail(Integer listingId) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId())
                    .orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(listing, listing.getProperty()),
                    "Chi tiết tin đăng của bạn"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] getMyListingDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // DELETE /seller/listings/{id} — Xoá mềm VĨNH VIỄN (status = DELETED)
    // ════════════════════════════════════════════════════════════════════════
    // TRƯỚC ĐÂY: chỉ set isActive=false — TRÙNG với hành vi tạm ẩn, không có cờ
    // nào phân biệt "đã xoá" khỏi "đang tạm ẩn". Hậu quả: (1) GET
    // /seller/listings & /seller/listings/{id} không lọc, Seller vẫn thấy được
    // tin đã xoá; (2) Seller có thể mở lại 1 tin đã "xoá" vì hệ thống chỉ
    // thấy isActive=false + verification=APPROVED, y hệt điều kiện mở lại hợp lệ.
    // NAY: dùng SellerListingStatusEnum.DELETED riêng biệt, và mọi query
    // findBySellerId/findByIdAndSellerId đều LOẠI TRỪ DELETED — vừa sửa triệt để
    // lỗi "xoá mềm rồi vẫn get ra được", vừa chặn mở lại vĩnh viễn.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> softDeleteListing(Integer listingId) {
        return changeSellerListingStatus(listingId, SellerListingStatusEnum.DELETED);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATCH /seller/listings/{id} — Seller tự đổi trạng thái hiển thị bài đăng
    // của mình bằng cách gửi lên "status" ĐÍCH muốn chuyển tới: HIDDEN (ẩn),
    // DELETED (xoá vĩnh viễn), ACTIVE (mở lại từ HIDDEN). Dùng CHUNG 1 enum
    // SellerListingStatusEnum cho cả trạng thái lưu trữ lẫn giá trị Seller gửi
    // lên — không còn enum "action" riêng (xem SellerListingStatusEnum).
    // KHÔNG liên quan tới quyết định duyệt của Staff (APPROVED/REJECTED) — đó
    // vẫn là verification status, quản lý riêng ở ListingVerificationService.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListingStatus(Integer listingId, UpdateListingStatusRequest request) {
        if (request.getStatus() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Bad_Request", "status không được để trống (ACTIVE, HIDDEN hoặc DELETED)"));
        }
        return changeSellerListingStatus(listingId, request.getStatus());
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper DÙNG CHUNG cho cả softDeleteListing (DELETE HTTP verb, tương thích
    // ngược với API cũ) và updateListingStatus (PATCH, status ACTIVE/HIDDEN/DELETED)
    // — đảm bảo CHỈ 1 nơi duy nhất chứa logic chuyển trạng thái, tránh lệch hành
    // vi giữa 2 endpoint như trước đây (soft-delete và tạm ẩn dùng 2 code path
    // khác nhau nhưng cùng chỉnh isActive, sinh ra lỗ hổng).
    // ════════════════════════════════════════════════════════════════════════
    private ResponseEntity<ApiResponse> changeSellerListingStatus(Integer listingId, SellerListingStatusEnum targetStatus) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            // findByIdAndSellerId đã LOẠI TRỪ status=DELETED — nếu tin đã bị xoá
            // trước đó, ở đây trả về null → 404, đúng nghĩa "không thể sửa/dùng lại".
            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId())
                    .orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            SellerListingStatusEnum currentStatus = listing.getStatus() != null
                    ? listing.getStatus() : SellerListingStatusEnum.ACTIVE;

            String msg;
            switch (targetStatus) {
                case ACTIVE -> {
                    if (currentStatus != SellerListingStatusEnum.HIDDEN) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.fail("Conflict",
                                        "Chỉ mở lại được bài đăng đang ở trạng thái tạm ẩn (HIDDEN). "
                                                + "Trạng thái hiện tại: " + currentStatus));
                    }
                    ListingVerification verification = listingVerificationRepository
                            .findByListing_ListingId(listingId).orElse(null);
                    ListingStatusEnum verificationStatus = verification != null ? verification.getStatus() : null;
                    if (verificationStatus != ListingStatusEnum.APPROVED) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiResponse.fail("Conflict",
                                        "Chỉ mở lại được bài đăng đã được Staff duyệt (APPROVED). "
                                                + "Trạng thái duyệt hiện tại: "
                                                + (verificationStatus == null ? "PENDING" : verificationStatus)));
                    }
                    listing.setStatus(SellerListingStatusEnum.ACTIVE);
                    listing.setIsActive(true);
                    listing.setDeletedAt(null);
                    msg = "Bật lại bài đăng thành công";
                }
                case HIDDEN -> {
                    if (currentStatus == SellerListingStatusEnum.HIDDEN) {
                        return ResponseEntity.ok(ApiResponse.success(
                                listingMapper.toListingDetail(listing, listing.getProperty()),
                                "Bài đăng đã tạm ẩn từ trước, không có gì thay đổi"));
                    }
                    listing.setStatus(SellerListingStatusEnum.HIDDEN);
                    listing.setIsActive(false);
                    msg = "Tạm ẩn bài đăng thành công";
                }
                case DELETED -> {
                    // currentStatus không bao giờ là DELETED tới đây (đã bị query lọc ra),
                    // nên đây luôn là lần xoá đầu tiên — không cần nhánh idempotent.
                    listing.setStatus(SellerListingStatusEnum.DELETED);
                    listing.setIsActive(false);
                    listing.setDeletedAt(LocalDateTime.now());
                    msg = "Xoá bài đăng thành công (id=" + listingId + ")";
                }
                default -> throw new IllegalStateException("Trạng thái không hợp lệ: " + targetStatus);
            }

            listing.setUpdatedAt(LocalDateTime.now());
            Listing updated = listingRepository.save(listing);

            log.info("[ListingService] changeSellerListingStatus: listingId={} targetStatus={} bởi sellerId={} -> status={}",
                    listingId, targetStatus, seller.getSellerId(), updated.getStatus());

            // Sau khi DELETE, listing đã bị loại khỏi mọi query "của Seller" nên không
            // trả full detail nữa (tránh gây hiểu lầm là vẫn còn dùng được) — trả null.
            Object payload = targetStatus == SellerListingStatusEnum.DELETED
                    ? null
                    : listingMapper.toListingDetail(updated, updated.getProperty());

            return ResponseEntity.ok(ApiResponse.success(payload, msg));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] changeSellerListingStatus lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST /listings/generate-content — Seller: AI sinh tiêu đề + mô tả bài đăng
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> generateListingContent(GenerateListingContentRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            getCurrentSeller(currentUser);

            PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId()).orElse(null);
            if (propertyType == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
            }

            String wardName = null;
            if (request.getWardCode() != null && !request.getWardCode().isBlank()) {
                Ward ward = wardRepository.findById(request.getWardCode()).orElse(null);
                wardName = ward != null ? ward.getFullName() : null;
            }

            String tone = (request.getTone() == null || request.getTone().isBlank()) ? "professional" : request.getTone();
            String highlights = (request.getHighlights() == null || request.getHighlights().isEmpty())
                    ? "Không có" : String.join(", ", request.getHighlights());

            String prompt = String.format(
                    "Bạn là chuyên gia viết bài đăng bất động sản tại Việt Nam. Dựa trên thông số sau, " +
                            "hãy viết 1 tiêu đề (dưới 100 ký tự) và 1 đoạn mô tả (150-300 từ) cho bài đăng rao bán/cho thuê, " +
                            "văn phong \"%s\", thu hút người mua nhưng KHÔNG bịa thêm thông tin không có trong dữ liệu:\n" +
                            "- Loại bất động sản: %s\n" +
                            "- Vị trí (phường/xã): %s\n" +
                            "- Địa chỉ cụ thể: %s\n" +
                            "- Dự án: %s\n" +
                            "- Diện tích: %s m2\n" +
                            "- Số phòng ngủ: %s\n" +
                            "- Số phòng tắm: %s\n" +
                            "- Tầng: %s\n" +
                            "- Hướng: %s\n" +
                            "- Tình trạng pháp lý: %s\n" +
                            "- Giá: %s VND\n" +
                            "- Điểm nhấn cần nhấn mạnh: %s\n" +
                            "YÊU CẦU BẮT BUỘC: Toàn bộ nội dung phải viết hoàn toàn bằng TIẾNG VIỆT.",
                    tone,
                    propertyType.getName(),
                    wardName != null ? wardName : "Không rõ",
                    request.getAddressParticular() != null ? request.getAddressParticular() : "Không rõ",
                    request.getProjectName() != null ? request.getProjectName() : "Không có",
                    request.getArea(),
                    request.getBedroom() != null ? request.getBedroom() : "Không rõ",
                    request.getBathroom() != null ? request.getBathroom() : "Không rõ",
                    request.getFloor() != null ? request.getFloor() : "Không rõ",
                    request.getDirection() != null ? request.getDirection() : "Không rõ",
                    request.getLegalStatus() != null ? request.getLegalStatus() : "Không rõ",
                    request.getPrice() != null ? request.getPrice() : "Không rõ",
                    highlights
            );

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(Schema.builder()
                            .type("OBJECT")
                            .properties(Map.of(
                                    "title", Schema.builder().type("STRING").build(),
                                    "description", Schema.builder().type("STRING").build()
                            ))
                            .required(List.of("title", "description"))
                            .build())
                    .build();

            GenerateContentResponse response = callGeminiWithRetry(prompt, config);

            Map<String, Object> parsed = objectMapper.readValue(
                    response.text().trim(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            // Mỗi câu (kết thúc bằng dấu chấm) xuống dòng riêng — dễ đọc hơn khi
            // Seller xem lại trước khi đăng, thay vì 1 đoạn văn liền không ngắt dòng.
            String rawDescription = String.valueOf(parsed.get("description"));
            String formattedDescription = formatSentencesOnNewLines(rawDescription);

            GenerateListingContentResponse result = GenerateListingContentResponse.builder()
                    .title(String.valueOf(parsed.get("title")))
                    .description(formattedDescription)
                    .build();

            log.info("[ListingService] generateListingContent: propertyTypeId={}, sellerAccountId={}",
                    request.getPropertyTypeId(), currentUser.getAccountId());

            return ResponseEntity.ok(ApiResponse.success(result, "Sinh nội dung bài đăng thành công"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] generateListingContent lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: mỗi câu (kết thúc bằng dấu chấm ".") xuống dòng riêng — dùng để
    // format lại mô tả bài đăng do AI (Gemini) sinh ra ở generateListingContent(),
    // giúp Seller đọc/chỉnh sửa dễ hơn thay vì 1 khối văn bản liền không ngắt.
    //
    // Regex CHỈ tách khi dấu chấm theo sau bởi khoảng trắng — KHÔNG tách số
    // thập phân (vd "75.5", "1.000.000") vì các số đó không có khoảng trắng
    // ngay sau dấu chấm. Nhiều khoảng trắng liên tiếp sau dấu chấm được gộp
    // lại thành đúng 1 lần xuống dòng (không tạo dòng trống thừa).
    // ════════════════════════════════════════════════════════════════════════
    private String formatSentencesOnNewLines(String text) {
        if (text == null || text.isBlank()) return text;
        return text.trim().replaceAll("\\.\\s+", ".\n");
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse> suggestListingPrice(PriceSuggestionRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            getCurrentSeller(currentUser);

            PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId()).orElse(null);
            if (propertyType == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
            }
            Ward ward = wardRepository.findById(request.getWardCode()).orElse(null);
            if (ward == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + request.getWardCode()));
            }

            List<Listing> comparables = listingRepository
                    .findComparableActiveListings(request.getPropertyTypeId(), request.getWardCode());

            List<Double> pricesPerSqm = new ArrayList<>();
            for (Listing l : comparables) {
                Property p = l.getProperty();
                if (p != null && p.getArea() != null && p.getArea() > 0 && l.getPrice() != null) {
                    pricesPerSqm.add(l.getPrice() / p.getArea());
                }
            }

            boolean hasMarketData = !pricesPerSqm.isEmpty();
            Double avgPricePerSqm = hasMarketData
                    ? pricesPerSqm.stream().mapToDouble(Double::doubleValue).average().orElse(0)
                    : null;
            Double minPricePerSqm = hasMarketData ? Collections.min(pricesPerSqm) : null;
            Double maxPricePerSqm = hasMarketData ? Collections.max(pricesPerSqm) : null;

            String marketSummary = hasMarketData
                    ? String.format(
                    "Dựa trên %d tin đăng tương đồng đang hoạt động (cùng loại BĐS, cùng phường/xã): " +
                            "đơn giá trung bình %.0f VND/m2, thấp nhất %.0f VND/m2, cao nhất %.0f VND/m2.",
                    pricesPerSqm.size(), avgPricePerSqm, minPricePerSqm, maxPricePerSqm)
                    : "Không có tin đăng tương đồng nào trên hệ thống để đối chiếu, hãy ước lượng dựa trên " +
                    "kiến thức chung về thị trường bất động sản khu vực này.";

            String prompt = String.format(
                    "Bạn là chuyên gia định giá bất động sản tại Việt Nam. " +
                            "Hãy đề xuất khoảng giá bán hợp lý (VND) cho tài sản sau:\n" +
                            "- Loại bất động sản: %s\n" +
                            "- Phường/xã: %s\n" +
                            "- Diện tích: %s m2\n" +
                            "- Số phòng ngủ: %s\n" +
                            "- Số phòng tắm: %s\n" +
                            "- Tầng: %s\n" +
                            "- Hướng: %s\n" +
                            "- Tình trạng pháp lý: %s\n" +
                            "- Dự án: %s\n\n" +
                            "Dữ liệu thị trường tham chiếu: %s\n\n" +
                            "Trả về suggestedPrice (giá đề xuất, VND), minPrice, maxPrice (khoảng giá hợp lý, VND), " +
                            "pricePerSqm (đơn giá VND/m2 tương ứng suggestedPrice), và reasoning (giải thích ngắn gọn " +
                            "bằng TIẾNG VIỆT lý do đề xuất mức giá này, tối đa 4 câu).",
                    propertyType.getName(),
                    ward.getFullName(),
                    request.getArea(),
                    request.getBedroom() != null ? request.getBedroom() : "Không rõ",
                    request.getBathroom() != null ? request.getBathroom() : "Không rõ",
                    request.getFloor() != null ? request.getFloor() : "Không rõ",
                    request.getDirection() != null ? request.getDirection() : "Không rõ",
                    request.getLegalStatus() != null ? request.getLegalStatus() : "Không rõ",
                    request.getProjectName() != null ? request.getProjectName() : "Không có",
                    marketSummary
            );

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(Schema.builder()
                            .type("OBJECT")
                            .properties(Map.of(
                                    "suggestedPrice", Schema.builder().type("INTEGER").build(),
                                    "minPrice", Schema.builder().type("INTEGER").build(),
                                    "maxPrice", Schema.builder().type("INTEGER").build(),
                                    "pricePerSqm", Schema.builder().type("INTEGER").build(),
                                    "reasoning", Schema.builder().type("STRING").build()
                            ))
                            .required(List.of("suggestedPrice", "minPrice", "maxPrice", "pricePerSqm", "reasoning"))
                            .build())
                    .build();

            GenerateContentResponse response = callGeminiWithRetry(prompt, config);

            Map<String, Object> parsed = objectMapper.readValue(
                    response.text().trim(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

            PriceSuggestionResponse result = PriceSuggestionResponse.builder()
                    .suggestedPrice(((Number) parsed.get("suggestedPrice")).longValue())
                    .minPrice(((Number) parsed.get("minPrice")).longValue())
                    .maxPrice(((Number) parsed.get("maxPrice")).longValue())
                    .pricePerSqm(((Number) parsed.get("pricePerSqm")).longValue())
                    .comparableCount(pricesPerSqm.size())
                    .basedOnMarketData(hasMarketData)
                    .reasoning(String.valueOf(parsed.get("reasoning")))
                    .build();

            log.info("[ListingService] suggestListingPrice: propertyTypeId={}, wardCode={}, comparableCount={}",
                    request.getPropertyTypeId(), request.getWardCode(), pricesPerSqm.size());

            return ResponseEntity.ok(ApiResponse.success(result, "Đề xuất giá bán thành công"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] suggestListingPrice lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // POST /listings/search — Tìm kiếm nâng cao tin đăng công khai
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> searchListings(ListingSearchRequest request) {
        try {
            Sort userSort = switch (request.getSortBy() == null ? ListingSortEnum.NEWEST : request.getSortBy()) {
                case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
                case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
                case AREA_ASC -> Sort.by(Sort.Direction.ASC, "property.area");
                case AREA_DESC -> Sort.by(Sort.Direction.DESC, "property.area");
                case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
                case MOST_VIEWED -> Sort.by(Sort.Direction.DESC, "viewCount");
                case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            };
            // MỚI: "priority" (mức ưu tiên gói dịch vụ, 1-4) LUÔN xếp TRƯỚC tiêu
            // chí sort investor chọn — tin đã mua gói ưu tiên cao hơn nổi lên đầu
            // trước, trong CÙNG mức priority mới xếp tiếp theo sortBy như cũ.
            Sort sort = Sort.by(Sort.Direction.DESC, "priority").and(userSort);

            // SỬA: dùng resolvePageable() dùng chung — page=0 & size=0 tường
            // minh (request.getPage()/getSize() cùng == 0, KHÔNG phải null)
            // => lấy hết, không phân trang, bỏ qua MAX_SEARCH_PAGE_SIZE.
            Pageable pageable = resolvePageable(request.getPage(), request.getSize(), sort);

            // Pattern 2-query giống getMarketListings (xem TwoStepPaginationUtil): query 1
            // dùng findAll(Specification, Pageable) MẶC ĐỊNH của JpaSpecificationExecutor
            // (không JOIN FETCH collection) để phân trang + sort CHUẨN ở tầng DB, query 2
            // fetch chi tiết theo đúng danh sách listingId đã phân trang đó.
            Specification<Listing> spec = ListingSpecification.fromRequest(request);
            Page<Listing> listingPage = TwoStepPaginationUtil.<Integer, Listing>paginate(
                    pageable,
                    p -> listingRepository.findAll(spec, p).map(Listing::getListingId),
                    listingRepository::findAllByListingIdInWithDetails,
                    Listing::getListingId
            );

            Account currentUser = authenUntil.getCurrentUSer();
            Set<Integer> favoritedIds = Collections.emptySet();
            if (currentUser != null) {
                Investor investor = investorRepository
                        .findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                if (investor != null) {
                    favoritedIds = new HashSet<>(
                            favoriteListingRepository.findFavoritedListingIdsByInvestorId(investor.getInvestorId()));
                }
            }

            // Ghi lịch sử tìm kiếm — CHỈ khi đã đăng nhập VÀ có từ khoá thật sự (không
            // ghi khi chỉ lọc theo giá/diện tích... mà không gõ chữ gì) — dùng cho mục
            // "Recent Search" ở GET /listings/search/suggestions.
            if (currentUser != null && request.getKeyword() != null && !request.getKeyword().isBlank()) {
                recordSearchHistory(currentUser, request.getKeyword().trim());
            }

            final Set<Integer> favIds = favoritedIds;
            List<ListingSummaryResponse> content = listingPage.getContent().stream()
                    .map(l -> listingMapper.toListingSummary(l, favIds.contains(l.getListingId())))
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", listingPage.getNumber());
            result.put("size", listingPage.getSize());
            result.put("totalElements", listingPage.getTotalElements());
            result.put("totalPages", listingPage.getTotalPages());
            result.put("last", listingPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Kết quả tìm kiếm tin đăng"));

        } catch (Exception e) {
            log.error("[ListingService] searchListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


    @Override
    public ResponseEntity<ApiResponse> getFeaturedListings(int page, int size) {
        try {
            // SỬA: page=0 & size=0 tường minh => lấy hết, không phân trang.
            // Sort ĐÃ nằm sẵn trong ORDER BY của @Query findFeaturedListingIds
            // (không cần truyền Sort riêng ở đây, khác searchListings/getMarketListings).
            Pageable pageable = (page == 0 && size == 0)
                    ? Pageable.unpaged()
                    : PageRequest.of(Math.max(page, 0), size > 0 ? Math.min(size, MAX_SEARCH_PAGE_SIZE) : PAGE_SIZE);

            Page<FeaturedListingProjection> topViewed = activeLogRepository
                    .findFeaturedListingIds(UserEventTypeEnum.VIEW, pageable);

            List<Integer> orderedIds = topViewed.getContent().stream()
                    .map(FeaturedListingProjection::getListingId)
                    .toList();

            Map<Integer, Long> viewCountByListingId = topViewed.getContent().stream()
                    .collect(Collectors.toMap(FeaturedListingProjection::getListingId, FeaturedListingProjection::getViewCount));

            Map<Integer, Listing> listingById = listingRepository.findAllByListingIdInWithDetails(orderedIds)
                    .stream().collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

            Account currentUser = authenUntil.getCurrentUSer();
            Set<Integer> favoritedIds = Collections.emptySet();
            if (currentUser != null) {
                Investor investor = investorRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                if (investor != null) {
                    favoritedIds = new HashSet<>(
                            favoriteListingRepository.findFavoritedListingIdsByInvestorId(investor.getInvestorId()));
                }
            }
            final Set<Integer> favIds = favoritedIds;

            // Giữ ĐÚNG thứ tự xếp hạng theo view count đã sort ở query (Map không
            // đảm bảo thứ tự, phải duyệt lại theo orderedIds).
            List<Map<String, Object>> content = orderedIds.stream()
                    .map(listingById::get)
                    .filter(java.util.Objects::nonNull)
                    .map(l -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listing", listingMapper.toListingSummary(l, favIds.contains(l.getListingId())));
                        item.put("viewCount", viewCountByListingId.get(l.getListingId()));
                        return item;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            result.put("page", topViewed.getNumber());
            result.put("size", topViewed.getSize());
            result.put("totalElements", topViewed.getTotalElements());
            result.put("totalPages", topViewed.getTotalPages());
            result.put("last", topViewed.isLast());

            return ResponseEntity.ok(ApiResponse.success(result, "Tin đăng nổi bật (nhiều lượt xem nhất)"));
        } catch (Exception e) {
            log.error("[ListingService] getFeaturedListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MỚI: GET /listings/compare — So sánh 2-4 tin đăng. Trả ĐẦY ĐỦ chi tiết
    // từng tin (dùng lại đúng listingMapper.toListingDetail như GET
    // /listings/{id}) để FE tự dựng bảng so sánh, KHÔNG rút gọn field ở BE.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public ResponseEntity<ApiResponse> compareListings(List<Integer> listingIds) {
        try {
            if (listingIds == null || listingIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Vui lòng gửi danh sách listingId cần so sánh"));
            }

            // Loại trùng nhưng GIỮ NGUYÊN thứ tự đầu tiên xuất hiện — Investor gửi
            // trùng 1 id 2 lần thì chỉ so sánh 1 lần, không lỗi cứng vì lý do nhỏ này.
            List<Integer> distinctIds = listingIds.stream().distinct().toList();

            if (distinctIds.size() < 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Cần tối thiểu 2 tin đăng khác nhau để so sánh"));
            }
            // Giới hạn tối đa 4 — bảng so sánh quá nhiều cột sẽ khó đọc trên FE;
            // tăng lên nếu sau này có nhu cầu thật (không phải giới hạn kỹ thuật).
            if (distinctIds.size() > 4) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Chỉ so sánh được tối đa 4 tin đăng cùng lúc"));
            }

            Map<Integer, Listing> listingById = listingRepository.findAllByListingIdInWithDetails(distinctIds)
                    .stream().collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

            List<String> notFoundIds = distinctIds.stream()
                    .filter(id -> !listingById.containsKey(id))
                    .map(String::valueOf)
                    .toList();

            // Giữ ĐÚNG thứ tự Investor gửi lên (Map không đảm bảo thứ tự) — quan
            // trọng để FE dựng cột bảng so sánh đúng vị trí đã chọn.
            List<Object> content = distinctIds.stream()
                    .map(listingById::get)
                    .filter(java.util.Objects::nonNull)
                    .map(l -> listingMapper.toListingDetail(l, l.getProperty()))
                    .collect(Collectors.toList());

            if (content.size() < 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request",
                        "Không đủ tin đăng hợp lệ để so sánh (không tìm thấy: " + String.join(", ", notFoundIds) + ")"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("listings", content);
            if (!notFoundIds.isEmpty()) {
                // Vẫn trả kết quả so sánh được với các tin TÌM THẤY, chỉ cảnh báo
                // riêng tin nào không tồn tại — không chặn cứng cả request vì 1 id sai.
                result.put("notFoundListingIds", notFoundIds);
            }

            String msg = notFoundIds.isEmpty()
                    ? "So sánh " + content.size() + " tin đăng"
                    : "So sánh " + content.size() + " tin đăng (bỏ qua " + notFoundIds.size() + " id không tồn tại)";

            return ResponseEntity.ok(ApiResponse.success(result, msg));
        } catch (Exception e) {
            log.error("[ListingService] compareListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SỬA (lỗi biên dịch thiếu hàm): searchListings() ở trên gọi
    // recordSearchHistory(currentUser, keyword) nhưng hàm này CHƯA từng được
    // định nghĩa — bổ sung tại đây. UPSERT theo (account, keyword không phân
    // biệt hoa/thường): nếu đã tìm keyword y hệt trước đó thì chỉ cập nhật
    // updatedAt (đẩy lên đầu danh sách "gần đây"), không tạo dòng trùng. Đây
    // cũng là nguồn dữ liệu DUY NHẤT cho nhóm "Recent Search" ở
    // GET /listings/search/suggestions (xem buildRecentSearchSuggestions).
    // Lỗi ở hàm này KHÔNG được làm hỏng luồng tìm kiếm chính — chỉ log lại.
    // ════════════════════════════════════════════════════════════════════════
    private void recordSearchHistory(Account account, String keyword) {
        try {
            String trimmed = keyword.trim();
            LocalDateTime now = LocalDateTime.now();

            // MỚI: ghi nhận SEARCH event cho Recommendation System — song song với
            // việc lưu lịch sử tìm kiếm (2 mục đích khác nhau: SearchHistory phục
            // vụ autocomplete "Recent Search", còn ActiveLog SEARCH phục vụ dựng
            // interaction matrix cho LightFM).
            userEventTrackingService.recordSilently(account, UserEventTypeEnum.SEARCH, null);

            SearchHistory existing = searchHistoryRepository
                    .findByAccount_AccountIdAndKeywordIgnoreCase(account.getAccountId(), trimmed)
                    .orElse(null);

            if (existing != null) {
                existing.setUpdatedAt(now);
                searchHistoryRepository.save(existing);
                return;
            }

            SearchHistory history = SearchHistory.builder()
                    .account(account)
                    .keyword(trimmed)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            searchHistoryRepository.save(history);

            // Giới hạn tối đa SEARCH_HISTORY_CAP dòng/tài khoản — dọn bớt bản ghi
            // cũ nhất nếu vượt ngưỡng, tránh bảng phình vô hạn theo thời gian.
            long total = searchHistoryRepository.countByAccount_AccountId(account.getAccountId());
            if (total > SEARCH_HISTORY_CAP) {
                searchHistoryRepository
                        .findTop5ByAccount_AccountIdOrderByUpdatedAtAsc(account.getAccountId())
                        .stream()
                        .limit(total - SEARCH_HISTORY_CAP)
                        .forEach(searchHistoryRepository::delete);
            }
        } catch (Exception e) {
            log.warn("[ListingService] recordSearchHistory lỗi, bỏ qua: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MỚI: GET /listings/search/suggestions — Autocomplete Suggestion.
    // q rỗng/null: chỉ trả Recent Search (nếu đã đăng nhập), 3 nhóm còn lại rỗng
    // (không lọc dữ liệu Location/Listing/PropertyType theo chuỗi rỗng để tránh
    // trả về ngẫu nhiên hàng nghìn dòng không có ý nghĩa gợi ý).
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getSearchSuggestions(String q) {
        try {
            String keyword = q == null ? "" : q.trim();

            List<SearchSuggestionItem> locations = Collections.emptyList();
            List<SearchSuggestionItem> listings = Collections.emptyList();
            List<SearchSuggestionItem> propertyTypes = Collections.emptyList();

            if (StringUtils.hasText(keyword)) {
                locations = buildLocationSuggestions(keyword);
                listings = buildListingSuggestions(keyword);
                propertyTypes = buildPropertyTypeSuggestions(keyword);
            }

            Account currentUser = authenUntil.getCurrentUSer();
            List<SearchSuggestionItem> recentSearches = currentUser != null
                    ? buildRecentSearchSuggestions(currentUser, keyword)
                    : Collections.emptyList();

            SearchSuggestionResponse result = SearchSuggestionResponse.builder()
                    .locations(locations)
                    .listings(listings)
                    .propertyTypes(propertyTypes)
                    .recentSearches(recentSearches)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(result, "Gợi ý tìm kiếm"));
        } catch (Exception e) {
            log.error("[ListingService] getSearchSuggestions lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // Nhóm LOCATION — khớp tên Phường/Xã trước (cụ thể hơn), rồi tới Tỉnh/Thành,
    // gộp chung rồi cắt về đúng SUGGESTION_LIMIT.
    private List<SearchSuggestionItem> buildLocationSuggestions(String keyword) {
        List<SearchSuggestionItem> result = new ArrayList<>();

        wardRepository
                .findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword, keyword)
                .forEach(w -> result.add(SearchSuggestionItem.builder()
                        .type("LOCATION")
                        .label(w.getProvince() != null
                                ? w.getFullName() + ", " + w.getProvince().getName()
                                : w.getFullName())
                        .value(w.getFullName())
                        .code(w.getWard_code())
                        .build()));

        provinceRepository
                .findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(keyword, keyword)
                .forEach(p -> result.add(SearchSuggestionItem.builder()
                        .type("LOCATION")
                        .label(p.getFullName())
                        .value(p.getFullName())
                        .code(p.getProvince_code())
                        .build()));

        return result.stream().limit(SUGGESTION_LIMIT).toList();
    }

    // Nhóm LISTING — khớp tiêu đề tin đăng hoặc tên dự án (property.projectName).
    // Label ưu tiên tên dự án (VD "Vinhome Grand Park") vì đó là cách người dùng
    // thường gõ tìm — fallback về tiêu đề tin nếu property không có projectName.
    private List<SearchSuggestionItem> buildListingSuggestions(String keyword) {
        Pageable top = PageRequest.of(0, SUGGESTION_LIMIT);
        return listingRepository.searchSuggestionsByTitleOrProjectName(keyword, top).stream()
                .map(l -> {
                    String label = (l.getProperty() != null && StringUtils.hasText(l.getProperty().getProjectName()))
                            ? l.getProperty().getProjectName()
                            : l.getTitle();
                    return SearchSuggestionItem.builder()
                            .type("LISTING")
                            .label(label)
                            .value(label)
                            .code(String.valueOf(l.getListingId()))
                            .build();
                })
                .toList();
    }

    // Nhóm PROPERTY_TYPE — khớp tên loại BĐS đang active.
    private List<SearchSuggestionItem> buildPropertyTypeSuggestions(String keyword) {
        Pageable top = PageRequest.of(0, SUGGESTION_LIMIT);
        return propertyTypeRepository.findTop5ByIsActiveTrueAndNameContainingIgnoreCase(keyword).stream()
                .map(pt -> SearchSuggestionItem.builder()
                        .type("PROPERTY_TYPE")
                        .label(pt.getName())
                        .value(pt.getName())
                        .code(String.valueOf(pt.getPropertyTypeId()))
                        .build())
                .toList();
    }

    // Nhóm RECENT_SEARCH — lịch sử tìm kiếm CỦA CHÍNH tài khoản đang đăng nhập.
    // keyword rỗng -> trả về gần đây nhất không lọc; có keyword -> lọc theo chuỗi đang gõ.
    private List<SearchSuggestionItem> buildRecentSearchSuggestions(Account account, String keyword) {
        List<SearchHistory> histories = StringUtils.hasText(keyword)
                ? searchHistoryRepository.findTop5ByAccount_AccountIdAndKeywordContainingIgnoreCaseOrderByUpdatedAtDesc(
                account.getAccountId(), keyword)
                : searchHistoryRepository.findTop5ByAccount_AccountIdOrderByUpdatedAtDesc(account.getAccountId());

        return histories.stream()
                .map(h -> SearchSuggestionItem.builder()
                        .type("RECENT_SEARCH")
                        .label(h.getKeyword())
                        .value(h.getKeyword())
                        .code(null)
                        .build())
                .toList();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Gọi Gemini kèm retry cho lỗi 429 (quá hạn mức)/503 (server bận),
    // dùng chung cho generateListingContent + suggestListingPrice.
    // Logic đồng nhất với InvestmentPlanServiceImplement để tránh lệch hành vi
    // retry giữa các module cùng gọi Gemini trong hệ thống.
    // ════════════════════════════════════════════════════════════════════════
    private GenerateContentResponse callGeminiWithRetry(String prompt, GenerateContentConfig config) {
        int retryCount = 0;
        GenerateContentResponse response = null;

        while (retryCount < GEMINI_MAX_RETRY) {
            try {
                response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, config);
                break;
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("Unavailable")
                        || errorMsg.contains("Quota exceeded") || errorMsg.contains("rate-limits")) {
                    retryCount++;
                    try {
                        Thread.sleep(GEMINI_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("Gemini API đang bận hoặc quá hạn mức (429/503). Vui lòng thử lại sau vài giây.");
        }
        return response;
    }
}