package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SubmitValuationRequest {

    @NotNull(message = "propertyId không được để trống")
    private Integer propertyId;

    /** Ghi chú thêm cho Staff (optional) — vd tình trạng sửa chữa gần đây, mong muốn về giá. */
    private String sellerNote;
}
