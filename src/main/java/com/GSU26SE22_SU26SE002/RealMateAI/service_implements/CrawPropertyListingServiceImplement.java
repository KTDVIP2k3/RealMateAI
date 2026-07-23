package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CrawPropertyListingServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.HeatmapZoneServiceInterface;
import com.microsoft.playwright.*;
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

    private static final int DAILY_TARGET_LISTINGS = 300;
    private int currentDailyCrawledCount = 0;
    private boolean hasTakenSnapshotToday = false;

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounter() {
        System.out.println("\n[SYSTEM] 🟢 SANG NGÀY MỚI (00:00)! Reset bộ đếm cào tin về 0 và cờ Snapshot.");
        this.currentDailyCrawledCount = 0;
        this.hasTakenSnapshotToday = false;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = 600000)
    @Override
    public void autoCrawlPropertyData() {
        if (currentDailyCrawledCount >= DAILY_TARGET_LISTINGS) {
            System.out.println("[SCHEDULE] 🛑 Đã cào đủ quota " + currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " tin hôm nay. Tắt cào chờ 00:00 ngày mai!");
            return;
        }

        int remainingQuota = DAILY_TARGET_LISTINGS - currentDailyCrawledCount;
        System.out.println("\n=================== BẮT ĐẦU CÀO DỮ LIỆU (Đã cào hôm nay: "
                + currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " - Cần cào thêm tối đa: " + remainingQuota + " tin) ===================");
        System.out.flush();

        boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null
                || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

        Path crashDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-crashes").toAbsolutePath();
        File crashFileDir = crashDir.toFile();
        if (!crashFileDir.exists()) crashFileDir.mkdirs();

        Path userDataDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-profile-bot-" + System.currentTimeMillis()).toAbsolutePath();
        File profileDir = userDataDir.toFile();
        if (!profileDir.exists()) profileDir.mkdirs();

        try (Playwright playwright = Playwright.create()) {
            List<String> browserArgs = Arrays.asList(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--disable-software-rasterizer",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                    "--disable-session-crashed-bubble",
                    "--hide-crash-restore-bubble",
                    "--window-size=1920,1080",
                    "--start-maximized",
                    "--lang=vi-VN,vi",
                    "--disable-crash-reporter",
                    "--disable-component-update",
                    "--no-crash-upload",
                    "--enable-webgl",
                    "--ignore-gpu-blocklist",
                    "--use-gl=angle",
                    "--use-angle=swiftshader",
                    "--enable-features=Vulkan,UseSkiaRenderer",
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

            BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir, options);

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });\n" +
                            "window.navigator.chrome = { runtime: {} };\n" +
                            "Object.defineProperty(navigator, 'languages', { get: () => ['vi-VN', 'vi', 'en-US', 'en'] });\n" +
                            "Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });\n" +
                            "Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });\n" +
                            "Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });"
            );

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            Set<String> existingUrlsInDb = crawPropertyListingRepository.findAll().stream()
                    .map(CrawPropertyListing::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> processedUrlsInBatch = new HashSet<>();
            int totalCrawledInBatch = 0;
            int pageNum = 1;
            int consecutiveEmptyPages = 0;

            while (totalCrawledInBatch < remainingQuota && pageNum <= 50) {
                String targetUrl = (pageNum == 1)
                        ? "https://batdongsan.com.vn/ban-nha-dat-tp-hcm"
                        : "https://batdongsan.com.vn/ban-nha-dat-tp-hcm/p" + pageNum;

                try {
                    System.out.println("\n[1] MỞ TRANG DANH SÁCH TỔNG (Trang " + pageNum + "): " + targetUrl);
                    System.out.flush();

                    page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(45000));
                    page.mouse().move(150, 200);
                    page.mouse().move(350, 450);
                    randomSleep(2500, 4000);

                    String pageTitle = page.title();
                    if (pageTitle.contains("Just a moment") || pageTitle.contains("Attention Required") || pageTitle.contains("Access Denied")) {
                        System.err.println(" ⚠️ [CLOUDFLARE BLOCK] IP bị Cloudflare chặn tại: " + targetUrl);
                        break;
                    }

                    Document doc = Jsoup.parse(page.content());
                    Elements propertyCards = doc.select(".re__card-full, .js__card");

                    if (propertyCards.isEmpty()) {
                        System.out.println(" --> Trang " + pageNum + " không tìm thấy card bất động sản nào.");
                        break;
                    }

                    List<CrawPropertyListing> freshCandidates = new ArrayList<>();
                    int duplicateCountInPage = 0;

                    for (Element card : propertyCards) {
                        CrawPropertyListing listing = extractBasicInfo(card);
                        if (listing != null && listing.getPrice() != null && listing.getArea() != null
                                && listing.getArea().compareTo(BigDecimal.ZERO) > 0) {

                            String url = listing.getSourceUrl();
                            if (existingUrlsInDb.contains(url) || processedUrlsInBatch.contains(url)) {
                                duplicateCountInPage++;
                            } else {
                                processedUrlsInBatch.add(url);
                                freshCandidates.add(listing);
                            }
                        }
                    }

                    System.out.println(" --> Tìm thấy " + freshCandidates.size() + " tin mới (Trùng/Cũ: " + duplicateCountInPage + " tin)");
                    System.out.flush();

                    if (freshCandidates.isEmpty()) {
                        consecutiveEmptyPages++;
                        System.out.println(" ⚠️ Trang " + pageNum + " không có tin mới. Trang trống liên tiếp: " + consecutiveEmptyPages);
                        if (consecutiveEmptyPages >= 3) {
                            System.out.println(" 🛑 [SAFE-BREAK] 3 trang liên tiếp không có tin mới. Đã quét hết tin mới hiện tại!");
                            break;
                        }
                    } else {
                        consecutiveEmptyPages = 0;
                    }

                    List<CrawPropertyListing> pageResultList = new ArrayList<>();
                    for (CrawPropertyListing candidate : freshCandidates) {
                        if (totalCrawledInBatch >= remainingQuota) break;

                        CrawPropertyListing fullListing = fetchCoordinates(candidate, page);
                        if (fullListing != null) {
                            pageResultList.add(fullListing);
                            totalCrawledInBatch++;
                        }
                    }

                    if (!pageResultList.isEmpty()) {
                        saveListingsInNewTransaction(pageResultList, "Trang " + pageNum);
                        existingUrlsInDb.addAll(pageResultList.stream().map(CrawPropertyListing::getSourceUrl).toList());
                    }

                    pageNum++;
                    randomSleep(2000, 4000);

                } catch (Exception crawlEx) {
                    System.err.println("Lỗi kết nối tại trang " + pageNum + " | Lý do: " + crawlEx.getMessage());
                    break;
                }
            }

            context.close();

            this.currentDailyCrawledCount += totalCrawledInBatch;

            System.out.println("\n=================== KẾT THÚC ĐỢT CÀO ===================");
            System.out.println("-> Cào được đợt này: " + totalCrawledInBatch + " tin mới.");
            System.out.println("-> Tổng tích lũy hôm nay: " + currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " tin.");

            if (this.currentDailyCrawledCount >= DAILY_TARGET_LISTINGS && !hasTakenSnapshotToday) {
                System.out.println("\n🎯 [TARGET REACHED] ĐÃ ĐẠT " + currentDailyCrawledCount + "/" + DAILY_TARGET_LISTINGS + " TIN HÔM NAY!");
                System.out.println("[HEATMAP] Tiến hành tính toán & TẠO SNAPSHOT HEATMAP NGAY BÂY GIỜ...");
                System.out.flush();
                try {
                    heatmapZoneService.generateDailySnapshot();
                    this.hasTakenSnapshotToday = true;
                    System.out.println("[HEATMAP] Tạo Snapshot Heatmap thành công!");
                } catch (Exception heatmapEx) {
                    System.err.println("[HEATMAP ERROR] Lỗi khi tạo Snapshot sớm: " + heatmapEx.getMessage());
                }
            }
            System.out.flush();

        } catch (Exception e) {
            System.err.println("Lỗi hệ thống Playwright: " + e.getMessage());
        } finally {
            try {
                FileSystemUtils.deleteRecursively(userDataDir);
            } catch (Exception ignored) {}
        }
    }

    @Scheduled(cron = "0 59 23 * * ?")
    public void scheduleDailyHeatmapSnapshot() {
        if (hasTakenSnapshotToday) {
            System.out.println("\n[HEATMAP SCHEDULED 23:59] Hôm nay đã đủ 300 tin và đã tạo Snapshot sớm. Bỏ qua lượt chốt cuối ngày.");
            return;
        }

        System.out.println("\n[HEATMAP SCHEDULED 23:59] Hết ngày! Tổng số tin cào được hôm nay: " + currentDailyCrawledCount + " tin.");
        System.out.println("[HEATMAP SCHEDULED 23:59] Bắt đầu khởi chạy tính toán Snapshot Heatmap chốt ngày...");
        System.out.flush();
        try {
            heatmapZoneService.generateDailySnapshot();
            this.hasTakenSnapshotToday = true;
            System.out.println("[HEATMAP SCHEDULED 23:59] Hoàn tất tạo Snapshot Heatmap cuối ngày thành công!");
        } catch (Exception heatmapEx) {
            System.err.println("[HEATMAP SCHEDULED ERROR] Lỗi khi tạo Snapshot cuối ngày: " + heatmapEx.getMessage());
        }
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

            BigDecimal price = parsePrice(card.select(".re__card-config-price").text());
            BigDecimal area = parseArea(card.select(".re__card-config-area").text());

            CrawPropertyListing listing = new CrawPropertyListing();
            listing.setSourceUrl(fullDetailUrl);
            listing.setPrice(price);
            listing.setArea(area);

            if (price != null && area != null && area.compareTo(BigDecimal.ZERO) > 0) {
                listing.setPricePerM2(price.divide(area, 2, RoundingMode.HALF_UP));
            }

            String dateText = card.select(".re__card-published-date").text();
            listing.setPosted_date(parsePostedDate(dateText));
            listing.setCraw_date(new Timestamp(System.currentTimeMillis()));

            return listing;
        } catch (Exception e) {
            return null;
        }
    }

    private CrawPropertyListing fetchCoordinates(CrawPropertyListing listing, Page page) {
        System.out.println("\n   [2] TRUY CẬP TRANG CHI TIẾT LẤY TỌA ĐỘ: " + listing.getSourceUrl());
        System.out.flush();

        try {
            page.navigate(listing.getSourceUrl(), new Page.NavigateOptions().setTimeout(30000));

            scrollPageSmoothly(page);
            randomSleep(1500, 2500);

            try {
                page.waitForSelector("iframe[src*='google.com/maps'], iframe[data-src*='google.com/maps'], div#re-map, div[data-lat], .re__section-map",
                        new Page.WaitForSelectorOptions().setTimeout(4000));
            } catch (Exception ignored) {}

            String latStr = null;
            String lngStr = null;

            try {
                Object rawCoords = page.evaluate("() => {" +
                        "  try {" +
                        "    const nextData = document.getElementById('__NEXT_DATA__');" +
                        "    if (nextData) {" +
                        "      const json = JSON.parse(nextData.innerHTML);" +
                        "      const details = json.props?.pageProps?.initialDetail || json.props?.pageProps?.productDetail || json.props?.pageProps?.detail;" +
                        "      if (details && details.latitude) return {lat: details.latitude, lng: details.longitude};" +
                        "    }" +
                        "  } catch(e){}" +
                        "  if (typeof initialData !== 'undefined' && initialData.latitude) return {lat: initialData.latitude, lng: initialData.longitude};" +
                        "  if (window.RE && window.RE.propertyDetail) return {lat: window.RE.propertyDetail.latitude, lng: window.RE.propertyDetail.longitude};" +
                        "  if (window.__INITIAL_STATE__ && window.__INITIAL_STATE__.details) return {lat: window.__INITIAL_STATE__.details.latitude, lng: window.__INITIAL_STATE__.details.longitude};" +
                        "  const mapElem = document.querySelector('[data-lat], [data-latitude], #re-map, .re__section-map');" +
                        "  if (mapElem) {" +
                        "    return {" +
                        "      lat: mapElem.getAttribute('data-lat') || mapElem.getAttribute('data-latitude')," +
                        "      lng: mapElem.getAttribute('data-long') || mapElem.getAttribute('data-lng') || mapElem.getAttribute('data-longitude')" +
                        "    };" +
                        "  }" +
                        "  return null;" +
                        "}");

                if (rawCoords instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) rawCoords;
                    if (map.get("lat") != null && map.get("lng") != null) {
                        latStr = map.get("lat").toString();
                        lngStr = map.get("lng").toString();
                    }
                }
            } catch (Exception ignored) {}

            if (latStr == null || lngStr == null) {
                try {
                    String mapIframeSrc = (String) page.evaluate("() => {" +
                            "  const iframe = document.querySelector('iframe[data-src*=\"google.com/maps\"], iframe[src*=\"google.com/maps\"]');" +
                            "  return iframe ? (iframe.getAttribute('data-src') || iframe.getAttribute('src')) : null;" +
                            "}");

                    if (mapIframeSrc != null) {
                        Matcher matcher = Pattern.compile("(?:q|ll|center)=([-\\d.]+)(?:,|%2C)([-\\d.]+)").matcher(mapIframeSrc);
                        if (matcher.find()) {
                            latStr = matcher.group(1);
                            lngStr = matcher.group(2);
                        }
                    }
                } catch (Exception ignored) {}
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
                listing.setLatitude(new BigDecimal(latStr));
                listing.setLongitude(new BigDecimal(lngStr));
                System.out.println("     ✅ [SUCCESS TỌA ĐỘ]: Lat=" + latStr + ", Long=" + lngStr);
            } else {
                System.out.println("     ⚠️ [WARNING] Không tìm thấy tọa độ cho bài này: " + listing.getSourceUrl());
            }

        } catch (Exception detailEx) {
            System.out.println("     ❌ [ERROR TIMEOUT/LOAD] Bỏ qua bài này do lỗi: " + detailEx.getMessage());
        } finally {
            System.out.flush();
        }

        return listing;
    }

    private void scrollPageSmoothly(Page page) {
        try {
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight / 3, behavior: 'smooth'});");
            randomSleep(800, 1200);
            page.evaluate("() => window.scrollTo({top: (document.body.scrollHeight / 3) * 2, behavior: 'smooth'});");
            randomSleep(800, 1200);
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'});");
        } catch (Exception ignored) {}
    }

    @Transactional
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listingResultList, String pageLabel) {
        if (listingResultList == null || listingResultList.isEmpty()) return;

        try {
            Set<String> urlsInBatch = listingResultList.stream()
                    .map(CrawPropertyListing::getSourceUrl)
                    .collect(Collectors.toSet());

            List<CrawPropertyListing> existingListings = crawPropertyListingRepository.findAll().stream()
                    .filter(item -> urlsInBatch.contains(item.getSourceUrl()))
                    .toList();

            Map<String, CrawPropertyListing> existingMap = existingListings.stream()
                    .collect(Collectors.toMap(CrawPropertyListing::getSourceUrl, item -> item, (a, b) -> a));

            List<CrawPropertyListing> entitiesToSave = new ArrayList<>();
            for (CrawPropertyListing scraped : listingResultList) {
                CrawPropertyListing entity = existingMap.get(scraped.getSourceUrl());
                if (entity != null) {
                    entity.setPrice(scraped.getPrice());
                    entity.setArea(scraped.getArea());
                    entity.setPricePerM2(scraped.getPricePerM2());
                    entity.setLatitude(scraped.getLatitude());
                    entity.setLongitude(scraped.getLongitude());
                    entity.setCraw_date(scraped.getCraw_date());
                    entitiesToSave.add(entity);
                } else {
                    entitiesToSave.add(scraped);
                }
            }

            List<CrawPropertyListing> saved = crawPropertyListingRepository.saveAllAndFlush(entitiesToSave);
            System.out.println(" --> [DB SUCCESS] Đã lưu thành công " + saved.size() + " tin vào DB (" + pageLabel + ")");
            System.out.flush();

        } catch (Exception e) {
            System.err.println(" --> [DB ERROR] Lỗi khi lưu DB tại " + pageLabel + ": " + e.getMessage());
            System.out.flush();
        }
    }

    private void randomSleep(long minMs, long maxMs) {
        try {
            long sleepTime = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {}
    }

    private BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isEmpty() || priceText.contains("Thỏa thuận")) return null;
        try {
            String clean = priceText.replaceAll("[^0-9.,]", "").trim().replace(",", ".");
            double value = Double.parseDouble(clean);
            if (priceText.contains("tỷ")) {
                return BigDecimal.valueOf(value * 1_000_000_000);
            } else if (priceText.contains("triệu")) {
                return BigDecimal.valueOf(value * 1_000_000);
            }
        } catch (Exception e) { return null; }
        return null;
    }

    private BigDecimal parseArea(String areaText) {
        if (areaText == null || areaText.isEmpty()) return null;
        try {
            String clean = areaText.replaceAll("[^0-9.,]", "").trim().replace(",", ".");
            return new BigDecimal(clean);
        } catch (Exception e) { return null; }
    }

    private Date parsePostedDate(String dateText) {
        if (dateText == null || dateText.isEmpty()) return new Date();
        if (dateText.contains("Hôm nay")) return new Date();
        if (dateText.contains("Hôm qua")) {
            return new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(dateText);
        } catch (ParseException e) { return new Date(); }
    }
}