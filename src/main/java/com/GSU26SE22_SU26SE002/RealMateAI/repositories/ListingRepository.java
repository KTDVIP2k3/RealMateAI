package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository

public interface ListingRepository extends JpaRepository<Listing, Integer>, JpaSpecificationExecutor<Listing> {

    /**
     * KHÔNG override findAll(Specification, Pageable) nữa (trước đây có gắn thêm
     * @EntityGraph fetch "property.propertyImages" — 1 collection @OneToMany kết hợp
     * Pageable khiến Hibernate phải phân trang TRONG MEMORY thay vì LIMIT/OFFSET ở DB,
     * rất chậm khi dữ liệu lớn dần).
     * Dùng lại method mặc định của JpaSpecificationExecutor: chỉ trả Listing "trần"
     * (không kèm collection) nhưng phân trang CHUẨN ở tầng DB — dùng làm QUERY 1 (lấy
     * ID đã phân trang) trong pattern 2-query cùng với {@link #findAllByListingIdInWithDetails}.
     * Xem cách dùng đầy đủ trong TwoStepPaginationUtil + ListingServiceImplement#searchListings.
     */

    /**
     * Tin đăng tương đồng đang hoạt động (cùng loại BĐS + cùng phường/xã) — dùng làm
     * dữ liệu thị trường tham chiếu cho POST /listings/price-suggestion.
     * Chỉ cần price + area của Property nên JOIN FETCH tối thiểu, không kéo ảnh/mô tả.
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            WHERE l.isActive = true
              AND p.isActive = true
              AND p.propertyType.propertyTypeId = :propertyTypeId
              AND p.location.ward.ward_code = :wardCode
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findComparableActiveListings(@Param("propertyTypeId") Integer propertyTypeId,
                                               @Param("wardCode") String wardCode);

    /**
     * QUERY 1 của pattern 2-query cho GET /listings (getMarketListings) — chỉ trả Listing
     * "trần" (không JOIN FETCH collection nào), để Hibernate phân trang CHUẨN ở tầng DB
     * bằng LIMIT/OFFSET. Kết hợp với {@link #findAllByListingIdInWithDetails} (QUERY 2)
     * qua {@code TwoStepPaginationUtil.paginate(...)}.
     */
    Page<Listing> findByIsActiveTrue(Pageable pageable);

