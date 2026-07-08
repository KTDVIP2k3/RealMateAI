package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.News;
import com.GSU26SE22_SU26SE002.RealMateAI.model.NewsCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PageRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.NewsDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PagedResponseDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsServiceInterface;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NewsServiceImplements implements NewsServiceInterface {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsCategoryRepository newsCategoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getAllNewsPaged(PageRequest pageRequest) {
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(
                    pageRequest.getPage(),
                    pageRequest.getSize(),
                    Sort.by("createdAt").descending()
            );

            Page<News> newsPage = newsRepository.findAll(pageable);

            List<NewsDTO> dtoList = newsPage.getContent().stream().map(news -> {
                NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                if (news.getNewsCategory() != null) {
                    dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                    dto.setNewsCategoryName(news.getNewsCategory().getName());
                }
                return dto;
            }).collect(Collectors.toList());

            int currentPage = newsPage.getNumber();
            int totalPages = newsPage.getTotalPages();
            int currentChunk = currentPage / 10;
            int startPage = currentChunk * 10;
            int endPage = Math.min(startPage + 9, totalPages - 1);

            List<Integer> pageNumbers = new ArrayList<>();
            for (int i = startPage; i <= endPage; i++) {
                pageNumbers.add(i);
            }

            PagedResponseDTO<NewsDTO> pagedResponse = PagedResponseDTO.<NewsDTO>builder()
                    .content(dtoList)
                    .pageNo(currentPage)
                    .pageSize(newsPage.getSize())
                    .totalElements(newsPage.getTotalElements())
                    .totalPages(totalPages)
                    .isLast(newsPage.isLast())
                    .pageNumbers(pageNumbers)
                    .hasPreviousChunk(startPage > 0)
                    .hasNextChunk(endPage < totalPages - 1)
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(pagedResponse, "Get news successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getNewsByCategoryIdPaged(Integer categoryId, PageRequest pageRequest) {
        try {
            Optional<NewsCategory> categoryOpt = newsCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Category not found"));
            }

            Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), Sort.by("createdAt").descending());
            Page<News> newsPage = newsRepository.findByNewsCategoryAndIsActiveTrue(categoryOpt.get(), pageable);

            List<NewsDTO> dtoList = newsPage.getContent().stream().map(news -> {
                NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                dto.setNewsCategoryName(news.getNewsCategory().getName());
                return dto;
            }).collect(Collectors.toList());

            PagedResponseDTO<NewsDTO> pagedResponse = PagedResponseDTO.<NewsDTO>builder()
                    .content(dtoList)
                    .pageNo(newsPage.getNumber())
                    .pageSize(newsPage.getSize())
                    .totalElements(newsPage.getTotalElements())
                    .totalPages(newsPage.getTotalPages())
                    .isLast(newsPage.isLast())
                    .build();

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(pagedResponse, "Get news by category successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Scheduled(cron = "0 0 2 */3 * *")
    @Override
    public void autoCrawlNewsData() {
        this.executeCrawlLogic();
    }

    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    public void crawlImmediatelyOnStartup() {
        this.executeCrawlLogic();
    }

    private void executeCrawlLogic() {
        try {
            List<NewsCategory> categories = newsCategoryRepository.findAll();

            for (NewsCategory category : categories) {
                if (category.getIsActive() == null || !category.getIsActive()) {
                    continue;
                }

                String searchKeyword = category.getName().trim();
                String targetUrl = "https://timkiem.vnexpress.net/?q=" + searchKeyword;

                Document doc;
                try {
                    doc = Jsoup.connect(targetUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .timeout(30000)
                            .get();
                } catch (Exception e) {
                    continue;
                }

                if (doc == null) {
                    continue;
                }

                Elements postElements = doc.select(".item-news");
                if (postElements.isEmpty()) {
                    continue;
                }

                int savedCount = 0;

                for (Element element : postElements) {
                    if (savedCount >= 5) {
                        break;
                    }

                    try {
                        Element titleElement = element.selectFirst(".title-news a");
                        if (titleElement == null) {
                            continue;
                        }

                        String title = titleElement.text();
                        String sourceUrl = titleElement.attr("href");

                        String summary = element.select(".description a").text();

                        Element imgElement = element.selectFirst(".thumb-art img");
                        String imageUrl = "";
                        if (imgElement != null) {
                            imageUrl = imgElement.attr("data-src");
                            if (imageUrl.isEmpty()) {
                                imageUrl = imgElement.attr("src");
                            }
                        }

                        if (title == null || title.trim().isEmpty()) {
                            continue;
                        }

                        if (newsRepository.existsByTitle(title)) {
                            continue;
                        }

                        String slug = convertToSlug(title);
                        if (newsRepository.existsBySlug(slug)) {
                            slug = slug + "-" + System.currentTimeMillis();
                        }

                        News news = News.builder()
                                .title(title)
                                .slug(slug)
                                .summary(summary)
                                .content(summary)
                                .thumbnailUrl(imageUrl)
                                .sourceUrl(sourceUrl)
                                .sourceName("VnExpress")
                                .newsCategory(category)
                                .viewCount(0)
                                .isFeatured(false)
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                        newsRepository.save(news);
                        savedCount++;

                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private String convertToSlug(String title) {
        String temp = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(temp).replaceAll("").toLowerCase();
        slug = slug.replaceAll("đ", "d");
        slug = slug.replaceAll("[^a-z0-9\\s-]", "");
        slug = slug.replaceAll("[\\s-]+", "-");
        slug = slug.replaceAll("^[-]+|[-]+$", "");
        return slug;
    }
}