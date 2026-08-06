package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.CrawPropertyListingRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Profile("local")
public class DataValidationService {

    @Autowired
    private CrawPropertyListingRepository repository;

    private static final int BATCH_SIZE = 50;

    @Async
    public void startAuditProcess() {
        long totalRecords = repository.count();
        System.out.println("\n========================================================");
        System.out.println("🚀 [FULL AUDIT] Bắt đầu đối soát TOÀN BỘ " + totalRecords + " tin trong Database!");

        if (totalRecords == 0) {
            System.out.println("⚠️ DB trống, không có dữ liệu để kiểm tra!");
            return;
        }

        boolean isServer = System.getenv("CI") != null || System.getenv("RENDER") != null
                || System.getenv("DOCKER") != null || System.getProperty("os.name").toLowerCase().contains("linux");

        Path crashDir = Paths.get(System.getProperty("java.io.tmpdir"), "chrome-crashes").toAbsolutePath();
        File crashFileDir = crashDir.toFile();
        if (!crashFileDir.exists()) crashFileDir.mkdirs();

        Path userDataDir = isServer
                ? Paths.get(System.getProperty("java.io.tmpdir"), "chrome-profile-audit-" + System.currentTimeMillis()).toAbsolutePath()
                : Paths.get(System.getProperty("user.home"), ".chrome-audit-profile").toAbsolutePath();
        File profileDir = userDataDir.toFile();
        if (!profileDir.exists()) profileDir.mkdirs();

        int totalChecked = 0;
        int matched = 0;
        int mismatched = 0;
        int failedToFetch = 0;

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

            int pageNumber = 0;
            boolean hasNextPage = true;

            while (hasNextPage) {
                org.springframework.data.domain.Page<CrawPropertyListing> listingPage =
                        repository.findAll(PageRequest.of(pageNumber, BATCH_SIZE));
                List<CrawPropertyListing> listings = listingPage.getContent();

                if (listings.isEmpty()) break;

                for (CrawPropertyListing listing : listings) {
                    if (listing.getSourceUrl() == null || listing.getSourceUrl().isEmpty()) continue;

                    totalChecked++;
                    System.out.println("\n--------------------------------------------------------");
                    System.out.println("📌 Tiến độ: [" + totalChecked + "/" + totalRecords + "] - ID DB: " + listing.getCrawPropertyListingId());
                    System.out.println("🔗 URL: " + listing.getSourceUrl());

                    try {
                        page.navigate(listing.getSourceUrl(), new Page.NavigateOptions().setTimeout(30000));
                        page.mouse().move(150, 200);
                        randomSleep(1000, 2000);

                        String title = page.title();
                        if (title.contains("Just a moment") || title.contains("Access Denied") || title.contains("Thực hiện xác minh bảo mật")) {
                            System.out.println("⚠️ [BLOCKED] Vẫn bị Cloudflare chặn!");
                            failedToFetch++;
                            continue;
                        }

                        Document doc = Jsoup.parse(page.content());

                        BigDecimal webArea = extractAreaFromWeb(doc);
                        BigDecimal webPrice = extractPriceFromWeb(doc, webArea);

                        boolean areaMatch = isCloseEnough(listing.getArea(), webArea);
                        boolean priceMatch = isCloseEnough(listing.getPrice(), webPrice);

                        System.out.println("📊 DB Data  -> Diện tích: " + (listing.getArea() != null ? listing.getArea().stripTrailingZeros().toPlainString() : "N/A") + " m² | Giá: " + formatVnd(listing.getPrice()));
                        System.out.println("🌐 Web Data -> Diện tích: " + (webArea != null ? webArea.stripTrailingZeros().toPlainString() : "N/A") + " m² | Giá: " + formatVnd(webPrice));

                        if (areaMatch && priceMatch) {
                            matched++;
                            System.out.println("✅ [MATCH] Khớp dữ liệu chuẩn!");
                        } else {
                            mismatched++;
                            List<String> mismatchDetails = new ArrayList<>();
                            if (!areaMatch) {
                                mismatchDetails.add("Diện tích [DB: " + (listing.getArea() != null ? listing.getArea().stripTrailingZeros().toPlainString() : "N/A") + " m² VS Web: " + (webArea != null ? webArea.stripTrailingZeros().toPlainString() : "N/A") + " m²]");
                            }
                            if (!priceMatch) {
                                mismatchDetails.add("Giá [DB: " + formatVnd(listing.getPrice()) + " VS Web: " + formatVnd(webPrice) + "]");
                            }
                            System.out.println("❌ [MISMATCH] Sai lệch: " + String.join(" | ", mismatchDetails));
                        }

                    } catch (Exception e) {
                        failedToFetch++;
                        System.out.println("⚠️ [ERROR] Lỗi truy cập: " + e.getMessage());
                    }
                }

                hasNextPage = listingPage.hasNext();
                pageNumber++;
            }

        } catch (Exception e) {
            System.out.println("❌ Lỗi hệ thống Playwright Audit: " + e.getMessage());
        } finally {
            if (page != null && !page.isClosed()) try { page.close(); } catch (Exception ignored) {}
            if (context != null) try { context.close(); } catch (Exception ignored) {}
            if (isServer) try { FileSystemUtils.deleteRecursively(userDataDir); } catch (Exception ignored) {}
        }

