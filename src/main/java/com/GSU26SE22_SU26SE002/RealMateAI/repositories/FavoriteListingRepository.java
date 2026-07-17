package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.FavoriteListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteListingRepository extends JpaRepository<FavoriteListing, Integer> {
    boolean existsByInvestor_InvestorIdAndListing_ListingId(Integer investorId, Integer listingId);

    /** Tìm 1 favorite để xóa, đảm bảo thuộc về investor hiện tại (IDOR protection) */
    Optional<FavoriteListing> findByFavoriteListingIdAndInvestor_InvestorId(
            Integer favoriteListingId, Integer investorId);

    /**
     * Lấy danh sách yêu thích của 1 investor, JOIN FETCH để tránh N+1
     * khi map sang ListingSummaryResponse (cần property, propertyType).
     * listingImages KHÔNG fetch join ở đây — join 1-N này từng làm nhân bản
     * dòng FavoriteListing (và cần thêm DISTINCT, khiến Postgres lỗi
     * "could not identify an equality operator for type json" vì Property
     * có 2 cột kiểu json). Ảnh được load lazy theo batch (@BatchSize trên
     * Listing#listingImages) khi FavoriteListingServiceImplement truy cập
     * l.getListingImages() để tính thumbnail.
     */
    @Query("""
            SELECT fl FROM FavoriteListing fl
            JOIN FETCH fl.listing l
            JOIN FETCH l.property p
            LEFT JOIN FETCH p.propertyType pt
            WHERE fl.investor.investorId = :investorId
            ORDER BY fl.createdAt DESC
            """)
    List<FavoriteListing> findByInvestorIdWithDetails(@Param("investorId") Integer investorId);

    /**
     * Tập hợp listingId mà investor đã yêu thích — dùng để đánh dấu isFavorited
     * trên Chợ BĐS mà không cần N query.
     */
    @Query("SELECT fl.listing.listingId FROM FavoriteListing fl WHERE fl.investor.investorId = :investorId")
    List<Integer> findFavoritedListingIdsByInvestorId(@Param("investorId") Integer investorId);
}
