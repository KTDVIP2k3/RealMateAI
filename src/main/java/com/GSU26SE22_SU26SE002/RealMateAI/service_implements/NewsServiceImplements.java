package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.News;
import com.GSU26SE22_SU26SE002.RealMateAI.model.NewsCategory;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsCategoryRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.NewsRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.NewsDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsServiceInterface;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final int DAILY_TARGET_NEWS = 500;
    private int currentDailyCrawledCount = 0;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getAllNewsPaged(int page, int size) {
        try {
            List<News> allNews = newsRepository.findAll();
            if (allNews == null) {
                allNews = Collections.emptyList();
            }

            List<News> sortedList = allNews.stream()
                    .filter(n -> Boolean.TRUE.equals(n.getIsActive()))
                    .sorted(Comparator.comparing(
                            News::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
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
                pagedContent = sortedList.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
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

                pagedContent = slicedNews.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
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

            List<News> allNews = newsRepository.findAll().stream()
                    .filter(news -> news.getNewsCategory() != null
                            && Objects.equals(news.getNewsCategory().getNewsCategoryId(), categoryId)
                            && Boolean.TRUE.equals(news.getIsActive()))
                    .toList();

            List<News> sortedList = allNews.stream()
                    .sorted(Comparator.comparing(
                            News::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
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
                pagedContent = sortedList.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
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

                pagedContent = slicedNews.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getNewsDetailById(Integer newsId) {
        try {
            Optional<News> newsOpt = newsRepository.findById(newsId);
            if (newsOpt.isEmpty() || !Boolean.TRUE.equals(newsOpt.get().getIsActive())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "News not found"));
            }

            News news = newsOpt.get();
            news.setViewCount((news.getViewCount() == null ? 0 : news.getViewCount()) + 1);
            newsRepository.save(news);

            NewsDTO detailDTO = convertToDetailDTO(news);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(detailDTO, "Get news detail successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getNewsDetailBySlug(String slug) {
        try {
            List<News> allNews = newsRepository.findAll();
            Optional<News> newsOpt = allNews.stream()
                    .filter(n -> slug.equalsIgnoreCase(n.getSlug()) && Boolean.TRUE.equals(n.getIsActive()))
                    .findFirst();

            if (newsOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "News not found"));
            }

            News news = newsOpt.get();
            news.setViewCount((news.getViewCount() == null ? 0 : news.getViewCount()) + 1);
            newsRepository.save(news);

            NewsDTO detailDTO = convertToDetailDTO(news);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(detailDTO, "Get news detail successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private NewsDTO convertToSummaryDTO(News news) {
        NewsDTO dto = modelMapper.map(news, NewsDTO.class);
        dto.setNewsId(news.getNewsId());
        dto.setContent(null);
        if (news.getNewsCategory() != null) {
            dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
            dto.setNewsCategoryName(news.getNewsCategory().getName());
        }
        return dto;
    }

    private NewsDTO convertToDetailDTO(News news) {
        NewsDTO dto = modelMapper.map(news, NewsDTO.class);
        dto.setNewsId(news.getNewsId());
        if (news.getNewsCategory() != null) {
            dto.setNewsCategoryId(news.getNewsCategory().getNewsCategoryId());
            dto.setNewsCategoryName(news.getNewsCategory().getName());
        }
        return dto;
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void resetDailyCounter() {
        this.currentDailyCrawledCount = 0;
    }

    @Override
    public void autoCrawlNewsData() {
        this.executeCrawlLogic();
    }

    public void crawlImmediatelyOnStartup() {
        this.executeCrawlLogic();
    }

    private void executeCrawlLogic() {
        if (this.currentDailyCrawledCount >= DAILY_TARGET_NEWS) {
            System.out.println("🎯 [NEWS CRAWLER] Đã cào đủ " + DAILY_TARGET_NEWS + " tin trong ngày hôm nay. Dừng cào!");
            return;
        }

        boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null
                || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

        Path crashDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-crashes").toAbsolutePath();
        File crashFileDir = crashDir.toFile();
        if (!crashFileDir.exists()) crashFileDir.mkdirs();

        Path userDataDir = isServer
                ? Paths.get(System.getProperty("java.io.tmpdir"), "chrome-profile-news-" + System.currentTimeMillis()).toAbsolutePath()
                : Paths.get(System.getProperty("user.home"), ".chrome-news-profile").toAbsolutePath();
        File profileDir = userDataDir.toFile();
        if (!profileDir.exists()) profileDir.mkdirs();

        int totalCrawledInBatch = 0;
        int currentPageNum = 1;

        BrowserContext context = null;
        Page listPage = null;

        try (Playwright playwright = Playwright.create()) {
            List<String> browserArgs = Arrays.asList(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                    "--window-size=1920,1080",
                    "--start-maximized",
                    "--lang=vi-VN,vi"
            );

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.put("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");

            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(isServer)
                    .setIgnoreDefaultArgs(Arrays.asList("--enable-automation"))
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .setExtraHTTPHeaders(headers)
                    .setArgs(browserArgs)
                    .setViewportSize(1920, 1080);

            if (!isServer) {
                Path localChromePath = Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
                if (localChromePath.toFile().exists()) {
                    options.setExecutablePath(localChromePath);
                }
            }

            context = playwright.chromium().launchPersistentContext(userDataDir, options);
            listPage = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            List<News> allExistingNews = newsRepository.findAll();
            Set<String> existingUrls = allExistingNews.stream()
                    .map(News::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));

            Set<String> processedUrlsInBatch = new HashSet<>();

            while (this.currentDailyCrawledCount < DAILY_TARGET_NEWS) {
                String targetUrl = "https://vnexpress.net/kinh-doanh/bat-dong-san-p" + currentPageNum;
                System.out.println("\n[NEWS] MỞ TRANG DANH SÁCH VNEXPRESS (TRANG " + currentPageNum + "): " + targetUrl);

                try {
                    listPage.navigate(targetUrl, new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(30000));
                } catch (Exception e) {
                    System.err.println("⚠️ Lỗi điều hướng trang danh sách: " + e.getMessage());
                    currentPageNum++;
                    continue;
                }

                randomSleep(1500, 2500);

                Document doc = Jsoup.parse(listPage.content());
                Elements articleItems = doc.select(".item-news, .item-news-common, article.item-news");

                if (articleItems.isEmpty()) {
                    System.out.println("🛑 Không tìm thấy thẻ bài viết nào trên trang " + currentPageNum + ". Kết thúc quét.");
                    break;
                }

                List<News> freshCandidates = new ArrayList<>();
                for (Element item : articleItems) {
                    News news = extractVnExpressBasicInfo(item);
                    if (news != null && news.getSourceUrl() != null) {
                        String url = news.getSourceUrl();

                        if (!existingUrls.contains(url) && !processedUrlsInBatch.contains(url)) {
                            processedUrlsInBatch.add(url);
                            freshCandidates.add(news);
                        }
                    }
                }

                System.out.println("--> Tìm thấy " + freshCandidates.size() + " tin tức BĐS mới hợp lệ tại Trang " + currentPageNum);

                List<News> pageResultList = new ArrayList<>();
                for (News candidate : freshCandidates) {
                    if (this.currentDailyCrawledCount >= DAILY_TARGET_NEWS) {
                        break;
                    }

                    News fullNews = fetchVnExpressDetail(candidate, context);
                    if (fullNews != null && isValidNews(fullNews)) {
                        pageResultList.add(fullNews);
                        this.currentDailyCrawledCount++;
                        totalCrawledInBatch++;
                        System.out.println(" ✅ [CÀO THÀNH CÔNG " + this.currentDailyCrawledCount + "/" + DAILY_TARGET_NEWS + "] " + fullNews.getTitle());

                        existingUrls.add(fullNews.getSourceUrl());
                    }
                }

                if (!pageResultList.isEmpty()) {
                    saveNewsInTransaction(pageResultList);
                }

                if (this.currentDailyCrawledCount >= DAILY_TARGET_NEWS) {
                    System.out.println("🎯 [NEWS CRAWLER] Đã đạt đủ chỉ tiêu " + DAILY_TARGET_NEWS + " bài tin tức BĐS.");
                    break;
                }

                currentPageNum++;
                randomSleep(2000, 4000);
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi hệ thống VnExpress News Crawler: " + e.getMessage());
        } finally {
            if (listPage != null && !listPage.isClosed()) try { listPage.close(); } catch (Exception ignored) {}
            if (context != null) try { context.close(); } catch (Exception ignored) {}
            if (isServer) try { FileSystemUtils.deleteRecursively(userDataDir); } catch (Exception ignored) {}
        }

        System.out.println("\n🎉 HOÀN THÀNH ĐỢT CÀO TIN TỨC VNEXPRESS: + " + totalCrawledInBatch + " bài mới! (Tổng hôm nay: " + this.currentDailyCrawledCount + "/" + DAILY_TARGET_NEWS + ")");
    }

    private News extractVnExpressBasicInfo(Element item) {
        try {
            Element titleLink = item.selectFirst(".title-news a, h2.title-news a, h3.title-news a");
            if (titleLink == null) {
                titleLink = item.selectFirst("a[href*='.html']");
            }
            if (titleLink == null) return null;

            String fullUrl = titleLink.attr("href").trim();
            if (!fullUrl.startsWith("http")) {
                fullUrl = "https://vnexpress.net" + fullUrl;
            }

            if (fullUrl.contains("/video/") || fullUrl.contains("/podcast/") || fullUrl.contains("/interactive/")) {
                return null;
            }

            String title = titleLink.text().trim();
            if (title.isEmpty() || title.length() < 10) return null;

            Element imgElem = item.selectFirst("picture img, .thumb-art img, img");
            String thumbnailUrl = "";
            if (imgElem != null) {
                thumbnailUrl = imgElem.hasAttr("data-src") ? imgElem.attr("data-src") : imgElem.attr("src");
            }

            Element summaryElem = item.selectFirst(".description a, .description, p.description");
            String summary = (summaryElem != null) ? summaryElem.text().trim() : "";

            return News.builder()
                    .title(title)
                    .slug(convertToSlug(title) + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .sourceUrl(fullUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .summary(summary)
                    .sourceName("VnExpress")
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

    private News fetchVnExpressDetail(News news, BrowserContext context) {
        Page detailPage = null;
        try {
            detailPage = context.newPage();
            detailPage.navigate(news.getSourceUrl(), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(25000));

            try {
                detailPage.waitForSelector("article.fck_detail, .sidebar-1", new Page.WaitForSelectorOptions().setTimeout(4000));
            } catch (Exception ignored) {}

            Document doc = Jsoup.parse(detailPage.content());

            Element contentElem = doc.selectFirst("article.fck_detail, .fck_detail");
            if (contentElem != null) {
                contentElem.select(".insert-link-news, .banner-ads, script, style, .table-tracking").remove();
                news.setContent(contentElem.html());
            }

            if (news.getContent() == null || news.getContent().trim().length() < 50) {
                if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                    news.setContent("<p>" + news.getSummary() + "</p>");
                } else {
                    news.setContent("<p>" + news.getTitle() + "</p>");
                }
            }

            String categoryName = "Bất Động Sản";
            Element breadcrumbElem = doc.selectFirst(".breadcrumb a, .parent-cate a");
            if (breadcrumbElem != null) {
                String extractedName = breadcrumbElem.text().trim();
                if (!extractedName.isEmpty() && !extractedName.equalsIgnoreCase("Kinh doanh")) {
                    categoryName = extractedName;
                }
            }

            NewsCategory category = getOrCreateCategory(categoryName);
            news.setNewsCategory(category);

            return news;
        } catch (Exception e) {
            return null;
        } finally {
            if (detailPage != null && !detailPage.isClosed()) {
                try {
                    detailPage.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private NewsCategory getOrCreateCategory(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "Bất Động Sản";
        }

        String normalizedName = Arrays.stream(categoryName.trim().split("\\s+"))
                .map(word -> word.isEmpty() ? "" : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        return newsCategoryRepository.findByName(normalizedName).orElseGet(() -> {
            NewsCategory newCat = NewsCategory.builder()
                    .name(normalizedName)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return newsCategoryRepository.save(newCat);
        });
    }

    private boolean isValidNews(News news) {
        if (news == null) return false;
        if (news.getTitle() == null || news.getTitle().length() < 8) return false;
        if (news.getContent() == null || news.getContent().length() < 30) return false;
        return true;
    }

    @Transactional
    public void saveNewsInTransaction(List<News> newsList) {
        for (News news : newsList) {
            try {
                newsRepository.save(news);
            } catch (Exception e) {
                System.err.println("❌ LỖI DB KHI LƯU TIN [" + news.getTitle() + "]: " + e.getMessage());
            }
        }
    }

    private void randomSleep(long minMs, long maxMs) {
        try {
            Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs)));
        } catch (InterruptedException ignored) {}
    }

    private String convertToSlug(String title) {
        if (title == null) return "";
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