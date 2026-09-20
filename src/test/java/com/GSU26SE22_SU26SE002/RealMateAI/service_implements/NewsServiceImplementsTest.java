package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.News;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.NewsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho NewsServiceImplements — Functions 110-111
 * (9.2 News Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NewsServiceImplements — News Management")
class NewsServiceImplementsTest {

    @Mock private NewsRepository newsRepository;
    @Mock private NewsCategoryRepository newsCategoryRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private NewsServiceImplements newsService;

    private News activeNews;

    @BeforeEach
    void setUp() {
        activeNews = News.builder()
                .newsId(1).title("Tin tuc bat dong san")
                .isActive(true).viewCount(10)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(modelMapper.map(any(News.class), eq(NewsDTO.class))).thenReturn(new NewsDTO());
    }

    // ── 110. View All News ────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllNewsPaged")
    class GetAllNewsPagedTests {
        @Test
        @DisplayName("Trả về danh sách tin tức đang active, đã lọc bỏ tin không active")
        void getAllNewsPaged_onlyActiveNews() {
            News inactiveNews = News.builder().newsId(2).isActive(false).build();
            when(newsRepository.findAll()).thenReturn(List.of(activeNews, inactiveNews));

            ResponseEntity<ApiResponse> response = newsService.getAllNewsPaged(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi chưa có tin tức nào")
        void getAllNewsPaged_empty_returnsEmptyList() {
            when(newsRepository.findAll()).thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = newsService.getAllNewsPaged(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 111. View News Details ─────────────────────────────────────────────
    @Nested
    @DisplayName("getNewsDetailById")
    class GetNewsDetailByIdTests {
        @Test
        @DisplayName("Tăng viewCount và trả về chi tiết khi tin tức đang active")
        void getNewsDetailById_active_incrementsViewCountAndReturnsDetail() {
            when(newsRepository.findById(1)).thenReturn(Optional.of(activeNews));
            when(newsRepository.save(any(News.class))).thenReturn(activeNews);

            ResponseEntity<ApiResponse> response = newsService.getNewsDetailById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(11, activeNews.getViewCount());
            verify(newsRepository).save(activeNews);
        }

        @Test
        @DisplayName("Trả 404 khi tin tức không tồn tại")
        void getNewsDetailById_notFound_returns404() {
            when(newsRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = newsService.getNewsDetailById(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 404 khi tin tức đã bị ẩn (isActive=false)")
        void getNewsDetailById_inactive_returns404() {
            News inactive = News.builder().newsId(3).isActive(false).build();
            when(newsRepository.findById(3)).thenReturn(Optional.of(inactive));

            ResponseEntity<ApiResponse> response = newsService.getNewsDetailById(3);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }
}
