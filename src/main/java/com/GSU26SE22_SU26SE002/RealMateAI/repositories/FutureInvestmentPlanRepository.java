package com.GSU26SE22_SU26SE002.RealMateAI.repositories;
import com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FutureInvestmentPlanRepository extends JpaRepository<FutureInvestmentPlan, Integer> {

    /** Danh sách future-plan sinh ra từ 1 version gốc — dùng cho GET /investment-plans/future/by-source/{sourceVersionId}. */
    List<FutureInvestmentPlan> findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(Integer sourceVersionId);

    /** Đếm số future-plan đã có của CÙNG 1 investment profile — dùng để tự sinh tên "Kết quả dự đoán N". */
    long countByInvestmentProfile_InvestmentProfileId(Integer investmentProfileId);
}
