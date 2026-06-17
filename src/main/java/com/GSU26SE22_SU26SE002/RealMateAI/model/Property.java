package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Location;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyCondition;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyImage;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyValuation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "property")
public class Property {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Integer propertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id")
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_condition_id")
    private PropertyCondition propertyCondition;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    private String title;
    @Column(columnDefinition = "Text")
    private String description;
    private Long price;
    private Double area;
    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;
    private String legalStatus;
    private String addressParticular;
    private String projectName;
    private Boolean isActive;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "property_attribute", columnDefinition = "json")
    private Map<String, Object> propertyAttribute;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "property_purpose", columnDefinition = "json")
    private List<String> propertyPurpose;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<PropertyImage> propertyImages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PropertyValuation> propertyValuations = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Listing> listings = new ArrayList<>();
}
