package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByPublicId(String publicId);

    List<MediaAsset> findByEntityTypeAndEntityIdAndIsActiveTrue(
            EntityType entityType, Long entityId);

    List<MediaAsset> findByAccount_AccountIdAndIsActiveTrue(Integer accountId);

    @Modifying
    @Query("UPDATE MediaAsset m SET m.isActive = false WHERE m.publicId = :publicId")
    int softDeleteByPublicId(@Param("publicId") String publicId);

    @Query("SELECT m FROM MediaAsset m WHERE m.entityType = :type AND m.entityId = :id AND m.isActive = true ORDER BY m.uploadedAt DESC")
    List<MediaAsset> findActiveByEntity(
            @Param("type") EntityType type,
            @Param("id")   Long id);

    /**
     * ATOMIC CLAIM — giải quyết race condition khi Seller mở nhiều tab /
     * gửi nhiều request POST /listings gần như đồng thời, mỗi request đều
     * cố gắng re-parent CÙNG 1 publicId draft vào Property khác nhau.
     *
     * Đây là 1 câu UPDATE có điều kiện ngay trong WHERE — khác với cách
     * "đọc rồi ghi" (findByPublicId() rồi set field rồi save()) ở tầng
     * application, kiểu đó luôn có khoảng hở giữa đọc và ghi: 2 transaction
     * có thể cùng đọc thấy "còn là draft" trước khi cả 2 kịp ghi, dẫn tới
     * 1 trong 2 tin đăng bị "cướp mất" ảnh mà không có lỗi nào báo ra.
     *
     * Với UPDATE trực tiếp ở DB, PostgreSQL tự đảm bảo tính nguyên tử ở
     * mức row-lock: chỉ DUY NHẤT 1 transaction được phép cập nhật thành công
     * (khớp điều kiện entity_type='ACCOUNT' AND entity_id=:accountId), giao
     * dịch thứ 2 chạy ngay sau đó sẽ thấy điều kiện đã không còn đúng
     * (entity_type đã đổi thành 'PROPERTY' bởi giao dịch đầu tiên) và trả
     * về 0 dòng bị ảnh hưởng — Service dựa vào số này để biết ảnh đã bị
     * tin đăng khác "giành" trước.
     *
     * @return số dòng được cập nhật — 1 nếu claim thành công, 0 nếu ảnh đã
     *         bị claim trước đó (bởi tin đăng khác) hoặc không thuộc account này.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE MediaAsset m
            SET m.entityType = :newEntityType, m.entityId = :newEntityId
            WHERE m.publicId = :publicId
              AND m.account.accountId = :accountId
              AND m.entityType = com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType.ACCOUNT
              AND m.entityId = :accountIdAsLong
              AND m.isActive = true
            """)
    int claimDraftAsset(@Param("publicId") String publicId,
                        @Param("accountId") Integer accountId,
                        @Param("accountIdAsLong") Long accountIdAsLong,
                        @Param("newEntityType") EntityType newEntityType,
                        @Param("newEntityId") Long newEntityId);
}
