SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_login_log`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
    `username`       varchar(100)  DEFAULT NULL COMMENT '登录账号',
    `login_ip`       varchar(64)   DEFAULT NULL COMMENT '登录IP',
    `login_location` varchar(255)  DEFAULT NULL COMMENT '登录属地',
    `user_agent`     varchar(1000) DEFAULT NULL COMMENT '用户代理',
    `browser`        varchar(100)  DEFAULT NULL COMMENT '浏览器',
    `os`             varchar(100)  DEFAULT NULL COMMENT '操作系统',
    `status`         tinyint(1)    NOT NULL DEFAULT '1' COMMENT '登录状态（1成功 0失败）',
    `message`        varchar(500)  DEFAULT NULL COMMENT '提示消息',
    `login_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_log_username` (`username`),
    KEY `idx_login_log_ip` (`login_ip`),
    KEY `idx_login_log_login_time` (`login_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

INSERT INTO `sys_dict_item` (`dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 'sys_login_status', '成功', '1', 'success', 'green', 1, '0', '登录日志结果'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `dict_type` = 'sys_login_status' AND `dict_value` = '1'
);

INSERT INTO `sys_dict_item` (`dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 'sys_login_status', '失败', '0', 'danger', 'red', 2, '0', '登录日志结果'
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `dict_type` = 'sys_login_status' AND `dict_value` = '0'
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 11, '审计中心', 0, 3, '/layout/audit', NULL, 'M', '0', NULL, 'fa-solid fa-shield-halved', '审计中心目录'
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 11);

UPDATE `sys_menu`
SET `parent_id` = 11,
    `order_num` = 1,
    `path` = '/layout/audit/logs',
    `component` = '/system/logs',
    `icon` = 'fa-solid fa-file-lines'
WHERE `id` = 6;

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 12, '登录日志', 11, 2, '/layout/audit/login-logs', '/system/loginlogs', 'C', '0', 'system:login-log:view', 'fa-solid fa-right-to-bracket', ''
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `id` = 12);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, 11
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 6
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` existing
    WHERE existing.`role_id` = rm.`role_id` AND existing.`menu_id` = 11
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 12
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 12
);
