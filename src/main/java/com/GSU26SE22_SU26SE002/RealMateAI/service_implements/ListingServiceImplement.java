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

    // Mỗi trang luôn cố định 10 bản ghi (trang 0: 1-10, trang 1: 11-20, ...).
    private static final int PAGE_SIZE = 10;

    /**
     * Exception nội bộ — dùng để CHỦ ĐỘNG kích hoạt rollback transaction
     * khi phát hiện lỗi nghiệp vụ giữa luồng xử lý (vd: ảnh bị claim trùng
     * do race condition). Khác với việc `return` thẳng 1 ApiResponse lỗi
     * giữa method @Transactional — cách đó KHÔNG tự rollback các thay đổi
     * đã `save()` trước đó trong cùng transaction (Spring chỉ rollback khi
     * có exception, không phải khi method return bình thường). Throw
     * exception này đảm bảo Property/Location vừa tạo bị rollback sạch,
     * sau đó được bắt lại ở catch (ListingConflictException e) để trả về
     * đúng HTTP status/message cho client.
     */
    private static class ListingConflictException extends RuntimeException {
        final HttpStatus status;
        ListingConflictException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }
    }

    // ════════════════════════════════════════════════════
    //  POST /listings — Tạo bài đăng mới
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> createListing(CreateListingRequest request) {
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

            // ── Validate: phải chọn ĐÚNG 1 trong 2 chế độ ──────────────
            boolean hasExisting = request.getExistingPropertyId() != null;
            boolean hasNew = request.getNewProperty() != null;

            if (hasExisting == hasNew) {
                // cả 2 cùng null HOẶC cả 2 cùng có giá trị đều là lỗi
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request",
                                "Phải chọn ĐÚNG 1 trong 2: existingPropertyId (đăng lại tài sản đã có) " +
                                        "HOẶC newProperty (tạo tài sản mới) — không được để cả hai cùng trống hoặc cùng có giá trị"));
            }

            Property targetProperty;
            boolean isNewProperty;

            if (hasExisting) {
                // ── Chế độ ① ĐĂNG LẠI tài sản đã có ───────────────────
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
                // ── Chế độ ② TẠO TÀI SẢN MỚI ───────────────────────────
                CreatePropertyRequest np = request.getNewProperty();

                // Bắt buộc có ảnh khi tạo tài sản mới — không cho phép tạo
                // 1 Property "trắng ảnh" rồi mới nghĩ tới chuyện bổ sung ảnh,
                // vì như đã nêu, Staff sẽ không thể duyệt nó.
                if (request.getDraftImagePublicIds() == null || request.getDraftImagePublicIds().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request",
                                    "Tạo tài sản mới phải kèm ít nhất 1 ảnh (draftImagePublicIds). " +
                                            "Hãy upload ảnh trước qua POST /api/v1/media/upload/multiple" +
                                            "?entityType=ACCOUNT&entityId=" + currentUser.getAccountId()));
                }

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
                isNewProperty = true;
            }

            // ── Re-parent ảnh draft (nếu có) về Property này ──────────
            // Áp dụng cho CẢ 2 chế độ: nếu Seller đăng lại tài sản cũ nhưng
            // vẫn muốn bổ sung thêm ảnh mới, draftImagePublicIds vẫn hoạt động
            // (ảnh mới được NỐI THÊM vào bộ ảnh hiện có, không xóa ảnh cũ).
            int reparented = reparentDraftImagesToProperty(
                    request.getDraftImagePublicIds(),
                    currentUser,
                    targetProperty,
                    request.getMainImageIndex());

            log.info("[ListingService] Đã gắn {} ảnh vào propertyId={}", reparented, targetProperty.getPropertyId());

            // ── Guard: Property MỚI không được phép trắng ảnh ─────────
            // Trường hợp này xảy ra khi TOÀN BỘ publicId gửi lên đều bị
            // claim thất bại (đã bị 1 request đăng tin khác "giành" trước
            // — race condition khi đăng nhiều tin cùng lúc, xem javadoc
            // claimDraftAsset()). Phải chặn ở đây và rollback transaction,
            // KHÔNG để Property được tạo ra mà không có ảnh nào — vì nếu
            // không, Listing sẽ không bao giờ được Staff duyệt (xem
            // ListingVerificationServiceImplement: APPROVED yêu cầu có ảnh),
            // và Seller sẽ không hiểu lý do tại sao bị kẹt mãi ở PENDING.
            if (isNewProperty && reparented == 0) {
                throw new ListingConflictException(HttpStatus.CONFLICT,
                        "Không thể gắn ảnh: toàn bộ ảnh đã gửi đều không hợp lệ hoặc đã được " +
                                "dùng cho một tin đăng khác đang được tạo cùng lúc. Vui lòng upload lại " +
                                "ảnh mới qua POST /api/v1/media/upload/multiple và thử đăng tin lại.");
            }

            // ── Tạo Listing — luôn isActive=false, chờ Staff duyệt ────
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

            log.info("[ListingService] Tạo mới: propertyId={}, listingId={}, sellerId={}, propertyMode={}",
                    targetProperty.getPropertyId(), savedListing.getListingId(), seller.getSellerId(),
                    isNewProperty ? "NEW" : "REUSE_EXISTING");

            // Refresh lại Property để propertyImages phản ánh đúng ảnh vừa re-parent
            Property refreshed = propertyRepository.findByIdWithDetails(targetProperty.getPropertyId())
                    .orElse(targetProperty);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            listingMapper.toListingDetail(savedListing, refreshed),
                            "Bài đăng đã được tạo cùng ảnh, đang chờ Staff duyệt"));

        } catch (ListingConflictException e) {
            // Bị throw từ guard "Property mới nhưng 0 ảnh được claim" —
            // tới đây nghĩa là Spring đã rollback toàn bộ Property/Location
            // vừa tạo trong transaction này, trả lỗi đúng status cho client.
            log.warn("[ListingService] createListing bị từ chối do conflict ảnh: {}", e.getMessage());
            return ResponseEntity.status(e.status).body(ApiResponse.fail("Conflict", e.getMessage()));

        } catch (Exception e) {
            log.error("[ListingService] createListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    /**
     * Chuyển sở hữu (re-parent) các MediaAsset draft từ EntityType.ACCOUNT
     * sang EntityType.PROPERTY, đồng thời tạo PropertyImage tương ứng.
     *
     * @param publicIds       danh sách publicId ảnh đã upload draft
     * @param owner           Account hiện tại — dùng để xác thực ảnh thực sự
     *                        thuộc về người đang gọi API (chống đoán publicId người khác)
     * @param property        Property sẽ nhận ảnh
     * @param mainImageIndex  index trong publicIds sẽ là ảnh đại diện (null = 0)
     * @return số ảnh đã re-parent thành công
     */
    private int reparentDraftImagesToProperty(List<String> publicIds, Account owner,
                                              Property property, Integer mainImageIndex) {
        if (publicIds == null || publicIds.isEmpty()) {
            return 0;
        }

        long existingCount = propertyImageRepository.countByProperty_PropertyId(property.getPropertyId());
        int mainIdx = (mainImageIndex == null) ? 0 : mainImageIndex;
        int saved = 0;
        Long propertyIdAsLong = property.getPropertyId().longValue();
        long ownerAccountIdAsLong = owner.getAccountId();

        for (int i = 0; i < publicIds.size(); i++) {
            String publicId = publicIds.get(i);

            // ════════════════════════════════════════════════
            // ATOMIC CLAIM — chống "ảnh bị cướp" khi Seller đăng nhiều
            // tin cùng lúc (mở nhiều tab, hoặc 2 request POST /listings
            // gửi trùng publicId do FE bug, hoặc 2 request chạy gần như
            // đồng thời thật sự — race condition mức DB).
            //
            // claimDraftAsset() là 1 UPDATE có điều kiện trực tiếp ở DB:
            // chỉ thành công (trả về 1) nếu publicId này CHƯA từng được
            // re-parent trước đó (vẫn còn entityType=ACCOUNT, entityId=
            // accountId của chính owner). Nếu publicId đã bị 1 request
            // khác claim trước (dù chỉ vài milli-giây trước), điều kiện
            // WHERE sẽ không khớp nữa → trả về 0 → ảnh này bị BỎ QUA,
            // không thể "cướp" về Property hiện tại.
            //
            // Khác với findByPublicId()+save() (đọc rồi ghi, có khoảng hở
            // giữa 2 bước), UPDATE...WHERE này được PostgreSQL đảm bảo
            // nguyên tử ở mức row — không có 2 transaction nào cùng claim
            // thành công 1 publicId.
            // ════════════════════════════════════════════════
            int claimed = mediaAssetRepository.claimDraftAsset(
                    publicId, owner.getAccountId(), ownerAccountIdAsLong,
                    EntityType.PROPERTY, propertyIdAsLong);

            if (claimed == 0) {
                log.warn("[ListingService] Bỏ qua publicId={} — ảnh không tồn tại, không thuộc " +
                        "accountId={}, hoặc đã được gắn vào tin đăng khác trước đó (có thể do " +
                        "đăng nhiều tin cùng lúc)", publicId, owner.getAccountId());
                continue;
            }

            // Claim thành công — lấy lại secureUrl để tạo PropertyImage.
            // Tại đây entityType/entityId trong DB đã chắc chắn là của
            // Property này (không còn race condition nữa vì đã claim xong).
            MediaAsset asset = mediaAssetRepository.findByPublicId(publicId).orElse(null);
            if (asset == null) {
                // Trường hợp lý thuyết không nên xảy ra (vừa claim xong),
                // nhưng vẫn xử lý an toàn để không NPE.
                log.error("[ListingService] Claim thành công nhưng không tìm lại được publicId={}", publicId);
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
    //  GET /seller/properties — Tài sản Seller đang sở hữu
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyProperties() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller"));
            }

            List<Property> properties = propertyRepository.findBySellerIdWithDetails(seller.getSellerId());

            List<PropertyDetailResponse> response = properties.stream()
                    .map(p -> {
                        int listingCount = (int) listingRepository.countByProperty_PropertyId(p.getPropertyId());
                        return listingMapper.toPropertyDetail(p, null, listingCount);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response,
                    "Danh sách tài sản bạn đang sở hữu — dùng existingPropertyId để đăng lại"));

        } catch (Exception e) {
            log.error("[ListingService] getMyProperties lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  GET /listings — Chợ BĐS
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMarketListings(int page, int size) {
        try {
            int safePage = Math.max(page, 0);
            Pageable pageable = PageRequest.of(safePage, PAGE_SIZE);

            Page<Listing> listingPage = listingRepository.findAllActiveWithDetails(pageable);

            Account currentUser = authenUntil.getCurrentUSer();
            Set<Integer> favoritedIds = Collections.emptySet();
            if (currentUser != null) {
                Investor investor = investorRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                if (investor != null) {
                    favoritedIds = new HashSet<>(favoriteListingRepository.findFavoritedListingIdsByInvestorId(investor.getInvestorId()));
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

    // ════════════════════════════════════════════════════
    //  GET /listings/{id} — Chi tiết công khai
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getListingDetail(Integer listingId) {
        try {
            Listing listing = listingRepository.findActiveById(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại hoặc chưa được duyệt: id=" + listingId));
            }

            ListingDetailResponse detail = listingMapper.toListingDetail(listing, listing.getProperty());
            return ResponseEntity.ok(ApiResponse.success(detail, "Chi tiết tin đăng"));

        } catch (Exception e) {
            log.error("[ListingService] getListingDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  GET /seller/listings — Bài đăng cá nhân của Seller
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getMyListings() {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller"));
            }

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

    // ════════════════════════════════════════════════════
    //  PUT /listings/{id} — Chỉnh sửa bài đăng + thông số BĐS
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateListing(Integer listingId, UpdateListingRequest request) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            Listing listing = listingRepository.findByIdWithDetails(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));
            }

            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            boolean isAdminOrStaff = roleName.equals("Admin") || roleName.equals("Staff");

            if (!isAdminOrStaff) {
                Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                boolean isOwner = seller != null && listing.getSeller() != null
                        && listing.getSeller().getSellerId().equals(seller.getSellerId());
                if (!isOwner) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Bạn không có quyền sửa bài đăng này"));
                }
                // Sửa nội dung/ảnh sau khi đã duyệt → bắt buộc duyệt lại từ đầu,
                // vì nội dung/ảnh đã thay đổi so với lần Staff đã xem xét.
                listing.setIsActive(false);
            }

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

                if (request.getPropertyTypeId() != null) {
                    PropertyType pt = propertyTypeRepository.findById(request.getPropertyTypeId()).orElse(null);
                    if (pt == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
                    }
                    property.setPropertyType(pt);
                }
                if (request.getPropertyConditionId() != null) {
                    PropertyCondition pc = propertyConditionRepository.findById(request.getPropertyConditionId()).orElse(null);
                    if (pc == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Tình trạng bất động sản không hợp lệ"));
                    }
                    property.setPropertyCondition(pc);
                }

                Location location = property.getLocation();
                if (location != null) {
                    if (request.getLatitude() != null) location.setLatitude(request.getLatitude());
                    if (request.getLongitude() != null) location.setLongitude(request.getLongitude());
                    if (request.getPostalCode() != null) location.setPostalCode(request.getPostalCode());
                    if (request.getWardCode() != null) {
                        Ward ward = wardRepository.findById(request.getWardCode()).orElse(null);
                        if (ward == null) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + request.getWardCode()));
                        }
                        location.setWard(ward);
                    }
                    locationRepository.save(location);
                }

                property.setUpdatedAt(LocalDateTime.now());
                propertyRepository.save(property);
            }

            // ── Re-parent ảnh mới (nếu Seller bổ sung thêm khi sửa bài) ──
            int requestedImageCount = 0;
            int reparentedImageCount = 0;
            if (request.getDraftImagePublicIds() != null && !request.getDraftImagePublicIds().isEmpty() && property != null) {
                requestedImageCount = request.getDraftImagePublicIds().size();
                reparentedImageCount = reparentDraftImagesToProperty(
                        request.getDraftImagePublicIds(), currentUser, property, request.getMainImageIndex());
                log.info("[ListingService] Bổ sung {}/{} ảnh mới khi sửa listingId={}",
                        reparentedImageCount, requestedImageCount, listingId);
            }

            Listing updated = listingRepository.save(listing);
            log.info("[ListingService] accountId={} đã cập nhật listingId={}", currentUser.getAccountId(), listingId);

            Property refreshed = property != null
                    ? propertyRepository.findByIdWithDetails(property.getPropertyId()).orElse(property)
                    : null;

            String updateMessage = "Cập nhật bài đăng thành công — cần Staff duyệt lại";
            if (requestedImageCount > 0 && reparentedImageCount < requestedImageCount) {
                updateMessage += String.format(
                        " (Lưu ý: chỉ %d/%d ảnh mới được gắn — số ảnh còn lại đã không hợp lệ " +
                                "hoặc đã được dùng cho một tin đăng khác)",
                        reparentedImageCount, requestedImageCount);
            }

            return ResponseEntity.ok(ApiResponse.success(
                    listingMapper.toListingDetail(updated, refreshed),
                    updateMessage));

        } catch (Exception e) {
            log.error("[ListingService] updateListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
