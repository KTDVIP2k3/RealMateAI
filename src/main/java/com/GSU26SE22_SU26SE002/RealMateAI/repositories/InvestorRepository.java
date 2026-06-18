package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface InvestorRepository extends JpaRepository<Investor, Integer> {

    Optional<Investor> findByAccount_AccountId(Integer accountId);

}
