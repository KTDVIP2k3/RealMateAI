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
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
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

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAllNewsPaged(int page, int size) {
        try {
            List<News> allNews = newsRepository.findAll();
            if (allNews == null) {
                allNews = java.util.Collections.emptyList();
            }

            List<News> sortedList = allNews.stream()
                    .sorted(java.util.Comparator.comparing(
                            News::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<NewsDTO> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream().map(news -> {
                    NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                    if (news.getNewsCategory() != null) {
                        dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                        dto.setNewsCategoryName(news.getNewsCategory().getName());
                    }
                    return dto;
                }).collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                int offset = effectivePage * effectiveSize;
                totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
                isLast = effectivePage >= totalPages - 1;

                List<News> slicedNews = sortedList.stream()
                        .skip(offset)
                        .limit(effectiveSize)
                        .toList();

                pagedContent = slicedNews.stream().map(news -> {
                    NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                    if (news.getNewsCategory() != null) {
                        dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                        dto.setNewsCategoryName(news.getNewsCategory().getName());
                    }
                    return dto;
                }).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Get news successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getNewsByCategoryIdPaged(Integer categoryId, int page, int size) {
        try {
            Optional<NewsCategory> categoryOpt = newsCategoryRepository.findById(categoryId);
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Category not found"));
            }

            List<News> allNews = newsRepository.findByNewsCategoryAndIsActiveTrue(categoryOpt.get());
            if (allNews == null) {
                allNews = java.util.Collections.emptyList();
            }

            List<News> sortedList = allNews.stream()
                    .sorted(java.util.Comparator.comparing(
                            News::getCreatedAt,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<NewsDTO> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream().map(news -> {
                    NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                    if (news.getNewsCategory() != null) {
                        dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                        dto.setNewsCategoryName(news.getNewsCategory().getName());
                    }
                    return dto;
                }).collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                int offset = effectivePage * effectiveSize;
                totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
                isLast = effectivePage >= totalPages - 1;

                List<News> slicedNews = sortedList.stream()
                        .skip(offset)
                        .limit(effectiveSize)
                        .toList();

                pagedContent = slicedNews.stream().map(news -> {
                    NewsDTO dto = modelMapper.map(news, NewsDTO.class);
                    if (news.getNewsCategory() != null) {
                        dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
                        dto.setNewsCategoryName(news.getNewsCategory().getName());
                    }
                    return dto;
                }).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Get news by category successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

//    @Scheduled(cron = "0 0 7 * * MON", zone = "Asia/Ho_Chi_Minh")
    @Override
    public void autoCrawlNewsData() {
        this.executeCrawlLogic();
    }

//    @Scheduled(initialDelay = 5000, fixedDelay = Long.MAX_VALUE)
    public void crawlImmediatelyOnStartup() {
        this.executeCrawlLogic();
    }

    private void executeCrawlLogic() {
        try {
            List<NewsCategory> categories = newsCategoryRepository.findAll();


            List<News> allExistingNews = newsRepository.findAll();

            Set<String> existingSourceUrls = allExistingNews.stream()
                    .map(News::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> existingTitles = allExistingNews.stream()
                    .map(n -> n.getTitle() != null ? n.getTitle().toLowerCase() : "")
                    .collect(Collectors.toSet());

            Set<String> existingSlugs = allExistingNews.stream()
                    .map(News::getSlug)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

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

                if (doc == null) continue;

                Elements postElements = doc.select(".item-news");
                if (postElements.isEmpty()) continue;


                List<News> newsToSaveList = postElements.stream()
                        .map(element -> parseElementToNews(element, category, existingSourceUrls, existingTitles, existingSlugs))
                        .filter(Objects::nonNull)
                        .limit(5)
                        .collect(Collectors.toList());

                if (!newsToSaveList.isEmpty()) {
                    newsRepository.saveAll(newsToSaveList);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi crawl news: " + e.getMessage());
        }
    }


    private News parseElementToNews(Element element, NewsCategory category, Set<String> existingSourceUrls, Set<String> existingTitles, Set<String> existingSlugs) {
        try {
            Element titleElement = element.selectFirst(".title-news a");
            if (titleElement == null) return null;

            String title = titleElement.text().trim();
            String sourceUrl = titleElement.attr("href").trim();

            if (title.isEmpty() || sourceUrl.isEmpty()) return null;


            if (existingSourceUrls.contains(sourceUrl) || existingTitles.contains(title.toLowerCase())) {
                return null;
            }

            String summary = element.select(".description a").text();

            Element imgElement = element.selectFirst(".thumb-art img");
            String imageUrl = "";
            if (imgElement != null) {
                imageUrl = imgElement.attr("data-src");
                if (imageUrl.isEmpty()) {
                    imageUrl = imgElement.attr("src");
                }
            }


            String slug = convertToSlug(title);
            if (existingSlugs.contains(slug)) {
                slug = slug + "-" + System.currentTimeMillis();
            }


            existingSourceUrls.add(sourceUrl);
            existingTitles.add(title.toLowerCase());
            existingSlugs.add(slug);

            return News.builder()
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

        } catch (Exception e) {
            return null;
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