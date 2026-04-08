package com.sak.service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MonitorOverviewResponse {

    private LocalDateTime snapshotTime;

    private long cacheTtlSeconds;

    private ServerInfo server;

    private CpuInfo cpu;

    private MemoryInfo memory;

    private JvmInfo jvm;

    private DatabaseInfo database;

    private RedisInfo redis;

    private List<DiskInfo> disks = new ArrayList<>();

    @Data
    public static class ServerInfo {
        private String hostName;
        private String hostAddress;
        private String osName;
        private String osVersion;
        private String osArch;
        private long processCount;
        private long threadCount;
        private LocalDateTime bootTime;
        private long uptimeSeconds;
    }

    @Data
    public static class CpuInfo {
        private String model;
        private int physicalPackages;
        private int physicalCores;
        private int logicalCores;
        private double usagePercent;
        private Double loadAverage1m;
        private Double loadAverage5m;
        private Double loadAverage15m;
    }

    @Data
    public static class MemoryInfo {
        private long total;
        private long available;
        private long used;
        private double usagePercent;
        private long swapTotal;
        private long swapUsed;
    }

    @Data
    public static class JvmInfo {
        private String name;
        private String vendor;
        private String version;
        private LocalDateTime startTime;
        private long uptimeMs;
        private long heapUsed;
        private long heapCommitted;
        private long heapMax;
        private long nonHeapUsed;
        private int threadCount;
    }

    @Data
    public static class DatabaseInfo {
        private boolean connected;
        private String productName;
        private String productVersion;
        private String driverName;
        private String url;
        private String username;
        private String databaseName;
        private Integer activeConnections;
        private Integer idleConnections;
        private Integer totalConnections;
        private Integer maxPoolSize;
        private String message;
    }

    @Data
    public static class RedisInfo {
        private boolean connected;
        private String version;
        private String mode;
        private String usedMemoryHuman;
        private Long uptimeSeconds;
        private Long connectedClients;
        private Long dbSize;
        private String message;
    }

    @Data
    public static class DiskInfo {
        private String name;
        private String mount;
        private String type;
        private String description;
        private long total;
        private long usable;
        private long used;
        private double usagePercent;
    }
}
