package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;
import org.geolatte.geom.Geometry;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "ward_boundary")
public class WardBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PK_ward_boundary_id")
    private Integer wardBoundaryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FK_ward_code", nullable = false)
    private Ward ward;

    @Column(name = "boundary")
    private Geometry boundary;

    @Column(name = "area_km2", precision = 18, scale = 2)
    private BigDecimal areaKm2;

    @Column(name = "population")
    private Integer population;

    @Column(name = "destiny", precision = 18, scale = 2)
    private BigDecimal destiny;
}