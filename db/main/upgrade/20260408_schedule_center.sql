SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_scheduled_task`
(
    `id`                   bigint        NOT NULL AUTO_INCREMENT COMMENT '调度任务ID',
    `task_name`            varchar(120)  NOT NULL COMMENT '任务名称',
    `task_type`            varchar(100)  NOT NULL COMMENT '任务类型',
    `execution_type`       varchar(30)   NOT NULL DEFAULT 'CRON' COMMENT '执行方式',
    `interval_value`       int           DEFAULT NULL COMMENT '间隔数值',
    `interval_unit`        varchar(30)   DEFAULT NULL COMMENT '间隔单位',
    `cron_expression`      varchar(120)  DEFAULT NULL COMMENT 'Cron表达式',
    `status`               varchar(30)   NOT NULL DEFAULT 'PAUSED' COMMENT '任务状态',
    `task_summary`         varchar(500)  DEFAULT NULL COMMENT '任务摘要',
    `task_config_json`     varchar(4000) NOT NULL COMMENT '任务配置JSON',
    `success_notify`       tinyint(1)    NOT NULL DEFAULT '0' COMMENT '成功通知',
    `failure_notify`       tinyint(1)    NOT NULL DEFAULT '1' COMMENT '失败通知',
    `notify_channels_json` varchar(1000) DEFAULT NULL COMMENT '通知渠道JSON',
    `notify_user_ids_json` varchar(2000) DEFAULT NULL COMMENT '通知用户JSON',
    `run_count`            bigint        NOT NULL DEFAULT 0 COMMENT '执行次数',
    `operator`             varchar(100)  DEFAULT NULL COMMENT '维护人',
    `remark`               varchar(500)  DEFAULT NULL COMMENT '备注',
    `last_run_time`        datetime      DEFAULT NULL COMMENT '最近执行时间',
    `next_run_time`        datetime      DEFAULT NULL COMMENT '下次执行时间',
    `last_success_time`    datetime      DEFAULT NULL COMMENT '最近成功时间',
    `last_failure_time`    datetime      DEFAULT NULL COMMENT '最近失败时间',
    `last_error_message`   varchar(1000) DEFAULT NULL COMMENT '最近错误信息',
    `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_scheduled_task_execution_type` (`execution_type`),
    KEY `idx_scheduled_task_status` (`status`),
    KEY `idx_scheduled_task_type` (`task_type`),
    KEY `idx_scheduled_task_next_run_time` (`next_run_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='调度任务表';

ALTER TABLE `sys_scheduled_task` ADD COLUMN `execution_type` varchar(30) NOT NULL DEFAULT 'CRON' COMMENT '执行方式' AFTER `task_type`;
ALTER TABLE `sys_scheduled_task` ADD COLUMN `interval_value` int DEFAULT NULL COMMENT '间隔数值' AFTER `execution_type`;
ALTER TABLE `sys_scheduled_task` ADD COLUMN `interval_unit` varchar(30) DEFAULT NULL COMMENT '间隔单位' AFTER `interval_value`;
ALTER TABLE `sys_scheduled_task` MODIFY COLUMN `cron_expression` varchar(120) DEFAULT NULL COMMENT 'Cron表达式';

UPDATE `sys_scheduled_task`
SET `execution_type` = 'CRON'
WHERE `execution_type` IS NULL OR `execution_type` = '';

CREATE TABLE IF NOT EXISTS `sys_scheduled_task_type`
(
    `id`               bigint        NOT NULL AUTO_INCREMENT COMMENT '任务类型ID',
    `task_type`        varchar(100)  NOT NULL COMMENT '任务类型编码',
    `task_name`        varchar(120)  NOT NULL COMMENT '任务类型名称',
    `description`      varchar(500)  DEFAULT NULL COMMENT '类型描述',
    `form_schema_json` varchar(8000) DEFAULT NULL COMMENT '动态表单定义JSON',
    `built_in`         tinyint(1)    NOT NULL DEFAULT '1' COMMENT '是否内置',
    `enabled`          tinyint(1)    NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scheduled_task_type` (`task_type`),
    KEY `idx_scheduled_task_type_enabled` (`enabled`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='调度任务类型表';

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 15, '调度中心', 2, 8, '/layout/system/schedules', '/system/schedules', 'C', '0', 'system:schedule:view', 'fa-solid fa-clock', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 15
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 801, '任务新增', 15, 1, '', '', 'F', '0', 'system:schedule:add', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 801
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 802, '任务编辑', 15, 2, '', '', 'F', '0', 'system:schedule:edit', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 802
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 803, '任务删除', 15, 3, '', '', 'F', '0', 'system:schedule:remove', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 803
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 804, '任务执行', 15, 4, '', '', 'F', '0', 'system:schedule:run', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 804
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 15
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 15
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 801
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 801
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 802
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 802
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 803
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 803
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 804
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 804
);