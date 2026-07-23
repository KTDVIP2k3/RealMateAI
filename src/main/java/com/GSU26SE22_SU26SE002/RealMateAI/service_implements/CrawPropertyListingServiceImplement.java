package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WardRepository;
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

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.Normalizer;
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
    private WardRepository wardRepository;

    @Autowired
    private HeatmapZoneServiceInterface heatmapZoneService;

    // Chạy tự động mỗi ngày (hoặc cấu hình cron tùy dự án)
    @Scheduled(initialDelay = 5000, fixedDelay = 86400000) // 86400000ms = 24 giờ
    @Override
    public void autoCrawlPropertyData() {
        final int TARGET_TOTAL_LISTINGS = 300;
        int totalCrawledInBatch = 0; // Biến đếm tổng số tin MỚI thực tế cào được

        try (Playwright playwright = Playwright.create()) {
            System.out.println("\n=================== BẮT ĐẦU CÀO DỮ LIỆU TỰ ĐỘNG ===================");
            System.out.println("[CONFIG] Chỉ tiêu đợt cào: Tối đa " + TARGET_TOTAL_LISTINGS + " tin mới.");
            System.out.flush();

            List<Ward> wardList = wardRepository.findWardsOnlyInHCM();
            if (wardList.isEmpty()) {
                System.out.println("[WARNING] DB không có dữ liệu Phường!");
                System.out.flush();
                return;
            }

            boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

            Path crashDir = Paths.get("/tmp/chrome-crashes").toAbsolutePath();
            File crashFileDir = crashDir.toFile();
            if (!crashFileDir.exists()) crashFileDir.mkdirs();

            List<String> browserArgs = new ArrayList<>(Arrays.asList(
                    "--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage",
                    "--disable-gpu", "--disable-software-rasterizer", "--disable-blink-features=AutomationControlled",
                    "--disable-infobars", "--window-size=1920,1080", "--start-maximized",
                    "--lang=vi-VN,vi", "--disable-crash-reporter", "--disable-component-update",
                    "--no-crash-upload", "--enable-webgl", "--ignore-gpu-blocklist",
                    "--use-gl=angle", "--use-angle=swiftshader", "--enable-features=Vulkan,UseSkiaRenderer",
                    "--crash-dumps-dir=" + crashDir.toString()
            ));

            Path userDataDir = Paths.get("/tmp/chrome-profile-bot").toAbsolutePath();
            File profileDir = userDataDir.toFile();
            if (!profileDir.exists()) profileDir.mkdirs();

            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.put("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");
            headers.put("Cache-Control", "max-age=0");
            headers.put("Sec-Ch-Ua", "\"Not/A)Brand\";v=\"8\", \"Chromium\";v=\"126\", \"Google Chrome\";v=\"126\"");
            headers.put("Sec-Ch-Ua-Mobile", "?0");
            headers.put("Sec-Ch-Ua-Platform", "\"Windows\"");

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
                            "window.navigator.chrome = { runtime: {} };"
            );

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            // 🔄 VÒNG LẶP DUYỆT QUA CÁC PHƯỜNG
            for (Ward ward : wardList) {

                // CHECK KỊCH BẢN 1: Đã đạt đủ 300 tin -> Dừng cào lập tức
                if (totalCrawledInBatch >= TARGET_TOTAL_LISTINGS) {
                    System.out.println("\n🎯 [TARGET MET] Đã cào đủ chỉ tiêu (" + totalCrawledInBatch + "/" + TARGET_TOTAL_LISTINGS + " tin). Dừng cào!");
                    System.out.flush();
                    break;
                }

                String wardName = ward.getFullName();
                String cleanWardSlug = removeAccent(wardName);

                String provinceSlug = "ho-chi-minh";
                if (ward.getProvince() != null && ward.getProvince().getFullName() != null) {
                    provinceSlug = removeAccent(ward.getProvince().getFullName());
                } else if (ward.getProvince() != null && ward.getProvince().getName() != null) {
                    provinceSlug = removeAccent(ward.getProvince().getName());
                }

                Set<String> existingUrlsInDb = crawPropertyListingRepository.findAll().stream()
                        .map(CrawPropertyListing::getSourceUrl)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                List<CrawPropertyListing> listingResultList = new ArrayList<>();
                Set<String> processedUrlsInBatch = new HashSet<>();
                int pageNum = 1;

                while (listingResultList.size() < 10 && pageNum <= 5) {
                    String targetUrl = (pageNum == 1)
                            ? "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-" + provinceSlug
                            : "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-" + provinceSlug + "/p" + pageNum;

                    try {
                        System.out.println("\n[1] MỞ TRANG: " + targetUrl);
                        System.out.flush();

                        page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(45000));
                        randomSleep(3000, 5000);

                        if (pageNum == 1 && !page.url().contains(cleanWardSlug)) {
                            System.err.println(" ⚠️ Slug sai / Redirect URL -> Bỏ qua phường: " + wardName);
                            break;
                        }

                        Document doc = Jsoup.parse(page.content());
                        Elements propertyCards = doc.select(".re__card-full");
                        if (propertyCards.isEmpty()) propertyCards = doc.select(".js__card");

                        if (propertyCards.isEmpty()) {
                            System.out.println(" --> Không tìm thấy bài đăng nào tại " + wardName);
                            break;
                        }

                        int skipCountInPage = 0;
                        List<CrawPropertyListing> freshCandidates = new ArrayList<>();

                        for (Element card : propertyCards) {
                            CrawPropertyListing listing = extractBasicInfo(card);
                            if (listing != null && listing.getPrice() != null && listing.getArea() != null && listing.getArea().compareTo(BigDecimal.ZERO) > 0) {
                                String url = listing.getSourceUrl();

                                if (existingUrlsInDb.contains(url)) {
                                    skipCountInPage++;
                                } else if (!processedUrlsInBatch.contains(url)) {
                                    processedUrlsInBatch.add(url);
                                    freshCandidates.add(listing);
                                }
                            }
                        }

                        if (skipCountInPage >= propertyCards.size() - 2) {
                            System.out.println(" 🛑 Trang này chứa toàn tin cũ đã cào. Dừng cào Phường: " + wardName);
                            break;
                        }

                        List<CrawPropertyListing> newItems = freshCandidates.stream()
                                .map(listing -> fetchCoordinates(listing, page))
                                .limit(10 - listingResultList.size())
                                .collect(Collectors.toList());

                        listingResultList.addAll(newItems);

                        if (listingResultList.size() >= 10) break;

                        pageNum++;
                        randomSleep(3000, 5000);

                    } catch (Exception crawlEx) {
                        System.err.println("Lỗi cào tại " + wardName + ": " + crawlEx.getMessage());
                        break;
                    }
                }

                // Lưu dữ liệu phường này
                if (!listingResultList.isEmpty()) {
                    saveListingsInNewTransaction(listingResultList, wardName);
                    totalCrawledInBatch += listingResultList.size();
                    System.out.println(" 📊 [TIẾN ĐỘ TỔNG] Đã thu thập: " + totalCrawledInBatch + " tin mới.");
                    System.out.flush();
                }

                randomSleep(3000, 6000);
            } // === KẾT THÚC VÒNG LẶP CÀO TẤT CẢ PHƯỜNG ===

            context.close();
            System.out.println("\n=================== KẾT THÚC TIẾN TRÌNH CÀO ===================");
            System.out.flush();

            // 🛡️ XỬ LÝ KỊCH BẢN TẠO SNAPSHOT DỰA TRÊN KẾT QUẢ THỰC TẾ
            if (totalCrawledInBatch > 0) {
                // KỊCH BẢN 1 & 2: Có tin mới (cho dù đủ 300 hay ít hơn 300) -> Vẫn tạo Snapshot!
                System.out.println("🚀 [HEATMAP SNAPSHOT] Tổng cộng thu thập được " + totalCrawledInBatch + " tin mới. Đang tính toán Heatmap...");
                System.out.flush();
                try {
                    heatmapZoneService.generateDailySnapshot();
                    System.out.println("✅ [HEATMAP SNAPSHOT] Tạo Snapshot thành công!");
                } catch (Exception heatmapEx) {
                    System.err.println("❌ [HEATMAP ERROR] Lỗi khi tạo Snapshot: " + heatmapEx.getMessage());
                }
            } else {
                // KỊCH BẢN 3: Không cào được tin nào mới cả
                System.out.println("ℹ️ [HEATMAP SNAPSHOT] Bỏ qua Snapshot vì đợt cào này không có tin mới nào (0/300).");
            }

            System.out.println("😴 [CRAWLER SLEEP] Bot hoàn thành công việc và nghỉ ngơi chờ đợt quét tiếp theo.");
            System.out.flush();

        } catch (Exception e) {
            System.err.println("Lỗi hệ thống Crawler: " + e.getMessage());
        }
    }

    // --- CÁC HÀM HELPER GIỮ NGUYÊN ---
    private CrawPropertyListing extractBasicInfo(Element card) {
        try {
            String detailLink = "";
            Element linkElem = card.selectFirst(".re__card-title a, h3.re__card-title a, a.js__product-link-for-product-id, h3 a");
            if (linkElem != null) {
                detailLink = linkElem.attr("href");
            } else {
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
        } catch (Exception e) { return null; }
    }

    private CrawPropertyListing fetchCoordinates(CrawPropertyListing listing, Page page) {
        try {
            page.navigate(listing.getSourceUrl(), new Page.NavigateOptions().setTimeout(25000));
            scrollPageSmoothly(page);
            randomSleep(1500, 2500);

            try {
                page.waitForSelector("iframe[src*='google.com/maps'], iframe[data-src*='google.com/maps'], div#re-map, div[data-lat], .re__section-map",
                        new Page.WaitForSelectorOptions().setTimeout(4000));
            } catch (Exception ignored) {}

            String latStr = null, lngStr = null;

            try {
                Object rawCoords = page.evaluate("() => {" +
                        "  try {" +
                        "    const nextData = document.getElementById('__NEXT_DATA__');" +
                        "    if (nextData) {" +
                        "      const json = JSON.parse(nextData.innerHTML);" +
                        "      const details = json.props?.pageProps?.initialDetail || json.props?.pageProps?.productDetail;" +
                        "      if (details && details.latitude) return {lat: details.latitude, lng: details.longitude};" +
                        "    }" +
                        "  } catch(e){}" +
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
                String fullHtml = page.content();
                Matcher jsonMatcher = Pattern.compile("[\"']latitude[\"']\\s*:\\s*([0-9.]+)\\s*,\\s*[\"']longitude[\"']\\s*:\\s*([0-9.]+)").matcher(fullHtml);
                if (jsonMatcher.find()) {
                    latStr = jsonMatcher.group(1);
                    lngStr = jsonMatcher.group(2);
                }
            }

            if (latStr != null && lngStr != null && !latStr.equals("0")) {
                listing.setLatitude(new BigDecimal(latStr));
                listing.setLongitude(new BigDecimal(lngStr));
            }
        } catch (Exception detailEx) {
            System.out.println(" [TIMEOUT] Bỏ qua tọa độ tin: " + listing.getSourceUrl());
        }
        return listing;
    }

    private void scrollPageSmoothly(Page page) {
        try {
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight / 2, behavior: 'smooth'});");
            randomSleep(800, 1200);
            page.evaluate("() => window.scrollTo({top: document.body.scrollHeight, behavior: 'smooth'});");
        } catch (Exception ignored) {}
    }

    @Transactional
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listingResultList, String wardName) {
        if (listingResultList == null || listingResultList.isEmpty()) return;
        try {
            Set<String> urlsInBatch = listingResultList.stream().map(CrawPropertyListing::getSourceUrl).collect(Collectors.toSet());
            List<CrawPropertyListing> existingListings = crawPropertyListingRepository.findAll().stream()
                    .filter(item -> urlsInBatch.contains(item.getSourceUrl())).toList();

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

            crawPropertyListingRepository.saveAllAndFlush(entitiesToSave);
            System.out.println(" --> [LƯU DB SUCCESS] " + entitiesToSave.size() + " tin tại " + wardName);
        } catch (Exception e) {
            System.err.println(" --> [LƯU DB ERROR] Lỗi tại " + wardName + ": " + e.getMessage());
        }
    }

    private void randomSleep(long minMs, long maxMs) {
        try { Thread.sleep(minMs + (long) (Math.random() * (maxMs - minMs))); } catch (InterruptedException ignored) {}
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized)
                .replaceAll("").toLowerCase().replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
    }

    private BigDecimal parsePrice(String priceText) {
        if (priceText == null || priceText.isEmpty() || priceText.contains("Thỏa thuận")) return null;
        try {
            String clean = priceText.replaceAll("[^0-9.,]", "").trim().replace(",", ".");
            double value = Double.parseDouble(clean);
            if (priceText.contains("tỷ")) return BigDecimal.valueOf(value * 1_000_000_000);
            if (priceText.contains("triệu")) return BigDecimal.valueOf(value * 1_000_000);
        } catch (Exception e) { return null; }
        return null;
    }

    private BigDecimal parseArea(String areaText) {
        if (areaText == null || areaText.isEmpty()) return null;
        try {
            return new BigDecimal(areaText.replaceAll("[^0-9.,]", "").trim().replace(",", "."));
        } catch (Exception e) { return null; }
    }

    private Date parsePostedDate(String dateText) {
        if (dateText == null || dateText.isEmpty() || dateText.contains("Hôm nay")) return new Date();
        if (dateText.contains("Hôm qua")) return new Date(System.currentTimeMillis() - 86400000L);
        try {
            return new SimpleDateFormat("dd/MM/yyyy").parse(dateText);
        } catch (ParseException e) { return new Date(); }
    }
}