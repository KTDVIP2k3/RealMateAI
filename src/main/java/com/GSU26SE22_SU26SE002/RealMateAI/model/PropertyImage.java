package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import jakarta.persistence.*;
import lombok.*;

/**
 * @deprecated LEGACY — ảnh nay được lưu theo Listing (xem {@link ListingImage}).
 * Bảng property_image / entity này được GIỮ LẠI chỉ để không phá vỡ dữ liệu
 * lịch sử đã có sẵn trước khi migrate; entity KHÔNG còn được Property tham
 * chiếu (đã bỏ quan hệ @OneToMany trong Property) và KHÔNG được dùng ở bất kỳ
 * luồng nghiệp vụ mới nào. Toàn bộ dữ liệu cũ đã được migrate sang
 * listing_image (xem migration V_listing_image_and_seller_status.sql).
 */
@Deprecated
@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "property_image")
public class PropertyImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_image_id")
    private Integer propertyImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    private Boolean isMain;
    private Integer displayOrder;
}
