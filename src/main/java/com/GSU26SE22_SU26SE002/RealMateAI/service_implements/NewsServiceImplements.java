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
import org.springframework.context.annotation.Profile;
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
//@Profile("local")
public class NewsServiceImplements implements NewsServiceInterface {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private NewsCategoryRepository newsCategoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    private static final int DAILY_TARGET_NEWS = 50;
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

//    @Scheduled(initialDelay = 5000, fixedDelay = 600000)
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
        int pageNum = 1;

        BrowserContext context = null;
        Page page = null;

        try (Playwright playwright = Playwright.create()) {
            List<String> browserArgs = Arrays.asList(
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                    "--disable-session-crashed-bubble",
                    "--hide-crash-restore-bubble",
                    "--window-size=1920,1080",
                    "--start-maximized",
                    "--lang=vi-VN,vi",
                    "--crash-dumps-dir=" + crashDir.toString()
            );

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.put("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");
            headers.put("Cache-Control", "max-age=0");
            headers.put("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
            headers.put("Sec-Ch-Ua-Mobile", "?0");
            headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "none");
            headers.put("Sec-Fetch-User", "?1");
            headers.put("Upgrade-Insecure-Requests", "1");

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

            context.addInitScript(
                    "(() => {\n" +
                            " Object.defineProperty(navigator, 'webdriver', { get: () => undefined });\n" +
                            " window.chrome = { runtime: {}, app: { isInstalled: false } };\n" +
                            " Object.defineProperty(navigator, 'languages', { get: () => ['vi-VN', 'vi', 'en-US', 'en'] });\n" +
                            " Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });\n" +
                            " Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });\n" +
                            " Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });\n" +
                            " const originalQuery = window.navigator.permissions.query;\n" +
                            " window.navigator.permissions.query = (parameters) => (\n" +
                            " parameters.name === 'notifications' ?\n" +
                            " Promise.resolve({ state: Notification.permission }) :\n" +
                            " originalQuery(parameters)\n" +
                            " );\n" +
                            "})();"
            );

            page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            List<News> allExistingNews = newsRepository.findAll();
            Set<String> existingUrls = allExistingNews.stream()
                    .map(News::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> existingTitles = allExistingNews.stream()
                    .map(n -> n.getTitle() != null ? n.getTitle().toLowerCase().trim() : "")
                    .collect(Collectors.toSet());

            Set<String> processedUrlsInBatch = new HashSet<>();

            while (this.currentDailyCrawledCount < DAILY_TARGET_NEWS && pageNum <= 10) {
                String targetUrl = (pageNum == 1)
                        ? "https://batdongsan.com.vn/tin-tuc"
                        : "https://batdongsan.com.vn/tin-tuc/p" + pageNum;

                try {
                    System.out.println("\n[NEWS] MỞ TRANG DANH SÁCH (Trang " + pageNum + "): " + targetUrl);
                    page.navigate(targetUrl, new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(45000));

                    page.mouse().move(150, 200);
                    page.mouse().move(350, 450);
                    randomSleep(2000, 3500);

                    String pageTitle = page.title();
                    if (pageTitle.contains("Just a moment") || pageTitle.contains("Attention Required")
                            || pageTitle.contains("Access Denied") || pageTitle.contains("Thực hiện xác minh bảo mật")) {
                        System.out.println("⚠️ [CLOUDFLARE BLOCK] IP hoặc trình duyệt bị Cloudflare chặn tại: " + targetUrl);
                        break;
                    }

                    Document doc = Jsoup.parse(page.content());
                    Elements articleCards = doc.select(".re__article-card, .re__news-card, .re__large-card, .re__card-full, .re__card-news, div[class*='article'], div[class*='news-card'], a[href*='/tin-tuc/']");

                    if (articleCards.isEmpty()) {
                        System.out.println("🛑 [HẾT TRANG] Trang " + pageNum + " không tìm thấy card tin tức nào.");
                        break;
                    }

                    List<News> freshCandidates = new ArrayList<>();

                    for (Element card : articleCards) {
                        News news = extractBasicNewsInfo(card);
                        if (news != null && news.getSourceUrl() != null) {
                            String url = news.getSourceUrl();
                            String titleLower = news.getTitle() != null ? news.getTitle().toLowerCase().trim() : "";

                            if (!existingUrls.contains(url) && !existingTitles.contains(titleLower) && !processedUrlsInBatch.contains(url)) {
                                processedUrlsInBatch.add(url);
                                freshCandidates.add(news);
                            }
                        }
                    }

                    System.out.println("--> Tìm thấy " + freshCandidates.size() + " tin tức mới hợp lệ trên trang " + pageNum);

                    List<News> pageResultList = new ArrayList<>();
                    for (News candidate : freshCandidates) {
                        if (this.currentDailyCrawledCount >= DAILY_TARGET_NEWS) {
                            System.out.println("🎯 [NEWS CRAWLER] Đã đạt chỉ tiêu " + DAILY_TARGET_NEWS + " tin trong ngày.");
                            break;
                        }

                        News fullNews = fetchNewsDetail(candidate, page);
                        if (fullNews != null && isValidNews(fullNews)) {
                            pageResultList.add(fullNews);
                            this.currentDailyCrawledCount++;
                            totalCrawledInBatch++;
                            System.out.println(" ✅ [CÀO THÀNH CÔNG " + this.currentDailyCrawledCount + "/" + DAILY_TARGET_NEWS + "] " + fullNews.getTitle());
                        }
                    }

                    if (!pageResultList.isEmpty()) {
                        saveNewsInTransaction(pageResultList);
                        pageResultList.forEach(n -> {
                            existingUrls.add(n.getSourceUrl());
                            existingTitles.add(n.getTitle().toLowerCase().trim());
                        });
                    }

                    if (this.currentDailyCrawledCount >= DAILY_TARGET_NEWS) {
                        break;
                    }

                    pageNum++;
                    randomSleep(2000, 4000);

                } catch (Exception pageEx) {
                    System.err.println("⚠️ Lỗi khi cào trang news " + pageNum + ": " + pageEx.getMessage());
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi hệ thống Playwright News Crawler: " + e.getMessage());
        } finally {
            if (page != null && !page.isClosed()) try { page.close(); } catch (Exception ignored) {}
            if (context != null) try { context.close(); } catch (Exception ignored) {}
            if (isServer) try { FileSystemUtils.deleteRecursively(userDataDir); } catch (Exception ignored) {}
        }

        System.out.println("\n🎉 HOÀN THÀNH BẢN CÀO TIN TỨC: + " + totalCrawledInBatch + " tin mới! (Tổng hôm nay: " + this.currentDailyCrawledCount + "/" + DAILY_TARGET_NEWS + ")");
    }

    private News extractBasicNewsInfo(Element card) {
        try {
            Element linkElem = card.is("a") ? card : card.selectFirst("a.re__card-title, h3.re__card-title a, a[href*='/tin-tuc/']");
            if (linkElem == null) return null;

            String detailLink = linkElem.attr("href");
            if (detailLink.isEmpty() || detailLink.equals("#") || detailLink.endsWith("/tin-tuc") || detailLink.endsWith("/tin-tuc/")) {
                return null;
            }

            String fullUrl = detailLink.startsWith("http") ? detailLink : "https://batdongsan.com.vn" + detailLink;

            String title = linkElem.text().trim();
            if (title.isEmpty()) {
                Element titleElem = card.selectFirst("h3, .re__card-title, .title");
                if (titleElem != null) {
                    title = titleElem.text().trim();
                }
            }
            if (title.isEmpty()) return null;

            Element imgElem = card.selectFirst("img");
            String thumbnailUrl = "";
            if (imgElem != null) {
                thumbnailUrl = imgElem.hasAttr("data-src") ? imgElem.attr("data-src") : imgElem.attr("src");
            }

            Element summaryElem = card.selectFirst(".re__card-summary, .re__article-summary, p");
            String summary = (summaryElem != null) ? summaryElem.text().trim() : "";

            return News.builder()
                    .title(title)
                    .slug(convertToSlug(title) + "-" + System.currentTimeMillis() % 10000)
                    .sourceUrl(fullUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .summary(summary)
                    .sourceName("Batdongsan.com.vn")
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

    private News fetchNewsDetail(News news, Page page) {
        try {
            page.navigate(news.getSourceUrl(), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30000));

            scrollPageSmoothly(page);
            randomSleep(1000, 2000);

            Document doc = Jsoup.parse(page.content());

            Element contentElem = doc.selectFirst(".re__section-body, .re__detail-content, article");
            if (contentElem != null) {
                contentElem.select(".re__banner-inarticle, .re__social-share, .re__related-news, script, style").remove();
                news.setContent(contentElem.html());
            } else if (news.getSummary() != null) {
                news.setContent(news.getSummary());
            }

            String categoryName = "Tin Tức Bất Động Sản";
            Elements breadcrumbs = doc.select(".re__breadcrumb a, .breadcrumb a");
            if (!breadcrumbs.isEmpty()) {
                categoryName = breadcrumbs.last().text().trim();
            }

            NewsCategory category = getOrCreateCategory(categoryName);
            news.setNewsCategory(category);

            return news;
        } catch (Exception e) {
            return null;
        }
    }

    private NewsCategory getOrCreateCategory(String categoryName) {
        return newsCategoryRepository.findByName(categoryName).orElseGet(() -> {
            NewsCategory newCat = NewsCategory.builder()
                    .name(categoryName)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return newsCategoryRepository.save(newCat);
        });
    }

    private boolean isValidNews(News news) {
        if (news == null) return false;
        if (news.getTitle() == null || news.getTitle().length() < 10) return false;
        if (news.getContent() == null || news.getContent().length() < 100) return false;
        return true;
    }

    @Transactional
    public void saveNewsInTransaction(List<News> newsList) {
        try {
            newsRepository.saveAll(newsList);
        } catch (Exception ignored) {}
    }

    private void scrollPageSmoothly(Page page) {
        try {
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight / 2, behavior: 'smooth'});");
            randomSleep(500, 1000);
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'});");
        } catch (Exception ignored) {}
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