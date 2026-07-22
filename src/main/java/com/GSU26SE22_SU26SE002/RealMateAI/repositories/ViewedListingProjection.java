package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import java.time.LocalDateTime;

public interface ViewedListingProjection {
    Integer getListingId();
    LocalDateTime getLastViewedAt();
    Long getViewCount();
}