        System.out.println("\n========================================================");
        System.out.println("🎉 KẾT QUẢ ĐỐI SOÁT TOÀN BỘ DATABASE (" + totalChecked + " tin)");
        System.out.println("-> Tổng tin đã kiểm tra : " + totalChecked);
        System.out.println("-> Khớp dữ liệu chuẩn (✅) : " + matched + " (" + String.format("%.2f", (matched * 100.0 / totalChecked)) + "%)");
        System.out.println("-> Bị sai lệch       (❌) : " + mismatched);
        System.out.println("-> Lỗi/Bị chặn web   (⚠️) : " + failedToFetch);
        System.out.println("========================================================\n");
    }

    private BigDecimal extractAreaFromWeb(Document doc) {
        try {
            Element areaElem = doc.selectFirst(".re__pr-specs-content-item:contains(Diện tích) .re__pr-specs-content-item-value");
            if (areaElem == null) {
                areaElem = doc.selectFirst(".re__short-title:contains(m²), .re__pr-short-info-item:contains(m²)");
            }
            if (areaElem != null) return parseNumber(areaElem.text());
        } catch (Exception ignored) {}
        return null;
    }

    private BigDecimal extractPriceFromWeb(Document doc, BigDecimal area) {
        try {
            Element priceElem = doc.selectFirst(".re__pr-specs-content-item:contains(Mức giá) .re__pr-specs-content-item-value, " +
                    ".re__pr-specs-content-item:contains(Khoảng giá) .re__pr-specs-content-item-value, " +
                    ".re__pr-specs-content-item:contains(Giá) .re__pr-specs-content-item-value");
            if (priceElem == null) {
                priceElem = doc.selectFirst(".re__short-title:contains(tỷ), .re__short-title:contains(triệu), " +
                        ".re__pr-short-info-item:contains(tỷ), .re__pr-short-info-item:contains(triệu)");
            }
            if (priceElem != null) {
                String priceText = priceElem.text().toLowerCase();
                if (priceText.contains("thỏa thuận")) return null;

                BigDecimal parsedNum = parseNumber(priceText);
                if (parsedNum == null) return null;

                if (priceText.contains("tỷ")) {
                    return parsedNum.multiply(new BigDecimal("1000000000")).setScale(2, RoundingMode.HALF_UP);
                } else if (priceText.contains("triệu/m²") || priceText.contains("tr/m²")) {
                    if (area != null) {
                        BigDecimal pricePerM2 = parsedNum.multiply(new BigDecimal("1000000"));
                        return pricePerM2.multiply(area).setScale(2, RoundingMode.HALF_UP);
                    }
                } else if (priceText.contains("triệu") || priceText.contains("tr")) {
                    return parsedNum.multiply(new BigDecimal("1000000")).setScale(2, RoundingMode.HALF_UP);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BigDecimal parseNumber(String text) {
        if (text == null || text.isEmpty()) return null;
        try {
            Matcher matcher = Pattern.compile("([0-9]+([.,][0-9]+)*)").matcher(text);
            if (matcher.find()) {
                String clean = matcher.group(1);
                if (clean.matches(".*[.,]\\d{3}$")) {
                    clean = clean.replaceAll("[.,]", "");
                } else {
                    clean = clean.replace(",", ".");
                }
                return new BigDecimal(clean);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean isCloseEnough(BigDecimal dbVal, BigDecimal webVal) {
        if (dbVal == null && webVal == null) return true;
        if (dbVal == null || webVal == null) return false;

        BigDecimal cleanDb = dbVal.stripTrailingZeros();
        BigDecimal cleanWeb = webVal.stripTrailingZeros();

        if (cleanDb.compareTo(cleanWeb) == 0) return true;

        BigDecimal diff = dbVal.subtract(webVal).abs();
        BigDecimal tolerance = dbVal.multiply(new BigDecimal("0.0001"));
        return diff.compareTo(tolerance) <= 0;
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "N/A";
        if (amount.compareTo(new BigDecimal("1000000000")) >= 0) {
            return amount.divide(new BigDecimal("1000000000"), 2, RoundingMode.HALF_UP) + " Tỷ";
        }
        return amount.divide(new BigDecimal("1000000"), 2, RoundingMode.HALF_UP) + " Tr";
    }

    private void randomSleep(long minMs, long maxMs) {
        try {
            long sleepTime = minMs + (long) (Math.random() * (maxMs - minMs));
            Thread.sleep(sleepTime);
        } catch (InterruptedException ignored) {}
    }
}