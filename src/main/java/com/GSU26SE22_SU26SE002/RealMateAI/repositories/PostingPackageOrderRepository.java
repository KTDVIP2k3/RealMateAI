package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostingPackageOrderRepository extends JpaRepository<PostingPackageOrder, Integer> {

    List<PostingPackageOrder> findByListing_ListingIdAndStartDateIsNull(Integer listingId);
}
