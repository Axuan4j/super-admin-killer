package com.sak.service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class RequestIpUtils {
    private RequestIpUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (!StringUtils.hasText(headerValue) || "unknown".equalsIgnoreCase(headerValue)) {
                continue;
            }
            String firstIp = headerValue.split(",")[0].trim();
            if (StringUtils.hasText(firstIp) && !"unknown".equalsIgnoreCase(firstIp)) {
                return normalizeIp(firstIp);
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private static String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "";
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }
}
