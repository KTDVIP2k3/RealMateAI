package com.GSU26SE22_SU26SE002.RealMateAI.repositories;


import com.GSU26SE22_SU26SE002.RealMateAI.model.FutureExecutionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FutureExecutionPlanRepository extends JpaRepository<FutureExecutionPlan, Integer> {

    List<FutureExecutionPlan> findByFutureInvestmentPlan_FutureInvestmentPlanId(Integer futureInvestmentPlanId);
}
