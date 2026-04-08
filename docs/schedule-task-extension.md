# 调度中心扩展说明

## 1. 这份文档解决什么问题

这份文档用于说明两件事：

- 新增一个调度任务类型时，需要实现哪些代码
- 为什么新增代码后，重启服务就能自动同步任务类型元数据到数据库

当前项目的调度中心采用的是：

- `ScheduledTaskDefinitionHandler` 作为任务类型扩展接口
- Spring 启动时自动收集所有任务类型
- 服务启动完成后自动同步到 `sys_scheduled_task_type`

这意味着：

- 新增一个任务类型，主要是新增一个 Java 类
- 不需要手工维护任务类型数据
- 不需要额外写“任务类型初始化 SQL”
- 部署新代码并重启后端后，页面就能看到新任务类型

## 2. 核心接口

核心接口在：

- [ScheduledTaskDefinitionHandler.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/service/ScheduledTaskDefinitionHandler.java)

接口定义如下：

```java
public interface ScheduledTaskDefinitionHandler {

    String getTaskType();

    ScheduledTaskTypeOptionResponse getTaskTypeDefinition();

    void validateTaskConfig(JsonNode taskConfig);

    String buildTaskSummary(JsonNode taskConfig);

    void execute(JsonNode taskConfig);
}
```

可以把它理解成“调度任务类型 SPI”。

每一个实现类，都代表一种可在页面上配置和执行的任务类型。

## 3. 需要实现哪些方法

新增一个任务类型时，通常只需要实现这 5 个方法。

### 3.1 `getTaskType()`

返回任务类型唯一编码。

要求：

- 全局唯一
- 建议全大写，下划线分隔
- 后续数据库和页面都用这个值做识别

示例：

```java
@Override
public String getTaskType() {
    return "DATA_CLEANUP";
}
```

### 3.2 `getTaskTypeDefinition()`

返回任务类型元数据，主要用于页面展示和动态表单渲染。

类型定义在：

- [ScheduledTaskTypeOptionResponse.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/dto/ScheduledTaskTypeOptionResponse.java)

可配置内容包括：

- 类型编码
- 显示名称
- 描述文案
- 动态表单 schema

动态表单相关 DTO 在：

- [ScheduledTaskFormFieldResponse.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/dto/ScheduledTaskFormFieldResponse.java)
- [ScheduledTaskFieldOptionResponse.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/dto/ScheduledTaskFieldOptionResponse.java)

常用 `component` 目前支持：

- `input`
- `textarea`
- `radio-group`
- `checkbox-group`
- `user-select`

### 3.3 `validateTaskConfig(JsonNode taskConfig)`

用于校验动态表单提交的任务参数。

建议做的事情：

- 必填字段校验
- 数值范围校验
- 条件字段校验
- 组合参数校验

如果参数不合法，直接抛出 `IllegalArgumentException`。

### 3.4 `buildTaskSummary(JsonNode taskConfig)`

生成任务摘要，用于调度中心列表展示。

建议：

- 输出简短
- 让用户一眼能看懂“这个任务要做什么”
- 不要塞太长的技术信息

### 3.5 `execute(JsonNode taskConfig)`

这里写实际的执行逻辑。

常见做法：

- 先把 `JsonNode` 转成内部配置类
- 调用业务 service
- 不要直接在这里拼过多基础设施代码

建议保持：

- 幂等或尽量接近幂等
- 异常信息明确
- 尽量不要依赖页面层逻辑

## 4. 一个最小实现模板

可以直接参考现有示例：

- [NotificationScheduledTaskDefinitionHandler.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/service/impl/NotificationScheduledTaskDefinitionHandler.java)

下面是一个通用模板：

