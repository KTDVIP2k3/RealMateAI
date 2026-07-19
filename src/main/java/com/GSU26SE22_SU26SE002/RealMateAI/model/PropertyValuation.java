package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Yêu cầu ĐỊNH GIÁ THỦ CÔNG — Seller chọn 1 Property của mình, gửi yêu cầu để
 * Staff kiểm tra thông tin và đưa ra mức giá đề xuất (totalValue). Entity này
 * TRƯỚC ĐÓ đã tồn tại sẵn trong schema (chưa có repository/service/controller
 * nào dùng) — TÁI SỬ DỤNG đúng entity này thay vì tạo bảng mới, đúng tinh
 * thần đã áp dụng cho audit_log/active_log (event tracking) và property_image
 * (tài liệu tích xanh).
 *
 * KHÁC với công cụ định giá AI tự động (XGBoost, POST /ai/property-valuation)
 * — đây là quy trình CON NGƯỜI (Staff) đánh giá, kết quả tin cậy cao hơn
 * nhưng cần thời gian xử lý. Seller có thể dùng cả 2: AI để tham khảo nhanh,
 * sau đó gửi yêu cầu này nếu muốn Staff xác nhận chính thức.
 *
 * account: Staff đã XỬ LÝ yêu cầu (null khi còn PENDING, set khi COMPLETED/FAILED).
 * Người GỬI yêu cầu suy ra từ property.getSeller() — 1 Property luôn thuộc
 * đúng 1 Seller, không cần field riêng.
 *
 * Các field marketUnitPrice/locationK/gfa/constructionNewPrice/remainingQuantity/
 * landPrice/constructionCost là chi tiết theo PHƯƠNG PHÁP CHI PHÍ (Cost
 * Approach) — OPTIONAL, Staff điền nếu muốn thể hiện cách tính chi tiết;
 * totalValue là BẮT BUỘC (mức giá đề xuất cuối cùng, dù có điền chi tiết
 * cách tính hay không).
 */

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "property_valuation")
public class PropertyValuation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_valuation_id")
    private Integer propertyValuationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    /** Staff đã xử lý yêu cầu — null khi còn PENDING. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    private Long marketUnitPrice;
    @Column(name = "locationk")
    private Long locationK;
    private Long gfa;
    private Long constructionNewPrice;
    private Long remainingQuantity;
    private Long landPrice;
    private Long constructionCost;

    /** Mức giá đề xuất cuối cùng — bắt buộc khi status = COMPLETED. */
    private Long totalValue;

    /** Ghi chú của Seller khi gửi yêu cầu (vd tình trạng sửa chữa gần đây, mong muốn giá). */
    @Column(name = "seller_note", columnDefinition = "TEXT")
    private String sellerNote;

    /** Nhận định/căn cứ của Staff khi đưa ra mức giá, hoặc lý do FAILED. */
    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private PropertyValuationStatusEnum propertyValuationStatus;
    private LocalDateTime reviewedAt;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
