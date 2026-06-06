package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "prompt_category")
public class PromptCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prompt_category_id")
    private Integer promptCategoryId;

    @Column(name = "category_name")
    private String categoryName;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "promptCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Prompt> prompts = new ArrayList<>();
}
