package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingSortEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingActionEnum;
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
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
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
    private final PropertyImageRepository propertyImageRepository;
    private final SellerRepository sellerRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyConditionRepository propertyConditionRepository;
    private final InvestorRepository investorRepository;
    private final FavoriteListingRepository favoriteListingRepository;
    private final WardRepository wardRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ListingMapper listingMapper;
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

            int reparented = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty()) {
                reparented = reparentUploadedImagesToProperty(
                        request.getDraftImagePublicIds(), currentUser, property, request.getMainImageIndex());
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
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Listing saved = listingRepository.save(listing);
            createPendingVerification(saved);

            log.info("[ListingService] Luồng①: listingId={}, propertyId={}, sellerId={}",
                    saved.getListingId(), property.getPropertyId(), seller.getSellerId());

            Property refreshed = propertyRepository
                    .findByIdWithDetails(property.getPropertyId()).orElse(property);

            String msg = "Bài đăng tạo thành công"
                    + (reparented > 0 ? ", thêm " + reparented + " ảnh mới" : "")
                    + ", đang chờ Staff duyệt";

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(listingMapper.toListingDetail(saved, refreshed), msg));

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

            // Re-parent ảnh
            List<String> publicIds = uploadedAssets.stream()
                    .map(MediaAssetResponse::getPublicId)
                    .collect(Collectors.toList());

            int reparented = reparentUploadedImagesToProperty(
                    publicIds, currentUser, savedProperty, request.getMainImageIndex());

            if (reparented == 0) {
                throw new ListingConflictException(HttpStatus.CONFLICT,
                        "Upload ảnh thành công nhưng không thể gắn vào tài sản. Vui lòng thử lại.");
            }

            // Tạo Listing
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
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Listing saved = listingRepository.save(listing);
            createPendingVerification(saved);

            log.info("[ListingService] Luồng②: listingId={}, propertyId={}, sellerId={}, ảnh={}",
                    saved.getListingId(), savedProperty.getPropertyId(), seller.getSellerId(), reparented);

            Property refreshed = propertyRepository
                    .findByIdWithDetails(savedProperty.getPropertyId()).orElse(savedProperty);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            listingMapper.toListingDetail(saved, refreshed),
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
            Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
            Page<Listing> listingPage = listingRepository.findAllActiveWithDetails(pageable);

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
    public ResponseEntity<ApiResponse> getMyListings() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            List<ListingSummaryResponse> listings = listingRepository
                    .findBySellerId(seller.getSellerId())
                    .stream()
                    .map(l -> listingMapper.toListingSummary(l, false))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(listings, "Danh sách tin đăng của bạn"));
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
    public ResponseEntity<ApiResponse> getMyProperties() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            List<Property> properties = propertyRepository.findBySellerIdWithDetails(seller.getSellerId())
                    .stream()
                    .distinct()
                    .collect(Collectors.toList());

            List<PropertyDetailResponse> response = properties.stream()
                    .map(p -> {
                        int listingCount = (int) listingRepository.countByProperty_PropertyId(p.getPropertyId());
                        return listingMapper.toPropertyDetail(p, null, listingCount);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response, "Danh sách tài sản bạn đang sở hữu"));
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

            // Re-parent images nếu có
            int reparentedCount = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty() && property != null) {
                reparentedCount = reparentUploadedImagesToProperty(
                        request.getDraftImagePublicIds(), currentUser, property, request.getMainImageIndex());
            }

            Listing updated = listingRepository.save(listing);
            Property refreshed = property != null
                    ? propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property)
                    : null;

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(updated, refreshed),
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
    // Helper: Re-parent MediaAsset
    // ════════════════════════════════════════════════════════════════════════
    private int reparentUploadedImagesToProperty(List<String> publicIds,
                                                 Account owner,
                                                 Property property,
                                                 Integer mainImageIndex) {
        if (publicIds == null || publicIds.isEmpty()) return 0;

        long existingCount = propertyImageRepository.countByProperty_PropertyId(property.getPropertyId());
        int mainIdx = (mainImageIndex == null) ? 0 : mainImageIndex;
        int saved = 0;
        Long propertyIdL = property.getPropertyId().longValue();
        long ownerIdL = owner.getAccountId();

        for (int i = 0; i < publicIds.size(); i++) {
            String publicId = publicIds.get(i);

            int claimed = mediaAssetRepository.claimDraftAsset(
                    publicId, owner.getAccountId(), ownerIdL,
                    EntityType.PROPERTY, propertyIdL);

            if (claimed == 0) {
                log.warn("[ListingService] Skip publicId={}", publicId);
                continue;
            }

            MediaAsset asset = mediaAssetRepository.findByPublicId(publicId).orElse(null);
            if (asset == null) continue;

            boolean isMain = (existingCount == 0) && (i == mainIdx);
            PropertyImage img = PropertyImage.builder()
                    .property(property)
                    .imageUrl(asset.getSecureUrl())
                    .isMain(isMain)
                    .displayOrder((int) existingCount + saved)
                    .build();

            propertyImageRepository.save(img);
            saved++;
        }
        return saved;
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
    // DELETE /seller/listings/{id} — Xoá mềm (isActive = false)
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> softDeleteListing(Integer listingId) {
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

            if (Boolean.FALSE.equals(listing.getIsActive())) {
                // Đã inactive rồi — idempotent, trả 200 luôn
                return ResponseEntity.ok(ApiResponse.success(null,
                        "Bài đăng đã được xoá trước đó"));
            }

            listing.setIsActive(false);
            listing.setUpdatedAt(LocalDateTime.now());
            listingRepository.save(listing);

            log.info("[ListingService] softDelete: listingId={} bởi sellerId={}", listingId, seller.getSellerId());

            return ResponseEntity.ok(ApiResponse.success(null,
                    "Xoá bài đăng thành công (id=" + listingId + ")"));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] softDeleteListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATCH /seller/listings/{id} — Seller tự đổi trạng thái hiển thị
    // (PAUSE/RESUME) cho bài đăng của mình. KHÔNG liên quan tới quyết định
    // duyệt của Staff (APPROVED/REJECTED) — đó vẫn là verification status.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListingStatus(Integer listingId, UpdateListingStatusRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            Seller seller = getCurrentSeller(currentUser);

            if (request.getAction() == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "action không được để trống (PAUSE hoặc RESUME)"));
            }

            Listing listing = listingRepository.findByIdAndSellerId(listingId, seller.getSellerId())
                    .orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found",
                                "Bài đăng không tồn tại hoặc không thuộc sở hữu của bạn: id=" + listingId));
            }

            ListingVerification verification = listingVerificationRepository
                    .findByListing_ListingId(listingId).orElse(null);
            ListingStatusEnum currentStatus = verification != null ? verification.getStatus() : null;

            if (request.getAction() == SellerListingActionEnum.RESUME) {
                if (currentStatus != ListingStatusEnum.APPROVED) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(ApiResponse.fail("Conflict",
                                    "Chỉ bật lại được bài đăng đã được Staff duyệt (APPROVED). "
                                            + "Trạng thái hiện tại: " + (currentStatus == null ? "PENDING" : currentStatus)));
                }
                if (Boolean.TRUE.equals(listing.getIsActive())) {
                    return ResponseEntity.ok(ApiResponse.success(
                            listingMapper.toListingDetail(listing, listing.getProperty()),
                            "Bài đăng đang hiển thị, không có gì thay đổi"));
                }
                listing.setIsActive(true);
            } else { // PAUSE
                if (Boolean.FALSE.equals(listing.getIsActive())) {
                    return ResponseEntity.ok(ApiResponse.success(
                            listingMapper.toListingDetail(listing, listing.getProperty()),
                            "Bài đăng đã tạm ẩn từ trước, không có gì thay đổi"));
                }
                listing.setIsActive(false);
            }

            listing.setUpdatedAt(LocalDateTime.now());
            Listing updated = listingRepository.save(listing);

            log.info("[ListingService] updateListingStatus: listingId={} action={} bởi sellerId={}",
                    listingId, request.getAction(), seller.getSellerId());

            String msg = request.getAction() == SellerListingActionEnum.RESUME
                    ? "Bật lại bài đăng thành công"
                    : "Tạm ẩn bài đăng thành công";

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(updated, updated.getProperty()), msg));

        } catch (RuntimeException e) {
            return handleAuthException(e);
        } catch (Exception e) {
            log.error("[ListingService] updateListingStatus lỗi", e);
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
            Page<Listing> listingPage = listingRepository.findAll(ListingSpecification.fromRequest(request), pageable);

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