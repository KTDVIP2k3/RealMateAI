package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PortfolioAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioAllocationRepository extends JpaRepository<PortfolioAllocation, Integer> {
    List<PortfolioAllocation> findByInvestmentPortfolio_InvestmentPortfolioId(Integer investmentPortfolioId);
}