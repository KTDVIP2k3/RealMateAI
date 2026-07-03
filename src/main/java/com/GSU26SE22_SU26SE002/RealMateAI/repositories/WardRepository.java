package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, String> {
    @Query("SELECT w FROM Ward w WHERE w.fullName = :wardName AND w.province.fullName = :provinceName")
    Optional<Ward> findByFullNameAndProvinceName(@Param("wardName") String wardName, @Param("provinceName") String provinceName);
}
