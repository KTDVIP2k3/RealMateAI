package com.GSU26SE22_SU26SE002.RealMateAI.scheduler;

import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NewsServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("crawler")
public class NewsCrawlerScheduler {

    @Autowired
    private NewsServiceInterface newsService;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void runAutoCrawl() {
        newsService.autoCrawlNewsData();
    }
}