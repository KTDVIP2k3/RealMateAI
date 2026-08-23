package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfile;
import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface InvestmentProfileVersionRepository extends JpaRepository<InvestmentProfileVersion, Integer> {

    List<InvestmentProfileVersion> findByBaseVersion_ProfileVersionIdOrderByCreatedAtDesc(Integer baseVersionId);
    long countByInvestmentProfile_InvestmentProfileId(Integer investmentProfileId);
}
