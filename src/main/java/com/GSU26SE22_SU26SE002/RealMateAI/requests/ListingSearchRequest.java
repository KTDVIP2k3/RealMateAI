package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingSortEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * POST /listings/search — Tìm kiếm nâng cao tin đăng công khai (Chợ BĐS).
 * Dùng POST thay vì GET query string vì số lượng filter tương đối nhiều và
 * có thể mở rộng thêm (mảng, object) trong tương lai mà không phá vỡ contract.
 * Tất cả field đều optional — không truyền field nào tương đương GET /listings.
 */

@Data
public class ListingSearchRequest {

    @Schema(example = "căn hộ view sông", description = "Từ khoá tìm trong tiêu đề / mô tả / địa chỉ")
    private String keyword;

    @Schema(example = "1")
    private Integer propertyTypeId;

    @Schema(example = "26734")
    private String wardCode;

    @Schema(example = "79")
    private String provinceCode;

    @Schema(example = "2000000000")
    private Long minPrice;

    @Schema(example = "5000000000")
    private Long maxPrice;

    @Schema(example = "50")
    private Double minArea;

    @Schema(example = "120")
    private Double maxArea;

    @Schema(example = "2", description = "Số phòng ngủ tối thiểu")
    private Integer minBedroom;

    @Schema(example = "1", description = "Số phòng tắm tối thiểu")
    private Integer minBathroom;

    @Schema(example = "Đông Nam")
    private String direction;

    @Schema(example = "1", description = "Lọc theo Seller cụ thể (sellerId)")
    private Integer sellerId;

    @Schema(example = "10.75", description = "Vĩ độ nhỏ nhất của khung bản đồ")
    private Double minLat;

    @Schema(example = "10.85", description = "Vĩ độ lớn nhất của khung bản đồ")
    private Double maxLat;

    @Schema(example = "106.60", description = "Kinh độ nhỏ nhất của khung bản đồ")
    private Double minLong;

    @Schema(example = "106.75", description = "Kinh độ lớn nhất của khung bản đồ")
    private Double maxLong;

    @Schema(defaultValue = "NEWEST")
    private ListingSortEnum sortBy;

    @Schema(defaultValue = "0")
    private Integer page;

    @Schema(defaultValue = "10")
    private Integer size;
}
