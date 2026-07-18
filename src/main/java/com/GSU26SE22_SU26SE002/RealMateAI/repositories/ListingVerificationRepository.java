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
     * Lấy verification HIỆN TẠI (chỉ 1 record duy nhất)
     */
    Optional<ListingVerification> findByListing_ListingId(Integer listingId);

    /**
     * Hàng đợi duyệt cho Staff/Admin. listingImages KHÔNG fetch join (từng
     * gây nhân bản dòng ListingVerification + lỗi Postgres "could not
     * identify an equality operator for type json" khi cần thêm DISTINCT do
     * Property có 2 cột kiểu json) — ảnh được load lazy theo batch
     * (@BatchSize trên Listing#listingImages) khi service truy cập.
     */
    @Query("""
            SELECT lv FROM ListingVerification lv
            JOIN FETCH lv.listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            WHERE lv.status = :status
            ORDER BY lv.listingVerificationId ASC
            """)
    List<ListingVerification> findPendingQueue(@Param("status") ListingStatusEnum status);
}