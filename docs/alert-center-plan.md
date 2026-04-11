# 告警中心建设方案

## 1. 文档目的

这份文档用于说明当前项目如何落地一套“类似云平台一键告警”的能力。

目标不是先做一个复杂的监控产品，而是在现有项目能力基础上，尽快搭出一条完整可用的告警闭环：

- 可配置规则
- 可自动评估
- 可生成事件
- 可去重降噪
- 可多渠道通知
- 可查看、认领、忽略、恢复

结合当前项目现状，这套能力应尽量复用：

- 系统监控
- 调度中心
- 通知中心
- 站内信
- WebSocket
- 邮件
- WxPusher

## 2. 目标与边界

### 2.1 目标

第一阶段希望实现的体验：

- 提供一组内置告警模板，用户可“一键启用”
- 支持少量核心阈值调整，而不是一开始就暴露复杂表达式
- 能对系统监控和调度异常做自动评估
- 告警触发后可以发站内信、邮件、WxPusher、WebSocket
- 告警事件支持恢复、静默、忽略、处理备注

### 2.2 不在第一阶段做的内容

为了控制复杂度，第一阶段不建议做：

- PromQL 类复杂表达式
- 自定义聚合函数
- 多条件嵌套布尔规则编排
- 跨项目、多租户、跨区域统一告警
- 极细粒度升级编排引擎

这些内容可以放到第二阶段或第三阶段。

## 3. 当前项目可复用能力

当前项目已经具备的基础能力：

- 监控概览接口：
  - `/system/monitor/overview`
- 调度中心：
  - 定时任务
  - 失败任务状态
  - 任务执行能力
- 通知能力：
  - 站内信
  - 邮件
  - WxPusher
  - WebSocket
- 审计能力：
  - 操作日志
  - 登录日志

因此告警中心不应该从零开始做“采集系统”，而应优先采用“规则评估器 + 事件中心 + 通知分发”的实现方式。

## 4. 整体架构

建议整体链路如下：

1. 采集监控快照
2. 加载启用中的告警规则
3. 按规则评估是否命中
4. 生成或更新告警事件
5. 根据去重/静默策略决定是否通知
6. 发送到通知渠道
7. 在前端事件中心展示
8. 用户认领、忽略、恢复、备注

可以理解为 4 个核心模块：

- 告警规则中心
- 告警评估器
- 告警事件中心
- 告警通知中心

## 5. 第一阶段建议覆盖的告警类型

建议第一阶段只做以下 4 类，优先保证稳定可用：

### 5.1 系统资源类

- CPU 使用率过高
- 内存使用率过高
- 磁盘使用率过高

### 5.2 中间件可用性类

- Redis 连接异常
- 数据库连接异常

### 5.3 调度异常类

- 任务失败数量大于阈值
- 最近失败任务存在且未恢复

### 5.4 安全异常类

- 今日登录失败次数大于阈值

这些指标全部可以直接从现有监控聚合结果里取，不需要额外接入外部监控系统。

## 6. 产品设计思路

### 6.1 “一键告警”的核心思路

所谓“一键告警”，本质上不是少字段，而是模板化。

也就是：

- 平台先提供一组内置规则模板
- 用户点启用即可生效
- 只暴露少量必要参数，例如阈值、持续时间、通知人、通知渠道

建议内置模板：

- CPU 使用率持续过高
- 内存使用率持续过高
- Redis 不可用
- 数据库不可用
- 调度失败任务告警
- 登录失败次数告警

### 6.2 配置体验

页面不建议上来就让用户填复杂规则表达式，而是：

- 选择告警模板
- 设置阈值
- 设置持续时间
- 设置通知渠道
- 设置通知人
- 选择是否启用

这就是最接近云平台“快速启用告警”的体验。

## 7. 数据模型设计

第一阶段建议至少建两张表：

- `sys_alert_rule`
- `sys_alert_event`

如需做通知记录闭环，也可扩展：

- `sys_alert_notify_log`

### 7.1 告警规则表 `sys_alert_rule`

建议字段：

- `id`
- `rule_name`
- `rule_code`
- `rule_type`
- `rule_level`
- `metric_key`
- `operator`
- `threshold_value`
- `duration_seconds`
- `silence_minutes`
- `notify_channels`
- `notify_user_ids`
- `enabled`
- `template_code`
- `remark`
- `create_by`
- `create_time`
- `update_by`
- `update_time`

字段说明：

- `rule_type`
  - 例如：`SYSTEM`、`SCHEDULE`、`SECURITY`
