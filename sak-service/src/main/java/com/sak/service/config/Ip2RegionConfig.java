package com.sak.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Ip2RegionProperties.class)
public class Ip2RegionConfig {
}
