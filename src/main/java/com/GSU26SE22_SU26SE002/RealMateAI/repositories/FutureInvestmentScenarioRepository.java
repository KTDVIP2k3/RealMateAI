package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FutureInvestmentScenarioRepository extends JpaRepository<FutureInvestmentScenario, Integer> {

    List<FutureInvestmentScenario> findByFutureInvestmentPlan_FutureInvestmentPlanId(Integer futureInvestmentPlanId);
}
