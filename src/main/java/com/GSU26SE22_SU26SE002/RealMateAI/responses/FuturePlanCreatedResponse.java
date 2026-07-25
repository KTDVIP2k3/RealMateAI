package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturePlanCreatedResponse {

    /** ID version mới vừa tạo — dùng để gọi GET /investment-plans/future/{newVersionId} */
    private Integer newVersionId;
    private String newVersionName;

    private Integer sourceVersionId;
    private String sourceVersionName;

    private LocalDateTime createdAt;
}
