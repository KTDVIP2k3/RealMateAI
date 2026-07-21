package com.GSU26SE22_SU26SE002.RealMateAI.controllers;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/heatmap")
@Tag(name = "Heatmap Zone", description = "Heatmap: Quản lý và truy vấn dữ liệu bản đồ nhiệt giá bất động sản")
public class HeatmapZoneController {

    @Autowired
    private HeatmapZoneServiceInterface heatmapZoneService;

    @GetMapping("/zones")
    @Operation(summary = "Lấy danh sách các ô Grid Heatmap theo khung nhìn (Viewport) và cấp độ Zoom (Tự động fallback về ngày gần nhất nếu thiếu data)")
    public ResponseEntity<List<HeatmapZone>> getHeatmapZones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer zoom,
            @RequestParam BigDecimal minLat,
            @RequestParam BigDecimal maxLat,
            @RequestParam BigDecimal minLong,
            @RequestParam BigDecimal maxLong) {

        List<HeatmapZone> result = heatmapZoneService.getHeatmapZonesByViewport(
                date, zoom, minLat, maxLat, minLong, maxLong
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/trigger-snapshot")
    @Operation(summary = "Admin/System: Kích hoạt thủ công tiến trình gom nhóm dữ liệu thô và tính toán Heatmap Snapshot cho ngày hiện tại")
    public ResponseEntity<String> triggerSnapshot() {
        heatmapZoneService.generateDailySnapshot();
        return ResponseEntity.ok("Trigger snapshot successfully!");
    }
}