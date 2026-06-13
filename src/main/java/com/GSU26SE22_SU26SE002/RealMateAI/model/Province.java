package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "province")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Province {
    @Id
    @Column(name = "province_code", length = 20)
    private String province_code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "code_name")
    private String codeName;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ward> wards;
}