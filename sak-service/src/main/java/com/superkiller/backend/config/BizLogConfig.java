package com.superkiller.backend.config;

import com.mzt.logapi.starter.annotation.EnableLogRecord;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableLogRecord(tenant = "superkiller-admin")
public class BizLogConfig {
}
