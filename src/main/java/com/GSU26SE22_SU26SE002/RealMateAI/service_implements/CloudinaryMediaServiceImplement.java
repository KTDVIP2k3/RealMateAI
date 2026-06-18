package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.MediaResourceType;
import com.GSU26SE22_SU26SE002.RealMateAI.exception.MediaUploadException;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.MediaAsset;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.MediaAssetRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MediaAssetResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CloudinaryMediaServiceInterface;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CloudinaryMediaServiceImplement implements CloudinaryMediaServiceInterface {


    private static final String ROOT_FOLDER       = "realmateai";
    private static final long   MAX_FILE_SIZE_MB  = 20;    // 20 MB
    private static final long   MAX_FILE_BYTES    = MAX_FILE_SIZE_MB * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGES =
            List.of("image/jpeg","image/png","image/webp","image/gif");
    private static final List<String> ALLOWED_FILES  =
            List.of("application/pdf","application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final Cloudinary cloudinary;
    private final MediaAssetRepository mediaAssetRepository;

    // ─────────────────────────────────────────────────────────────
    // UPLOAD SINGLE
    // ─────────────────────────────────────────────────────────────

    @Override
    public MediaAssetResponse uploadFile(MultipartFile file,
                                         Account uploader,
                                         EntityType entityType,
                                         Long entityId) {
        validateFile(file);

        String folder    = buildFolder(entityType, entityId);
        String publicId  = buildPublicId(folder, file.getOriginalFilename());
        boolean isImage  = isImageFile(file);
        String resourceType = isImage ? "image" : "raw";

        Map uploadParams = ObjectUtils.asMap(
                "public_id",     publicId,
                "folder",        folder,
                "resource_type", resourceType,
                "overwrite",     false,
                // Auto-quality & format optimization
                "quality",       "auto",
                "fetch_format",  "auto",
                // Eager transformation: tạo sẵn thumbnail 800x600
                "eager",         List.of(
                        new Transformation().width(800).height(600).crop("limit").quality("auto")
                ),
                "eager_async",   true
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            MediaAsset asset = buildAndSaveAsset(result, uploader, entityType, entityId, folder, isImage);
            log.info("[Cloudinary] Uploaded: publicId={}, size={}bytes", asset.getPublicId(), asset.getFileSizeBytes());
            return toResponse(asset);
        } catch (IOException e) {
            log.error("[Cloudinary] Upload failed for file={}", file.getOriginalFilename(), e);
            throw new MediaUploadException("Upload ảnh thất bại: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // UPLOAD MULTIPLE
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<MediaAssetResponse> uploadMultiple(List<MultipartFile> files,
                                                   Account uploader,
                                                   EntityType entityType,
                                                   Long entityId) {
        if (files == null || files.isEmpty()) {
            throw new MediaUploadException("Danh sách file không được rỗng");
        }
        if (files.size() > 20) {
            throw new MediaUploadException("Tối đa 20 file mỗi lần upload");
        }
        List<MediaAssetResponse> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadFile(file, uploader, entityType, entityId));
        }
        return results;
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────

    @Override
    public void deleteFile(String publicId) {
        try {
            // Xóa thực sự trên Cloudinary
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String outcome = (String) result.get("result");

            if ("ok".equals(outcome) || "not found".equals(outcome)) {
                // Soft delete trong DB
                int rows = mediaAssetRepository.softDeleteByPublicId(publicId);
                log.info("[Cloudinary] Deleted publicId={}, dbRows={}", publicId, rows);
            } else {
                log.warn("[Cloudinary] Unexpected delete result: {}", result);
            }
        } catch (IOException e) {
            log.error("[Cloudinary] Delete failed for publicId={}", publicId, e);
            throw new MediaUploadException("Xóa file thất bại: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GENERATE TRANSFORM URL (không cần lưu DB)
    // ─────────────────────────────────────────────────────────────

    @Override
    public String generateThumbnailUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new Transformation()
                        .width(width).height(height).crop("fill").quality("auto").fetchFormat("auto"))
                .secure(true)
                .generate(publicId);
    }

    @Override
    public String generateOptimizedUrl(String publicId) {
        return cloudinary.url()
                .transformation(new Transformation().quality("auto").fetchFormat("auto"))
                .secure(true)
                .generate(publicId);
    }

    // ─────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> getAssetsByEntity(EntityType entityType, Long entityId) {
        return mediaAssetRepository
                .findActiveByEntity(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MediaUploadException("File không được để trống");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new MediaUploadException(
                    String.format("File vượt quá kích thước cho phép (%dMB)", MAX_FILE_SIZE_MB));
        }
        String contentType = file.getContentType();
        boolean valid = ALLOWED_IMAGES.contains(contentType) || ALLOWED_FILES.contains(contentType);
        if (!valid) {
            throw new MediaUploadException(
                    "Định dạng không được hỗ trợ: " + contentType +
                            ". Cho phép: JPEG, PNG, WebP, GIF, PDF, DOCX");
        }
    }

    private boolean isImageFile(MultipartFile file) {
        String ct = file.getContentType();
        return ct != null && ct.startsWith("image/");
    }

    private String buildFolder(EntityType entityType, Long entityId) {
        if (entityType == null) return ROOT_FOLDER + "/misc";
        String sub = entityType.name().toLowerCase() + "s";
        return entityId != null
                ? ROOT_FOLDER + "/" + sub + "/" + entityId
                : ROOT_FOLDER + "/" + sub;
    }

    private String buildPublicId(String folder, String originalFilename) {
        String name = originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        // Bỏ extension vì Cloudinary tự thêm
        int dot = name.lastIndexOf('.');
        if (dot > 0) name = name.substring(0, dot);
        return folder + "/" + name + "_" + System.currentTimeMillis();
    }

    private MediaAsset buildAndSaveAsset(Map<?, ?> result,
                                         Account uploader,
                                         EntityType entityType,
                                         Long entityId,
                                         String folder,
                                         boolean isImage) {
        MediaAsset asset = MediaAsset.builder()
                .account(uploader)
                .publicId((String) result.get("public_id"))
                .secureUrl((String) result.get("secure_url"))
                .resourceType(isImage ? MediaResourceType.IMAGE : MediaResourceType.RAW)
                .format((String) result.get("format"))
                .fileSizeBytes(result.get("bytes") != null ? ((Number) result.get("bytes")).longValue() : null)
                .width(result.get("width") != null ? ((Number) result.get("width")).intValue() : null)
                .height(result.get("height") != null ? ((Number) result.get("height")).intValue() : null)
                .folder(folder)
                .entityType(entityType)
                .entityId(entityId)
                .isActive(true)
                .build();
        return mediaAssetRepository.save(asset);
    }

    private MediaAssetResponse toResponse(MediaAsset a) {
        return MediaAssetResponse.builder()
                .assetId(a.getAssetId())
                .publicId(a.getPublicId())
                .secureUrl(a.getSecureUrl())
                .thumbnailUrl(generateThumbnailUrl(a.getPublicId(), 400, 300))
                .resourceType(a.getResourceType())
                .format(a.getFormat())
                .fileSizeBytes(a.getFileSizeBytes())
                .width(a.getWidth())
                .height(a.getHeight())
                .folder(a.getFolder())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
