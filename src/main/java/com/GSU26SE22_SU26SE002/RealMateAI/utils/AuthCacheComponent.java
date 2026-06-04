package com.GSU26SE22_SU26SE002.RealMateAI.utils;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthCacheComponent {
    private static final Map<String, RegisterRequest> registerDataCache = new ConcurrentHashMap<>();
    private static final Map<String, String> loginUserCache = new ConcurrentHashMap<>();
    private static String lastActiveEmail = null;
    public Map<String, RegisterRequest> getRegisterDataCache() {
        return registerDataCache;
    }

    public Map<String, String> getLoginUserCache() {
        return loginUserCache;
    }

    public String getLastActiveEmail() {
        return lastActiveEmail;
    }

    public void setLastActiveEmail(String lastActiveEmail) {
        this.lastActiveEmail = lastActiveEmail;
    }
}
