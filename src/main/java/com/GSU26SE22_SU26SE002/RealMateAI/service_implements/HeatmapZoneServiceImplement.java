package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.HeatmapZoneRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import jakarta.annotation.PostConstruct;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private ListingRepository listingRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private Geometry safeLandGeometry;

    @PostConstruct
    public void initLandMap() {
        try {
            Coordinate[] landBoundary = new Coordinate[] {
                    new Coordinate(105.8000, 11.4000),
                    new Coordinate(107.4000, 11.4000),
                    new Coordinate(107.4000, 10.4500),
                    new Coordinate(106.9000, 10.4500),
                    new Coordinate(106.6000, 10.5500),
                    new Coordinate(105.8000, 10.5500),
                    new Coordinate(105.8000, 11.4000)
            };
            safeLandGeometry = geometryFactory.createPolygon(landBoundary);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void generateDailySnapshot() {
        List<CrawPropertyListing> listings = crawPropertyListingRepository.findAll().stream()
                .filter(l -> l.getLatitude() != null && l.getLongitude() != null)
                .filter(l -> l.getLatitude() >= 10.00 && l.getLatitude() <= 11.50)
                .filter(l -> l.getLongitude() >= 105.60 && l.getLongitude() <= 107.60)
                .toList();

        if (listings.isEmpty()) {
            return;
        }

        heatmapZoneRepository.clearJoinTable();
        heatmapZoneRepository.deleteAllInBatch();
        heatmapZoneRepository.flush();

        int[] targetZoomLevels = {19, 20};
        List<HeatmapZone> snapshotBatch = new ArrayList<>();

        for (int zoom : targetZoomLevels) {
            Map<String, List<CrawPropertyListing>> gridGroupMap = listings.stream()
                    .collect(Collectors.groupingBy(listing -> {
                        int gridX = lonToGridX(listing.getLongitude(), zoom);
                        int gridY = latToGridY(listing.getLatitude(), zoom);
                        return gridX + "_" + gridY;
                    }));

            List<HeatmapZone> levelZones = new ArrayList<>();

            gridGroupMap.forEach((key, gridListings) -> {
                String[] parts = key.split("_");
                int gridX = Integer.parseInt(parts[0]);
                int gridY = Integer.parseInt(parts[1]);

                double avgLat = gridListings.stream()
                        .mapToDouble(CrawPropertyListing::getLatitude)
                        .average()
                        .orElseGet(() -> gridYToLat(gridY + 0.5, zoom));

                double avgLon = gridListings.stream()
                        .mapToDouble(CrawPropertyListing::getLongitude)
                        .average()
                        .orElseGet(() -> gridXToLon(gridX + 0.5, zoom));

                CrawPropertyListing bestCenter = gridListings.stream()
                        .min((l1, l2) -> {
                            boolean l1IsSpam = isRawRoundedCoordinate(l1.getLongitude(), l1.getLatitude());
                            boolean l2IsSpam = isRawRoundedCoordinate(l2.getLongitude(), l2.getLatitude());

                            if (l1IsSpam && !l2IsSpam) return 1;
                            if (!l1IsSpam && l2IsSpam) return -1;

                            double dist1 = Math.pow(l1.getLatitude() - avgLat, 2) + Math.pow(l1.getLongitude() - avgLon, 2);
                            double dist2 = Math.pow(l2.getLatitude() - avgLat, 2) + Math.pow(l2.getLongitude() - avgLon, 2);
                            return Double.compare(dist1, dist2);
                        })
                        .orElse(gridListings.get(0));

                double finalLat = bestCenter.getLatitude();
                double finalLon = bestCenter.getLongitude();

                if (isRawRoundedCoordinate(finalLon, finalLat)) {
                    finalLon += 0.0003;
                    finalLat += 0.0002;
                }

                BigDecimal averagePricePerM2 = calculateAveragePricePerM2(gridListings);

                HeatmapZone zone = HeatmapZone.builder()
                        .zoomLevel(zoom)
                        .gridX(gridX)
                        .gridY(gridY)
                        .centerLatitude(finalLat)
                        .centerLongitude(finalLon)
                        .listingCount(gridListings.size())
                        .medianPricePerM2(averagePricePerM2)
                        .listings(gridListings)
                        .build();

                levelZones.add(zone);
            });

            calculateHeatLevelsForZoomLevel(levelZones);
            snapshotBatch.addAll(levelZones);
        }

        List<HeatmapZone> savedZones = heatmapZoneRepository.saveAll(snapshotBatch);

        for (HeatmapZone zone : savedZones) {
            if (zone.getListings() != null) {
                for (CrawPropertyListing listing : zone.getListings()) {
                    if (listing.getHeatmapZones() == null) {
                        listing.setHeatmapZones(new ArrayList<>());
                    }
                    listing.getHeatmapZones().add(zone);
                }
            }
        }
    }

    private boolean isRawRoundedCoordinate(double lon, double lat) {
        double lonNormalized = lon * 100;
        double latNormalized = lat * 100;
        boolean lonIsFlat = Math.abs(lonNormalized - Math.round(lonNormalized)) < 0.001;
        boolean latIsFlat = Math.abs(latNormalized - Math.round(latNormalized)) < 0.001;
        return lonIsFlat || latIsFlat;
    }

    private double[] shiftCoordinatesToLand(double lon, double lat) {
        if (isLocationOnSafeLand(lon, lat)) {
            return new double[]{lon, lat};
        }

        double[][] directions = {
                {0, 1}, {1, 0}, {0, -1}, {-1, 0},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
        };

        double baseStep = 0.002;

        for (int ring = 1; ring <= 30; ring++) {
            double currentStep = baseStep * ring;
            for (double[] dir : directions) {
                double testLon = lon + dir[0] * currentStep;
                double testLat = lat + dir[1] * currentStep;

                if (isLocationOnSafeLand(testLon, testLat)) {
                    return new double[]{testLon, testLat};
                }
            }
        }

        return new double[]{106.6980, 10.7750};
    }

    private boolean isLocationOnSafeLand(double lon, double lat) {
        if (safeLandGeometry == null) {
            return true;
        }
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        return safeLandGeometry.contains(point);
    }

    private int lonToGridX(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int latToGridY(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }

    private double gridXToLon(double x, int zoom) {
        return x / (double) (1 << zoom) * 360.0 - 180.0;
    }

    private double gridYToLat(double y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / (double) (1 << zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private BigDecimal calculateAveragePricePerM2(List<CrawPropertyListing> listings) {
        List<BigDecimal> validPrices = listings.stream()
                .map(CrawPropertyListing::getPricePerM2)
                .filter(Objects::nonNull)
                .toList();

        if (validPrices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = validPrices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(BigDecimal.valueOf(validPrices.size()), 2, RoundingMode.HALF_UP);
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

    @Override
    public List<HeatmapZone> getHeatmapZonesByViewport(
            Integer zoomLevel,
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong) {
        double actualMinLat = minLat.min(maxLat).doubleValue();
        double actualMaxLat = minLat.max(maxLat).doubleValue();
        double actualMinLong = minLong.min(maxLong).doubleValue();
        double actualMaxLong = minLong.max(maxLong).doubleValue();
        List<HeatmapZone> candidateZones = heatmapZoneRepository.findByZoomLevel(zoomLevel);
        if (candidateZones.isEmpty()) return Collections.emptyList();
        return candidateZones.stream()
                .filter(zone -> zone.getCenterLatitude() >= actualMinLat && zone.getCenterLatitude() <= actualMaxLat)
                .filter(zone -> zone.getCenterLongitude() >= actualMinLong && zone.getCenterLongitude() <= actualMaxLong)
                .collect(Collectors.toList());
    }

    @Override
    public List<HeatmapZone> getHeatmapZonesByViewportV2(
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong) {
        double actualMinLat = minLat.min(maxLat).doubleValue();
        double actualMaxLat = minLat.max(maxLat).doubleValue();
        double actualMinLong = minLong.min(maxLong).doubleValue();
        double actualMaxLong = minLong.max(maxLong).doubleValue();
        List<HeatmapZone> candidateZones = heatmapZoneRepository.findAll();
        if (candidateZones.isEmpty()) return Collections.emptyList();
        return candidateZones.stream()
                .filter(zone -> zone.getCenterLatitude() >= actualMinLat && zone.getCenterLatitude() <= actualMaxLat)
                .filter(zone -> zone.getCenterLongitude() >= actualMinLong && zone.getCenterLongitude() <= actualMaxLong)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getListingsByViewportPaged(
            BigDecimal minLat,
            BigDecimal maxLat,
            BigDecimal minLong,
            BigDecimal maxLong,
            int page,
            int size) {
        try {
            BigDecimal actualMinLat = minLat.min(maxLat);
            BigDecimal actualMaxLat = minLat.max(maxLat);
            BigDecimal actualMinLong = minLong.min(maxLong);
            BigDecimal actualMaxLong = minLong.max(maxLong);

            List<Listing> allListings = listingRepository.findAll().stream()
                    .filter(listing -> listing != null
                            && listing.getProperty() != null
                            && listing.getProperty().getLocation() != null
                            && listing.getProperty().getLocation().getLatitude() != null
                            && listing.getProperty().getLocation().getLongitude() != null
                            && Boolean.TRUE.equals(listing.getIsActive())
                            && listing.getStatus() == SellerListingStatusEnum.ACTIVE
                            && listing.getProperty().getLocation().getLatitude().compareTo(actualMinLat) >= 0
                            && listing.getProperty().getLocation().getLatitude().compareTo(actualMaxLat) <= 0
                            && listing.getProperty().getLocation().getLongitude().compareTo(actualMinLong) >= 0
                            && listing.getProperty().getLocation().getLongitude().compareTo(actualMaxLong) <= 0)
                    .toList();

            List<Listing> sortedList = allListings.stream()
                    .sorted(java.util.Comparator.comparing(
                            Listing::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);
            List<ListingDTO> pagedContent;
            int effectivePage = 0;
            int totalElements = sortedList.size();
            int effectiveSize = totalElements;
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream()
                        .map(this::mapToListingDTO)
                        .collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);
                totalPages = totalElements == 0 ? 1 : (int) Math.ceil((double) totalElements / effectiveSize);
                isLast = effectivePage >= totalPages - 1;
                int offset = effectivePage * effectiveSize;
                List<Listing> slicedListings;
                if (offset >= totalElements) {
                    slicedListings = java.util.Collections.emptyList();
                } else {
                    slicedListings = sortedList.stream()
                            .skip(offset)
                            .limit(effectiveSize)
                            .toList();
                }
                pagedContent = slicedListings.stream()
                        .map(this::mapToListingDTO)
                        .collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(result, "Get listings by viewport successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private ListingDTO mapToListingDTO(Listing listing) {
        if (listing == null) return null;
        ListingDTO dto = ListingDTO.builder()
                .listingId(listing.getListingId())
                .title(listing.getTitle())
                .price(listing.getPrice())
                .isActive(listing.getIsActive())
                .viewCount(listing.getViewCount())
                .createdAt(listing.getCreatedAt())
                .isVerified(listing.getIsVerified())
                .verificationStatus(listing.getCertificationStatus() != null ? listing.getCertificationStatus().name() : null)
                .isFavorited(false)
                .build();

        if (listing.getProperty() != null) {
            if (listing.getProperty().getLocation() != null) {
                dto.setLatitude(listing.getProperty().getLocation().getLatitude());
                dto.setLongitude(listing.getProperty().getLocation().getLongitude());
            }
            dto.setArea(listing.getProperty().getArea());
            dto.setBedroom(listing.getProperty().getBedroom());
            dto.setBathroom(listing.getProperty().getBathroom());
            if (listing.getProperty().getPropertyType() != null) {
                dto.setPropertyTypeName(listing.getProperty().getPropertyType().getName());
            }
        }
        if (listing.getListingImages() != null && !listing.getListingImages().isEmpty()) {
            dto.setThumbnailUrl(listing.getListingImages().get(0).getImageUrl());
        }
        return dto;
    }
}