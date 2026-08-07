package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CrawPropertyListingServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CrawPropertyListingServiceImplement implements CrawPropertyListingServiceInterface {

    @Autowired
    private CrawPropertyListingRepository crawPropertyListingRepository;

    @Autowired
    private HeatmapZoneServiceInterface heatmapZoneService;

    private static final int DAILY_TARGET_LISTINGS = 1800;
    private int currentDailyCrawledCount = 0;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounter() {
        System.out.println("\n[SYSTEM] 🟢 SANG NGÀY MỚI (00:00)! Reset bộ đếm cào tin về 0.");
        this.currentDailyCrawledCount = 0;
    }
    @Override
    public void autoCrawlPropertyData() {
        boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null
                || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

        LocalDate today = LocalDate.now();
        int actualTodayInDb = (int) crawPropertyListingRepository.findAll().stream()
                .filter(l -> l.getCraw_date() != null &&
                        l.getCraw_date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().equals(today))
                .count();

        this.currentDailyCrawledCount = Math.max(this.currentDailyCrawledCount, actualTodayInDb);

        if (this.currentDailyCrawledCount >= DAILY_TARGET_LISTINGS) {
            System.out.println("[SCHEDULE] 🛑 Đã cào đủ quota hôm nay (" + this.currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " tin). Chờ 00:00 ngày mai!");
            return;
        }

        int remainingQuota = DAILY_TARGET_LISTINGS - this.currentDailyCrawledCount;
        System.out.println("\n=================== BẮT ĐẦU CÀO DỮ LIỆU (Đã cào hôm nay: "
                + this.currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " - Cần cào thêm tối đa: " + remainingQuota + " tin) ===================");
        System.out.flush();

        Path crashDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-crashes").toAbsolutePath();
        File crashFileDir = crashDir.toFile();
        if (!crashFileDir.exists()) crashFileDir.mkdirs();

        Path userDataDir = isServer
                ? Paths.get(System.getProperty("java.io.tmpdir"), "chrome-profile-bot-" + System.currentTimeMillis()).toAbsolutePath()
                : Paths.get(System.getProperty("user.home"), ".chrome-bot-profile").toAbsolutePath();
        File profileDir = userDataDir.toFile();
        if (!profileDir.exists()) profileDir.mkdirs();

        int totalCrawledInBatch = 0;
        int pageNum = 1;
        boolean reachedEndOfSource = false;

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

            Set<String> existingUrlsInDb = crawPropertyListingRepository.findAll().stream()
                    .map(CrawPropertyListing::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> processedUrlsInBatch = new HashSet<>();

            while ((this.currentDailyCrawledCount + totalCrawledInBatch) < DAILY_TARGET_LISTINGS) {
                String targetUrl = (pageNum == 1)
                        ? "https://batdongsan.com.vn/ban-nha-dat-tp-hcm"
                        : "https://batdongsan.com.vn/ban-nha-dat-tp-hcm/p" + pageNum;

                try {
                    System.out.println("\n[1] MỞ TRANG DANH SÁCH TỔNG (Trang " + pageNum + "): " + targetUrl);
                    System.out.flush();

                    try {
                        page.navigate(targetUrl, new Page.NavigateOptions()
                                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                                .setTimeout(45000));
                        page.mouse().move(150, 200);
                        page.mouse().move(350, 450);
                    } catch (TimeoutError te) {
                        System.out.println("⚠️ [TIMEOUT] Trang " + pageNum + " phản hồi quá lâu (>45s). Đánh dấu dừng cào!");
                        reachedEndOfSource = true;
                        break;
                    }

                    randomSleep(2500, 4000);

                    String currentUrl = page.url();
                    if (pageNum > 1 && !currentUrl.contains("/p" + pageNum)) {
                        System.out.println(" 🛑 [HẾT TRANG] Website tự chuyển hướng về trang gốc. Đã quét đến cuối nguồn!");
                        reachedEndOfSource = true;
                        break;
                    }

                    String pageTitle = page.title();
                    if (pageTitle.contains("Just a moment") || pageTitle.contains("Attention Required") || pageTitle.contains("Access Denied") || pageTitle.contains("Thực hiện xác minh bảo mật")) {
                        System.out.println(" ⚠️ [CLOUDFLARE BLOCK] IP hoặc trình duyệt bị Cloudflare chặn tại: " + targetUrl);
                        break;
                    }

                    Document doc = Jsoup.parse(page.content());
                    Elements propertyCards = doc.select(".re__card-full, .js__card");

                    if (propertyCards.isEmpty()) {
                        System.out.println(" 🛑 [HẾT TRANG] Trang " + pageNum + " không tìm thấy card bất động sản nào. Đã hết nguồn!");
                        reachedEndOfSource = true;
                        break;
                    }

                    List<CrawPropertyListing> freshCandidates = new ArrayList<>();
                    int duplicateCountInPage = 0;

                    for (Element card : propertyCards) {
                        if ((this.currentDailyCrawledCount + totalCrawledInBatch + freshCandidates.size()) >= DAILY_TARGET_LISTINGS) {
                            System.out.println(" 🎯 [QUOTA ALERT] Đã đủ danh sách tin cần cào! Ngừng gom tin thêm.");
                            break;
                        }

                        CrawPropertyListing listing = extractBasicInfo(card);
                        if (listing != null) {
                            String url = listing.getSourceUrl();
                            if (existingUrlsInDb.contains(url) || processedUrlsInBatch.contains(url)) {
                                duplicateCountInPage++;
                            } else {
                                processedUrlsInBatch.add(url);
                                freshCandidates.add(listing);
                            }
                        }
                    }

                    System.out.println(" --> Tìm thấy " + freshCandidates.size() + " tin mới hợp lệ (Trùng/Cũ: " + duplicateCountInPage + " tin)");
                    System.out.flush();

                    List<CrawPropertyListing> pageResultList = new ArrayList<>();
                    for (CrawPropertyListing candidate : freshCandidates) {
                        if ((this.currentDailyCrawledCount + totalCrawledInBatch) >= DAILY_TARGET_LISTINGS) {
                            System.out.println(" 🎯 [STOP CRAWL DETAILED] Đã đạt chính xác " + DAILY_TARGET_LISTINGS + " tin!");
                            break;
                        }

                        CrawPropertyListing fullListing = fetchCoordinates(candidate, page);

                        if (fullListing != null && fullListing.getLatitude() != null && fullListing.getLongitude() != null) {
                            pageResultList.add(fullListing);
                            totalCrawledInBatch++;
                        }
                    }

                    if (!pageResultList.isEmpty()) {
                        saveListingsInNewTransaction(pageResultList, "Trang " + pageNum);
                        existingUrlsInDb.addAll(pageResultList.stream().map(CrawPropertyListing::getSourceUrl).toList());
                    }

                    if ((this.currentDailyCrawledCount + totalCrawledInBatch) >= DAILY_TARGET_LISTINGS) {
                        System.out.println("\n🎯 [TARGET REACHED] Đã cào đủ quota " + DAILY_TARGET_LISTINGS + " tin hôm nay!");
                        break;
                    }

                    pageNum++;
                    randomSleep(2000, 4000);

                } catch (Exception crawlEx) {
                    System.out.println("Lỗi kết nối tại trang " + pageNum + " | Lý do: " + crawlEx.getMessage());
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi hệ thống Playwright: " + e.getMessage());
        } finally {
            if (page != null && !page.isClosed()) {
                try {
                    page.close();
                } catch (Exception ignored) {
                }
            }
            if (context != null) {
                try {
                    context.close();
                } catch (Exception ignored) {
                }
            }

            if (isServer) {
                try {
                    FileSystemUtils.deleteRecursively(userDataDir);
                } catch (Exception ignored) {
                }
            }
        }

        this.currentDailyCrawledCount += totalCrawledInBatch;

        System.out.println("\n=================== KẾT THÚC ĐỢT CÀO ===================");
        System.out.println("-> Cào được đợt này: " + totalCrawledInBatch + " tin mới.");
        System.out.println("-> Tổng tích lũy hôm nay: " + this.currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " tin.");

        boolean isQuotaReached = this.currentDailyCrawledCount >= DAILY_TARGET_LISTINGS;

        if (isQuotaReached || reachedEndOfSource) {
            System.out.println("\n🔥 [TRIGGER SNAPSHOT] Chốt sổ dữ liệu cào trong ngày (" + this.currentDailyCrawledCount + " tin).");
            System.out.println("[HEATMAP] Tiến hành lọc mẫu theo ngưỡng (>= N tin) và Tạo Snapshot Heatmap...");
            System.out.flush();
            try {
                heatmapZoneService.generateDailySnapshot();
                System.out.println("[HEATMAP] ✅ Đã hoàn tất tạo Snapshot Heatmap cho ngày hôm nay!");
            } catch (Exception heatmapEx) {
                System.out.println("[HEATMAP ERROR] ❌ Lỗi khi tạo Snapshot: " + heatmapEx.getMessage());
            }
        } else {
            System.out.println("[HEATMAP] ℹ️ Đang cào dở dang (" + this.currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " tin). Chờ đợt cào tiếp theo để chốt Snapshot.");
        }
        System.out.flush();
    }

    private CrawPropertyListing extractBasicInfo(Element card) {
        try {
            Element linkElem = card.selectFirst(".re__card-title a, h3.re__card-title a, a.js__product-link-for-product-id, h3 a");
            String detailLink = (linkElem != null) ? linkElem.attr("href") : "";

            if (detailLink.isEmpty()) {
                Element fallbackLink = card.selectFirst("a[href*='/ban-']");
                if (fallbackLink != null) detailLink = fallbackLink.attr("href");
            }

            if (detailLink.isEmpty() || detailLink.equals("#")) return null;

            String fullDetailUrl = detailLink.startsWith("http") ? detailLink : "https://batdongsan.com.vn" + detailLink;

            String rawPriceText = card.select(".re__card-config-price").text();
            BigDecimal area = parseArea(card.select(".re__card-config-area").text());

            BigDecimal price = null;
            BigDecimal pricePerM2 = null;

            if (rawPriceText != null && !rawPriceText.isEmpty() && !rawPriceText.contains("Thỏa thuận")) {
                if (rawPriceText.contains("tỷ") && (rawPriceText.contains("/m²") || rawPriceText.contains("/m2"))) {
                    String mainPriceText = rawPriceText.split("[~/]")[0].trim();
                    price = parsePriceTextToAmount(mainPriceText);

                    int index = rawPriceText.indexOf(rawPriceText.contains("~") ? "~" : "/");
                    String m2Text = rawPriceText.substring(index).trim();
                    pricePerM2 = parsePriceTextToAmount(m2Text);
                } else if (rawPriceText.contains("/m²") || rawPriceText.contains("/m2")) {
                    pricePerM2 = parsePriceTextToAmount(rawPriceText);
                    if (pricePerM2 != null && area != null && area.compareTo(BigDecimal.ZERO) > 0) {
                        price = pricePerM2.multiply(area).setScale(2, RoundingMode.HALF_UP);
                    }
                } else {
                    price = parsePriceTextToAmount(rawPriceText);
                    pricePerM2 = null;
                }
            }

            CrawPropertyListing listing = new CrawPropertyListing();
            listing.setSourceUrl(fullDetailUrl);
            listing.setPrice(price);
            listing.setArea(area);
            listing.setPricePerM2(pricePerM2);

            String dateText = card.select(".re__card-published-date").text();
            listing.setPosted_date(parsePostedDate(dateText));
            listing.setCraw_date(new Timestamp(System.currentTimeMillis()));

            if (!isValidListing(listing)) {
                return null;
            }

            return listing;
        } catch (Exception e) {
            return null;
        }
    }

    private CrawPropertyListing fetchCoordinates(CrawPropertyListing listing, Page page) {
        try {
            page.navigate(listing.getSourceUrl(), new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30000));

            scrollPageSmoothly(page);
            randomSleep(1500, 2500);

            try {
                page.waitForSelector("iframe[src*='google.com/maps'], iframe[data-src*='google.com/maps'], div#re-map, div[data-lat], .re__section-map",
                        new Page.WaitForSelectorOptions().setTimeout(4000));
            } catch (Exception ignored) {
            }

            String latStr = null;
            String lngStr = null;

            try {
                Object rawCoords = page.evaluate("() => {" +
                        " try {" +
                        " const nextData = document.getElementById('__NEXT_DATA__');" +
                        " if (nextData) {" +
                        " const json = JSON.parse(nextData.innerHTML);" +
                        " const details = json.props?.pageProps?.initialDetail || json.props?.pageProps?.productDetail || json.props?.pageProps?.detail;" +
                        " if (details && details.latitude) return {lat: details.latitude, lng: details.longitude};" +
                        " }" +
                        " } catch(e){}" +
                        " if (typeof initialData !== 'undefined' && initialData.latitude) return {lat: initialData.latitude, lng: initialData.longitude};" +
                        " if (window.RE && window.RE.propertyDetail) return {lat: window.RE.propertyDetail.latitude, lng: window.RE.propertyDetail.longitude};" +
                        " if (window.__INITIAL_STATE__ && window.__INITIAL_STATE__.details) return {lat: window.__INITIAL_STATE__.details.latitude, lng: window.__INITIAL_STATE__.details.longitude};" +
                        " const mapElem = document.querySelector('[data-lat], [data-latitude], #re-map, .re__section-map');" +
                        " if (mapElem) {" +
                        " return {" +
                        " lat: mapElem.getAttribute('data-lat') || mapElem.getAttribute('data-latitude')," +
                        " lng: mapElem.getAttribute('data-long') || mapElem.getAttribute('data-lng') || mapElem.getAttribute('data-longitude')" +
                        " };" +
                        " }" +
                        " return null;" +
                        "}");

                if (rawCoords instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) rawCoords;
                    if (map.get("lat") != null && map.get("lng") != null) {
                        latStr = map.get("lat").toString();
                        lngStr = map.get("lng").toString();
                    }
                }
            } catch (Exception ignored) {
            }

            if (latStr == null || lngStr == null) {
                try {
                    String mapIframeSrc = (String) page.evaluate("() => {" +
                            " const iframe = document.querySelector('iframe[data-src*=\"google.com/maps\"], iframe[src*=\"google.com/maps\"]');" +
                            " return iframe ? (iframe.getAttribute('data-src') || iframe.getAttribute('src')) : null;" +
                            "}");

                    if (mapIframeSrc != null) {
                        Matcher matcher = Pattern.compile("(?:q|ll|center)=([-\\d.]+)(?:,|%2C)([-\\d.]+)").matcher(mapIframeSrc);
                        if (matcher.find()) {
                            latStr = matcher.group(1);
                            lngStr = matcher.group(2);
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (latStr == null || lngStr == null) {
                String fullHtml = page.content();
                Matcher jsonMatcher = Pattern.compile("[\"']latitude[\"']\\s*:\\s*([0-9.]+)\\s*,\\s*[\"']longitude[\"']\\s*:\\s*([0-9.]+)").matcher(fullHtml);

                if (jsonMatcher.find()) {
                    latStr = jsonMatcher.group(1);
                    lngStr = jsonMatcher.group(2);
                } else {
                    Matcher mapUrlMatcher = Pattern.compile("(?:q|ll|center)=([-\\d.]+)(?:,|%2C)([-\\d.]+)").matcher(fullHtml);
                    if (mapUrlMatcher.find()) {
                        latStr = mapUrlMatcher.group(1);
                        lngStr = mapUrlMatcher.group(2);
                    } else {
                        Matcher latM = Pattern.compile("[\"']?(?:latitude|lat)[\"']?\\s*[:=]\\s*[\"']?([1-9]\\d*\\.\\d+)[\"']?").matcher(fullHtml);
                        Matcher lngM = Pattern.compile("[\"']?(?:longitude|lng|long)[\"']?\\s*[:=]\\s*[\"']?([1-9]\\d*\\.\\d+)[\"']?").matcher(fullHtml);
                        if (latM.find() && lngM.find()) {
                            latStr = latM.group(1);
                            lngStr = lngM.group(1);
                        }
                    }
                }
            }

            if (latStr != null && lngStr != null && !latStr.equals("0") && !latStr.isEmpty()) {
                double lat = Double.parseDouble(latStr);
                double lng = Double.parseDouble(lngStr);

                boolean isLatValid = lat >= 10.3 && lat <= 11.2;
                boolean isLngValid = lng >= 106.3 && lng <= 107.1;

                if (isLatValid && isLngValid) {
                    listing.setLatitude(lat);
                    listing.setLongitude(lng);
                    return listing;
                } else {
                    System.out.println(String.format("   👉 🚫 [VALIDATION REJECT] Tọa độ ngoài phạm vi TP.HCM: (%s, %s) -> Hủy bỏ Candidate!", lat, lng));
                    System.out.flush();
                    return null;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    private void scrollPageSmoothly(Page page) {
        try {
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight / 3, behavior: 'smooth'});");
            randomSleep(800, 1200);
            page.evaluate("() => window.scrollTo({top: (document.body.scrollHeight / 3) * 2, behavior: 'smooth'});");
            randomSleep(800, 1200);
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'});");
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listingResultList, String pageLabel) {
        if (listingResultList == null || listingResultList.isEmpty()) return;

        try {
            Set<String> existingUrls = crawPropertyListingRepository.findAll().stream()
                    .map(CrawPropertyListing::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<CrawPropertyListing> newEntitiesToSave = listingResultList.stream()
                    .filter(scraped -> scraped.getSourceUrl() != null && !existingUrls.contains(scraped.getSourceUrl()))
                    .collect(Collectors.toList());

            if (newEntitiesToSave.isEmpty()) {
                System.out.println(" --> [DB INFO] Không có tin mới nào để lưu (Tất cả đều trùng URL) (" + pageLabel + ")");
                return;
            }

            List<CrawPropertyListing> saved = crawPropertyListingRepository.saveAllAndFlush(newEntitiesToSave);
            System.out.println(" --> [DB SUCCESS] Đã lưu thành công " + saved.size() + " tin vào DB (" + pageLabel + ")");
            System.out.flush();

        } catch (Exception e) {
            System.out.println(" --> [DB ERROR] Lỗi khi lưu DB tại " + pageLabel + ": " + e.getMessage());
            System.out.flush();
        }
    }

    private void randomSleep(long minMs, long maxMs) {
        try {
            long sleepTime = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {
        }
    }

    private BigDecimal parsePriceTextToAmount(String priceText) {
        if (priceText == null || priceText.isEmpty() || priceText.contains("Thỏa thuận")) {
            return null;
        }

        try {
            String lowerText = priceText.toLowerCase().trim();

            if (lowerText.contains("-")) {
                lowerText = lowerText.split("-")[0].trim();
            }

            Matcher matcher = Pattern.compile("([0-9]+([.,][0-9]+)*)").matcher(lowerText);
            if (!matcher.find()) return null;

            String rawNum = matcher.group(1);

            if (rawNum.contains(".") && rawNum.contains(",")) {
                if (rawNum.lastIndexOf(".") < rawNum.lastIndexOf(",")) {
                    rawNum = rawNum.replace(".", "").replace(",", ".");
                } else {
                    rawNum = rawNum.replace(",", "");
                }
            } else if (rawNum.contains(".") || rawNum.contains(",")) {
                if (rawNum.matches(".*[.,]\\d{3}$")) {
                    rawNum = rawNum.replaceAll("[.,]", "");
                } else {
                    rawNum = rawNum.replace(",", ".");
                }
            }

            BigDecimal value = new BigDecimal(rawNum);

            if (lowerText.contains("tỷ")) {
                return value.multiply(BigDecimal.valueOf(1_000_000_000)).setScale(2, RoundingMode.HALF_UP);
            } else if (lowerText.contains("triệu") || lowerText.contains("tr")) {
                return value.multiply(BigDecimal.valueOf(1_000_000)).setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private BigDecimal parseArea(String areaText) {
        if (areaText == null || areaText.isBlank()) return BigDecimal.ZERO;
        try {
            String rawText = areaText.trim();

            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^\\s*([0-9.,]+)").matcher(rawText);
            if (matcher.find()) {
                rawText = matcher.group(1);
            } else {
                return BigDecimal.ZERO;
            }

            rawText = rawText.replace(".", "");
            rawText = rawText.replace(",", ".");

            BigDecimal area = new BigDecimal(rawText).setScale(2, java.math.RoundingMode.HALF_UP);
            return area.compareTo(BigDecimal.ZERO) > 0 ? area : BigDecimal.ZERO;
        } catch (Exception e) {
            System.out.println(" ⚠️ [PARSE AREA ERROR] Định dạng chuỗi diện tích không hợp lệ từ: " + areaText);
        }
        return BigDecimal.ZERO;
    }

    private Date parsePostedDate(String dateText) {
        if (dateText == null || dateText.isEmpty()) return new Date();
        if (dateText.contains("Hôm nay")) return new Date();
        if (dateText.contains("Hôm qua")) {
            return new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(dateText);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private boolean isValidListing(CrawPropertyListing listing) {
        if (listing == null) return false;

        if (listing.getPrice() == null || listing.getArea() == null || listing.getPricePerM2() == null) {
            return false;
        }

        BigDecimal minPricePerM2 = new BigDecimal("5000000");
        BigDecimal maxPricePerM2 = new BigDecimal("2000000000");

        BigDecimal minArea = new BigDecimal("5");
        BigDecimal maxArea = new BigDecimal("10000");

        BigDecimal minPrice = new BigDecimal("100000000");
        BigDecimal maxPrice = new BigDecimal("1000000000000");

        if (listing.getPricePerM2().compareTo(minPricePerM2) < 0 || listing.getPricePerM2().compareTo(maxPricePerM2) > 0) {
            System.out.println(" ⚠️ [VALIDATION REJECT] Giá/m2 bất thường: " + listing.getPricePerM2() + " VNĐ/m2 -> URL: " + listing.getSourceUrl());
            return false;
        }

        if (listing.getArea().compareTo(minArea) < 0 || listing.getArea().compareTo(maxArea) > 0) {
            System.out.println(" ⚠️ [VALIDATION REJECT] Diện tích bất thường: " + listing.getArea() + " m2 -> URL: " + listing.getSourceUrl());
            return false;
        }

        if (listing.getPrice().compareTo(minPrice) < 0 || listing.getPrice().compareTo(maxPrice) > 0) {
            System.out.println(" ⚠️ [VALIDATION REJECT] Tổng giá bất thường: " + listing.getPrice() + " VNĐ -> URL: " + listing.getSourceUrl());
            return false;
        }

        BigDecimal calculatedTotalPrice = listing.getPricePerM2().multiply(listing.getArea());
        BigDecimal diff = calculatedTotalPrice.subtract(listing.getPrice()).abs();
        BigDecimal allowedTolerance = listing.getPrice().multiply(new BigDecimal("0.05"));

        if (diff.compareTo(allowedTolerance) > 0) {
            System.out.println(" ⚠️ [VALIDATION REJECT] Lệch logic Giá/DiệnTích/ĐơnGiá! (Price: " + listing.getPrice() + ", Area: " + listing.getArea() + ", Price/m2: " + listing.getPricePerM2() + ")");
            return false;
        }

        return true;
    }

    private boolean isValidCoordinates(double latitude, double longitude) {
        boolean validLat = latitude >= 10.30 && latitude <= 11.20;
        boolean validLng = longitude >= 106.35 && longitude <= 107.00;

        if (!validLat || !validLng) {
            System.out.println(" ⚠️ [VALIDATION REJECT] Tọa độ ngoài khu vực TP.HCM: (" + latitude + ", " + longitude + ")");
        }

        return validLat && validLng;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 600000)
    public void fixExistingListingsCoordinates() {
        boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null
                || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

        List<CrawPropertyListing> entities = crawPropertyListingRepository.findAll();

        if (entities.isEmpty()) {
            System.out.println("[AUTO-MIGRATION] ✨ Tọa độ & Diện tích (area) cũ đã hoàn chỉnh và chính xác!");
            return;
        }

        System.out.println("\n==========================================================================================");
        System.out.println("[AUTO-MIGRATION] 🔍 Bắt đầu rà soát " + entities.size() + " tin (Kiểm tra Tọa độ & Đối chiếu Diện tích).");
        System.out.println("==========================================================================================\n");
        System.out.flush();

        Path crashDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-crashes-migration").toAbsolutePath();
        File crashFileDir = crashDir.toFile();
        if (!crashFileDir.exists()) crashFileDir.mkdirs();

        Path userDataDir = isServer
                ? Paths.get(System.getProperty("java.io.tmpdir"), "chrome-profile-migration-" + System.currentTimeMillis()).toAbsolutePath()
                : Paths.get(System.getProperty("user.home"), ".chrome-migration-profile-" + System.currentTimeMillis()).toAbsolutePath();
        File profileDir = userDataDir.toFile();
        if (!profileDir.exists()) profileDir.mkdirs();

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
                    "--disable-extensions",
                    "--disable-component-extensions-with-background-pages",
                    "--disable-background-networking",
                    "--allow-running-insecure-content",
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
                    .setIgnoreDefaultArgs(Arrays.asList("--enable-automation", "--disable-extensions"))
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .setExtraHTTPHeaders(headers)
                    .setArgs(browserArgs)
                    .setViewportSize(1920, 1080);

            if (!isServer) {
                Path localChromePath = Paths.get("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
                if (localChromePath.toFile().exists()) options.setExecutablePath(localChromePath);
            }

            context = playwright.chromium().launchPersistentContext(userDataDir, options);

            context.addInitScript(
                    "(() => {\n" +
                            "  Object.defineProperty(navigator, 'webdriver', { get: () => undefined });\n" +
                            "  window.chrome = { runtime: {}, app: { isInstalled: false } };\n" +
                            "  Object.defineProperty(navigator, 'languages', { get: () => ['vi-VN', 'vi', 'en-US', 'en'] });\n" +
                            "  Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });\n" +
                            "  Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });\n" +
                            "  Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });\n" +
                            "  const originalQuery = window.navigator.permissions.query;\n" +
                            "  window.navigator.permissions.query = (parameters) => (\n" +
                            "    parameters.name === 'notifications' ?\n" +
                            "    Promise.resolve({ state: Notification.permission }) :\n" +
                            "    originalQuery(parameters)\n" +
                            "  );\n" +
                            "})();"
            );

            page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);
            page.setDefaultTimeout(45000);
            page.setDefaultNavigationTimeout(45000);

            int processedCount = 0;
            int updatedCount = 0;
            List<CrawPropertyListing> batchToSave = new ArrayList<>();
            List<CrawPropertyListing> batchToDelete = new ArrayList<>();

            for (CrawPropertyListing listing : entities) {
                if (listing.getSourceUrl() == null) continue;

                processedCount++;
                System.out.println(String.format("[MIGRATION] 🌐 (%d / %d) ➔ Check URL: %s",
                        processedCount, entities.size(), listing.getSourceUrl()));
                System.out.flush();

                BigDecimal dbArea = listing.getArea();
                CrawPropertyListing updated = fetchCoordinates(listing, page);

                String pageTitle = "";
                try {
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
                    pageTitle = page.title();
                } catch (Exception e) {
                    System.out.println(" ⚠️ [WARN] Môi trường bị hủy/Đang nhảy trang. Đang đợi 3 giây để ổn định lại...");
                    try {
                        page.waitForTimeout(3000);
                        pageTitle = page.title();
                    } catch (Exception ignored) {
                        pageTitle = "Unknown (Navigation Error)";
                    }
                }

                if (pageTitle.contains("Just a moment") || pageTitle.contains("Attention Required") || pageTitle.contains("Access Denied") || pageTitle.contains("Thực hiện xác minh bảo mật")) {
                    System.out.println(" ⚠️ [CLOUDFLARE BLOCK] IP hoặc trình duyệt bị Cloudflare chặn tại chi tiết tin. Bỏ qua!");
                    continue;
                }

                BigDecimal webArea = null;
                boolean isInvalidPage = false;

                try {
                    String htmlContent = "";
                    try {
                        htmlContent = page.content();
                    } catch (Exception e) {
                        System.out.println(" ⚠️ [WARN] Không thể đọc content do trang đang nhảy link. Thử lại sau 2s...");
                        page.waitForTimeout(2000);
                        try { htmlContent = page.content(); } catch (Exception ignored) {}
                    }

                    if (htmlContent != null && !htmlContent.isBlank()) {
                        Document detailDoc = Jsoup.parse(htmlContent);

                        boolean hasSearchBar = detailDoc.selectFirst(".re__search-bar, input[placeholder*='Đường Lê Hồng Phong'], button:contains(Tìm kiếm)") != null;
                        boolean hasProductList = detailDoc.selectFirst(".re__left-container, .re__srp-list, #product-lists-page") != null;
                        boolean hasDetailContainer = detailDoc.selectFirst(".re__pr-specs-content, .re__ldp-container, #product-detail-page, .re__main-content") != null;

                        if ((hasSearchBar || hasProductList) && !hasDetailContainer) {
                            System.out.println(" ❌ [INVALID PAGE] Trang danh sách bộ lọc (Link cũ đã chết/Đổi hướng). Đánh dấu XÓA TIN!");
                            isInvalidPage = true;
                        }

                        if (!isInvalidPage) {
                            Element areaBlock = null;

                            Element specsContainer = detailDoc.selectFirst(".re__pr-specs-content, .re__section-body");
                            if (specsContainer != null) {
                                Element labelSpecs = specsContainer.selectFirst(".re__pr-specs-content-item-title:contains(Diện tích), .re__pr-specs-title:contains(Diện tích)");
                                if (labelSpecs != null) {
                                    areaBlock = labelSpecs.nextElementSibling();
                                }
                            }

                            if (areaBlock == null) {
                                Element areaItem = detailDoc.select("div.re__pr-short-info-item").stream()
                                        .filter(el -> el.select(".re__pr-short-info-item-title").text().contains("Diện tích"))
                                        .findFirst()
                                        .orElse(null);
                                if (areaItem != null) {
                                    areaBlock = areaItem.selectFirst("span.value");
                                }
                            }

                            if (areaBlock == null) {
                                Element mainContent = detailDoc.selectFirst(".re__main-content, .re__pr-short-info, #product-detail-page");
                                if (mainContent != null) {
                                    Elements tags = mainContent.select("span, div");
                                    for (Element el : tags) {
                                        String txt = el.text().trim();
                                        if (txt.contains("m²") && !txt.contains("/m²") && !txt.contains("PN") && txt.matches(".*\\d+.*") && txt.length() < 30) {
                                            areaBlock = el;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (areaBlock != null) {
                                String rawAreaText = areaBlock.text().split("\n")[0].trim();
                                if (rawAreaText.length() > 50) {
                                    System.out.println(" ❌ [INVALID PAGE] Chuỗi diện tích quá dài (Bốc trúng menu bộ lọc). Đánh dấu XÓA TIN!");
                                    isInvalidPage = true;
                                } else {
                                    if (rawAreaText.contains("m²")) {
                                        rawAreaText = rawAreaText.substring(0, rawAreaText.indexOf("m²") + 2);
                                    }
                                    webArea = parseAreaSafety(rawAreaText);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (isInvalidPage) {
                    updated = null;
                }

                if (updated != null) {
                    boolean hasChanges = false;

                    Double currentLat = listing.getLatitude();
                    Double currentLng = listing.getLongitude();
                    Double newLat = updated.getLatitude();
                    Double newLng = updated.getLongitude();

                    boolean isLatValid = currentLat != null && currentLat != 0.0 && String.valueOf(currentLat).length() > 5;
                    boolean isLngValid = currentLng != null && currentLng != 0.0 && String.valueOf(currentLng).length() > 5;

                    if (isLatValid && isLngValid && newLat != null && newLng != null && Double.compare(currentLat, newLat) == 0 && Double.compare(currentLng, newLng) == 0) {
                        System.out.println(String.format("   👉  TỌA ĐỘ ĐÚNG! Giữ nguyên: [%s, %s]", currentLat, currentLng));
                    } else if (newLat != null && newLng != null) {
                        System.out.println(String.format("   👉 📍 Tọa độ cập nhật: [%s, %s]", newLat, newLng));
                        hasChanges = true;
                    } else {
                        System.out.println(String.format("   👉 ⚠️ Không lấy được tọa độ mới, giữ nguyên DB: [%s, %s]", currentLat, currentLng));
                    }

                    if (webArea != null && webArea.compareTo(BigDecimal.ZERO) > 0) {
                        if (dbArea == null || dbArea.compareTo(webArea) != 0) {
                            System.out.println(String.format("   👉 ❌ DIỆN TÍCH SAI! [Diện tích cũ: %s m²] ➔ [Sửa thành: %s m²]",
                                    (dbArea != null ? dbArea : "Trống"), webArea));
                            updated.setArea(webArea);
                            hasChanges = true;
                        } else {
                            System.out.println(String.format("   👉  DIỆN TÍCH ĐÚNG! Giữ nguyên: %s m²", dbArea));
                        }
                    } else {
                        System.out.println(String.format("   👉 ⚠️ Không tìm thấy diện tích trên Web, giữ nguyên DB: %s m²", (dbArea != null ? dbArea : "Trống")));
                    }

                    System.out.flush();

                    if (hasChanges) {
                        batchToSave.add(updated);
                        updatedCount++;
                    }
                } else {
                    batchToDelete.add(listing);
                }

                if (batchToSave.size() >= 20) {
                    saveBatchInNewTransaction(batchToSave);
                    System.out.println("   💾 [DATABASE] --> Đã lưu đợt 20 tin.");
                    System.out.flush();
                    batchToSave.clear();
                }

                if (batchToDelete.size() >= 20) {
                    deleteBatchInNewTransaction(batchToDelete);
                    System.out.println("   💾 [DATABASE] --> Đã xóa đợt 20 tin ngoài phạm vi hoặc link chết.");
                    System.out.flush();
                    batchToDelete.clear();
                }

                randomSleep(2000, 4500);
            }

            if (!batchToSave.isEmpty()) {
                saveBatchInNewTransaction(batchToSave);
                System.out.println("   💾 [DATABASE] --> Đã lưu các tin cuối cùng.");
                System.out.flush();
            }

            if (!batchToDelete.isEmpty()) {
                deleteBatchInNewTransaction(batchToDelete);
                System.out.println("   💾 [DATABASE] --> Đã xóa các tin chết cuối cùng.");
                System.out.flush();
            }

            System.out.println("\n==========================================================================================");
            System.out.println(" --> [MIGRATION DONE] Hoàn tất quét lỗi! Số lượng tin đã xử lý: " + updatedCount);
            System.out.println("==========================================================================================\n");
            System.out.flush();

        } catch (Exception e) {
            System.out.println(" --> [AUTO-MIGRATION ERROR]: " + e.getMessage());
        } finally {
            if (page != null && !page.isClosed()) { try { page.close(); } catch (Exception ignored) {} }
            if (context != null) { try { context.close(); } catch (Exception ignored) {} }
            try {
                org.springframework.util.FileSystemUtils.deleteRecursively(userDataDir);
            } catch (Exception ignored) {}
        }
    }

    private BigDecimal parseAreaSafety(String rawText) {
        if (rawText == null || rawText.isBlank()) return BigDecimal.ZERO;
        try {
            String cleanText = rawText.replaceAll("[^0-9.,]", "").trim();
            if (cleanText.isEmpty()) return BigDecimal.ZERO;

            cleanText = cleanText.replace(".", "");
            cleanText = cleanText.replace(",", ".");

            BigDecimal area = new BigDecimal(cleanText).setScale(2, java.math.RoundingMode.HALF_UP);
            return area.compareTo(BigDecimal.ZERO) > 0 ? area : BigDecimal.ZERO;
        } catch (Exception e) {
            System.out.println(" ⚠️ [PARSE AREA ERROR] Định dạng chuỗi diện tích không hợp lệ.");
        }
        return BigDecimal.ZERO;
    }

    @Transactional
    public void saveBatchInNewTransaction(List<CrawPropertyListing> batch) {
        crawPropertyListingRepository.saveAllAndFlush(batch);
    }

    @Transactional
    public void deleteBatchInNewTransaction(List<CrawPropertyListing> batch) {
        for (CrawPropertyListing listing : batch) {
            if (listing.getHeatmapZones() != null) {
                listing.getHeatmapZones().clear();
            }
            crawPropertyListingRepository.delete(listing);
        }
        crawPropertyListingRepository.flush();
    }
}