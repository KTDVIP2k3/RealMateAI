package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.*;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithExistingPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingWithNewPropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateListingContentRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.ListingSearchRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PriceSuggestionRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingStatusRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CloudinaryMediaServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
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
    private final ListingMapper listingMapper;
    private final NotificationService notificationService;
    private final UserEventTrackingService userEventTrackingService;
    private final AuthenUntil authenUntil;
    private final CloudinaryMediaServiceInterface cloudinaryMediaService;
    private final Client geminiClient;
    private final ObjectMapper objectMapper;

    private static final int PAGE_SIZE = 10;
    private static final int GEMINI_MAX_RETRY = 5;
    private static final long GEMINI_RETRY_DELAY_MS = 8000;
    private static final int MAX_SEARCH_PAGE_SIZE = 50;

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
    // Luồng ①: Đăng lại tài sản ĐÃ CÓ SẴN
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createListingWithExistingProperty(
            CreateListingWithExistingPropertyRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            Property property = propertyRepository
                    .findById(request.getExistingPropertyId()).orElse(null);
            if (property == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Tài sản không tồn tại: id=" + request.getExistingPropertyId()));
            }

            if (property.getSeller() == null ||
                    !property.getSeller().getSellerId().equals(seller.getSellerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài sản này không thuộc sở hữu của bạn"));
            }

            LocalDateTime now = LocalDateTime.now();
            Listing listing = Listing.builder()
                    .property(property)
                    .seller(seller)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .contactPerson(request.getContactPerson())
                    .contactPersonName(request.getContactPersonName())
                    .contactPersonPhone(request.getContactPersonPhone())
                    .linkSocialContactPerson(request.getLinkSocialContactPerson())
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

            // Ảnh gắn theo LISTING (không còn theo Property nữa). Nếu Seller upload
            // ảnh mới (draftImagePublicIds) thì dùng ảnh đó cho bài đăng mới; nếu
            // không, tự động COPY lại bộ ảnh của lần đăng gần nhất (nếu có) từ
            // cùng Property để bài đăng mới vẫn có ảnh ngay mà không bắt Seller
            // upload lại từ đầu (giữ đúng trải nghiệm cũ của luồng "đăng lại tài sản").
            int reparented = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty()) {
                reparented = reparentUploadedImagesToListing(
                        request.getDraftImagePublicIds(), currentUser, saved, request.getThumbnailImageIndex());
            } else {
                reparented = copyImagesFromOtherListingsOfProperty(property, saved);
            }

            log.info("[ListingService] Luồng①: listingId={}, propertyId={}, sellerId={}",
                    saved.getListingId(), property.getPropertyId(), seller.getSellerId());

            notificationService.notify(seller.getAccount(),
                    "Bạn vừa đăng tin \"" + saved.getTitle() + "\" thành công, đang chờ Staff duyệt.",
                    NotificationTypeEnum.LISTING);

            Property refreshed = propertyRepository
                    .findByIdWithDetails(property.getPropertyId()).orElse(property);
            Listing refreshedListing = listingRepository.findByIdWithDetails(saved.getListingId()).orElse(saved);

            String msg = "Bài đăng tạo thành công"
                    + (reparented > 0 ? ", thêm " + reparented + " ảnh" : "")
                    + ", đang chờ Staff duyệt";

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(listingMapper.toListingDetail(refreshedListing, refreshed), msg));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] createListingWithExistingProperty lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Luồng ②: Tạo tài sản MỚI + đăng tin
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createListingWithNewProperty(
            CreateListingWithNewPropertyRequest request,
            List<MultipartFile> images) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            boolean hasImages = images != null && !images.isEmpty();
            if (!hasImages) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tạo tài sản mới phải kèm ít nhất 1 ảnh"));
            }

            PropertyType propertyType = propertyTypeRepository
                    .findById(request.getPropPropertyTypeId()).orElse(null);
            if (propertyType == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
            }

            PropertyCondition propertyCondition = null;
            Integer conditionId = request.getPropPropertyConditionId();
            if (conditionId != null) {
                if (conditionId <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "propPropertyConditionId phải lớn hơn 0"));
                }
                propertyCondition = propertyConditionRepository
                        .findById(conditionId).orElse(null);
                if (propertyCondition == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Tình trạng bất động sản không tồn tại với id = " + conditionId));
                }
            }

            Ward ward = wardRepository.findById(request.getPropWardCode()).orElse(null);
            if (ward == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + request.getPropWardCode()));
            }

            // Upload ảnh draft
            List<MediaAssetResponse> uploadedAssets = cloudinaryMediaService.uploadMultiple(
                    images, currentUser, EntityType.ACCOUNT, (long) currentUser.getAccountId());

            log.info("[ListingService] Uploaded {} ảnh draft cho accountId={}",
                    uploadedAssets.size(), currentUser.getAccountId());

            // Tạo Location
            Location location = Location.builder()
                    .latitude(request.getPropLatitude())
                    .longitude(request.getPropLongitude())
                    .postalCode(request.getPropPostalCode())
                    .ward(ward)
                    .build();
            Location savedLocation = locationRepository.save(location);

            // Tạo Property
            LocalDateTime now = LocalDateTime.now();
            Property property = Property.builder()
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
                    // Property MỚI tạo cùng Listing phải ở trạng thái CHỜ DUYỆT
                    // (isActive=false), chỉ bật lên khi Staff APPROVE bài đăng
                    // (xem ListingVerificationServiceImplement#verifyListing).
                    .isActive(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Property savedProperty = propertyRepository.save(property);
            propertyRepository.flush();

            // Tạo Listing TRƯỚC (ảnh nay gắn theo Listing, cần listingId để re-parent)
            Listing listing = Listing.builder()
                    .property(savedProperty)
                    .seller(seller)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .price(request.getPrice())
                    .contactPerson(request.getContactPerson())
                    .contactPersonName(request.getContactPersonName())
                    .contactPersonPhone(request.getContactPersonPhone())
                    .linkSocialContactPerson(request.getLinkSocialContactPerson())
                    .viewingDate(request.getViewingDate())
                    .startTime(request.getStartTime() != null ? java.time.LocalTime.parse(request.getStartTime()) : null)
                    .endTime(request.getEndTime() != null ? java.time.LocalTime.parse(request.getEndTime()) : null)
                    .isActive(false)
                    .status(SellerListingStatusEnum.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Listing saved = listingRepository.save(listing);
            listingRepository.flush();

            // Re-parent ảnh — gắn thẳng vào LISTING vừa tạo (không còn theo Property)
            List<String> publicIds = uploadedAssets.stream()
                    .map(MediaAssetResponse::getPublicId)
                    .collect(Collectors.toList());

            int reparented = reparentUploadedImagesToListing(
                    publicIds, currentUser, saved, request.getThumbnailImageIndex());

            if (reparented == 0) {
                throw new ListingConflictException(HttpStatus.CONFLICT,
                        "Upload ảnh thành công nhưng không thể gắn vào bài đăng. Vui lòng thử lại.");
            }

            createPendingVerification(saved);

            log.info("[ListingService] Luồng②: listingId={}, propertyId={}, sellerId={}, ảnh={}",
                    saved.getListingId(), savedProperty.getPropertyId(), seller.getSellerId(), reparented);

            notificationService.notify(seller.getAccount(),
                    "Bạn vừa tạo tài sản mới + đăng tin \"" + saved.getTitle() + "\" thành công, đang chờ Staff duyệt.",
                    NotificationTypeEnum.LISTING);

            Property refreshed = propertyRepository
                    .findByIdWithDetails(savedProperty.getPropertyId()).orElse(savedProperty);
            Listing refreshedListing = listingRepository.findByIdWithDetails(saved.getListingId()).orElse(saved);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            listingMapper.toListingDetail(refreshedListing, refreshed),
                            "Bài đăng tạo thành công kèm " + reparented + " ảnh, đang chờ Staff duyệt"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] createListingWithNewProperty lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper: Tạo bản ghi ListingVerification ở trạng thái PENDING ngay khi
    // Listing được tạo — bắt buộc để GET /staff/listings/pending nhìn thấy
    // bài đăng mới ngay lập tức (trước đây record này chỉ được tạo khi Staff
    // gọi verify lần đầu → hàng đợi chờ duyệt luôn rỗng với bài đăng mới).
    // ════════════════════════════════════════════════════════════════════════
    private void createPendingVerification(Listing listing) {
        ListingVerification verification = ListingVerification.builder()
                .listing(listing)
                .status(ListingStatusEnum.PENDING)
                .build();
        listingVerificationRepository.save(verification);
        // Gán ngược để object Listing trong bộ nhớ phản ánh đúng ngay lập tức
        // (response trả về sau khi tạo cũng hiển thị verificationStatus=PENDING).
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

    // Các method khác giữ nguyên (public + seller)
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMarketListings(int page, int size) {
        try {
            int effectiveSize = size > 0 ? Math.min(size, MAX_SEARCH_PAGE_SIZE) : PAGE_SIZE;
            Pageable pageable = PageRequest.of(Math.max(page, 0), effectiveSize,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

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

            // Ghi nhận VIEW (audit_log/active_log) — xem UserEventTrackingService,
            // AuditLogController. Chỉ ghi khi xác định được người xem (đã đăng
            // nhập) — khách ẩn danh bị bỏ qua ngay trong recordSilently. Không
            // throw ra ngoài dù lỗi gì (đã REQUIRES_NEW + try/catch nội bộ).
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
            if (request.getContactPersonName() != null) listing.setContactPersonName(request.getContactPersonName());
            if (request.getContactPersonPhone() != null) listing.setContactPersonPhone(request.getContactPersonPhone());
            if (request.getLinkSocialContactPerson() != null) listing.setLinkSocialContactPerson(request.getLinkSocialContactPerson());
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

            GenerateListingContentResponse result = GenerateListingContentResponse.builder()
                    .title(String.valueOf(parsed.get("title")))
                    .description(String.valueOf(parsed.get("description")))
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
    // POST /listings/price-suggestion — Seller: AI đề xuất khoảng giá bán
    // ════════════════════════════════════════════════════════════════════════
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
            int page = (request.getPage() == null || request.getPage() < 0) ? 0 : request.getPage();
            int size = (request.getSize() == null || request.getSize() <= 0) ? PAGE_SIZE : request.getSize();
            size = Math.min(size, MAX_SEARCH_PAGE_SIZE);

            Sort sort = switch (request.getSortBy() == null ? ListingSortEnum.NEWEST : request.getSortBy()) {
                case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
                case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price");
                case AREA_ASC -> Sort.by(Sort.Direction.ASC, "property.area");
                case AREA_DESC -> Sort.by(Sort.Direction.DESC, "property.area");
                case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            };

            Pageable pageable = PageRequest.of(page, size, sort);

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