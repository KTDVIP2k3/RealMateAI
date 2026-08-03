package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {

    /** MỚI: dùng để tìm/tạo Portfolio placeholder "Chưa phân loại" — xem InvestmentFuturePlanServiceImplement. */
    java.util.Optional<Portfolio> findFirstByName(String name);
}
