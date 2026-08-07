package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "news")
public class News {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Integer newsId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_category_id", nullable = false)
    @JsonIgnore
    private NewsCategory newsCategory;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String sourceUrl;

    private String sourceName;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Boolean isFeatured = false;


    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
