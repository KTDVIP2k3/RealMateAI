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
    private final ActiveLogRepository activeLogRepository;
    private final ListingMapper listingMapper;
    private final NotificationService notificationService;
    private final UserEventTrackingService userEventTrackingService;
    private final AuthenUntil authenUntil;
    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final PostingPackageOrderServiceInterface postingPackageOrderServiceInterface;
    private final SearchHistoryRepository searchHistoryRepository;

    @Autowired
    @Lazy
    private ListingServiceImplement self;

    private static final int PAGE_SIZE = 10;
    private static final int GEMINI_MAX_RETRY = 5;
    private static final long GEMINI_RETRY_DELAY_MS = 8000;
    private static final int MAX_SEARCH_PAGE_SIZE = 50;
    private static final int SUGGESTION_LIMIT = 5;
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

    /**
     * Gắn postingPackageOrders (đã fetch package + category) vào từng Listing
     * để mapper lấy được postingPackageCategoryName — tránh lazy null.
     */
    private void attachPostingPackageOrders(List<Listing> listings) {
        if (listings == null || listings.isEmpty()) {
            return;
        }
        List<Integer> ids = listings.stream()
                .map(Listing::getListingId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<PostingPackageOrder> orders = listingRepository.findOrdersWithPackageByListingIds(ids);
        Map<Integer, List<PostingPackageOrder>> byListingId = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getListing().getListingId()));
        for (Listing l : listings) {
            l.setPostingPackageOrders(byListingId.getOrDefault(l.getListingId(), new ArrayList<>()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CREATE LISTING
    // ════════════════════════════════════════════════════════════════════════
    @Override
    public ResponseEntity<ApiResponse> createListing(CreateListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

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

            NewListingCreationResult creationResult = self.persistNewListingCore(request, currentUser, seller);
            Property property = creationResult.property;
            Listing saved = creationResult.listing;
            int reparented = creationResult.reparentedImageCount;

            Property refreshed = propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property);
            Listing refreshedListing = listingRepository.findByIdWithDetails(saved.getListingId()).orElse(saved);

            Object listingDetail = listingMapper.toListingDetail(refreshedListing, refreshed);

            if (request.getPostingPackageId() != null) {
                PaymentAttemptResult paymentResult = postingPackageOrderServiceInterface
                        .attemptAutoPaymentForNewListing(
                                saved.getListingId(),
                                request.getPostingPackageId(),
                                request.getDuration(),
                                request.getTotalAmount());

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
                        saved.getListingId(), paymentResult.isSuccess(), paymentResult.getErrorCode(),
                        paymentResult.getPostingPackageOrderId());

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, finalMsg));
            }

            String msg = "Bài đăng tạo thành công"
                    + (reparented > 0 ? ", kèm " + reparented + " ảnh" : "")
                    + ", vui lòng thanh toán gói dịch vụ đăng tin để gửi duyệt";

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

    private static class NewListingCreationResult {
        Property property;
        Listing listing;
        int reparentedImageCount;
    }

    @Transactional
    public NewListingCreationResult persistNewListingCore(CreateListingRequest request, Account currentUser, Seller seller) {
        boolean reuseExisting = Boolean.TRUE.equals(request.getReuseExistingProperty());
        LocalDateTime now = LocalDateTime.now();
        Property property;
        boolean isNewProperty;

        if (reuseExisting) {
            if (request.getExistingPropertyId() == null) {
                throw new ListingConflictException(HttpStatus.BAD_REQUEST,
                        "existingPropertyId không được để trống khi reuseExistingProperty=true");
            }
            property = propertyRepository.findById(request.getExistingPropertyId()).orElse(null);
            if (property == null) {
                throw new ListingConflictException(HttpStatus.NOT_FOUND,
                        "Tài sản không tồn tại: id=" + request.getExistingPropertyId());
            }
            if (property.getSeller() == null
                    || !property.getSeller().getSellerId().equals(seller.getSellerId())) {
                throw new ListingConflictException(HttpStatus.FORBIDDEN, "Tài sản này không thuộc sở hữu của bạn");
            }
            isNewProperty = false;
        } else {
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
                    .isActive(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            property = propertyRepository.save(newProperty);
            propertyRepository.flush();
            isNewProperty = true;
        }

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
                .viewCount(0)
                .priority(0)
                .isActive(false)
                .status(SellerListingStatusEnum.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Listing saved = listingRepository.save(listing);
        listingRepository.flush();
        createPendingVerification(saved);

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

    private void createPendingVerification(Listing listing) {
        ListingVerification verification = ListingVerification.builder()
                .listing(listing)
                .status(ListingStatusEnum.WAITING_PAYMENT)
                .build();
        listingVerificationRepository.save(verification);
        listing.setListingVerification(verification);
    }

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

    private ResponseEntity<ApiResponse> handleAuthException(RuntimeException e) {
        if (e instanceof ListingConflictException lce) {
            return ResponseEntity.status(lce.status).body(ApiResponse.fail(lce.status.toString(), e.getMessage()));
        }
        if (e.getMessage() != null && e.getMessage().contains("Unauthorized")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));
        }
        if (e.getMessage() != null && e.getMessage().contains("Forbidden")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Forbidden", e.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Server_Error", e.getMessage()));
    }

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

    // ════════════════════════════════════════════════════════════════════════
    // GET MARKET / DETAIL / MY LISTINGS
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMarketListings(int page, int size) {
        try {
            Pageable pageable = resolvePageable(page, size,
                    Sort.by(Sort.Direction.DESC, "priority")
                            .and(Sort.by(Sort.Direction.DESC, "createdAt")));

            Page<Listing> listingPage = TwoStepPaginationUtil.<Integer, Listing>paginate(
                    pageable,
                    p -> listingRepository.findByIsActiveTrue(p).map(Listing::getListingId),
                    listingRepository::findAllByListingIdInWithDetails,
                    Listing::getListingId
            );

            attachPostingPackageOrders(listingPage.getContent());

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

            List<Integer> pageListingIds = listingPage.getContent().stream()
                    .map(Listing::getListingId)
                    .toList();
            Map<Integer, Long> realViewCountByListingId = pageListingIds.isEmpty()
                    ? Map.of()
                    : activeLogRepository.countGroupedByListingId(pageListingIds, UserEventTypeEnum.VIEW).stream()
                    .collect(Collectors.toMap(
                            FeaturedListingProjection::getListingId,
                            FeaturedListingProjection::getViewCount));

            List<ListingSummaryResponse> content = listingPage.getContent().stream()
                    .map(l -> listingMapper.toListingSummary(
                            l,
                            favIds.contains(l.getListingId()),
                            realViewCountByListingId.get(l.getListingId())))
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

            Pageable pageable = resolvePageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Listing> listingPage = listingRepository.findBySellerId(seller.getSellerId(), pageable);

            attachPostingPackageOrders(listingPage.getContent());

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

            Pageable pageable = resolvePageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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
    public ResponseEntity<ApiResponse> getMyPropertyDetail(Integer propertyId) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            Property property = propertyRepository.findByIdWithDetails(propertyId).orElse(null);
            if (property == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Tài sản không tồn tại: id=" + propertyId));
            }
            if (property.getSeller() == null || !property.getSeller().getSellerId().equals(seller.getSellerId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài sản này không thuộc sở hữu của bạn"));
            }

            int listingCount = (int) listingRepository.countByProperty_PropertyId(property.getPropertyId());
            PropertyDetailResponse detail = listingMapper.toPropertyDetail(property, listingCount);

            return ResponseEntity.ok(ApiResponse.success(detail, "Chi tiết tài sản"));
        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] getMyPropertyDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UPDATE LISTING
    // ════════════════════════════════════════════════════════════════════════
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

            if (listing.getStatus() == SellerListingStatusEnum.DELETED) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Conflict", "Bài đăng đã bị xoá, không thể chỉnh sửa"));
            }

            if (!isAdminOrStaff) {
                Seller seller = getCurrentSeller(currentUser);
                boolean isOwner = listing.getSeller() != null
                        && listing.getSeller().getSellerId().equals(seller.getSellerId());
                if (!isOwner) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Bạn không có quyền sửa bài đăng này"));
                }
                listing.setIsActive(false);
                resetVerificationToPending(listing);
            }

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

            Property property = listing.getProperty();
            if (property != null) {
                if (request.getPropertyTitle() != null) property.setTitle(request.getPropertyTitle());
                if (request.getPropertyDescription() != null) property.setDescription(request.getPropertyDescription());
                if (request.getPropertyPrice() != null) property.setPrice(request.getPropertyPrice());
                if (request.getArea() != null) property.setArea(request.getArea());
                if (request.getFloor() != null) property.setFloor(request.getFloor());
                if (request.getBedroom() != null) property.setBedroom(request.getBedroom());
                if (request.getBathroom() != null) property.setBathroom(request.getBathroom());
                if (request.getDirection() != null) property.setDirection(request.getDirection());
                if (request.getFurniture() != null) property.setFurniture(request.getFurniture());
                property.setUpdatedAt(LocalDateTime.now());
                propertyRepository.save(property);
            }

            int reparentedCount = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty()) {
                reparentedCount = reparentUploadedImagesToListing(
                        request.getDraftImagePublicIds(), currentUser, listing, request.getThumbnailImageIndex());
            }

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
                    "Cập nhật bài đăng thành công — cần Staff duyệt lại"
                            + (reparentedCount > 0 ? " (đã gắn " + reparentedCount + " ảnh mới)" : "")));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] updateListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private int reparentUploadedImagesToListing(List<String> publicIds,
                                                Account owner,
                                                Listing listing,
                                                Integer thumbnailImageIndex) {
        if (publicIds == null || publicIds.isEmpty()) return 0;

        long existingCount = listingImageRepository.countByListing_ListingId(listing.getListingId());
        boolean callerSpecifiedThumbnail = thumbnailImageIndex != null;
        int targetIdx = callerSpecifiedThumbnail ? thumbnailImageIndex : 0;

        if (callerSpecifiedThumbnail) {
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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyListingDetail(Integer listingId) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId()).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            attachPostingPackageOrders(List.of(listing));

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> softDeleteListing(Integer listingId) {
        UpdateListingStatusRequest req = new UpdateListingStatusRequest();
        req.setStatus(SellerListingStatusEnum.DELETED);
        return updateListingStatus(listingId, req);
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListingStatus(Integer listingId, UpdateListingStatusRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId()).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            SellerListingStatusEnum targetStatus = request.getStatus();
            SellerListingStatusEnum currentStatus = listing.getStatus();
            String msg;

            switch (targetStatus) {
                case ACTIVE -> {
                    ListingVerification lv = listing.getListingVerification();
                    ListingStatusEnum verificationStatus = lv != null ? lv.getStatus() : null;
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
    // AI / SEARCH / FEATURED / COMPARE / SUGGESTIONS
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
                    response.text().trim(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

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
                    response.text().trim(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

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
            Sort sort = Sort.by(Sort.Direction.DESC, "priority").and(userSort);
            Pageable pageable = resolvePageable(request.getPage(), request.getSize(), sort);

            Specification<Listing> spec = ListingSpecification.fromRequest(request);
            Page<Listing> listingPage = TwoStepPaginationUtil.<Integer, Listing>paginate(
                    pageable,
                    p -> listingRepository.findAll(spec, p).map(Listing::getListingId),
                    listingRepository::findAllByListingIdInWithDetails,
                    Listing::getListingId
            );

            attachPostingPackageOrders(listingPage.getContent());

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
    @Transactional
    public ResponseEntity<ApiResponse> getFeaturedListings(int page, int size) {
        try {
            Pageable pageable = (page == 0 && size == 0)
                    ? Pageable.unpaged()
                    : PageRequest.of(Math.max(page, 0), size > 0 ? Math.min(size, MAX_SEARCH_PAGE_SIZE) : PAGE_SIZE);

            Page<FeaturedListingProjection> topViewed = activeLogRepository
                    .findFeaturedListingIds(UserEventTypeEnum.VIEW, pageable);

            List<Integer> orderedIds = topViewed.getContent().stream()
                    .map(FeaturedListingProjection::getListingId)
                    .toList();

            Map<Integer, Long> viewCountByListingId = topViewed.getContent().stream()
                    .collect(Collectors.toMap(
                            FeaturedListingProjection::getListingId,
                            FeaturedListingProjection::getViewCount));

            List<Listing> loaded = listingRepository.findAllByListingIdInWithDetails(orderedIds);
            attachPostingPackageOrders(loaded);

            Map<Integer, Listing> listingById = loaded.stream()
                    .collect(Collectors.toMap(Listing::getListingId, l -> l, (a, b) -> a));

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

            List<Map<String, Object>> content = orderedIds.stream()
                    .map(listingById::get)
                    .filter(Objects::nonNull)
                    .map(l -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listing", listingMapper.toListingSummary(
                                l,
                                favIds.contains(l.getListingId()),
                                viewCountByListingId.get(l.getListingId())));
                        item.put("viewCount", viewCountByListingId.getOrDefault(l.getListingId(), 0L));
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

    @Override
    public ResponseEntity<ApiResponse> compareListings(List<Integer> listingIds) {
        try {
            if (listingIds == null || listingIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Vui lòng gửi danh sách listingId cần so sánh"));
            }

            List<Integer> distinctIds = listingIds.stream().distinct().toList();

            if (distinctIds.size() < 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Cần tối thiểu 2 tin đăng khác nhau để so sánh"));
            }
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

            List<Object> content = distinctIds.stream()
                    .map(listingById::get)
                    .filter(Objects::nonNull)
                    .map(l -> listingMapper.toListingDetail(l, l.getProperty()))
                    .collect(Collectors.toList());

            if (content.size() < 2) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request",
                        "Không đủ tin đăng hợp lệ để so sánh (không tìm thấy: " + String.join(", ", notFoundIds) + ")"));
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("listings", content);
            if (!notFoundIds.isEmpty()) {
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

    private void recordSearchHistory(Account account, String keyword) {
        try {
            String trimmed = keyword.trim();
            LocalDateTime now = LocalDateTime.now();

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

    private List<SearchSuggestionItem> buildPropertyTypeSuggestions(String keyword) {
        return propertyTypeRepository.findTop5ByIsActiveTrueAndNameContainingIgnoreCase(keyword).stream()
                .map(pt -> SearchSuggestionItem.builder()
                        .type("PROPERTY_TYPE")
                        .label(pt.getName())
                        .value(pt.getName())
                        .code(String.valueOf(pt.getPropertyTypeId()))
                        .build())
                .toList();
    }

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