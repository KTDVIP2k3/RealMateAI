package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {

    /**
     * BĐS — chỉ lấy bài đã duyệt (isActive=true), phân trang.
     * JOIN FETCH property + propertyType + location + propertyImages, tránh N+1.
     */
    @Query(value = """
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH p.propertyImages pi
            WHERE l.isActive = true
            ORDER BY l.createdAt DESC
            """,
            countQuery = "SELECT COUNT(l) FROM Listing l WHERE l.isActive = true")
    Page<Listing> findAllActiveWithDetails(Pageable pageable);

    /**
     * Chi tiết 1 bài đăng công khai — JOIN FETCH toàn bộ liên kết cần thiết.
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH p.propertyImages pi
            LEFT JOIN FETCH l.seller s
            LEFT JOIN FETCH s.account a
            WHERE l.listingId = :listingId AND l.isActive = true
            """)
    Optional<Listing> findActiveById(@Param("listingId") Integer listingId);

    /**
     * Seller xem tất cả bài đăng của mình — bao gồm cả chưa duyệt.
     * Vì 1 Property có thể được đăng lại bởi nhiều Listing,
     * propertyImages luôn lấy từ property (không phụ thuộc listing).
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyImages pi
            WHERE l.seller.sellerId = :sellerId
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findBySellerId(@Param("sellerId") Integer sellerId);

    /**
     * Lấy chi tiết Listing kèm Property + Location (dùng cho update / ownership check).
     */
    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.propertyImages pi
            WHERE l.listingId = :listingId
            """)
    Optional<Listing> findByIdWithDetails(@Param("listingId") Integer listingId);
}
