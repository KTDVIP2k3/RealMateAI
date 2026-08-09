package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;


import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Location;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.ListingSearchRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Xây dựng Specification<Listing> động dựa trên các filter (đều optional) của
 * POST /listings/search. Tách riêng khỏi ListingServiceImplement để giữ service
 * gọn và dễ unit-test độc lập việc build tiêu chí tìm kiếm.
 *
 * Luôn ép buộc l.isActive = true AND property.isActive = true — API này chỉ
 * dành cho Chợ BĐS công khai (Investor tìm mua), giống hành vi của GET /listings.
 */

public final class ListingSpecification {

    private ListingSpecification() {
    }

    public static Specification<Listing> fromRequest(ListingSearchRequest req) {
        return (root, query, cb) -> {
            // Tránh nhân bản dòng khi JOIN 1-N (propertyImages) do fetch join phía sau
            if (query != null) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            Join<Listing, Property> property = root.join("property", JoinType.INNER);

            predicates.add(cb.isTrue(root.get("isActive")));
            predicates.add(cb.isTrue(property.get("isActive")));

            if (req == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (StringUtils.hasText(req.getKeyword())) {
                String like = "%" + req.getKeyword().trim().toLowerCase() + "%";
                Predicate byTitle = cb.like(cb.lower(root.get("title")), like);
                Predicate byDescription = cb.like(cb.lower(root.get("description")), like);
                Predicate byAddress = cb.like(cb.lower(property.get("addressParticular")), like);
                predicates.add(cb.or(byTitle, byDescription, byAddress));
            }

            if (req.getPropertyTypeId() != null) {
                predicates.add(cb.equal(property.get("propertyType").get("propertyTypeId"), req.getPropertyTypeId()));
            }

            boolean needsLocationJoin = StringUtils.hasText(req.getWardCode())
                    || StringUtils.hasText(req.getProvinceCode())
                    || (req.getMinLat() != null && req.getMaxLat() != null)
                    || (req.getMinLong() != null && req.getMaxLong() != null);

            if (needsLocationJoin) {
                // Chỉ JOIN "ward"/"location" MỘT LẦN DUY NHẤT rồi tái sử dụng cho cả
                // ward/province LẪN bounding box toạ độ bên dưới — tránh JOIN dư thừa
                // tới cùng 1 bảng trong SQL (đã có tiền lệ bug tương tự ở đây trước đó).
                Join<Property, Location> location = property.join("location", JoinType.INNER);

                if (StringUtils.hasText(req.getWardCode()) || StringUtils.hasText(req.getProvinceCode())) {
                    Join<Location, Ward> ward = location.join("ward", JoinType.INNER);
                    if (StringUtils.hasText(req.getWardCode())) {
                        predicates.add(cb.equal(ward.get("ward_code"), req.getWardCode()));
                    }
                    if (StringUtils.hasText(req.getProvinceCode())) {
                        predicates.add(cb.equal(ward.get("province").get("province_code"), req.getProvinceCode()));
                    }
                }

                // MỚI: tìm theo khung toạ độ (bounding box) — chỉ lọc khi có ĐỦ CẢ
                // 2 đầu (min VÀ max) cho từng trục, thiếu 1 trong 2 thì bỏ qua trục
                // đó (không lọc sai ý vì "chỉ có min" không xác định được biên trên).
                if (req.getMinLat() != null && req.getMaxLat() != null) {
                    predicates.add(cb.between(location.get("latitude"),
                            java.math.BigDecimal.valueOf(req.getMinLat()), java.math.BigDecimal.valueOf(req.getMaxLat())));
                }
                if (req.getMinLong() != null && req.getMaxLong() != null) {
                    predicates.add(cb.between(location.get("longitude"),
                            java.math.BigDecimal.valueOf(req.getMinLong()), java.math.BigDecimal.valueOf(req.getMaxLong())));
                }
            }

            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getMaxPrice()));
            }

            if (req.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(property.get("area"), req.getMinArea()));
            }
            if (req.getMaxArea() != null) {
                predicates.add(cb.lessThanOrEqualTo(property.get("area"), req.getMaxArea()));
            }

            if (req.getMinBedroom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(property.get("bedroom"), req.getMinBedroom()));
            }
            if (req.getMinBathroom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(property.get("bathroom"), req.getMinBathroom()));
            }

            if (StringUtils.hasText(req.getDirection())) {
                predicates.add(cb.equal(property.get("direction"), req.getDirection()));
            }

            if (req.getSellerId() != null) {
                predicates.add(cb.equal(root.get("seller").get("sellerId"), req.getSellerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
