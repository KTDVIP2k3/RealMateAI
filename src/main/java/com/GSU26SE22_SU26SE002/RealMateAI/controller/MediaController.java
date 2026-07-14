package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MediaAssetResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CloudinaryMediaServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Upload & quản lý ảnh, file qua Cloudinary")
public class MediaController {

    private final CloudinaryMediaServiceInterface mediaService;
    private final AuthenUntil securityUtils;

    // ─────────────────────────────────────────────────────────────
    // POST /media/upload
    // Upload 1 file (ảnh hoặc tài liệu)
    // ─────────────────────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload 1 file lên Cloudinary")
    public ResponseEntity<MediaAssetResponse> uploadSingle(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) Long entityId) {

        Account uploader = securityUtils.getCurrentUSer();
        MediaAssetResponse response = mediaService.uploadFile(file, uploader, entityType, entityId);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────
    // POST /media/upload/multiple
    // Upload nhiều file cùng lúc (max 20)
    // ─────────────────────────────────────────────────────────────
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload nhiều file cùng lúc (tối đa 20)")
    public ResponseEntity<List<MediaAssetResponse>> uploadMultiple(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) Long entityId) {

        Account uploader = securityUtils.getCurrentUSer();
        List<MediaAssetResponse> responses = mediaService.uploadMultiple(files, uploader, entityType, entityId);
        return ResponseEntity.ok(responses);
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /media/{publicId}
    // Xóa file khỏi Cloudinary + soft-delete DB
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/{publicId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xóa file theo publicId")
    public ResponseEntity<Void> deleteFile(
            @PathVariable("publicId") @NotBlank String publicId) {

        // URL decode publicId vì có thể chứa '/'
        String decoded = java.net.URLDecoder.decode(publicId, java.nio.charset.StandardCharsets.UTF_8);
        mediaService.deleteFile(decoded);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────
    // GET /media?entityType=LISTING&entityId=5
    // Lấy danh sách file theo entity
    // ─────────────────────────────────────────────────────────────
    @GetMapping
    @Operation(summary = "Lấy danh sách media theo entity")
    public ResponseEntity<List<MediaAssetResponse>> getByEntity(
            @RequestParam EntityType entityType,
            @RequestParam Long entityId) {

        return ResponseEntity.ok(mediaService.getAssetsByEntity(entityType, entityId));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /media/thumbnail?publicId=xxx&w=400&h=300
    // Tạo URL thumbnail động (không cần auth)
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/thumbnail")
    @Operation(summary = "Lấy URL thumbnail đã transform")
    public ResponseEntity<String> getThumbnailUrl(
            @RequestParam String publicId,
            @RequestParam(defaultValue = "400") int width,
            @RequestParam(defaultValue = "300") int height) {

        String url = mediaService.generateThumbnailUrl(publicId, width, height);
        return ResponseEntity.ok(url);
    }
}