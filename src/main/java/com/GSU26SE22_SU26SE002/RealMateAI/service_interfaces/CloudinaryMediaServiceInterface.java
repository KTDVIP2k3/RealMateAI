package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MediaAssetResponse;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
public interface CloudinaryMediaServiceInterface {

    /** Upload 1 file, gắn với entity cụ thể */
    MediaAssetResponse uploadFile(MultipartFile file,
                                  Account uploader,
                                  EntityType entityType,
                                  Long entityId);

    /** Upload nhiều file cùng lúc (max 20) */
    List<MediaAssetResponse> uploadMultiple(List<MultipartFile> files,
                                            Account uploader,
                                            EntityType entityType,
                                            Long entityId);

    /** Xóa file khỏi Cloudinary + soft-delete DB */
    void deleteFile(String publicId);

    /** Tạo URL thumbnail động */
    String generateThumbnailUrl(String publicId, int width, int height);

    /** Tạo URL đã optimize chất lượng */
    String generateOptimizedUrl(String publicId);

    /** Lấy danh sách assets theo entity */
    List<MediaAssetResponse> getAssetsByEntity(EntityType entityType, Long entityId);
}
