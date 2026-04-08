package com.sak.service.util;

import org.springframework.util.StringUtils;

public final class UserAgentUtils {
    private UserAgentUtils() {
    }

    public static String resolveBrowser(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "未知浏览器";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("micromessenger")) {
            return "WeChat";
        }
        if (ua.contains("postmanruntime")) {
            return "Postman";
        }
        return "其他";
    }

    public static String resolveOs(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "未知系统";
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "其他";
    }
}
