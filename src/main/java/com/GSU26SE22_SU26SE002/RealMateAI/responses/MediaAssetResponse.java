package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.MediaResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaAssetResponse {
    private Long assetId;
    private String publicId;
    private String secureUrl;
    /** URL đã transform: resize về 800x600 thumbnail */
    private String thumbnailUrl;
    private MediaResourceType resourceType;
    private String format;
    private Long fileSizeBytes;
    private Integer width;
    private Integer height;
    private String folder;
    private EntityType entityType;
    private Long entityId;
    private LocalDateTime uploadedAt;
}
