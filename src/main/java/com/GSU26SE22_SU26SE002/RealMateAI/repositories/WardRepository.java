package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
    @Query("SELECT w FROM Ward w WHERE w.fullName = :wardName AND w.province.fullName = :provinceName")
    Optional<Ward> findByFullNameAndProvinceName(@Param("wardName") String wardName, @Param("provinceName") String provinceName);

    @Query("SELECT w FROM Ward w LEFT JOIN FETCH w.province")
    List<Ward> findAllWithProvince();

    @Query("SELECT w FROM Ward w LEFT JOIN FETCH w.province WHERE w.province.province_code IN :provinceCodes")
    List<Ward> findWardsBySpecificProvinces(@Param("provinceCodes") List<String> provinceCodes);

    @Query("SELECT w FROM Ward w LEFT JOIN FETCH w.province WHERE w.province.province_code = '79'")
    List<Ward> findWardsOnlyInHCM();

    @Query("SELECT w FROM Ward w WHERE LOWER(w.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\' "
            + "OR LOWER(w.fullName) LIKE LOWER(CONCAT('%', :fullName, '%')) ESCAPE '\\'")
    List<Ward> findTop5ByNameOrFullNameContainingEscaped(
            @Param("name") String name, @Param("fullName") String fullName, Pageable pageable);

    /** MỚI: Gợi ý Location (nhóm Phường/Xã) cho GET /listings/search/suggestions. */
    default List<Ward> findTop5ByNameContainingIgnoreCaseOrFullNameContainingIgnoreCase(String name, String fullName) {
        return findTop5ByNameOrFullNameContainingEscaped(name, fullName, PageRequest.of(0, 5));
    }

}
