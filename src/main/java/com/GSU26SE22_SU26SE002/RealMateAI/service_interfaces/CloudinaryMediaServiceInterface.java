package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MediaAssetResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MediaUploadResponse;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryMediaServiceInterface {

    /**
     * Upload 1 file, gắn với entity cụ thể.
     * Khi entityType=ACCOUNT: entityId LUÔN được BE tự lấy từ accountId của
     * người đang đăng nhập (tham số entityId truyền vào bị bỏ qua, không dùng
     * để tránh FE phải truyền thừa / tránh spoof accountId người khác).
     */
    MediaUploadResponse uploadFile(MultipartFile file,
                                   Account uploader,
                                   EntityType entityType,
                                   Long entityId);

    /**
     * Upload nhiều file cùng lúc (max 20). Không còn nhận entityId — entityType
     * hiện tại luôn là ACCOUNT ở luồng thực tế (upload ảnh draft trước khi tạo
     * tin đăng), entityId được BE tự suy ra từ accountId người đang đăng nhập.
     */
    List<MediaUploadResponse> uploadMultiple(List<MultipartFile> files,
                                             Account uploader,
                                             EntityType entityType);

    /** Xóa file khỏi Cloudinary + soft-delete DB */
    void deleteFile(String publicId);

    /** Tạo URL thumbnail động */
    String generateThumbnailUrl(String publicId, int width, int height);

    /** Tạo URL đã optimize chất lượng */
    String generateOptimizedUrl(String publicId);

    /** Lấy danh sách assets theo entity */
    List<MediaAssetResponse> getAssetsByEntity(EntityType entityType, Long entityId);
}