- `rule_level`
  - 例如：`INFO`、`WARNING`、`CRITICAL`
- `metric_key`
  - 例如：`cpu.usagePercent`、`redis.connected`
- `operator`
  - 例如：`GT`、`GTE`、`EQ`、`NE`
- `notify_channels`
  - 建议 JSON 数组，便于多渠道扩展
- `notify_user_ids`
  - 建议 JSON 数组，便于多用户通知

### 7.2 告警事件表 `sys_alert_event`

建议字段：

- `id`
- `rule_id`
- `rule_name`
- `alert_title`
- `alert_content`
- `alert_level`
- `alert_status`
- `metric_key`
- `current_value`
- `threshold_value`
- `trigger_count`
- `first_trigger_time`
- `last_trigger_time`
- `recover_time`
- `last_notify_time`
- `notify_count`
- `assignee_id`
- `assignee_name`
- `ignore_reason`
- `handle_remark`
- `create_time`
- `update_time`

`alert_status` 建议值：

- `ACTIVE`
- `RECOVERED`
- `IGNORED`
- `HANDLED`

### 7.3 告警通知记录表 `sys_alert_notify_log`

第一阶段可选。

如果要做，建议字段：

- `id`
- `event_id`
- `channel_type`
- `receiver`
- `send_status`
- `send_message`
- `create_time`

## 8. 规则模型设计

### 8.1 最小规则结构

第一阶段建议规则判断结构足够简单：

- 一个指标
- 一个比较符
- 一个阈值
- 一个持续时间

例如：

- CPU 使用率 `>` `85`
- 持续 `300` 秒

### 8.2 指标键建议

建议统一使用“点路径”表达指标：

- `cpu.usagePercent`
- `memory.usagePercent`
- `database.connected`
- `redis.connected`
- `scheduledTask.failureCount`
- `security.todayLoginFailureCount`

这样规则配置、评估器和前端展示都可以统一。

## 9. 告警评估器设计

### 9.1 实现方式

建议直接复用现有调度中心，新增一个系统内置调度任务：

- 每 1 分钟执行一次
- 拉取监控概览
- 遍历启用规则
- 逐条评估
- 更新告警事件状态

### 9.2 为什么用轮询评估

原因很简单：

- 当前项目已经有监控概览接口
- 已有调度中心可以承接定时执行
- 第一阶段实现成本最低
- 稳定性和可控性最好

### 9.3 评估流程

每次执行时建议按以下步骤：

1. 获取监控快照
2. 获取所有启用规则
3. 读取规则对应指标值
4. 判断是否命中阈值
5. 若命中则查询是否已有未恢复事件
6. 有未恢复事件则更新 `last_trigger_time` 和 `trigger_count`
7. 无未恢复事件则创建新事件
8. 若本次未命中且存在活动事件，则将事件标记为已恢复
9. 根据静默策略决定是否通知

## 10. 去重、静默与恢复策略

这部分是告警中心体验好坏的核心。

### 10.1 去重

同一条规则、同一类事件，在未恢复前不要不断创建新事件。

建议策略：

- `rule_id + ACTIVE` 作为活动事件唯一约束逻辑
- 活动中只更新，不重复建新事件

### 10.2 静默

避免通知风暴。

建议规则：

- 首次触发立即通知
- 若仍在告警中，则在 `silence_minutes` 内不重复通知
- 超过静默时间且仍未恢复，可再次通知

### 10.3 恢复通知

恢复通知必须做。

建议：

- 当活动事件从命中变为不命中时
- 更新状态为 `RECOVERED`
- 发送恢复通知

否则用户只知道“出问题了”，不知道“已经恢复了”。

## 11. 通知分发设计

### 11.1 统一通知入口

建议新增一个统一服务：

- `AlertNotifyService`

职责：

- 接收告警事件
- 将事件转换为通知文案
- 分发给站内信、邮件、WxPusher、WebSocket

### 11.2 通知渠道

建议直接复用当前项目已有通知能力：

- 站内信
- 邮件
- WxPusher
- WebSocket

### 11.3 告警通知文案

建议统一结构：

- 告警级别
- 告警标题
- 当前值 / 阈值
- 触发时间
- 快速处理建议

例如：

- `【严重】CPU 使用率过高`
- `当前值：91.2%，阈值：85%`
- `已持续 5 分钟`
- `建议尽快检查系统负载与任务执行情况`

## 12. 桌面端页面规划

