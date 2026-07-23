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

    @Scheduled(initialDelay = 5000, fixedDelay = 600000)
    @Override
    public void autoCrawlPropertyData() {
        try (Playwright playwright = Playwright.create()) {
            System.out.println("\n=================== BẮT ĐẦU CÀO DỮ LIỆU (ANTI-CLOUDFLARE) ===================");

            List<Ward> wardList = wardRepository.findWardsOnlyInHCM();
            if (wardList.isEmpty()) {
                System.out.println("[WARNING] DB không có dữ liệu Ward!");
                return;
            }

            boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

            List<String> browserArgs = new ArrayList<>(Arrays.asList(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu",
                    "--disable-software-rasterizer",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                    "--window-size=1920,1080",
                    "--start-maximized",
                    "--lang=vi-VN,vi",
                    "--disable-crash-reporter",
                    "--disable-breakpad",
                    "--no-zygote"
            ));

            if (isServer) {
                browserArgs.add("--headless=new");
            }

            Path userDataDir = Paths.get("/tmp/chrome-profile-bot").toAbsolutePath();
            File profileDir = userDataDir.toFile();
            if (!profileDir.exists()) {
                profileDir.mkdirs();
            }

            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(isServer)
                    .setIgnoreDefaultArgs(Arrays.asList("--enable-automation"))
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .setArgs(browserArgs)
                    .setViewportSize(1920, 1080);

            BrowserContext context = playwright.chromium().launchPersistentContext(userDataDir, options);

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });\n" +
                            "window.navigator.chrome = { runtime: {} };\n" +
                            "Object.defineProperty(navigator, 'languages', { get: () => ['vi-VN', 'vi', 'en-US', 'en'] });\n" +
                            "Object.defineProperty(navigator, 'plugins', { get: () => [1, 2, 3, 4, 5] });"
            );

            Page page = context.pages().isEmpty() ? context.newPage() : context.pages().get(0);

            for (Ward ward : wardList) {
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
                    String targetUrl;
                    if (pageNum == 1) {
                        targetUrl = "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-" + provinceSlug;
                    } else {
                        targetUrl = "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-" + provinceSlug + "/p" + pageNum;
                    }

                    try {
                        System.out.println("\n[1] MỞ TRANG DANH SÁCH (Trang " + pageNum + "): " + targetUrl);

                        page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(45000));
                        randomSleep(3000, 5000);

                        Document doc = Jsoup.parse(page.content());

                        Elements propertyCards = doc.select(".re__card-full");
                        if (propertyCards.isEmpty()) {
                            propertyCards = doc.select(".js__card");
                        }

                        if (propertyCards.isEmpty()) {
                            System.out.println(" --> Trống dữ liệu tại " + wardName + " (Trang " + pageNum + ")");
                            break;
                        }

                        System.out.println(" --> Tìm thấy " + propertyCards.size() + " card. Đang lọc tin mới...");

                        List<CrawPropertyListing> newItems = propertyCards.stream()
                                .skip(pageNum == 1 ? 9 : 0)
                                .map(this::extractBasicInfo)
                                .filter(Objects::nonNull)
                                .filter(listing -> listing.getPrice() != null)
                                .filter(listing -> listing.getArea() != null && listing.getArea().compareTo(BigDecimal.ZERO) > 0)
                                .filter(listing -> {
                                    String url = listing.getSourceUrl();
                                    if (existingUrlsInDb.contains(url)) {
                                        System.out.println("     [SKIP] Tin đã có trong DB, bỏ qua: " + url);
                                        return false;
                                    }
                                    if (processedUrlsInBatch.contains(url)) {
                                        return false;
                                    }
                                    processedUrlsInBatch.add(url);
                                    return true;
                                })
                                .map(listing -> fetchCoordinates(listing, page))
                                .limit(10 - listingResultList.size())
                                .collect(Collectors.toList());

                        listingResultList.addAll(newItems);

                        if (listingResultList.size() >= 10) {
                            break;
                        }

                        pageNum++;
                        randomSleep(3000, 5000);

                    } catch (Exception crawlEx) {
                        System.err.println("Lỗi kết nối tại " + wardName + " | Lý do: " + crawlEx.getMessage());
                        break;
                    }
                }

                if (!listingResultList.isEmpty()) {
                    saveListingsInNewTransaction(listingResultList, wardName);
                }

                randomSleep(4000, 7000);
            }

            context.close();
            System.out.println("\n=================== KẾT THÚC CÀO DỮ LIỆU ===================");

            System.out.println("[HEATMAP] Bắt đầu tính toán & tạo Heatmap Snapshot...");
            try {
                heatmapZoneService.generateDailySnapshot();
                System.out.println("[HEATMAP] Tạo Heatmap Snapshot hoàn tất thành công!");
            } catch (Exception heatmapEx) {
                System.err.println("[HEATMAP ERROR] Lỗi khi tạo Heatmap Snapshot: " + heatmapEx.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Lỗi hệ thống: " + e.getMessage());
        }
    }

    private CrawPropertyListing extractBasicInfo(Element card) {
        try {
            String detailLink = "";

            Element linkElem = card.selectFirst(".re__card-title a, h3.re__card-title a, a.js__product-link-for-product-id, h3 a");

            if (linkElem != null) {
                detailLink = linkElem.attr("href");
            } else {
                Element fallbackLink = card.selectFirst("a[href*='/ban-']");
                if (fallbackLink != null) {
                    detailLink = fallbackLink.attr("href");
                }
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
        System.out.println("\n   [2] TRUY CẬP TRANG CHI TIẾT: " + listing.getSourceUrl());

        randomSleep(2000, 3500);

        try {
            page.navigate(listing.getSourceUrl(), new Page.NavigateOptions().setTimeout(45000));

            scrollPageSmoothly(page);

            randomSleep(1500, 2500);

            String latStr = null;
            String lngStr = null;

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

            if (latStr == null || lngStr == null) {
                String fullHtml = page.content();
                Matcher mapUrlMatcher = Pattern.compile("(?:q|ll|center)=([-\\d.]+)(?:,|%2C)([-\\d.]+)").matcher(fullHtml);

                if (mapUrlMatcher.find()) {
                    latStr = mapUrlMatcher.group(1);
                    lngStr = mapUrlMatcher.group(2);
                } else {
                    Matcher latM = Pattern.compile("[\"']?latitude[\"']?\\s*[:=]\\s*([-\\d.]+)").matcher(fullHtml);
                    Matcher lngM = Pattern.compile("[\"']?longitude[\"']?\\s*[:=]\\s*([-\\d.]+)").matcher(fullHtml);
                    if (latM.find() && lngM.find()) {
                        latStr = latM.group(1);
                        lngStr = lngM.group(1);
                    }
                }
            }

            if (latStr == null || lngStr == null) {
                try {
                    Object result = page.evaluate("() => {" +
                            "  if (typeof initialData !== 'undefined' && initialData.latitude) return {lat: initialData.latitude, lng: initialData.longitude};" +
                            "  if (window.RE && window.RE.propertyDetail) return {lat: window.RE.propertyDetail.latitude, lng: window.RE.propertyDetail.longitude};" +
                            "  return null;" +
                            "}");

                    if (result instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) result;
                        if (map.get("lat") != null && map.get("lng") != null) {
                            latStr = map.get("lat").toString();
                            lngStr = map.get("lng").toString();
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (latStr != null && lngStr != null) {
                listing.setLatitude(new BigDecimal(latStr));
                listing.setLongitude(new BigDecimal(lngStr));
                System.out.println("     [SUCCESS] TỌA ĐỘ BÀI ĐĂNG: Lat=" + latStr + ", Long=" + lngStr);
            } else {
                System.out.println("     [WARNING] Không lấy được tọa độ bài đăng: " + listing.getSourceUrl());
            }

        } catch (Exception detailEx) {
            System.err.println("[ERROR] Lỗi mở trang chi tiết: " + detailEx.getMessage());
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
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listingResultList, String wardName) {
        if (listingResultList == null || listingResultList.isEmpty()) {
            return;
        }

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
            System.out.println(" --> [DB SUCCESS] Đã lưu thành công " + saved.size() + " tin vào DB cho: " + wardName);

        } catch (Exception e) {
            System.err.println(" --> [DB ERROR] Lỗi khi lưu DB tại " + wardName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void randomSleep(long minMs, long maxMs) {
        try {
            long sleepTime = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {}
    }

    private String removeAccent(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern patternAccent = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return patternAccent.matcher(normalized)
                .replaceAll("")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
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