    /**
     * QUERY 2 DÙNG CHUNG của pattern 2-query — fetch chi tiết đầy đủ (property,
     * propertyType, location) cho ĐÚNG danh sách listingId đã phân trang sẵn ở
     * QUERY 1 (findByIsActiveTrue hoặc findAll(Specification, Pageable)).
     * KHÔNG JOIN FETCH listingImages (xem lý do ở Listing#listingImages —
     * @BatchSize đảm nhiệm việc load ảnh theo batch, KHÔNG dùng JOIN FETCH vì
     * sẽ cần DISTINCT, mà DISTINCT trên các cột json (property_attribute,
     * property_purpose) khiến Postgres báo lỗi "could not identify an
     * equality operator for type json" — đây chính là nguyên nhân lỗi 500 ở
     * GET /listings trước khi sửa). property/propertyType/location đều là
     * quan hệ *ToOne nên JOIN FETCH các quan hệ này không làm nhân bản dòng,
     * không cần DISTINCT.
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.location loc
            WHERE l.listingId IN :ids
            """)
    List<Listing> findAllByListingIdInWithDetails(@Param("ids") List<Integer> ids);

    /**
     * Chi tiết 1 bài đăng công khai — JOIN FETCH toàn bộ liên kết *ToOne cần
     * thiết. listingImages KHÔNG fetch join ở đây (xem giải thích ở
     * {@link #findAllByListingIdInWithDetails}) — sẽ được load lazy theo
     * batch (@BatchSize) khi service/mapper truy cập l.getListingImages().
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH l.seller s
            LEFT JOIN FETCH s.account a
            WHERE l.listingId = :listingId AND l.isActive = true
            """)
    Optional<Listing> findActiveById(@Param("listingId") Integer listingId);

    /**
     * Seller xem tất cả bài đăng của mình — bao gồm cả chưa duyệt và đang ẩn (HIDDEN).
     * KHÔNG bao gồm bài đăng đã bị xoá mềm vĩnh viễn (status = DELETED) — 1 khi đã
     * DELETE thì tin đăng biến mất hoàn toàn khỏi danh sách quản lý của Seller, kể cả
     * chính chủ sở hữu cũng không còn thấy/sửa/dùng lại được nữa.
     * Vì 1 Property có thể được đăng lại bởi nhiều Listing, ảnh luôn lấy từ
     * chính Listing đó (listingImages, load lazy theo batch — không JOIN
     * FETCH ở đây, xem giải thích ở {@link #findAllByListingIdInWithDetails}).
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH l.listingVerification lv
            WHERE l.seller.sellerId = :sellerId
              AND (l.status IS NULL OR l.status <> com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.DELETED)
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * Lấy chi tiết Listing kèm Property + Location (dùng cho update / ownership check).
     * Bao gồm cả DELETED — service layer (ListingServiceImplement#updateListing) sẽ tự
     * kiểm tra status = DELETED và chặn thao tác sửa nếu cần (dùng cho path Admin/Staff
     * hoặc PUT /listings/{id} không lọc theo seller).
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH l.listingVerification lv
            WHERE l.listingId = :listingId
            """)
    Optional<Listing> findByIdWithDetails(@Param("listingId") Integer listingId);
    /**
     * Seller xem chi tiết 1 listing của chính mình (kể cả chưa duyệt, kể cả HIDDEN).
     * Ownership được xác thực ngay trong query (AND l.seller.sellerId = :sellerId).
     * KHÔNG trả về bài đăng đã DELETED — xem giải thích ở {@link #findBySellerId}.
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH l.seller s
            LEFT JOIN FETCH s.account a
            LEFT JOIN FETCH l.listingVerification lv
            WHERE l.listingId = :listingId
              AND l.seller.sellerId = :sellerId
              AND (l.status IS NULL OR l.status <> com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.DELETED)
            """)
    Optional<Listing> findByIdAndSellerId(@Param("listingId") Integer listingId,
                                          @Param("sellerId") Integer sellerId);

    /**
     * Các Listing KHÁC (không tính chính nó) đang tham chiếu tới cùng 1 Property
     * — dùng để COPY lại ảnh khi Seller đăng lại tài sản đã có (luồng ①:
     * createListingWithExistingProperty) mà không upload ảnh mới. listingImages
     * KHÔNG fetch join (xem giải thích ở {@link #findAllByListingIdInWithDetails}) —
     * truy cập l.getListingImages() trong service sẽ tự batch-load lazy.
     * Sắp xếp mới nhất trước để ưu tiên bộ ảnh gần đây nhất.
     */
    @Query("""
            SELECT l FROM Listing l
            WHERE l.property.propertyId = :propertyId
              AND l.listingId <> :excludeListingId
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findOtherListingsOfPropertyWithImages(@Param("propertyId") Integer propertyId,
                                                        @Param("excludeListingId") Integer excludeListingId);




    /**
     * Đếm số Listing (mọi trạng thái) hiện đang tham chiếu tới 1 Property.
     * Dùng để hiển thị "tài sản này đã được đăng N lần" trong GET /seller/properties.
     */
    long countByProperty_PropertyId(Integer propertyId);
    @Query("SELECT l FROM Listing l " +
            "JOIN l.property p " +
            "JOIN p.propertyCondition pc " +
            "JOIN pc.propertyType pt " +
            "JOIN p.location loc " +
            "JOIN loc.ward w " +
            "WHERE (w.name = :ward OR :ward IS NULL) " +
            "AND pt.propertyTypeId = :propertyTypeId " +
            "AND l.price <= :maxPrice " +
            "AND l.isActive = true " +
            "AND p.isActive = true " +
            "ORDER BY l.price DESC")
    List<Listing> findRealPropertiesByAiStrategy(
            @Param("ward") String ward,
            @Param("propertyTypeId") Integer propertyTypeId,
            @Param("maxPrice") Long maxPrice
    );
}
