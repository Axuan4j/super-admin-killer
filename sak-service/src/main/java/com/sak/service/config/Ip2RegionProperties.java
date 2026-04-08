package com.sak.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ip2region")
public class Ip2RegionProperties {
    private boolean enabled = true;
    private String ipv4Db = "./ip2region/ip2region_v4.xdb";
    private String ipv6Db = "";
    private String ipv4Classpath = "";
    private String ipv6Classpath = "";
}
