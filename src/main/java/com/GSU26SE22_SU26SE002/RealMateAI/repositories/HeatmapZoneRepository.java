package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HeatmapZoneRepository extends JpaRepository<HeatmapZone, UUID> {

    List<HeatmapZone> findBySnapshotDateLessThanEqualAndZoomLevel(LocalDate targetDate, Integer zoomLevel);

    List<HeatmapZone> findBySnapshotDate(LocalDate snapshotDate);
}