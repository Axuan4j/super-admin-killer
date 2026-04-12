-- 下载中心与异步导出升级脚本

CREATE TABLE IF NOT EXISTS `sys_export_record`
(
    `id`              bigint        NOT NULL AUTO_INCREMENT COMMENT '导出记录ID',
    `biz_type`        varchar(100)  NOT NULL COMMENT '业务类型',
    `file_name`       varchar(255)  NOT NULL COMMENT '文件名',
    `query_condition` varchar(2000) DEFAULT NULL COMMENT '查询条件',
    `status`          varchar(30)   NOT NULL DEFAULT 'PENDING' COMMENT '导出状态',
    `file_path`       varchar(500)  DEFAULT NULL COMMENT '文件相对路径',
    `file_size`       bigint        DEFAULT NULL COMMENT '文件大小',
    `total_count`     bigint        DEFAULT NULL COMMENT '导出条数',
    `operator`        varchar(100)  DEFAULT NULL COMMENT '操作人',
    `err_msg`         varchar(500)  DEFAULT NULL COMMENT '错误信息',
    `finish_time`     datetime      DEFAULT NULL COMMENT '完成时间',
    `create_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_export_record_biz_type` (`biz_type`),
    KEY `idx_export_record_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='导出记录表';

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
VALUES (10, '下载中心', 2, 5, '/layout/system/downloads', '/system/downloads', 'C', '0', 'system:export:view', 'fa-solid fa-download', '下载中心')
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `order_num` = VALUES(`order_num`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `menu_type` = VALUES(`menu_type`),
    `visible` = VALUES(`visible`),
    `perms` = VALUES(`perms`),
    `icon` = VALUES(`icon`),
    `remark` = VALUES(`remark`);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
VALUES (601, '操作日志导出', 6, 1, '', '', 'F', '0', 'system:log:export', '#', '操作日志导出按钮')
ON DUPLICATE KEY UPDATE
    `menu_name` = VALUES(`menu_name`),
    `parent_id` = VALUES(`parent_id`),
    `order_num` = VALUES(`order_num`),
    `path` = VALUES(`path`),
    `component` = VALUES(`component`),
    `menu_type` = VALUES(`menu_type`),
    `visible` = VALUES(`visible`),
    `perms` = VALUES(`perms`),
    `icon` = VALUES(`icon`),
    `remark` = VALUES(`remark`);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 10), (1, 601);
