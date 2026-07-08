package com.GSU26SE22_SU26SE002.RealMateAI.responses;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsCategoryDTO {
    private Integer newsCategoryId;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}