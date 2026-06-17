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
}
