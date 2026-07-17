package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @deprecated LEGACY — xem {@link com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingImageRepository}.
 * Giữ lại chỉ để tương thích dữ liệu lịch sử, KHÔNG dùng trong luồng nghiệp vụ mới.
 */
@Deprecated
@Repository
public interface PropertyImageRepository extends JpaRepository<PropertyImage, Integer> {
    List<PropertyImage> findByProperty_PropertyIdOrderByDisplayOrderAsc(Integer propertyId);

    long countByProperty_PropertyId(Integer propertyId);
}
