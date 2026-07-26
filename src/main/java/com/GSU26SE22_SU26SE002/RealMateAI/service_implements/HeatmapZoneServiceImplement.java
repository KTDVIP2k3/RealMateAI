package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.HeatmapZoneRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HeatmapZoneServiceImplement implements HeatmapZoneServiceInterface {

    @Autowired
    private CrawPropertyListingRepository crawPropertyListingRepository;

    @Autowired
    private HeatmapZoneRepository heatmapZoneRepository;

    private static final double MIN_HCM_LAT = 10.35;
    private static final double MAX_HCM_LAT = 11.16;
    private static final double MIN_HCM_LON = 106.35;
    private static final double MAX_HCM_LON = 106.90;

    @Override
    @Transactional
    public void generateDailySnapshot() {
        List<CrawPropertyListing> listings = crawPropertyListingRepository.findAll().stream()
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .filter(l -> {
                    double lat = l.getLatitude().doubleValue();
                    double lon = l.getLongitude().doubleValue();
                    return lat >= MIN_HCM_LAT && lat <= MAX_HCM_LAT && lon >= MIN_HCM_LON && lon <= MAX_HCM_LON;
                })
                .toList();

        if (listings.isEmpty()) {
            return;
        }

        int[] targetZoomLevels = {10, 11, 12, 13, 14, 15};
        List<HeatmapZone> snapshotBatch = new ArrayList<>();

        for (int zoom : targetZoomLevels) {
            Map<String, List<CrawPropertyListing>> gridGroupMap = listings.stream()
                    .collect(Collectors.groupingBy(listing -> {
                        int gridX = lonToGridX(listing.getLongitude().doubleValue(), zoom);
                        int gridY = latToGridY(listing.getLatitude().doubleValue(), zoom);
                        return gridX + "_" + gridY;
                    }));

            List<HeatmapZone> levelZones = new ArrayList<>();

            gridGroupMap.forEach((key, gridListings) -> {
                String[] parts = key.split("_");
                int gridX = Integer.parseInt(parts[0]);
                int gridY = Integer.parseInt(parts[1]);

                double centerLon = gridXToLon(gridX + 0.5, zoom);
                double centerLat = gridYToLat(gridY + 0.5, zoom);

                BigDecimal medianPricePerM2 = calculateMedianPricePerM2(gridListings);

                HeatmapZone zone = HeatmapZone.builder()
                        .zoomLevel(zoom)
                        .gridX(gridX)
                        .gridY(gridY)
                        .centerLatitude(BigDecimal.valueOf(centerLat).setScale(7, RoundingMode.HALF_UP))
                        .centerLongitude(BigDecimal.valueOf(centerLon).setScale(7, RoundingMode.HALF_UP))
                        .listingCount(gridListings.size())
                        .medianPricePerM2(medianPricePerM2)
                        .listings(gridListings)
                        .build();

                levelZones.add(zone);
            });

            calculateHeatLevelsForZoomLevel(levelZones);

            snapshotBatch.addAll(levelZones);
        }

        heatmapZoneRepository.clearJoinTable();
        heatmapZoneRepository.deleteAllInBatch();
        heatmapZoneRepository.saveAll(snapshotBatch);
    }

    @Override
    public List<HeatmapZone> getHeatmapZonesByViewport(
            Integer zoomLevel,
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong) {

        BigDecimal actualMinLat = minLat.min(maxLat);
        BigDecimal actualMaxLat = minLat.max(maxLat);
        BigDecimal actualMinLong = minLong.min(maxLong);
        BigDecimal actualMaxLong = minLong.max(maxLong);

        List<HeatmapZone> candidateZones = heatmapZoneRepository.findByZoomLevel(zoomLevel);

        if (candidateZones.isEmpty()) {
            return Collections.emptyList();
        }

        return candidateZones.stream()
                .filter(zone -> zone.getCenterLatitude().compareTo(actualMinLat) >= 0 && zone.getCenterLatitude().compareTo(actualMaxLat) <= 0)
                .filter(zone -> zone.getCenterLongitude().compareTo(actualMinLong) >= 0 && zone.getCenterLongitude().compareTo(actualMaxLong) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<HeatmapZone> getHeatmapZonesByViewportV2(
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong) {

        BigDecimal actualMinLat = minLat.min(maxLat);
        BigDecimal actualMaxLat = minLat.max(maxLat);
        BigDecimal actualMinLong = minLong.min(maxLong);
        BigDecimal actualMaxLong = minLong.max(maxLong);

        List<HeatmapZone> candidateZones = heatmapZoneRepository.findAll();

        if (candidateZones.isEmpty()) {
            return Collections.emptyList();
        }

        return candidateZones.stream()
                .filter(zone -> zone.getCenterLatitude().compareTo(actualMinLat) >= 0 && zone.getCenterLatitude().compareTo(actualMaxLat) <= 0)
                .filter(zone -> zone.getCenterLongitude().compareTo(actualMinLong) >= 0 && zone.getCenterLongitude().compareTo(actualMaxLong) <= 0)
                .collect(Collectors.toList());
    }

    private void calculateHeatLevelsForZoomLevel(List<HeatmapZone> zones) {
        if (zones.isEmpty()) return;

        int maxListingCount = zones.stream()
                .mapToInt(HeatmapZone::getListingCount)
                .max()
                .orElse(1);

        BigDecimal maxPrice = zones.stream()
                .map(HeatmapZone::getMedianPricePerM2)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        for (HeatmapZone zone : zones) {
            int densityLevel = (int) Math.round(((double) zone.getListingCount() / maxListingCount) * 100.0);
            zone.setDensityHeatLevel(densityLevel);

            if (zone.getMedianPricePerM2() != null && maxPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal priceLevel = zone.getMedianPricePerM2()
                        .divide(maxPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
                zone.setPriceHeatLevel(priceLevel);
            } else {
                zone.setPriceHeatLevel(BigDecimal.ZERO);
            }
        }
    }

    private int lonToGridX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int latToGridY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }

    private double gridXToLon(double x, int zoom) {
        return x / (1 << zoom) * 360.0 - 180.0;
    }

    private double gridYToLat(double y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / (1 << zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private BigDecimal calculateMedianPricePerM2(List<CrawPropertyListing> listings) {
        List<BigDecimal> prices = listings.stream()
                .map(CrawPropertyListing::getPricePerM2)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        if (prices.isEmpty()) return BigDecimal.ZERO;

        int size = prices.size();
        if (size % 2 == 1) {
            return prices.get(size / 2);
        } else {
            return prices.get(size / 2 - 1).add(prices.get(size / 2))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        }
    }
}