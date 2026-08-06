package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/heatmap")
@Tag(name = "Heatmap Zone", description = "Heatmap: Quản lý và truy vấn dữ liệu bản đồ nhiệt giá bất động sản")
public class HeatmapZoneController {

    @Autowired
    private HeatmapZoneServiceInterface heatmapZoneService;

    @GetMapping("/zones/test")
    @Operation(summary = "[Test Only]. Lấy danh sách các ô Grid Heatmap theo khung nhìn (Viewport) và cấp độ Zoom")
    public ResponseEntity<List<HeatmapZone>> getHeatmapZones(
            @RequestParam Integer zoom,
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLong,
            @RequestParam BigDecimal maxLong) {

        List<HeatmapZone> result = heatmapZoneService.getHeatmapZonesByViewport(
                zoom, minLat, maxLat, minLong, maxLong
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/zones")
    @Operation(summary = "Lấy danh sách các ô Grid Heatmap theo khung nhìn (Viewport) bỏ qua cấp độ Zoom")
    public ResponseEntity<List<HeatmapZone>> getHeatmapZonesV2(
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLong,
            @RequestParam BigDecimal maxLong) {

        List<HeatmapZone> result = heatmapZoneService.getHeatmapZonesByViewportV2(
                minLat, maxLat, minLong, maxLong
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/trigger-snapshot")
    @Operation(summary = "[Test Only]. Admin/System: Kích hoạt thủ công tiến trình gom nhóm dữ liệu thô và tính toán Heatmap Snapshot")
    public ResponseEntity<String> triggerSnapshot() {
        heatmapZoneService.generateDailySnapshot();
        return ResponseEntity.ok("Trigger snapshot successfully!");
    }

    @GetMapping("/listing")
    @Operation(summary = "Lấy danh sách bài đăng (Listing) theo heatmap có phân trang")
    public ResponseEntity<ApiResponse> getListingsByViewportPaged(
            @Parameter(description = "Vĩ độ nhỏ nhất (Bottom-Left Lat)", example = "10.7000000")
            @RequestParam BigDecimal minLat,

            @Parameter(description = "Vĩ độ lớn nhất (Top-Right Lat)", example = "10.8000000")
            @RequestParam BigDecimal maxLat,

            @Parameter(description = "Kinh độ nhỏ nhất (Bottom-Left Lon)", example = "106.6000000")
            @RequestParam BigDecimal minLong,

            @Parameter(description = "Kinh độ lớn nhất (Top-Right Lon)", example = "106.7000000")
            @RequestParam BigDecimal maxLong,

            @Parameter(description = "Trang hiện tại (bắt đầu từ 0). Truyền page=0 & size=0 để lấy tất cả", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng phần tử trên mỗi trang (mặc định 20)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        return heatmapZoneService.getListingsByViewportPaged(minLat, maxLat, minLong, maxLong, page, size);
    }
}