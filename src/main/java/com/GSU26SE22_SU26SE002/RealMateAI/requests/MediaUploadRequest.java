package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
@Data
public class MediaUploadRequest {
    /** File cần upload – bắt buộc */
    @NotNull(message = "File không được để trống")
    private MultipartFile file;

    /** Loại entity gắn với file này */
    private EntityType entityType;

    /** ID của entity (listingId, propertyId, ...) */
    private Long entityId;

    /**
     * Subfolder tùy chọn trong Cloudinary.
     * Nếu null → service tự build: realmateai/{entityType}/{entityId}
     */
    private String folder;
}
