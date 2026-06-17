package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateListingRequest {

    // ── Listing ───────────────────────────────────────────
    private String title;
    private String description;
    private Long price;
    private String contactPerson;
    private String contactPersonName;
    private String contactPersonPhone;
    private String linkSocialContactPerson;
    private String viewingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // ── Property ──────────────────────────────────────────
    private String propertyTitle;
    private String propertyDescription;
    private Long propertyPrice;
    private Double area;
    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;
    private Integer propertyTypeId;
    private Integer propertyConditionId;

    // ── Location ──────────────────────────────────────────
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String postalCode;
    private String wardCode;
}
