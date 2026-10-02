package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.SearchHistory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SearchHistoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SearchHistoryTrackingService;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.UserEventTrackingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Tách khỏi ListingServiceImplement — xem javadoc SearchHistoryTrackingService.
 * Logic nghiệp vụ giữ NGUYÊN như bản cũ (findOrCreate + dọn bớt theo CAP),
 * chỉ thêm bước cắt keyword về tối đa MAX_KEYWORD_LENGTH ký tự trước khi lưu
 * (tránh lỗi constraint DB khi keyword quá dài làm transaction rollback).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchHistoryTrackingServiceImplement implements SearchHistoryTrackingService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserEventTrackingService userEventTrackingService;

    // Giữ nguyên giá trị như hằng số cũ bên ListingServiceImplement.
    private static final int SEARCH_HISTORY_CAP = 20;
    // MỚI: giới hạn độ dài keyword lưu vào DB — tránh lỗi constraint cột
    // keyword (VD varchar(255)) làm transaction rollback-only.
    private static final int MAX_KEYWORD_LENGTH = 255;

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void recordSearchHistory(Account account, String keyword) {
        try {
            String trimmed = keyword.trim();
            if (trimmed.length() > MAX_KEYWORD_LENGTH) {
                trimmed = trimmed.substring(0, MAX_KEYWORD_LENGTH);
            }
            LocalDateTime now = LocalDateTime.now();

            userEventTrackingService.recordSilently(account, UserEventTypeEnum.SEARCH, null);

            SearchHistory existing = searchHistoryRepository
                    .findByAccount_AccountIdAndKeywordIgnoreCase(account.getAccountId(), trimmed)
                    .orElse(null);

            if (existing != null) {
                existing.setUpdatedAt(now);
                searchHistoryRepository.save(existing);
                return;
            }

            SearchHistory history = SearchHistory.builder()
                    .account(account)
                    .keyword(trimmed)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            searchHistoryRepository.save(history);

            long total = searchHistoryRepository.countByAccount_AccountId(account.getAccountId());
            if (total > SEARCH_HISTORY_CAP) {
                searchHistoryRepository
                        .findTop5ByAccount_AccountIdOrderByUpdatedAtAsc(account.getAccountId())
                        .stream()
                        .limit(total - SEARCH_HISTORY_CAP)
                        .forEach(searchHistoryRepository::delete);
            }
        } catch (Exception e) {
            // Cố ý CHỈ log — lỗi ghi lịch sử tìm kiếm KHÔNG được làm hỏng kết
            // quả search chính (xem javadoc SearchHistoryTrackingService).
            log.warn("[SearchHistoryTrackingService] recordSearchHistory lỗi, bỏ qua: {}", e.getMessage());
        }
    }
}
