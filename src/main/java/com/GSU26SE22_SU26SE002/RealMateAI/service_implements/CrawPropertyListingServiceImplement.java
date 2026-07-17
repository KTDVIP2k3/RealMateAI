package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WardRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.CrawPropertyListingServiceInterface;
import com.microsoft.playwright.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CrawPropertyListingServiceImplement implements CrawPropertyListingServiceInterface {

    @Autowired
    private CrawPropertyListingRepository crawPropertyListingRepository;

    @Autowired
    private WardRepository wardRepository;

    // Tiến trình định kỳ chạy cào dữ liệu tự động
    @Scheduled(initialDelay = 5000, fixedRate = 3600000)
    @Override
    public void autoCrawlPropertyData() {
        try (Playwright playwright = Playwright.create()) {
            System.out.println("=== BẮT ĐẦU TIẾN TRÌNH CÀO DỮ LIỆU TỰ ĐỘNG ===");

            // CHỈ LẤY CÁC PHƯỜNG THUỘC TP.HCM (MÃ 79)
            List<Ward> wardList = wardRepository.findWardsOnlyInHCM();
            if (wardList.isEmpty()) {
                System.out.println("Không tìm thấy dữ liệu Phường/Xã nào của TP.HCM (Mã 79) trong DB.");
                return;
            }

            Collections.shuffle(wardList);

            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"));

            Page page = context.newPage();

            for (Ward ward : wardList) {
                String wardName = ward.getFullName();

                String cleanWardSlug = removeAccent(wardName);
                String targetUrl = "https://batdongsan.com.vn/ban-nha-dat-" + cleanWardSlug + "-ho-chi-minh";

                try {
                    System.out.println("Đang điều hướng trình duyệt đến: " + targetUrl);

                    page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(20000));
                    page.waitForTimeout(3000);

                    String htmlContent = page.content();
                    Document doc = Jsoup.parse(htmlContent);
                    Elements propertyCards = doc.select(".re__card-info");


                    if (propertyCards.isEmpty()) {
                        System.out.println("-> Không có dữ liệu tại " + wardName + " (TP. Hồ Chí Minh). Tự động bỏ qua.");
                        continue;
                    }

                    List<CrawPropertyListing> listingResultList = new ArrayList<>();
                    int count = 0;
                    int maxItemsPerWard = 3;

                    for (Element card : propertyCards) {
                        if (count >= maxItemsPerWard) break;

                        try {
                            CrawPropertyListing listing = new CrawPropertyListing();

                            String priceText = card.select(".re__card-config-price").text();
                            BigDecimal price = parsePrice(priceText);
                            listing.setPrice(price);

                            String areaText = card.select(".re__card-config-area").text();
                            BigDecimal area = parseArea(areaText);
                            listing.setArea(area);

                            if (price != null && area != null && area.compareTo(BigDecimal.ZERO) > 0) {
                                listing.setPricePerM2(price.divide(area, 2, BigDecimal.ROUND_HALF_UP));
                            }

                            String latText = card.attr("data-lat");
                            String lonText = card.attr("data-long");
                            if (!latText.isEmpty() && !lonText.isEmpty()) {
                                listing.setLatitude(new BigDecimal(latText));
                                listing.setLongitude(new BigDecimal(lonText));
                            }

                            String dateText = card.select(".re__card-published-date").text();
                            listing.setPosted_date(parsePostedDate(dateText));
                            listing.setCraw_date(new Timestamp(System.currentTimeMillis()));

                            listingResultList.add(listing);
                            count++;

                        } catch (Exception parseEx) {
                            System.err.println("Lỗi parse card: " + parseEx.getMessage());
                        }
                    }


                    if (!listingResultList.isEmpty()) {
                        saveListingsInNewTransaction(listingResultList, wardName);
                    }

                    Thread.sleep(2500);

                } catch (Exception crawlEx) {
                    System.err.println("Lỗi kết nối tại " + wardName + " | Lý do: " + crawlEx.getMessage());
                }
            }

            browser.close();
            System.out.println("=== KẾT THÚC TIẾN TRÌNH CÀO DỮ LIỆU TỰ ĐỘNG ===");

        } catch (Exception e) {
            System.err.println("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveListingsInNewTransaction(List<CrawPropertyListing> listings, String wardName) {
        crawPropertyListingRepository.saveAll(listings);
        System.out.println("-> Đã LƯU & COMMIT thành công " + listings.size() + " tin mẫu cho: " + wardName);
    }


    private String removeAccent(String s) {
        if (s == null) return "";

        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern patternAccent = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return patternAccent.matcher(normalized).replaceAll("")
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