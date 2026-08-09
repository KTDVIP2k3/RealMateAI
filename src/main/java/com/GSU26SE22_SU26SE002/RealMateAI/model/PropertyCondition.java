package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "property_condition")
public class PropertyCondition {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_condition_id")
    private Integer propertyConditionId;

    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id")
    @JsonIgnore
    private PropertyType propertyType;

    @OneToMany(mappedBy = "propertyCondition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Property> properties = new ArrayList<>();
}
