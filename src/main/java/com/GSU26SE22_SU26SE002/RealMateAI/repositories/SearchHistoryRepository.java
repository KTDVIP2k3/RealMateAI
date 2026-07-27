package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Integer> {

    /** Dùng để UPSERT — nếu tài khoản đã từng tìm đúng keyword này (không phân biệt hoa/thường) thì chỉ cập nhật updatedAt. */
    Optional<SearchHistory> findByAccount_AccountIdAndKeywordIgnoreCase(Integer accountId, String keyword);

    /** Recent Search khi q rỗng — 5 từ khoá tìm gần đây nhất của tài khoản. */
    List<SearchHistory> findTop5ByAccount_AccountIdOrderByUpdatedAtDesc(Integer accountId);

    /** Recent Search khi có q — lọc theo từ khoá đang gõ, mới nhất trước. */
    List<SearchHistory> findTop5ByAccount_AccountIdAndKeywordContainingIgnoreCaseOrderByUpdatedAtDesc(
            Integer accountId, String keyword);

    /** Bản ghi cũ nhất — dùng để dọn bớt khi vượt giới hạn SEARCH_HISTORY_CAP mỗi tài khoản. */
    List<SearchHistory> findTop5ByAccount_AccountIdOrderByUpdatedAtAsc(Integer accountId);

    long countByAccount_AccountId(Integer accountId);
}
