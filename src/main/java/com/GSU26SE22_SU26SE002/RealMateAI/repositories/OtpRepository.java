package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpRepository extends JpaRepository<OTP, Integer> {
}