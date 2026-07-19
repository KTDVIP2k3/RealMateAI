package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyValuationRepository extends JpaRepository<PropertyValuation, Integer> {

    boolean existsByProperty_PropertyIdAndPropertyValuationStatus(Integer propertyId, PropertyValuationStatusEnum status);

    @Query("""
            SELECT pv FROM PropertyValuation pv
            JOIN FETCH pv.property p
            WHERE p.seller.sellerId = :sellerId
            ORDER BY pv.createdAt DESC
            """)
    List<PropertyValuation> findBySellerIdWithDetails(@Param("sellerId") Integer sellerId);

    @Query("""
            SELECT pv FROM PropertyValuation pv
            JOIN FETCH pv.property p
            WHERE pv.propertyValuationId = :id AND p.seller.sellerId = :sellerId
            """)
    Optional<PropertyValuation> findByIdAndSellerIdWithDetails(@Param("id") Integer id,
                                                               @Param("sellerId") Integer sellerId);

    @Query("""
            SELECT pv FROM PropertyValuation pv
            JOIN FETCH pv.property p
            JOIN FETCH p.seller s
            WHERE pv.propertyValuationStatus = :status
            ORDER BY pv.createdAt ASC
            """)
    List<PropertyValuation> findByStatusWithDetails(@Param("status") PropertyValuationStatusEnum status);

    @Query("""
            SELECT pv FROM PropertyValuation pv
            JOIN FETCH pv.property p
            JOIN FETCH p.seller s
            WHERE pv.propertyValuationId = :id
            """)
    Optional<PropertyValuation> findByIdWithDetails(@Param("id") Integer id);
}
