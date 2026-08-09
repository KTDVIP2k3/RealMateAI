package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "recommendation_result")
public class RecommendationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_result_id")
    private Long recommendationResultId;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "listing_id", nullable = false)
    private Integer listingId;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
