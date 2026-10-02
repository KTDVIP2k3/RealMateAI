package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.SearchHistory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Integer> {

    Optional<SearchHistory> findByAccount_AccountIdAndKeywordIgnoreCase(Integer accountId, String keyword);

    List<SearchHistory> findTop5ByAccount_AccountIdOrderByUpdatedAtDesc(Integer accountId);


    @Query("SELECT sh FROM SearchHistory sh WHERE sh.account.accountId = :accountId "
            + "AND LOWER(sh.keyword) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' "
            + "ORDER BY sh.updatedAt DESC")
    List<SearchHistory> findTop5ByAccountIdAndKeywordContainingEscaped(
            @Param("accountId") Integer accountId, @Param("keyword") String keyword, Pageable pageable);

    default List<SearchHistory> findTop5ByAccount_AccountIdAndKeywordContainingIgnoreCaseOrderByUpdatedAtDesc(
            Integer accountId, String keyword) {
        return findTop5ByAccountIdAndKeywordContainingEscaped(accountId, keyword, PageRequest.of(0, 5));
    }

    /** Bản ghi cũ nhất — dùng để dọn bớt khi vượt giới hạn SEARCH_HISTORY_CAP mỗi tài khoản. */
    List<SearchHistory> findTop5ByAccount_AccountIdOrderByUpdatedAtAsc(Integer accountId);

    long countByAccount_AccountId(Integer accountId);
}
