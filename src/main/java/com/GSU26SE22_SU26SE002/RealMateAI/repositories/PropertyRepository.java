package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Integer> {

    /**
     * Danh sách tài sản mà 1 Seller đang sở hữu — dùng cho:
     *  - GET /api/v1/seller/properties (màn hình chọn "đăng lại tài sản đã có")
     * JOIN FETCH propertyType, propertyCondition, location.ward, propertyImages
     * để tránh N+1 khi map sang PropertyDetailResponse.
     */
    @Query(value = """
            SELECT p FROM Property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH loc.ward w
            WHERE p.seller.sellerId = :sellerId
            ORDER BY p.createdAt DESC NULLS LAST    
            """,
            countQuery = "SELECT COUNT(p) FROM Property p WHERE p.seller.sellerId = :sellerId")
    Page<Property> findBySellerIdWithDetails(@Param("sellerId") Integer sellerId, Pageable pageable);

    /**
     * Lấy 1 Property kèm đầy đủ thông tin liên kết — dùng khi cần kiểm tra
     * ownership + hiển thị chi tiết (vd trước khi re-parent ảnh draft).
     */
    @Query("""
            SELECT p FROM Property p
            LEFT JOIN FETCH p.propertyType pt
            LEFT JOIN FETCH p.propertyCondition pc
            LEFT JOIN FETCH p.location loc
            LEFT JOIN FETCH loc.ward w
            WHERE p.propertyId = :propertyId
            """)
    Optional<Property> findByIdWithDetails(@Param("propertyId") Integer propertyId);
}