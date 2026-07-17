package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {
    private String full_name;

    @Schema(
            type = "string",
            format = "date",
            example = "2003-08-27"
    )
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birth_date;
    private MultipartFile avatar;
    private String phone;
}
