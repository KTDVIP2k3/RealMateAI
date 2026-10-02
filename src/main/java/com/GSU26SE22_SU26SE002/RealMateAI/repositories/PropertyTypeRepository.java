package com.GSU26SE22_SU26SE002.RealMateAI.repositories;


import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyType;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyTypeRepository extends JpaRepository<PropertyType, Integer> {

    // SỬA: đổi từ derived query ("Containing") sang @Query tường minh có
    // ESCAPE '\' — xem giải thích đầy đủ ở ProvinceRepository (cùng lý do).
    // Giữ NGUYÊN chữ ký hàm công khai cũ (default method) để không đổi code
    // đang gọi nó.
    @Query("SELECT pt FROM PropertyType pt WHERE pt.isActive = true "
            + "AND LOWER(pt.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\'")
    java.util.List<PropertyType> findTop5ByIsActiveTrueAndNameContainingEscaped(
            @Param("name") String name, Pageable pageable);

    default java.util.List<PropertyType> findTop5ByIsActiveTrueAndNameContainingIgnoreCase(String name) {
        return findTop5ByIsActiveTrueAndNameContainingEscaped(name, PageRequest.of(0, 5));
    }
}
