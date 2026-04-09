package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.sak.service.dto.MonitorOverviewResponse;
import com.sak.service.entity.SysLoginLog;
import com.sak.service.entity.SysScheduledTask;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysLoginLogMapper;
import com.sak.service.mapper.SysScheduledTaskMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.SystemMonitorService;
import com.sak.service.service.TokenService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private static final Duration SNAPSHOT_TTL = Duration.ofSeconds(5);
    private static final ZoneId SYSTEM_ZONE_ID = ZoneId.systemDefault();

    private final DataSource dataSource;
    private final DataSourceProperties dataSourceProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationContext applicationContext;
    private final CaffeineCacheManager caffeineCacheManager;
    private final SysScheduledTaskMapper sysScheduledTaskMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysUserMapper sysUserMapper;
    private final TokenService tokenService;

    private final Object snapshotMonitor = new Object();

    private volatile CachedSnapshot cachedSnapshot;

    @Override
    public MonitorOverviewResponse getOverview() {
        CachedSnapshot snapshot = cachedSnapshot;
        long now = System.currentTimeMillis();
        if (snapshot != null && now - snapshot.cachedAtMillis < SNAPSHOT_TTL.toMillis()) {
            return snapshot.response;
        }

        synchronized (snapshotMonitor) {
            snapshot = cachedSnapshot;
            now = System.currentTimeMillis();
            if (snapshot != null && now - snapshot.cachedAtMillis < SNAPSHOT_TTL.toMillis()) {
                return snapshot.response;
            }

            MonitorOverviewResponse response = collectOverview();
            cachedSnapshot = new CachedSnapshot(now, response);
            return response;
        }
    }

    private MonitorOverviewResponse collectOverview() {
        MonitorOverviewResponse response = new MonitorOverviewResponse();
        response.setSnapshotTime(LocalDateTime.now());
        response.setCacheTtlSeconds(SNAPSHOT_TTL.toSeconds());
        response.setJvm(collectJvmInfo());
        response.setDatabase(collectDatabaseInfo());
        response.setRedis(collectRedisInfo());
        response.setThreadPools(collectThreadPools());
        response.setCache(collectCacheOverview());
        response.setScheduledTask(collectScheduledTaskOverview());
        response.setSecurity(collectSecurityOverview());

        try {
            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
            response.setServer(collectServerInfo(operatingSystem));
            response.setCpu(collectCpuInfo(hardware.getProcessor()));
            response.setMemory(collectMemoryInfo(hardware.getMemory()));
            response.setDisks(collectDiskInfo(operatingSystem.getFileSystem()));
        } catch (RuntimeException ex) {
            log.warn("collecting hardware monitor snapshot failed", ex);
        }

        return response;
    }

    private MonitorOverviewResponse.ServerInfo collectServerInfo(OperatingSystem operatingSystem) {
        MonitorOverviewResponse.ServerInfo info = new MonitorOverviewResponse.ServerInfo();
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            info.setHostName(localHost.getHostName());
            info.setHostAddress(localHost.getHostAddress());
        } catch (UnknownHostException ex) {
            log.debug("resolve local host failed", ex);
            info.setHostName("unknown");
            info.setHostAddress("unknown");
        }
        info.setOsName(operatingSystem.getFamily());
        info.setOsVersion(operatingSystem.getVersionInfo().getVersion());
        info.setOsArch(System.getProperty("os.arch"));
        info.setProcessCount(operatingSystem.getProcessCount());
        info.setThreadCount(operatingSystem.getThreadCount());
        info.setBootTime(toLocalDateTime(operatingSystem.getSystemBootTime()));
        info.setUptimeSeconds(operatingSystem.getSystemUptime());
        return info;
    }

    private MonitorOverviewResponse.CpuInfo collectCpuInfo(CentralProcessor processor) {
        MonitorOverviewResponse.CpuInfo info = new MonitorOverviewResponse.CpuInfo();
        info.setModel(processor.getProcessorIdentifier().getName());
        info.setPhysicalPackages(processor.getPhysicalPackageCount());
        info.setPhysicalCores(processor.getPhysicalProcessorCount());
        info.setLogicalCores(processor.getLogicalProcessorCount());
        info.setUsagePercent(toPercent(processor.getSystemCpuLoad(200)));
        double[] loadAverage = processor.getSystemLoadAverage(3);
        info.setLoadAverage1m(toNullableDouble(loadAverage, 0));
        info.setLoadAverage5m(toNullableDouble(loadAverage, 1));
        info.setLoadAverage15m(toNullableDouble(loadAverage, 2));
        return info;
    }

    private MonitorOverviewResponse.MemoryInfo collectMemoryInfo(GlobalMemory memory) {
        MonitorOverviewResponse.MemoryInfo info = new MonitorOverviewResponse.MemoryInfo();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = Math.max(total - available, 0);
        info.setTotal(total);
        info.setAvailable(available);
        info.setUsed(used);
        info.setUsagePercent(total <= 0 ? 0D : round((double) used / total * 100));
        info.setSwapTotal(memory.getVirtualMemory().getSwapTotal());
        info.setSwapUsed(memory.getVirtualMemory().getSwapUsed());
        return info;
    }

    private MonitorOverviewResponse.JvmInfo collectJvmInfo() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        MonitorOverviewResponse.JvmInfo info = new MonitorOverviewResponse.JvmInfo();
        info.setName(runtimeMXBean.getVmName());
        info.setVendor(runtimeMXBean.getVmVendor());
        info.setVersion(runtimeMXBean.getVmVersion());
        info.setStartTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(runtimeMXBean.getStartTime()), SYSTEM_ZONE_ID));
        info.setUptimeMs(runtimeMXBean.getUptime());
        info.setHeapUsed(heap.getUsed());
        info.setHeapCommitted(heap.getCommitted());
        info.setHeapMax(heap.getMax());
        info.setNonHeapUsed(nonHeap.getUsed());
        info.setThreadCount(threadMXBean.getThreadCount());
        return info;
    }

    private MonitorOverviewResponse.DatabaseInfo collectDatabaseInfo() {
        MonitorOverviewResponse.DatabaseInfo info = new MonitorOverviewResponse.DatabaseInfo();
        info.setUrl(firstNonBlank(dataSourceProperties.getUrl(), dataSourceProperties.determineUrl()));
        info.setUsername(firstNonBlank(dataSourceProperties.getUsername(), dataSourceProperties.determineUsername()));
        applyDataSourcePoolStats(info);

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            info.setConnected(true);
            info.setProductName(metaData.getDatabaseProductName());
            info.setProductVersion(metaData.getDatabaseProductVersion());
            info.setDriverName(metaData.getDriverName());
            info.setUrl(firstNonBlank(info.getUrl(), metaData.getURL()));
            info.setUsername(firstNonBlank(info.getUsername(), metaData.getUserName()));
            info.setDatabaseName(connection.getCatalog());
            info.setMessage("连接正常");
        } catch (Exception ex) {
            info.setConnected(false);
            info.setMessage(ex.getMessage());
            log.debug("collect database monitor snapshot failed", ex);
        }

        return info;
    }

    private void applyDataSourcePoolStats(MonitorOverviewResponse.DatabaseInfo info) {
        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {
            return;
        }

        info.setMaxPoolSize(hikariDataSource.getMaximumPoolSize());
        HikariPoolMXBean poolMXBean = hikariDataSource.getHikariPoolMXBean();
        if (poolMXBean == null) {
            return;
        }
        info.setActiveConnections(poolMXBean.getActiveConnections());
        info.setIdleConnections(poolMXBean.getIdleConnections());
        info.setTotalConnections(poolMXBean.getTotalConnections());
    }

    private MonitorOverviewResponse.RedisInfo collectRedisInfo() {
        try {
            MonitorOverviewResponse.RedisInfo info = redisTemplate.execute((RedisCallback<MonitorOverviewResponse.RedisInfo>) connection -> {
                MonitorOverviewResponse.RedisInfo redisInfo = new MonitorOverviewResponse.RedisInfo();
                redisInfo.setConnected(true);
                Properties properties = connection.serverCommands().info();
                redisInfo.setVersion(readProperty(properties, "redis_version"));
                redisInfo.setMode(readProperty(properties, "redis_mode"));
                redisInfo.setUsedMemoryHuman(readProperty(properties, "used_memory_human"));
                redisInfo.setUptimeSeconds(readLongProperty(properties, "uptime_in_seconds"));
                redisInfo.setConnectedClients(readLongProperty(properties, "connected_clients"));
                redisInfo.setDbSize(connection.serverCommands().dbSize());
                redisInfo.setMessage("连接正常");
                return redisInfo;
            });

            if (info != null) {
                return info;
            }
        } catch (DataAccessException ex) {
            log.debug("collect redis monitor snapshot failed", ex);
            return buildRedisUnavailable(ex.getMessage());
        } catch (RuntimeException ex) {
            log.debug("collect redis monitor snapshot failed", ex);
            return buildRedisUnavailable(ex.getMessage());
        }

        return buildRedisUnavailable("Redis 连接不可用");
    }

    private MonitorOverviewResponse.RedisInfo buildRedisUnavailable(String message) {
        MonitorOverviewResponse.RedisInfo info = new MonitorOverviewResponse.RedisInfo();
        info.setConnected(false);
        info.setMessage(message);
        return info;
    }

    private List<MonitorOverviewResponse.DiskInfo> collectDiskInfo(FileSystem fileSystem) {
        List<MonitorOverviewResponse.DiskInfo> disks = new ArrayList<>();
        for (OSFileStore fileStore : fileSystem.getFileStores()) {
            long total = fileStore.getTotalSpace();
            long usable = fileStore.getUsableSpace();
            long used = Math.max(total - usable, 0);

            MonitorOverviewResponse.DiskInfo info = new MonitorOverviewResponse.DiskInfo();
            info.setName(fileStore.getName());
            info.setMount(fileStore.getMount());
            info.setType(fileStore.getType());
            info.setDescription(fileStore.getDescription());
            info.setTotal(total);
            info.setUsable(usable);
            info.setUsed(used);
            info.setUsagePercent(total <= 0 ? 0D : round((double) used / total * 100));
            disks.add(info);
        }
        return disks;
    }

    private List<MonitorOverviewResponse.ThreadPoolInfo> collectThreadPools() {
        Map<String, ThreadPoolTaskExecutor> executors = applicationContext.getBeansOfType(ThreadPoolTaskExecutor.class);
        if (executors.isEmpty()) {
            return List.of();
        }

        List<MonitorOverviewResponse.ThreadPoolInfo> threadPools = new ArrayList<>();
        executors.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> threadPools.add(toThreadPoolInfo(entry.getKey(), entry.getValue())));
        return threadPools;
    }

    private MonitorOverviewResponse.CacheOverview collectCacheOverview() {
        MonitorOverviewResponse.CacheOverview overview = new MonitorOverviewResponse.CacheOverview();
        List<MonitorOverviewResponse.CacheInfo> caches = new ArrayList<>();
        List<String> cacheNames = caffeineCacheManager.getCacheNames().stream().sorted().toList();
        for (String cacheName : cacheNames) {
            org.springframework.cache.Cache springCache = caffeineCacheManager.getCache(cacheName);
            if (!(springCache instanceof CaffeineCache caffeineCache)) {
                continue;
            }
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats stats = nativeCache.stats();

            MonitorOverviewResponse.CacheInfo cacheInfo = new MonitorOverviewResponse.CacheInfo();
            cacheInfo.setName(cacheName);
            cacheInfo.setEstimatedSize(nativeCache.estimatedSize());
            cacheInfo.setHitCount(stats.hitCount());
            cacheInfo.setMissCount(stats.missCount());
            cacheInfo.setHitRate(round(stats.hitRate() * 100));
            cacheInfo.setEvictionCount(stats.evictionCount());
            caches.add(cacheInfo);
        }
        overview.setCacheCount(caches.size());
        overview.setCaches(caches);
        return overview;
    }

    private MonitorOverviewResponse.ScheduledTaskOverview collectScheduledTaskOverview() {
        MonitorOverviewResponse.ScheduledTaskOverview overview = new MonitorOverviewResponse.ScheduledTaskOverview();
        overview.setTotalCount(sysScheduledTaskMapper.selectCount(null));
        overview.setScheduledCount(countScheduledTasksByStatus("SCHEDULED"));
        overview.setPausedCount(countScheduledTasksByStatus("PAUSED"));
        overview.setRunningCount(countScheduledTasksByStatus("RUNNING"));
        overview.setFailureCount(sysScheduledTaskMapper.selectCount(new LambdaQueryWrapper<SysScheduledTask>()
                .isNotNull(SysScheduledTask::getLastFailureTime)));

        SysScheduledTask latestFailureTask = sysScheduledTaskMapper.selectOne(new LambdaQueryWrapper<SysScheduledTask>()
                .isNotNull(SysScheduledTask::getLastFailureTime)
                .orderByDesc(SysScheduledTask::getLastFailureTime)
                .last("limit 1"));
        if (latestFailureTask != null) {
            overview.setLatestFailureTaskName(latestFailureTask.getTaskName());
            overview.setLatestFailureTime(latestFailureTask.getLastFailureTime());
            overview.setLatestFailureMessage(latestFailureTask.getLastErrorMessage());
        }

        SysScheduledTask nextRunTask = sysScheduledTaskMapper.selectOne(new LambdaQueryWrapper<SysScheduledTask>()
                .eq(SysScheduledTask::getStatus, "SCHEDULED")
                .isNotNull(SysScheduledTask::getNextRunTime)
                .orderByAsc(SysScheduledTask::getNextRunTime)
                .last("limit 1"));
        if (nextRunTask != null) {
            overview.setNearestNextRunTaskName(nextRunTask.getTaskName());
            overview.setNearestNextRunTime(nextRunTask.getNextRunTime());
        }
        return overview;
    }

    private MonitorOverviewResponse.SecurityOverview collectSecurityOverview() {
        MonitorOverviewResponse.SecurityOverview overview = new MonitorOverviewResponse.SecurityOverview();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        overview.setTodayLoginSuccessCount(sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                .eq(SysLoginLog::getStatus, 1)
                .ge(SysLoginLog::getLoginTime, startOfDay)));
        overview.setTodayLoginFailureCount(sysLoginLogMapper.selectCount(new LambdaQueryWrapper<SysLoginLog>()
                .eq(SysLoginLog::getStatus, 0)
                .ge(SysLoginLog::getLoginTime, startOfDay)));
        overview.setOnlineSessionCount(tokenService.listOnlineSessions().size());
        overview.setMfaEnabledUserCount(sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMfaEnabled, 1)));
        return overview;
    }

    private long countScheduledTasksByStatus(String status) {
        return sysScheduledTaskMapper.selectCount(new LambdaQueryWrapper<SysScheduledTask>()
                .eq(SysScheduledTask::getStatus, status));
    }

    private MonitorOverviewResponse.ThreadPoolInfo toThreadPoolInfo(String beanName, ThreadPoolTaskExecutor executor) {
        MonitorOverviewResponse.ThreadPoolInfo info = new MonitorOverviewResponse.ThreadPoolInfo();
        info.setBeanName(beanName);
        info.setThreadNamePrefix(executor.getThreadNamePrefix());
        info.setCorePoolSize(executor.getCorePoolSize());
        info.setMaxPoolSize(executor.getMaxPoolSize());

        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        if (threadPoolExecutor != null) {
            info.setPoolSize(threadPoolExecutor.getPoolSize());
            info.setActiveCount(threadPoolExecutor.getActiveCount());
            info.setQueueSize(threadPoolExecutor.getQueue().size());
            info.setQueueRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());
            info.setCompletedTaskCount(threadPoolExecutor.getCompletedTaskCount());
            info.setTaskCount(threadPoolExecutor.getTaskCount());
            info.setLargestPoolSize(threadPoolExecutor.getLargestPoolSize());
            info.setShutdown(threadPoolExecutor.isShutdown());
            info.setTerminated(threadPoolExecutor.isTerminated());
        }
        return info;
    }

    private LocalDateTime toLocalDateTime(long epochSeconds) {
        if (epochSeconds <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), SYSTEM_ZONE_ID);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String readProperty(Properties properties, String key) {
        if (properties == null) {
            return null;
        }
        return properties.getProperty(key);
    }

    private Long readLongProperty(Properties properties, String key) {
        String value = readProperty(properties, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double toNullableDouble(double[] values, int index) {
        if (values == null || values.length <= index || values[index] < 0) {
            return null;
        }
        return round(values[index]);
    }

    private double toPercent(double value) {
        return round(Math.max(value, 0D) * 100);
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record CachedSnapshot(long cachedAtMillis, MonitorOverviewResponse response) {
        private CachedSnapshot {
            Objects.requireNonNull(response, "response");
        }
    }
}
