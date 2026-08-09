package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ActiveLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ActiveLogRepository extends JpaRepository<ActiveLog, UUID> {

    /** Số lượt xem (VIEW) của 1 listing — dùng cho GET /listings/{listingId}/views. */
    long countByListingIdAndEventType(Integer listingId, UserEventTypeEnum eventType);

    // MỚI: Top listing theo SỐ LƯỢT XEM THẬT (đếm từ ActiveLog, KHÔNG dùng cột
    // Listing.viewCount cũ — cột đó có method incrementViewCount() sẵn nhưng
    // CHƯA TỪNG được gọi ở đâu trong hệ thống, luôn = 0, không phản ánh đúng
    // thực tế). Dùng cho GET /listings/featured ("tin nổi bật"). Chỉ tính tin
    // đang APPROVED + active (không đề xuất tin ẩn/chưa duyệt/đã hết hạn).
    @Query(value = """
            SELECT al.listingId AS listingId, COUNT(al) AS viewCount
            FROM ActiveLog al
            JOIN Listing l ON l.listingId = al.listingId
            JOIN l.listingVerification lv
            WHERE al.eventType = :eventType
              AND al.listingId IS NOT NULL
              AND l.isActive = true
              AND l.status = com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum.ACTIVE
              AND lv.status = com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum.APPROVED
            GROUP BY al.listingId
            ORDER BY COUNT(al) DESC
            """)
    Page<FeaturedListingProjection> findFeaturedListingIds(@Param("eventType") UserEventTypeEnum eventType, Pageable pageable);

    /**
     * Danh sách listing 1 account đã từng xem — GỘP theo listingId (1 người
     * xem đi xem lại 1 tin chỉ tính 1 dòng, kèm lần xem gần nhất + tổng số
     * lần xem), sắp xếp mới nhất trước. Dùng cho GET /account/viewed-listings.
     * Không JOIN FETCH entity nào (chỉ SELECT scalar/aggregate) nên phân
     * trang trực tiếp AN TOÀN, không dính lỗi nhân bản dòng như JOIN FETCH
     * collection.
     */
    @Query(value = """
            SELECT al.listingId AS listingId, MAX(al.createdAt) AS lastViewedAt, COUNT(al) AS viewCount
            FROM ActiveLog al JOIN al.auditLog a
            WHERE a.account.accountId = :accountId
              AND al.eventType = :eventType
              AND al.listingId IS NOT NULL
            GROUP BY al.listingId
            ORDER BY MAX(al.createdAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT al.listingId) FROM ActiveLog al JOIN al.auditLog a
            WHERE a.account.accountId = :accountId
              AND al.eventType = :eventType
              AND al.listingId IS NOT NULL
            """)
    Page<ViewedListingProjection> findViewedListingsByAccount(@Param("accountId") Integer accountId,
                                                              @Param("eventType") UserEventTypeEnum eventType,
                                                              Pageable pageable);
}