package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response cho GET /investment-plans/versions/{versionId}/future-versions —
 * danh sách TÓM TẮT các FUTURE_PLAN version được sinh ra từ 1 version gốc
 * (bình thường) cụ thể.
 *
 * Field ID CỐ TÌNH đặt tên "futureVersionId" (KHÔNG dùng lại tên
 * "investmentProfileVersionId" như {@link ProfileVersionDTO}, dù về bản chất
 * kỹ thuật cả 2 đều là InvestmentProfileVersion.profileVersionId) — để FE
 * không nhầm lẫn giữa 2 loại ID khi cả 2 xuất hiện trên cùng 1 màn hình
 * (ví dụ màn hình "So sánh" vừa hiển thị version gốc vừa hiển thị danh sách
 * future-version phái sinh từ nó). Dùng đúng "futureVersionId" này để gọi
 * tiếp GET /investment-plans/future/{futureVersionId} lấy chi tiết đầy đủ.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FutureVersionSummaryDTO {

    /** = InvestmentProfileVersion.profileVersionId — dùng gọi GET /investment-plans/future/{futureVersionId} */
    private Integer futureVersionId;
    private String futureVersionName;

    /** Version gốc mà future-version này được sinh ra từ đó (baseVersion) */
    private Integer sourceVersionId;

    private Double actualCalculatedYield;
    private Double yieldDelta;
    private Integer portfolioScore;

    private Boolean isActive;
    private LocalDateTime createdAt;
}
