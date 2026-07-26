package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FutureInvestmentPortfolioRepository extends JpaRepository<FutureInvestmentPortfolio, Integer> {

    List<FutureInvestmentPortfolio> findByFutureInvestmentPlan_FutureInvestmentPlanId(Integer futureInvestmentPlanId);
}
