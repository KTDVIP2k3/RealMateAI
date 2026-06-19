package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfile;
import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentProfileVersionRepository extends JpaRepository<InvestmentProfileVersion, Integer> {
}
