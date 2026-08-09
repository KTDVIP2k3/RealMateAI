package com.GSU26SE22_SU26SE002.RealMateAI.responses;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewsDTO {
    private Integer newsId;
    private Integer newsCategoryId;
    private String newsCategoryName;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String thumbnailUrl;
    private String sourceUrl;
    private String sourceName;
    private Integer viewCount;
    private Boolean isFeatured;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
