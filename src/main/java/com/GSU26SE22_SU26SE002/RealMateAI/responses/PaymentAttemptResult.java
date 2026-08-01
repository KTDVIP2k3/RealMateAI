package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptResult {

    private boolean success;
    private String errorCode;
    private String message;

    private Integer postingPackageOrderId;

    public static PaymentAttemptResult ok(Integer postingPackageOrderId) {
        return PaymentAttemptResult.builder()
                .success(true)
                .postingPackageOrderId(postingPackageOrderId)
                .message("Thanh toán gói dịch vụ đăng tin thành công")
                .build();
    }

    /** Dùng khi chưa kịp tạo order (postingPackageId/listingId không tồn tại). */
    public static PaymentAttemptResult fail(String errorCode, String message) {
        return PaymentAttemptResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    /** MỚI: dùng khi order ĐÃ được tạo (status=FAILED) — kèm orderId để FE gọi retry-pay. */
    public static PaymentAttemptResult fail(String errorCode, String message, Integer postingPackageOrderId) {
        return PaymentAttemptResult.builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .postingPackageOrderId(postingPackageOrderId)
                .build();
    }
}
