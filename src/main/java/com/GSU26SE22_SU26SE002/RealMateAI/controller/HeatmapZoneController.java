package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/heatmap")
@Tag(name = "Heatmap Zone", description = "Heatmap: Quản lý và truy vấn dữ liệu bản đồ nhiệt giá bất động sản")
public class HeatmapZoneController {

    @Autowired
    private HeatmapZoneServiceInterface heatmapZoneService;

    @GetMapping("/zones")
    @Operation(summary = "Lấy danh sách các ô Grid Heatmap theo khung nhìn (Viewport) và cấp độ Zoom")
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

    @GetMapping("/zones/v2")
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
    @Operation(summary = "Admin/System: Kích hoạt thủ công tiến trình gom nhóm dữ liệu thô và tính toán Heatmap Snapshot")
    public ResponseEntity<String> triggerSnapshot() {
        heatmapZoneService.generateDailySnapshot();
        return ResponseEntity.ok("Trigger snapshot successfully!");
    }
}