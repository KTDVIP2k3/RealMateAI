package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.EntityType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaMultiUploadRequest {

    @NotEmpty(message = "Danh sách file không được rỗng")
    private List<MultipartFile> files;

    private EntityType entityType;

    private Long entityId;

    private String folder;
}
