package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreatePropertyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateListingRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CloudinaryMediaServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
/**
 * *  CƠ CHẾ ẢNH ĐI KÈM LÚC ĐĂNG TIN — "DRAFT ASSET RE-PARENTING"
*  Bước 1 (FE thực hiện, KHÔNG nằm trong service này):
        *     Seller gọi POST /api/v1/media/upload/multiple
 *              ?entityType=ACCOUNT&entityId={accountId của Seller}
        *     → Ảnh lên Cloudinary THẬT, MediaAsset được lưu với entityType=ACCOUNT,
 *       entityId=accountId (nghĩa là "ảnh này đang treo dưới tài khoản này,
        *       chưa thuộc về tài sản nào cụ thể"). FE nhận lại publicId từng ảnh.
        *
        *  Bước 2 (xử lý trong createListing() dưới đây):
        *     Seller gọi POST /listings, gửi kèm draftImagePublicIds = [publicId,...]
        *     Trong CÙNG 1 transaction:
        *       a. Tạo/lấy Property → có propertyId
 *       b. Tạo Listing → có listingId
 *       c. Với mỗi publicId trong draftImagePublicIds:
        *          - Tìm MediaAsset theo publicId (phải đang thuộc về đúng accountId
        *            này, đề phòng người khác đoán publicId của người khác)
 *          - "Re-parent": set lại entityType=PROPERTY, entityId=propertyId
 *          - Tạo 1 row PropertyImage tương ứng (imageUrl=secureUrl)
 *
         * Lợi ích của cách này so với việc "tạo Listing rỗng rồi PATCH ảnh sau":
        *   - Đúng 1 lần gọi API để hoàn tất đăng tin có ảnh — không có khoảng thời
 *     gian nào tồn tại 1 Listing "trắng ảnh" trong DB.
        *   - Ảnh đã lên Cloudinary thật từ Bước 1 nên nếu Bước 2 thất bại (validate
 *     lỗi, transaction rollback), ảnh vẫn còn nguyên trên Cloudinary dưới
 *     entityType=ACCOUNT — Seller chỉ cần gọi lại POST /listings với cùng
 *     publicId đó, không cần upload lại từ đầu.
 *   - Vì duyệt tin (ListingVerificationServiceImplement) kiểm tra Property
 *     phải có ảnh mới được APPROVED, cơ chế này đảm bảo KHÔNG BAO GIỜ có
 *     Listing được tạo ra mà thiếu ảnh ngay từ đầu.
        * ════════════════════════════════════════════════════════════════════════
        */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListingServiceImplement implements ListingServiceInterface {

    private final ListingRepository listingRepository;
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

    private static final int PAGE_SIZE = 10;

    private static class ListingConflictException extends RuntimeException {
        final HttpStatus status;
        ListingConflictException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    // ════════════════════════════════════════════════════
    //  POST /listings — Tạo bài đăng + upload ảnh cùng lúc
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createListing(CreateListingRequest request,
                                                     List<MultipartFile> images) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller hoặc chưa được kích hoạt"));
            }

            boolean hasExisting = request.getExistingPropertyId() != null;
            boolean hasNew      = request.getNewProperty() != null;
            if (hasExisting == hasNew) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request",
                                "Phải chọn ĐÚNG 1 trong 2: existingPropertyId (đăng lại tài sản cũ) " +
                                        "HOẶC newProperty (tạo tài sản mới)"));
            }

            // Validate ảnh bắt buộc khi tạo Property mới
            boolean hasImages = images != null && !images.isEmpty();
            if (hasNew && !hasImages) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request",
                                "Tạo tài sản mới phải kèm ít nhất 1 ảnh (part 'images' trong multipart request)"));
            }

            // ── Step 1: Upload ảnh lên Cloudinary ngay đầu transaction ──
            // Upload với entityType=ACCOUNT trước (draft), sau khi có propertyId sẽ re-parent
            List<MediaAssetResponse> uploadedAssets = Collections.emptyList();
            if (hasImages) {
                uploadedAssets = cloudinaryMediaService.uploadMultiple(
                        images, currentUser, EntityType.ACCOUNT, (long) currentUser.getAccountId());
                log.info("[ListingService] Uploaded {} ảnh draft cho accountId={}",
                        uploadedAssets.size(), currentUser.getAccountId());
            }

            // ── Step 2: Tạo hoặc lấy Property ──────────────────────────
            Property targetProperty;
            boolean isNewProperty;

            if (hasExisting) {
                targetProperty = propertyRepository.findById(request.getExistingPropertyId()).orElse(null);
                if (targetProperty == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.fail("Not_Found", "Tài sản không tồn tại: id=" + request.getExistingPropertyId()));
                }
                if (targetProperty.getSeller() == null
                        || !targetProperty.getSeller().getSellerId().equals(seller.getSellerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Tài sản này không thuộc sở hữu của bạn"));
                }
                isNewProperty = false;

            } else {
                CreatePropertyRequest np = request.getNewProperty();

                PropertyType propertyType = propertyTypeRepository.findById(np.getPropertyTypeId()).orElse(null);
                if (propertyType == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
                }

                PropertyCondition propertyCondition = null;
                if (np.getPropertyConditionId() != null) {
                    propertyCondition = propertyConditionRepository.findById(np.getPropertyConditionId()).orElse(null);
                    if (propertyCondition == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Tình trạng bất động sản không hợp lệ"));
                    }
                }

                Ward ward = null;
                if (np.getWardCode() != null && !np.getWardCode().isBlank()) {
                    ward = wardRepository.findById(np.getWardCode()).orElse(null);
                    if (ward == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + np.getWardCode()));
                    }
                }

                Location location = Location.builder()
                        .latitude(np.getLatitude())
                        .longitude(np.getLongitude())
                        .postalCode(np.getPostalCode())
                        .ward(ward)
                        .build();
                Location savedLocation = locationRepository.save(location);

                LocalDateTime now = LocalDateTime.now();
                Property property = Property.builder()
                        .seller(seller)
                        .propertyType(propertyType)
                        .propertyCondition(propertyCondition)
                        .location(savedLocation)
                        .title(np.getTitle())
                        .description(np.getDescription())
                        .price(np.getPrice())
                        .area(np.getArea())
                        .floor(np.getFloor())
                        .bedroom(np.getBedroom())
                        .bathroom(np.getBathroom())
                        .direction(np.getDirection())
                        .legalStatus(np.getLegalStatus())
                        .addressParticular(np.getAddressParticular())
                        .projectName(np.getProjectName())
                        .propertyAttribute(np.getPropertyAttribute())
                        .propertyPurpose(np.getPropertyPurpose())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                targetProperty = propertyRepository.save(property);
                isNewProperty  = true;
            }

            // ── Step 3: Re-parent ảnh đã upload về Property ─────────────
            int reparented = 0;
            if (!uploadedAssets.isEmpty()) {
                List<String> publicIds = uploadedAssets.stream()
                        .map(MediaAssetResponse::getPublicId)
                        .collect(Collectors.toList());
                reparented = reparentUploadedImagesToProperty(
                        publicIds, currentUser, targetProperty, request.getMainImageIndex());
                log.info("[ListingService] Reparented {} ảnh vào propertyId={}",
                        reparented, targetProperty.getPropertyId());
            }

            if (isNewProperty && reparented == 0 && hasImages) {
                throw new ListingConflictException(HttpStatus.CONFLICT,
                        "Upload ảnh thành công nhưng không thể gắn vào tài sản. Vui lòng thử lại.");
            }

            // ── Step 4: Tạo Listing ──────────────────────────────────────
            LocalDateTime now = LocalDateTime.now();
            Listing listing = Listing.builder()
                    .property(targetProperty)
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
            Listing savedListing = listingRepository.save(listing);

            log.info("[ListingService] Tạo mới: propertyId={}, listingId={}, sellerId={}, mode={}",
                    targetProperty.getPropertyId(), savedListing.getListingId(),
                    seller.getSellerId(), isNewProperty ? "NEW" : "REUSE");

            Property refreshed = propertyRepository.findByIdWithDetails(targetProperty.getPropertyId())
                    .orElse(targetProperty);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            listingMapper.toListingDetail(savedListing, refreshed),
                            "Bài đăng đã được tạo kèm " + reparented + " ảnh, đang chờ Staff duyệt"));

        } catch (ListingConflictException e) {
            log.warn("[ListingService] Conflict: {}", e.getMessage());
            return ResponseEntity.status(e.status).body(ApiResponse.fail("Conflict", e.getMessage()));
        } catch (Exception e) {
            log.error("[ListingService] createListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    /**
     * Re-parent các MediaAsset (đã upload với entityType=ACCOUNT) sang PROPERTY.
     * Dùng atomic claimDraftAsset() để chống race condition.
     */
    private int reparentUploadedImagesToProperty(List<String> publicIds, Account owner,
                                                 Property property, Integer mainImageIndex) {
        if (publicIds == null || publicIds.isEmpty()) return 0;

        long existingCount = propertyImageRepository.countByProperty_PropertyId(property.getPropertyId());
        int mainIdx        = (mainImageIndex == null) ? 0 : mainImageIndex;
        int saved          = 0;
        Long propertyIdL   = property.getPropertyId().longValue();
        long ownerIdL      = owner.getAccountId();

        for (int i = 0; i < publicIds.size(); i++) {
            String publicId = publicIds.get(i);

            int claimed = mediaAssetRepository.claimDraftAsset(
                    publicId, owner.getAccountId(), ownerIdL,
                    EntityType.PROPERTY, propertyIdL);

            if (claimed == 0) {
                log.warn("[ListingService] Skip publicId={} — không claim được", publicId);
                continue;
            }

            MediaAsset asset = mediaAssetRepository.findByPublicId(publicId).orElse(null);
            if (asset == null) {
                log.error("[ListingService] Claim OK nhưng không tìm lại được publicId={}", publicId);
                continue;
            }

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

    // ════════════════════════════════════════════════════
    //  Các method còn lại — giữ nguyên logic cũ
    // ════════════════════════════════════════════════════

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyProperties() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));

            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller"));

            List<Property> properties = propertyRepository.findBySellerIdWithDetails(seller.getSellerId());
            List<PropertyDetailResponse> response = properties.stream()
                    .map(p -> {
                        int listingCount = (int) listingRepository.countByProperty_PropertyId(p.getPropertyId());
                        return listingMapper.toPropertyDetail(p, null, listingCount);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response, "Danh sách tài sản bạn đang sở hữu"));
        } catch (Exception e) {
            log.error("[ListingService] getMyProperties lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMarketListings(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
            Page<Listing> listingPage = listingRepository.findAllActiveWithDetails(pageable);

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
            if (listing == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));

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
            if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));

            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller"));

            List<ListingSummaryResponse> listings = listingRepository.findBySellerId(seller.getSellerId())
                    .stream()
                    .map(l -> listingMapper.toListingSummary(l, false))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(listings, "Danh sách tin đăng của bạn"));
        } catch (Exception e) {
            log.error("[ListingService] getMyListings lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập"));

            Listing listing = listingRepository.findByIdWithDetails(listingId).orElse(null);
            if (listing == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));

            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            boolean isAdminOrStaff = roleName.equals("Admin") || roleName.equals("Staff");

            if (!isAdminOrStaff) {
                Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                boolean isOwner = seller != null && listing.getSeller() != null
                        && listing.getSeller().getSellerId().equals(seller.getSellerId());
                if (!isOwner) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Bạn không có quyền sửa bài đăng này"));
                listing.setIsActive(false);
            }

            if (request.getTitle() != null)                 listing.setTitle(request.getTitle());
            if (request.getDescription() != null)           listing.setDescription(request.getDescription());
            if (request.getPrice() != null)                 listing.setPrice(request.getPrice());
            if (request.getContactPerson() != null)         listing.setContactPerson(request.getContactPerson());
            if (request.getContactPersonName() != null)     listing.setContactPersonName(request.getContactPersonName());
            if (request.getContactPersonPhone() != null)    listing.setContactPersonPhone(request.getContactPersonPhone());
            if (request.getLinkSocialContactPerson() != null) listing.setLinkSocialContactPerson(request.getLinkSocialContactPerson());
            if (request.getViewingDate() != null)           listing.setViewingDate(request.getViewingDate());
            if (request.getStartTime() != null)             listing.setStartTime(request.getStartTime());
            if (request.getEndTime() != null)               listing.setEndTime(request.getEndTime());
            listing.setUpdatedAt(LocalDateTime.now());

            Property property = listing.getProperty();
            if (property != null) {
                if (request.getPropertyTitle() != null)       property.setTitle(request.getPropertyTitle());
                if (request.getPropertyDescription() != null) property.setDescription(request.getPropertyDescription());
                if (request.getPropertyPrice() != null)       property.setPrice(request.getPropertyPrice());
                if (request.getArea() != null)                property.setArea(request.getArea());
                if (request.getFloor() != null)               property.setFloor(request.getFloor());
                if (request.getBedroom() != null)             property.setBedroom(request.getBedroom());
                if (request.getBathroom() != null)            property.setBathroom(request.getBathroom());
                if (request.getDirection() != null)           property.setDirection(request.getDirection());

                if (request.getPropertyTypeId() != null) {
                    PropertyType pt = propertyTypeRepository.findById(request.getPropertyTypeId()).orElse(null);
                    if (pt == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
                    property.setPropertyType(pt);
                }
                if (request.getPropertyConditionId() != null) {
                    PropertyCondition pc = propertyConditionRepository.findById(request.getPropertyConditionId()).orElse(null);
                    if (pc == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Tình trạng bất động sản không hợp lệ"));
                    property.setPropertyCondition(pc);
                }

                Location location = property.getLocation();
                if (location != null) {
                    if (request.getLatitude() != null)   location.setLatitude(request.getLatitude());
                    if (request.getLongitude() != null)  location.setLongitude(request.getLongitude());
                    if (request.getPostalCode() != null) location.setPostalCode(request.getPostalCode());
                    if (request.getWardCode() != null) {
                        Ward ward = wardRepository.findById(request.getWardCode()).orElse(null);
                        if (ward == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + request.getWardCode()));
                        location.setWard(ward);
                    }
                    locationRepository.save(location);
                }
                property.setUpdatedAt(LocalDateTime.now());
                propertyRepository.save(property);
            }

            int reparentedCount = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty() && property != null) {
                reparentedCount = reparentUploadedImagesToProperty(
                        request.getDraftImagePublicIds(), currentUser, property, request.getMainImageIndex());
                log.info("[ListingService] Bổ sung {}/{} ảnh khi sửa listingId={}",
                        reparentedCount, request.getDraftImagePublicIds().size(), listingId);
            }

            Listing updated = listingRepository.save(listing);
            Property refreshed = property != null
                    ? propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property)
                    : null;

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(updated, refreshed),
                    "Cập nhật bài đăng thành công — cần Staff duyệt lại"));
        } catch (Exception e) {
            log.error("[ListingService] updateListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
