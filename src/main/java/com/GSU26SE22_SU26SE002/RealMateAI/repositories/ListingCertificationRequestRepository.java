package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingCertificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingCertificationRequestRepository extends JpaRepository<ListingCertificationRequest, Integer> {

    boolean existsByListing_ListingIdAndStatus(Integer listingId, CertificationStatusEnum status);

    @Query("""
            SELECT r FROM ListingCertificationRequest r
            JOIN FETCH r.listing l
            WHERE r.seller.sellerId = :sellerId
            ORDER BY r.createdAt DESC
            """)
    List<ListingCertificationRequest> findBySellerIdWithDetails(@Param("sellerId") Integer sellerId);

    @Query("""
            SELECT r FROM ListingCertificationRequest r
            JOIN FETCH r.listing l
            WHERE r.certificationRequestId = :id AND r.seller.sellerId = :sellerId
            """)
    Optional<ListingCertificationRequest> findByIdAndSellerIdWithDetails(@Param("id") Integer id,
                                                                         @Param("sellerId") Integer sellerId);

    @Query("""
            SELECT r FROM ListingCertificationRequest r
            JOIN FETCH r.listing l
            JOIN FETCH r.seller s
            WHERE r.status = :status
            ORDER BY r.createdAt ASC
            """)
    List<ListingCertificationRequest> findByStatusWithDetails(@Param("status") CertificationStatusEnum status);

    @Query("""
            SELECT r FROM ListingCertificationRequest r
            JOIN FETCH r.listing l
            JOIN FETCH r.seller s
            WHERE r.certificationRequestId = :id
            """)
    Optional<ListingCertificationRequest> findByIdWithDetails(@Param("id") Integer id);
}
