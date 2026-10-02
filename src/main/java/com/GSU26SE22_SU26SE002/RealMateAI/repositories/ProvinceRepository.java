package com.GSU26SE22_SU26SE002.RealMateAI.repositories;


import com.GSU26SE22_SU26SE002.RealMateAI.model.Province;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, String> {

    @Query("SELECT p FROM Province p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\' "
            + "OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')) ESCAPE '\\'")
    java.util.List<Province> findTop5ByNameOrFullNameContainingEscaped(
            @Param("name") String name, @Param("fullName") String fullName, Pageable pageable);

    default java.util.List<Province> findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(String name, String fullName) {
        return findTop5ByNameOrFullNameContainingEscaped(name, fullName, PageRequest.of(0, 5));
    }
}