第一阶段建议至少做两个桌面端页面：

### 12.1 告警规则页

建议功能：

- 规则列表
- 启停规则
- 模板快速创建
- 编辑规则阈值
- 配置通知人和通知渠道

建议菜单：

- 系统管理 / 告警规则

### 12.2 告警事件页

建议功能：

- 当前活动告警
- 已恢复告警
- 已忽略告警
- 告警详情
- 认领
- 忽略
- 标记处理
- 备注

建议菜单：

- 系统管理 / 告警中心

## 13. 移动端规划

移动端不建议承接复杂规则配置，建议只做轻处理：

- 查看当前告警
- 查看告警详情
- 确认收到
- 忽略
- 认领
- 备注

移动端更适合承接：

- 值班场景
- 推送跳转
- 即时确认

而不适合承接：

- 大量规则编辑
- 通知策略复杂编排

## 14. 后端接口建议

### 14.1 规则接口

- `GET /system/alert-rules`
- `POST /system/alert-rules`
- `PUT /system/alert-rules/{id}`
- `DELETE /system/alert-rules/{id}`
- `PUT /system/alert-rules/{id}/enabled`
- `GET /system/alert-rules/templates`

### 14.2 事件接口

- `GET /system/alert-events`
- `GET /system/alert-events/{id}`
- `PUT /system/alert-events/{id}/assign`
- `PUT /system/alert-events/{id}/ignore`
- `PUT /system/alert-events/{id}/handle`
- `PUT /system/alert-events/{id}/recover`

### 14.3 系统动作接口

- `POST /system/alert-engine/evaluate`
  - 手动触发一次告警评估
- `POST /system/alert-engine/clear-cache`
  - 如果后续有规则缓存，可预留此接口

## 15. 权限设计建议

建议权限点：

- `system:alert-rule:view`
- `system:alert-rule:add`
- `system:alert-rule:edit`
- `system:alert-rule:remove`
- `system:alert-event:view`
- `system:alert-event:handle`
- `system:alert-engine:run`

## 16. 数据库脚本建议

第一阶段建议新增：

- `sys_alert_rule`
- `sys_alert_event`
- 菜单与权限 SQL
- 管理员默认授权 SQL

不建议把告警规则继续塞进 `sys_config`，原因是：

- 查询不方便
- 结构不清晰
- 后续分页、筛选、状态管理都麻烦

## 17. 与现有模块的关系

### 17.1 与系统监控

关系：

- 系统监控提供评估输入
- 告警中心负责阈值判断和事件管理

### 17.2 与调度中心

关系：

- 调度中心既是告警来源之一
- 也是告警评估任务的运行载体

### 17.3 与通知中心

关系：

- 告警中心不重复造发送轮子
- 只负责生成通知内容和分发决策

## 18. 第一阶段实施顺序

建议按下面顺序推进：

1. 设计并落库 `sys_alert_rule`
2. 设计并落库 `sys_alert_event`
3. 实现内置模板枚举
4. 实现规则 CRUD
5. 实现告警评估器
6. 实现事件创建 / 更新 / 恢复逻辑
7. 接通知渠道
8. 实现桌面端规则页
9. 实现桌面端事件页
10. 最后接移动端轻处理页

## 19. 最小可用版本建议

如果要先做一个最小可用版，建议范围进一步收敛为：

- 3 个系统规则模板：
  - CPU
  - 内存
  - Redis/数据库可用性
- 1 个调度模板：
  - 失败任务数告警
- 2 个页面：
  - 告警规则
  - 告警事件
- 2 个通知渠道：
  - 站内信
  - WxPusher

这样成本可控，但已经可以跑出完整价值。

## 20. 风险与注意事项

### 20.1 告警风暴

如果不做静默和去重，通知量会很快失控。

### 20.2 恢复判定不准确

如果恢复逻辑不统一，会导致大量“假恢复”或“永不恢复”事件。

### 20.3 规则过度灵活

一开始就做复杂表达式会明显拉高开发和维护成本。

### 20.4 指标来源不稳定

第一阶段应尽量使用已有监控聚合结果，不要一开始就引入更多外部依赖。

## 21. 一句话结论

当前项目最适合的告警中心落地方式，不是直接做复杂监控平台，而是：

- 复用现有系统监控做输入
- 复用调度中心做规则评估
- 复用通知中心做消息分发
- 通过“内置模板 + 规则配置 + 事件中心”实现一键告警

这样既能尽快交付，也最符合你当前项目的架构基础。
