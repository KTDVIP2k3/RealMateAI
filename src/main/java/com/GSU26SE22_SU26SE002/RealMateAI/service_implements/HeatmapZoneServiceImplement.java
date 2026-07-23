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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HeatmapZoneServiceImplement implements HeatmapZoneServiceInterface {

    @Autowired
    private CrawPropertyListingRepository crawPropertyListingRepository;

    @Autowired
    private HeatmapZoneRepository heatmapZoneRepository;

    @Override
    @Transactional
    public void generateDailySnapshot() {
        List<CrawPropertyListing> listings = crawPropertyListingRepository.findAll().stream()
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .toList();

        if (listings.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        int[] targetZoomLevels = {10, 13, 15, 17};

        List<HeatmapZone> snapshotBatch = new ArrayList<>();

        for (int zoom : targetZoomLevels) {
            Map<String, List<CrawPropertyListing>> gridGroupMap = listings.stream()
                    .collect(Collectors.groupingBy(listing -> {
                        int gridX = lonToGridX(listing.getLongitude().doubleValue(), zoom);
                        int gridY = latToGridY(listing.getLatitude().doubleValue(), zoom);
                        return gridX + "_" + gridY;
                    }));

            gridGroupMap.forEach((key, gridListings) -> {
                String[] parts = key.split("_");
                int gridX = Integer.parseInt(parts[0]);
                int gridY = Integer.parseInt(parts[1]);

                double centerLon = gridXToLon(gridX + 0.5, zoom);
                double centerLat = gridYToLat(gridY + 0.5, zoom);

                BigDecimal medianPricePerM2 = calculateMedianPricePerM2(gridListings);
                BigDecimal priceChangePercent = calculatePriceChangePercent(gridX, gridY, zoom, yesterday, medianPricePerM2);

                HeatmapZone zone = HeatmapZone.builder()
                        .snapshotDate(today)
                        .zoomLevel(zoom)
                        .gridX(gridX)
                        .gridY(gridY)
                        .centerLatitude(BigDecimal.valueOf(centerLat).setScale(7, RoundingMode.HALF_UP))
                        .centerLongitude(BigDecimal.valueOf(centerLon).setScale(7, RoundingMode.HALF_UP))
                        .listingCount(gridListings.size())
                        .medianPricePerM2(medianPricePerM2)
                        .priceChangePercent(priceChangePercent)
                        .build();

                snapshotBatch.add(zone);
            });
        }

        heatmapZoneRepository.deleteBySnapshotDate(today);
        heatmapZoneRepository.saveAll(snapshotBatch);
    }

    @Override
    public List<HeatmapZone> getHeatmapZonesByViewport(
            LocalDate targetDate,
            Integer zoomLevel,
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong) {

        LocalDate dateQuery = (targetDate != null) ? targetDate : LocalDate.now();

        List<HeatmapZone> candidateZones = heatmapZoneRepository.findBySnapshotDateLessThanEqualAndZoomLevel(dateQuery, zoomLevel);

        if (candidateZones.isEmpty()) {
            return Collections.emptyList();
        }

        Optional<LocalDate> latestDateOpt = candidateZones.stream()
                .map(HeatmapZone::getSnapshotDate)
                .max(LocalDate::compareTo);

        if (latestDateOpt.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate effectiveDate = latestDateOpt.get();

        return candidateZones.stream()
                .filter(zone -> zone.getSnapshotDate().equals(effectiveDate))
                .filter(zone -> zone.getCenterLatitude().compareTo(minLat) >= 0 && zone.getCenterLatitude().compareTo(maxLat) <= 0)
                .filter(zone -> zone.getCenterLongitude().compareTo(minLong) >= 0 && zone.getCenterLongitude().compareTo(maxLong) <= 0)
                .collect(Collectors.toList());
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

    private BigDecimal calculatePriceChangePercent(int gridX, int gridY, int zoom, LocalDate yesterday, BigDecimal currentPrice) {
        if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        List<HeatmapZone> yesterdayZones = heatmapZoneRepository.findBySnapshotDate(yesterday);

        return yesterdayZones.stream()
                .filter(zone -> zone.getZoomLevel() == zoom && zone.getGridX() == gridX && zone.getGridY() == gridY)
                .map(HeatmapZone::getMedianPricePerM2)
                .filter(Objects::nonNull)
                .filter(oldPrice -> oldPrice.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .map(oldPrice -> currentPrice.subtract(oldPrice)
                        .divide(oldPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO);
    }
}