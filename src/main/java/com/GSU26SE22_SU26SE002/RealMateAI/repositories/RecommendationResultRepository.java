package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    /** Top-N gợi ý của 1 user, đã sắp xếp sẵn theo rank tăng dần (rank=1 là gợi ý tốt nhất). */
    List<RecommendationResult> findByAccountIdOrderByRankAsc(Integer accountId);
}
