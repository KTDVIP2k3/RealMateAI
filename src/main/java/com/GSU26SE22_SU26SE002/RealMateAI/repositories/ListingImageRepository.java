package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Integer> {

    List<ListingImage> findByListing_ListingIdOrderByDisplayOrderAsc(Integer listingId);

    long countByListing_ListingId(Integer listingId);

    /** Ownership check khi Seller muốn đổi thumbnail sang 1 ảnh ĐÃ CÓ SẴN (UpdateListingRequest#thumbnailListingImageId). */
    Optional<ListingImage> findByListingImageIdAndListing_ListingId(Integer listingImageId, Integer listingId);

    /**
     * Bỏ đánh dấu thumbnail của TẤT CẢ ảnh hiện có của 1 Listing — dùng TRƯỚC
     * khi gán thumbnail mới (đảm bảo mỗi Listing luôn có DUY NHẤT 1 thumbnail,
     * dù đổi giữa ảnh cũ hay ảnh mới upload).
     */
    @Modifying
    @Query("UPDATE ListingImage li SET li.isThumbnail = false WHERE li.listing.listingId = :listingId")
    void clearThumbnailByListingId(@Param("listingId") Integer listingId);
}
