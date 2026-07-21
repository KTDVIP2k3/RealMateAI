package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface HeatmapZoneServiceInterface {

    void generateDailySnapshot();

    List<HeatmapZone> getHeatmapZonesByViewport(
            LocalDate targetDate,
            Integer zoomLevel,
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong
    );
}