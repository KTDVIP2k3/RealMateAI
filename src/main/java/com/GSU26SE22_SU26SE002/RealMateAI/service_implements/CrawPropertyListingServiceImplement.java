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

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Scheduled(cron = "0 15 17 * * ?", zone = "Asia/Ho_Chi_Minh")
//    @Scheduled(initialDelay = 5000, fixedDelay = 600000)
    @Override
    public void autoCrawlPropertyData() {
        try (Playwright playwright = Playwright.create()) {
            System.out.println("\n=================== BẮT ĐẦU CÀO DỮ LIỆU (PURE STREAM API) ===================");

            List<Ward> wardList = wardRepository.findWardsOnlyInHCM();
            if (wardList.isEmpty()) {
                System.out.println("[WARNING] DB không có dữ liệu Ward TP.HCM!");
                return;
            }

            Set<String> existingUrlsInDb = crawPropertyListingRepository.findAll().stream()
                    .map(CrawPropertyListing::getSourceUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                    .setViewportSize(1366, 768));

            Page page = context.newPage();

            for (Ward ward : wardList) {
                String wardName = ward.getFullName();
                String cleanWardSlug = removeAccent(wardName);
                String targetUrl = "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-ho-chi-minh";

                try {
                    System.out.println("\n[1] MỞ TRANG DANH SÁCH: " + targetUrl);

                    page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(12000));
                    page.waitForTimeout(1500);

                    Document doc = Jsoup.parse(page.content());
                    Elements propertyCards = doc.select(".re__card-full, .re__card-info, .js__card");

                    if (propertyCards.isEmpty()) {
                        System.out.println(" --> Trống dữ liệu tại " + wardName + ", bỏ qua.");
                        continue;
                    }

                    System.out.println(" --> Tìm thấy " + propertyCards.size() + " card. Đang xử lý bằng Stream API...");

                    List<CrawPropertyListing> listingResultList = propertyCards.stream()
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
                                existingUrlsInDb.add(url);
                                return true;
                            })
                            .map(listing -> fetchCoordinates(listing, context))
                            .filter(listing -> listing.getLatitude() != null && listing.getLongitude() != null)
                            .limit(10)
                            .collect(Collectors.toList());

                    if (!listingResultList.isEmpty()) {
                        saveListingsInNewTransaction(listingResultList, wardName);
                    }

                    Thread.sleep(1500);

                } catch (Exception crawlEx) {
                    System.err.println("Lỗi kết nối tại " + wardName + " | Lý do: " + crawlEx.getMessage());
                }
            }

            browser.close();
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
            Element linkElem = card.selectFirst("a[href*='/ban-'], .re__card-title a, h3 a, a.js__product-link-for-product-id");

            if (linkElem != null) {
                detailLink = linkElem.attr("href");
            } else if (card.parent() != null && card.parent().tagName().equals("a")) {
                detailLink = card.parent().attr("href");
            }

            if (detailLink.isEmpty()) return null;

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

    private CrawPropertyListing fetchCoordinates(CrawPropertyListing listing, BrowserContext context) {
        System.out.println("\n   [2] TRUY CẬP TRANG CHI TIẾT: " + listing.getSourceUrl());

        try (Page detailPage = context.newPage()) {
            detailPage.navigate(listing.getSourceUrl(), new Page.NavigateOptions().setTimeout(25000));
            detailPage.waitForTimeout(2000);

            detailPage.evaluate("() => {" +
                    "  const mapEl = document.querySelector('.re__pr-map');" +
                    "  if (mapEl) {" +
                    "      mapEl.scrollIntoView();" +
                    "  } else {" +
                    "      window.scrollTo(0, document.body.scrollHeight / 2);" +
                    "  }" +
                    "}");
            detailPage.waitForTimeout(2500);

            Pattern coordPattern = Pattern.compile("q=([-\\d.]+),([-\\d.]+)");

            String exactDataSrc = (String) detailPage.evaluate("() => {" +
                    "  const iframes = document.querySelectorAll('iframe');" +
                    "  for (let f of iframes) {" +
                    "      let val = f.getAttribute('data-src') || f.getAttribute('src') || f.getAttribute('nitro-lazy-src') || '';" +
                    "      if (val.includes('q=')) return val;" +
                    "  }" +
                    "  const mapDiv = document.querySelector('.re__pr-map');" +
                    "  return mapDiv ? mapDiv.innerHTML : null;" +
                    "}");

            boolean foundCoord = false;
            if (exactDataSrc != null) {
                Matcher m = coordPattern.matcher(exactDataSrc);
                if (m.find()) {
                    listing.setLatitude(new BigDecimal(m.group(1)));
                    listing.setLongitude(new BigDecimal(m.group(2)));
                    System.out.println("     [SUCCESS] TỌA ĐỘ: Lat=" + m.group(1) + ", Long=" + m.group(2));
                    foundCoord = true;
                }
            }

            if (!foundCoord) {
                String fullHtml = detailPage.content();
                Matcher mHtml = coordPattern.matcher(fullHtml);
                if (mHtml.find()) {
                    listing.setLatitude(new BigDecimal(mHtml.group(1)));
                    listing.setLongitude(new BigDecimal(mHtml.group(2)));
                    System.out.println("     [SUCCESS] TỌA ĐỘ (HTML Fallback): Lat=" + mHtml.group(1) + ", Long=" + mHtml.group(2));
                    foundCoord = true;
                }
            }

            if (!foundCoord) {
                System.err.println("     [CRITICAL ERROR] KHÔNG THỂ BẮT TỌA ĐỘ URL: " + listing.getSourceUrl());
            }

            Thread.sleep(1000);

        } catch (Exception detailEx) {
            System.err.println("     [ERROR] Lỗi mở trang chi tiết: " + detailEx.getMessage());
        }

        return listing;
    }

    @Transactional
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listingResultList, String wardName) {
        Map<String, CrawPropertyListing> existingMap = crawPropertyListingRepository.findAll().stream()
                .filter(item -> listingResultList.stream().anyMatch(scraped -> scraped.getSourceUrl().equals(item.getSourceUrl())))
                .collect(Collectors.toMap(CrawPropertyListing::getSourceUrl, item -> item, (existing, replacement) -> existing));

        List<CrawPropertyListing> listingsToSave = listingResultList.stream()
                .map(scrapedListing -> {
                    CrawPropertyListing existing = existingMap.get(scrapedListing.getSourceUrl());
                    if (existing != null) {
                        existing.setPrice(scrapedListing.getPrice());
                        existing.setArea(scrapedListing.getArea());
                        existing.setPricePerM2(scrapedListing.getPricePerM2());
                        existing.setLatitude(scrapedListing.getLatitude());
                        existing.setLongitude(scrapedListing.getLongitude());
                        existing.setCraw_date(scrapedListing.getCraw_date());
                        return existing;
                    }
                    return scrapedListing;
                })
                .toList();

        crawPropertyListingRepository.saveAll(listingsToSave);
        System.out.println(" --> Đã lưu toàn bộ " + listingsToSave.size() + " tin cho phường: " + wardName);
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