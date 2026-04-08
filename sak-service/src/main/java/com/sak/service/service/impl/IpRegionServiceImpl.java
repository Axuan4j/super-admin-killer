package com.sak.service.service.impl;

import com.sak.service.config.Ip2RegionProperties;
import com.sak.service.service.IpRegionService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.service.Config;
import org.lionsoul.ip2region.service.Ip2Region;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class IpRegionServiceImpl implements IpRegionService {

    private final Ip2Region ip2Region;

    public IpRegionServiceImpl(Ip2RegionProperties properties, ResourceLoader resourceLoader) {
        this.ip2Region = initializeSearcher(properties, resourceLoader);
    }

    @Override
    public String resolveRegion(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "";
        }
        if (isLocalIp(ip)) {
            return "内网IP";
        }
        if (ip2Region == null) {
            return "";
        }
        try {
            String region = ip2Region.search(ip);
            return simplifyRegion(region);
        } catch (Exception ex) {
            log.debug("Resolve ip region failed, ip={}", ip, ex);
            return "";
        }
    }

    @PreDestroy
    public void destroy() {
        if (ip2Region == null) {
            return;
        }
        try {
            ip2Region.close();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Ip2Region initializeSearcher(Ip2RegionProperties properties, ResourceLoader resourceLoader) {
        if (properties == null || !properties.isEnabled()) {
            log.info("ip2region is disabled, login ip location resolving will be skipped");
            return null;
        }
        try {
            Config v4Config = buildConfig(resourceLoader, properties.getIpv4Db(), properties.getIpv4Classpath(), true);
            Config v6Config = buildConfig(resourceLoader, properties.getIpv6Db(), properties.getIpv6Classpath(), false);
            if (v4Config == null && v6Config == null) {
                log.warn("ip2region database not found in either external path or classpath fallback, login ip location will be empty");
                return null;
            }
            log.info("ip2region initialized successfully, login ip location resolving is enabled");
            return Ip2Region.create(v4Config, v6Config);
        } catch (Exception ex) {
            log.warn("Initialize ip2region searcher failed, login ip location will be empty", ex);
            return null;
        }
    }

    private Config buildConfig(ResourceLoader resourceLoader, String externalLocation, String classpathLocation, boolean ipv4) throws Exception {
        Config externalConfig = buildExternalConfig(externalLocation, ipv4);
        if (externalConfig != null) {
            return externalConfig;
        }
        return buildClasspathConfig(resourceLoader, classpathLocation, ipv4);
    }

    private Config buildExternalConfig(String location, boolean ipv4) throws Exception {
        if (!StringUtils.hasText(location)) {
            log.info("ip2region {} external path is empty, skip external loading", versionLabel(ipv4));
            return null;
        }
        Path path = Path.of(location).toAbsolutePath().normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            log.info("ip2region {} external file not found: {}", versionLabel(ipv4), path);
            return null;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            if (ipv4) {
                log.info("Load ip2region IPv4 database from external path: {}", path);
                return Config.custom().setCachePolicy(Config.BufferCache).setXdbInputStream(inputStream).asV4();
            }
            log.info("Load ip2region IPv6 database from external path: {}", path);
            return Config.custom().setCachePolicy(Config.BufferCache).setXdbInputStream(inputStream).asV6();
        }
    }

    private Config buildClasspathConfig(ResourceLoader resourceLoader, String location, boolean ipv4) throws Exception {
        if (!StringUtils.hasText(location)) {
            log.info("ip2region {} classpath fallback is empty, skip classpath loading", versionLabel(ipv4));
            return null;
        }
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            log.info("ip2region {} classpath resource not found: {}", versionLabel(ipv4), location);
            return null;
        }
        try (InputStream inputStream = resource.getInputStream()) {
            if (ipv4) {
                log.info("Load ip2region IPv4 database from classpath resource: {}", location);
                return Config.custom().setCachePolicy(Config.BufferCache).setXdbInputStream(inputStream).asV4();
            }
            log.info("Load ip2region IPv6 database from classpath resource: {}", location);
            return Config.custom().setCachePolicy(Config.BufferCache).setXdbInputStream(inputStream).asV6();
        }
    }

    private boolean isLocalIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }

    private String simplifyRegion(String rawRegion) {
        if (!StringUtils.hasText(rawRegion)) {
            return "";
        }
        String[] parts = rawRegion.split("\\|");
        List<String> values = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1 && parts[i].length() <= 4) {
                continue;
            }
            String part = parts[i].trim();
            if (!StringUtils.hasText(part) || "0".equals(part) || "null".equalsIgnoreCase(part)) {
                continue;
            }
            values.add(part);
        }
        return String.join(" ", values);
    }

    private String versionLabel(boolean ipv4) {
        return ipv4 ? "IPv4" : "IPv6";
    }
}
