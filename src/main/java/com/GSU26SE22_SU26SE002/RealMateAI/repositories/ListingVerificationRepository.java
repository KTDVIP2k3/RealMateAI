package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingVerificationRepository extends JpaRepository<ListingVerification, Integer> {
    /**
     * Lịch sử duyệt của 1 Listing, mới nhất trước — 1 Listing có thể bị
     * REJECTED rồi Seller sửa lại, tạo ra PENDING mới, nên cần xem theo thời gian.
     */
    List<ListingVerification> findByListing_ListingIdOrderByListingVerificationIdDesc(Integer listingId);

    /**
     * Bản ghi duyệt mới nhất của 1 Listing (nếu có) — dùng để kiểm tra
     * "listing này đang ở trạng thái PENDING hay đã APPROVED/REJECTED rồi".
     */
    @Query("""
            SELECT lv FROM ListingVerification lv
            WHERE lv.listing.listingId = :listingId
            ORDER BY lv.listingVerificationId DESC
            LIMIT 1
            """)
    Optional<ListingVerification> findLatestByListingId(@Param("listingId") Integer listingId);

    /**
     * Hàng đợi duyệt cho Staff/Admin — toàn bộ Listing đang PENDING,
     * JOIN FETCH property + propertyImages để Staff xem được cả nội dung và ảnh
     * ngay trong 1 lần load (không cần gọi thêm API khác để duyệt ảnh riêng).
     */
    @Query("""
            SELECT lv FROM ListingVerification lv
            JOIN FETCH lv.listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyImages pi
            LEFT JOIN FETCH p.propertyType pt
            WHERE lv.status = :status
            ORDER BY lv.listingVerificationId ASC
            """)
    List<ListingVerification> findPendingQueue(@Param("status") ListingStatusEnum status);
}
