package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface CrawPropertyListingRepository extends JpaRepository<CrawPropertyListing, Integer> {
    @Query("SELECT COUNT(c) FROM CrawPropertyListing c WHERE FUNCTION('DATE', c.craw_date) = :today")
    int countByCrawDateToday(@Param("today") LocalDate today);
}
