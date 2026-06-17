package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Data
public class MediaMultiUploadRequest {

    @NotEmpty(message = "Danh sách file không được rỗng")
    private List<MultipartFile> files;

    private EntityType entityType;

    private Long entityId;

    private String folder;
}
