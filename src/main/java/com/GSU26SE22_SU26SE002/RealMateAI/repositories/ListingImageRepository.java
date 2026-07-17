package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Integer> {

    List<ListingImage> findByListing_ListingIdOrderByDisplayOrderAsc(Integer listingId);

    long countByListing_ListingId(Integer listingId);
}