```java
@Service
@RequiredArgsConstructor
public class DataCleanupScheduledTaskDefinitionHandler implements ScheduledTaskDefinitionHandler {

    private static final String TASK_TYPE = "DATA_CLEANUP";

    private final ObjectMapper objectMapper;
    private final SomeBizService someBizService;

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public ScheduledTaskTypeOptionResponse getTaskTypeDefinition() {
        return new ScheduledTaskTypeOptionResponse(
                TASK_TYPE,
                "数据清理任务",
                "按条件清理历史数据",
                buildFormSchema()
        );
    }

    @Override
    public void validateTaskConfig(JsonNode taskConfig) {
        CleanupTaskConfig config = parseConfig(taskConfig);
        if (config.getRetentionDays() == null || config.getRetentionDays() <= 0) {
            throw new IllegalArgumentException("保留天数必须大于 0");
        }
    }

    @Override
    public String buildTaskSummary(JsonNode taskConfig) {
        CleanupTaskConfig config = parseConfig(taskConfig);
        return "清理 " + config.getBizType() + "，保留 " + config.getRetentionDays() + " 天";
    }

    @Override
    public void execute(JsonNode taskConfig) {
        CleanupTaskConfig config = parseConfig(taskConfig);
        someBizService.cleanup(config.getBizType(), config.getRetentionDays());
    }

    private CleanupTaskConfig parseConfig(JsonNode taskConfig) {
        if (taskConfig == null || taskConfig.isNull()) {
            throw new IllegalArgumentException("任务配置不能为空");
        }
        return objectMapper.convertValue(taskConfig, CleanupTaskConfig.class);
    }

    private List<ScheduledTaskFormFieldResponse> buildFormSchema() {
        return List.of();
    }

    @Data
    private static class CleanupTaskConfig {
        private String bizType;
        private Integer retentionDays;
    }
}
```

## 5. 为什么重启后会自动同步到数据库

这里的关键不是数据库自动识别，而是应用启动时自动同步。

核心逻辑在：

- [ScheduledTaskServiceImpl.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/service/impl/ScheduledTaskServiceImpl.java)

主要流程如下。

### 5.1 Spring 会先收集所有任务类型 Bean

在 `ScheduledTaskServiceImpl` 里有这个字段：

```java
private final List<ScheduledTaskDefinitionHandler> taskDefinitionHandlers;
```

只要你的实现类：

- 实现了 `ScheduledTaskDefinitionHandler`
- 被 Spring 扫描到
- 成功注册成 Bean

它就会出现在这个列表里。

### 5.2 应用启动完成后会触发自动恢复逻辑

`ScheduledTaskServiceImpl` 里有：

```java
@EventListener(ApplicationReadyEvent.class)
public void restoreScheduledTasks() {
    refreshTaskTypes();
    ...
}
```

这里用了 `ApplicationReadyEvent`，所以在应用启动完成后，会自动执行 `restoreScheduledTasks()`。

### 5.3 `restoreScheduledTasks()` 会先调用 `refreshTaskTypes()`

这一步就是“自动同步任务类型元数据”的入口。

`refreshTaskTypes()` 做的事情是：

1. 遍历当前 Spring 容器中的所有 `ScheduledTaskDefinitionHandler`
2. 调用每个 handler 的 `getTaskTypeDefinition()`
3. 把任务类型元数据写入 `sys_scheduled_task_type`

### 5.4 同步规则

当前同步规则是：

- 数据库里没有这个 `task_type`：执行 `insert`
- 数据库里已经有这个 `task_type`：执行 `update`
- 以前是内置类型，但当前代码里已经没有这个 handler：更新为 `enabled = 0`

对应实体与 mapper 在：

- [SysScheduledTaskType.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/entity/SysScheduledTaskType.java)
- [SysScheduledTaskTypeMapper.java](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/java/com/sak/service/mapper/SysScheduledTaskTypeMapper.java)

所以这张表的定位应该理解为：

- 代码是任务类型定义的真实来源
- `sys_scheduled_task_type` 是运行时同步结果

不建议把它当作一张长期手工维护的配置表。

## 6. 新增一个任务类型的标准步骤

推荐按下面顺序做。

1. 新建一个 `ScheduledTaskDefinitionHandler` 实现类
2. 给类加上 `@Service`
3. 实现 5 个核心方法
4. 把参数解析成内部配置类，不直接裸读 `JsonNode`
5. 启动本地服务验证页面是否出现该任务类型
6. 部署新代码并重启服务

完成后系统会自动：

- 同步任务类型到 `sys_scheduled_task_type`
- 在调度中心页面展示出来

## 7. 是否需要写任务类型初始化 SQL

一般不需要。

原因是任务类型元数据现在由代码自动同步：

- 新增 handler 后，重启服务即可
- 不需要手工向 `sys_scheduled_task_type` 插数据

真正仍然需要写 SQL 的场景通常是：

- 新增业务表结构
- 新增菜单和按钮权限
- 新增字典或基础配置

但“任务类型本身”不建议走 SQL 主维护。

## 8. 当前模式的边界

当前模式适合：

- 项目内部新增任务类型
- 新版本发布时一起上线
- 通过重启服务完成生效

当前模式不支持：

- 运行时加载新 class
- 运行时扫描新 jar
- 无重启插件化扩展

如果未来真的需要这类能力，再考虑插件化方案更合适；就当前项目来说，现有模式更简单，也更稳定。
