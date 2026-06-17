package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateListingRequest;
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
    private final CloudinaryMediaServiceInterface cloudinaryMediaService;
    private final AuthenUntil authenUntil;

    // Mỗi trang luôn cố định 10 bản ghi (trang 0: 1-10, trang 1: 11-20, ...).
    // Tham số "size" từ client KHÔNG còn được dùng để thay đổi kích thước trang.
    private static final int PAGE_SIZE = 10;

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

            // 1. Xác minh Seller
            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Tài khoản không phải Seller hoặc chưa được kích hoạt"));
            }

            Property targetProperty;

            if (request.getPropertyId() != null) {
                // ── Chế độ ĐĂNG LẠI tài sản đã có ─────────────
                targetProperty = propertyRepository.findById(request.getPropertyId()).orElse(null);
                if (targetProperty == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.fail("Not_Found", "Tài sản không tồn tại: id=" + request.getPropertyId()));
                }
                // Ownership: Property phải thuộc Seller hiện tại
                if (targetProperty.getSeller() == null
                        || !targetProperty.getSeller().getSellerId().equals(seller.getSellerId())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Tài sản này không thuộc sở hữu của bạn"));
                }

            } else {
                // ── Chế độ TẠO TÀI SẢN MỚI ─────────────────────
                if (request.getPropertyTitle() == null || request.getPropertyTitle().isBlank()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Tiêu đề tài sản không được để trống"));
                }
                if (request.getPropertyPrice() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Giá tài sản không được để trống"));
                }
                if (request.getArea() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Diện tích không được để trống"));
                }
                if (request.getPropertyTypeId() == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không được để trống"));
                }

                PropertyType propertyType = propertyTypeRepository.findById(request.getPropertyTypeId()).orElse(null);
                if (propertyType == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Bad_Request", "Loại bất động sản không hợp lệ"));
                }

                PropertyCondition propertyCondition = null;
                if (request.getPropertyConditionId() != null) {
                    propertyCondition = propertyConditionRepository.findById(request.getPropertyConditionId()).orElse(null);
                    if (propertyCondition == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Tình trạng bất động sản không hợp lệ"));
                    }
                }

                // Tạo Location — Location liên kết tới Ward qua quan hệ @ManyToOne (không có field wardCode trực tiếp)
                Ward ward = null;
                if (request.getWardCode() != null && !request.getWardCode().isBlank()) {
                    ward = wardRepository.findById(request.getWardCode()).orElse(null);
                    if (ward == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.fail("Bad_Request", "Mã phường/xã không hợp lệ: " + request.getWardCode()));
                    }
                }

                Location location = Location.builder()
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .postalCode(request.getPostalCode())
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
                        .title(request.getPropertyTitle())
                        .description(request.getPropertyDescription())
                        .price(request.getPropertyPrice())
                        .area(request.getArea())
                        .floor(request.getFloor())
                        .bedroom(request.getBedroom())
                        .bathroom(request.getBathroom())
                        .direction(request.getDirection())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                targetProperty = propertyRepository.save(property);
            }

            // 2. Tạo Listing (bài đăng thương mại) — is_active=false, chờ Staff duyệt
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

            log.info("[ListingService] Tạo mới: propertyId={}, listingId={}, sellerId={}, reused={}",
                    targetProperty.getPropertyId(), savedListing.getListingId(), seller.getSellerId(),
                    request.getPropertyId() != null);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            toListingDetail(savedListing, targetProperty),
                            "Bài đăng đã được tạo, đang chờ duyệt"));

        } catch (Exception e) {
            log.error("[ListingService] createListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  POST /listings/{id}/images — Upload ảnh thực tế cho Property
    // ════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> uploadListingImages(Integer listingId, List<MultipartFile> files, Integer mainImageIndex) {
        try {
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Bạn cần đăng nhập để thực hiện hành động này"));
            }

            if (files == null || files.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Danh sách file không được rỗng"));
            }

            // 1. Listing phải tồn tại (để lấy Property liên kết)
            Listing listing = listingRepository.findByIdWithDetails(listingId).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng không tồn tại: id=" + listingId));
            }

            Property property = listing.getProperty();
            if (property == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Bài đăng chưa liên kết tài sản"));
            }

            // 2. Ownership: Seller chỉ upload ảnh cho tài sản của mình
            Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
            boolean isOwner = seller != null && property.getSeller() != null
                    && property.getSeller().getSellerId().equals(seller.getSellerId());
            String roleName = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            boolean isAdminOrStaff = roleName.equals("Admin") || roleName.equals("Staff");

            if (!isOwner && !isAdminOrStaff) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail("Forbidden", "Bạn không có quyền upload ảnh cho tài sản này"));
            }

            Integer propertyId = property.getPropertyId();

            // 3. Đếm ảnh hiện có để tính displayOrder liên tục
            long existingCount = propertyImageRepository.countByProperty_PropertyId(propertyId);

            // 4. Upload lên Cloudinary qua MediaAsset (entityType=PROPERTY, entityId=propertyId)
            List<MediaAssetResponse> uploaded = cloudinaryMediaService.uploadMultiple(
                    files, currentUser, EntityType.PROPERTY, propertyId.longValue());

            // 5. Lưu vào property_image
            int mainIdx = (mainImageIndex == null) ? 0 : mainImageIndex;
            List<PropertyImage> saved = new ArrayList<>();
            for (int i = 0; i < uploaded.size(); i++) {
                MediaAssetResponse asset = uploaded.get(i);
                boolean isMain = (existingCount == 0) && (i == mainIdx);

                PropertyImage img = PropertyImage.builder()
                        .property(property)
                        .imageUrl(asset.getSecureUrl())
                        .isMain(isMain)
                        .displayOrder((int) existingCount + i)
                        .build();
                saved.add(propertyImageRepository.save(img));
            }

            log.info("[ListingService] Đã upload {} ảnh cho propertyId={} (qua listingId={})",
                    saved.size(), propertyId, listingId);

            List<PropertyImageResponse> response = saved.stream()
                    .map(this::toPropertyImageResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(response, "Upload ảnh thành công"));

        } catch (Exception e) {
            log.error("[ListingService] uploadListingImages lỗi", e);
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
            // Theo yêu cầu nghiệp vụ: mỗi trang LUÔN cố định 10 bản ghi.
            // Tham số "size" do client gửi lên bị bỏ qua để đảm bảo tính nhất quán
            // (trang 0: bản ghi 1-10, trang 1: bản ghi 11-20, ...).
            int safePage = Math.max(page, 0);
            Pageable pageable = PageRequest.of(safePage, PAGE_SIZE);

            Page<Listing> listingPage = listingRepository.findAllActiveWithDetails(pageable);

            // Đánh dấu isFavorited nếu user đã đăng nhập VÀ là Investor
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
                    .map(l -> toListingSummary(l, favIds.contains(l.getListingId())))
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

            ListingDetailResponse detail = toListingDetail(listing, listing.getProperty());
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
                    .map(l -> toListingSummary(l, false))
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
                // Seller chỉ sửa bài của chính mình
                Seller seller = sellerRepository.findByAccount_AccountId(currentUser.getAccountId()).orElse(null);
                boolean isOwner = seller != null && listing.getSeller() != null
                        && listing.getSeller().getSellerId().equals(seller.getSellerId());
                if (!isOwner) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Forbidden", "Bạn không có quyền sửa bài đăng này"));
                }
                // Seller sửa bài → reset về chờ duyệt lại
                listing.setIsActive(false);
            }
            // Admin/Staff sửa: giữ nguyên isActive

            // ── Patch Listing fields ──────────────────────
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

            // ── Patch Property fields ─────────────────────
            // Lưu ý: Property có thể được dùng bởi nhiều Listing khác (đăng lại),
            // nên việc sửa thông số Property ở đây sẽ ảnh hưởng tới TẤT CẢ Listing
            // liên kết tới Property đó. Đây là hành vi chủ ý (Property là nguồn dữ liệu gốc).
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

                // ── Patch Location ───────────────────────
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

            Listing updated = listingRepository.save(listing);
            log.info("[ListingService] accountId={} đã cập nhật listingId={}", currentUser.getAccountId(), listingId);

            return ResponseEntity.ok(ApiResponse.success(toListingDetail(updated, updated.getProperty()), "Cập nhật bài đăng thành công"));

        } catch (Exception e) {
            log.error("[ListingService] updateListing lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════
    //  Internal Mappers
    // ════════════════════════════════════════════════════

    private ListingDetailResponse toListingDetail(Listing l, Property p) {

        // Ảnh thực tế tài sản (property_image)
        List<PropertyImageResponse> propertyImages = (p == null || p.getPropertyImages() == null)
                ? Collections.emptyList()
                : p.getPropertyImages().stream()
                .sorted(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPropertyImageResponse)
                .collect(Collectors.toList());

        // Thông số Property
        PropertyDetailResponse propertyDetail = null;
        if (p != null) {
            Location loc = p.getLocation();
            propertyDetail = PropertyDetailResponse.builder()
                    .propertyId(p.getPropertyId())
                    .title(p.getTitle())
                    .description(p.getDescription())
                    .area(p.getArea())
                    .price(p.getPrice())
                    .floor(p.getFloor())
                    .bedroom(p.getBedroom())
                    .bathroom(p.getBathroom())
                    .direction(p.getDirection())
                    .propertyTypeName(p.getPropertyType() != null ? p.getPropertyType().getName() : null)
                    .propertyConditionName(p.getPropertyCondition() != null ? p.getPropertyCondition().getName() : null)
                    .latitude(loc != null ? loc.getLatitude() : null)
                    .longitude(loc != null ? loc.getLongitude() : null)
                    .postalCode(loc != null ? loc.getPostalCode() : null)
                    .wardCode(loc != null && loc.getWard() != null ? loc.getWard().getWard_code() : null)
                    .images(propertyImages)
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .build();
        }

        // Thông tin Seller
        Seller seller = l.getSeller();
        Account sellerAccount = seller != null ? seller.getAccount() : null;

        return ListingDetailResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .contactPerson(l.getContactPerson())
                .contactPersonName(l.getContactPersonName())
                .contactPersonPhone(l.getContactPersonPhone())
                .linkSocialContactPerson(l.getLinkSocialContactPerson())
                .viewingDate(l.getViewingDate())
                .startTime(l.getStartTime())
                .endTime(l.getEndTime())
                .isActive(l.getIsActive())
                .property(propertyDetail)
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(sellerAccount != null ? sellerAccount.getFull_name() : null)
                .sellerAvatar(sellerAccount != null ? sellerAccount.getAvatar() : null)
                .sellerPhone(sellerAccount != null ? sellerAccount.getPhone() : null)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private ListingSummaryResponse toListingSummary(Listing l, boolean isFavorited) {
        Property p = l.getProperty();

        String thumbnail = (p == null || p.getPropertyImages() == null || p.getPropertyImages().isEmpty())
                ? null
                : p.getPropertyImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getPropertyImages().stream()
                        .min(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(PropertyImage::getImageUrl)
                        .orElse(null));

        return ListingSummaryResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .price(l.getPrice())
                .area(p != null ? p.getArea() : null)
                .bedroom(p != null ? p.getBedroom() : null)
                .bathroom(p != null ? p.getBathroom() : null)
                .propertyTypeName(p != null && p.getPropertyType() != null ? p.getPropertyType().getName() : null)
                .thumbnailUrl(thumbnail)
                .isActive(l.getIsActive())
                .createdAt(l.getCreatedAt())
                .isFavorited(isFavorited)
                .build();
    }

    private PropertyImageResponse toPropertyImageResponse(PropertyImage img) {
        return PropertyImageResponse.builder()
                .propertyImageId(img.getPropertyImageId())
                .imageUrl(img.getImageUrl())
                .isMain(img.getIsMain())
                .displayOrder(img.getDisplayOrder())
                .build();
    }
}
