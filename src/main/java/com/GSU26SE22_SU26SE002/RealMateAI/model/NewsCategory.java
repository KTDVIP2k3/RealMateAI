package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.News;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "news_category")
public class NewsCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_category_id")
    private Integer newsCategoryId;

    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "newsCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<News> newsList = new ArrayList<>();
}
