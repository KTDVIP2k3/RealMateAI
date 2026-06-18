package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "property_type")
public class PropertyType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_type_id")
    private Integer propertyTypeId;

    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "propertyType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PropertyCondition> propertyConditions = new ArrayList<>();

    @OneToMany(mappedBy = "propertyType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Property> properties = new ArrayList<>();
}
