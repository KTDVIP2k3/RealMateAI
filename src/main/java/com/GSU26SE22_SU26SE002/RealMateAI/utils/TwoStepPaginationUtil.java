package com.GSU26SE22_SU26SE002.RealMateAI.utils;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper DÙNG CHUNG cho toàn hệ thống khi cần phân trang một danh sách entity mà
 * đồng thời phải JOIN FETCH một collection (quan hệ {@code @OneToMany}/{@code @ManyToMany},
 * ví dụ {@code property.propertyImages}).
 * <p>
 * Vấn đề gốc: nếu vừa gắn {@code @EntityGraph}/JOIN FETCH một collection vừa dùng
 * {@code Pageable} trong CÙNG 1 query, Hibernate không thể phân trang bằng SQL
 * {@code LIMIT/OFFSET} (vì 1 dòng entity có thể nhân bản thành N dòng do collection),
 * nên nó buộc phải load TOÀN BỘ kết quả khớp điều kiện WHERE vào memory rồi tự cắt
 * trang bằng tay (log sẽ cảnh báo {@code firstResult/maxResults specified with
 * collection fetch; applying in memory}). Với bảng nhỏ thì không sao, nhưng dữ liệu
 * càng lớn thì càng chậm và tốn RAM.
 * <p>
 * Cách khắc phục triệt để — tách làm 2 query:
 * <ol>
 *   <li><b>Query 1 (idPageFetcher):</b> chỉ lấy ID đã phân trang thật sự bằng
 *       {@code LIMIT/OFFSET} ở tầng DB — KHÔNG fetch collection nào cả, nên Hibernate
 *       phân trang chuẩn, nhanh, không cảnh báo.</li>
 *   <li><b>Query 2 (detailFetcher):</b> fetch chi tiết đầy đủ (kèm JOIN FETCH
 *       collection cần thiết) CHỈ cho đúng danh sách ID ở bước 1 — KHÔNG kèm
 *       {@code Pageable} nên không bị giới hạn LIMIT/OFFSET, tránh mất dữ liệu.</li>
 * </ol>
 * Vì JPA {@code ... IN (:ids)} không đảm bảo thứ tự trả về, class này tự sắp xếp lại
 * kết quả bước 2 theo ĐÚNG thứ tự ID của bước 1 trước khi đóng gói thành {@link Page}.
 * <p>
 * Bất kỳ module nào trong hệ thống (News, Account, Investment...) gặp tình huống
 * tương tự (phân trang + JOIN FETCH collection) đều nên tái sử dụng helper này thay
 * vì tự viết lại logic phân trang thủ công.
 *
 * <pre>{@code
 * Page<Listing> page = TwoStepPaginationUtil.paginate(
 *         pageable,
 *         p -> listingRepository.findByIsActiveTrue(p).map(Listing::getListingId),
 *         ids -> listingRepository.findAllByListingIdInWithDetails(ids),
 *         Listing::getListingId
 * );
 * }</pre>
 */
public final class TwoStepPaginationUtil {



    private TwoStepPaginationUtil() {
    }

    /**
     * @param pageable       thông tin phân trang (page/size/sort) do caller truyền vào,
     *                       cũng chính là {@link Pageable} dùng để build lại {@link Page} kết quả.
     * @param idPageFetcher  query 1 — nhận {@link Pageable}, trả về {@link Page} chứa ID đã
     *                       phân trang + sort đúng theo yêu cầu (KHÔNG fetch collection).
     * @param detailFetcher  query 2 — nhận danh sách ID của trang hiện tại, trả về chi tiết
     *                       đầy đủ (CÓ fetch collection), không giới hạn số lượng.
     * @param idExtractor    hàm lấy ID từ 1 entity chi tiết (để map ngược lại theo thứ tự ID).
     * @return {@link Page} chứa entity chi tiết, đúng thứ tự, đúng metadata phân trang
     *         (totalElements/totalPages lấy từ query 1 — là nguồn "sự thật" duy nhất).
     */
    public static <ID, T> Page<T> paginate(
            Pageable pageable,
            Function<Pageable, Page<ID>> idPageFetcher,
            Function<List<ID>, List<T>> detailFetcher,
            Function<T, ID> idExtractor
    ) {
        Page<ID> idPage = idPageFetcher.apply(pageable);
        List<ID> orderedIds = idPage.getContent();

        if (orderedIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        List<T> details = detailFetcher.apply(orderedIds);

        // Map ID -> entity để sắp xếp lại đúng thứ tự đã phân trang/sort ở query 1
        // (query 2 dùng "IN (:ids)" nên DB không đảm bảo thứ tự trả về).
        Map<ID, T> byId = new LinkedHashMap<>();
        for (T item : details) {
            byId.put(idExtractor.apply(item), item);
        }

        List<T> ordered = orderedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull) // phòng trường hợp entity vừa bị xoá giữa 2 query
                .toList();

        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }
}
