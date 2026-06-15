package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {
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
