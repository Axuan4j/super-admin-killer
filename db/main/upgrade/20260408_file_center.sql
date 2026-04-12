SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sys_file_record`
(
    `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '文件记录ID',
    `biz_type`     varchar(100)  NOT NULL DEFAULT 'COMMON' COMMENT '业务类型',
    `file_name`    varchar(255)  NOT NULL COMMENT '原始文件名',
    `storage_name` varchar(255)  NOT NULL COMMENT '存储文件名',
    `file_path`    varchar(500)  NOT NULL COMMENT '文件相对路径',
    `content_type` varchar(200)  DEFAULT NULL COMMENT '文件类型',
    `file_ext`     varchar(50)   DEFAULT NULL COMMENT '文件扩展名',
    `file_size`    bigint        NOT NULL DEFAULT 0 COMMENT '文件大小',
    `operator`     varchar(100)  DEFAULT NULL COMMENT '上传人',
    `remark`       varchar(500)  DEFAULT NULL COMMENT '备注',
    `create_time`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_record_biz_type` (`biz_type`),
    KEY `idx_file_record_operator` (`operator`),
    KEY `idx_file_record_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文件中心记录表';

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 14, '文件中心', 2, 7, '/layout/system/files', '/system/files', 'C', '0', 'system:file:view', 'fa-solid fa-folder-tree', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 14
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 701, '文件上传', 14, 1, '', '', 'F', '0', 'system:file:upload', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 701
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 702, '文件删除', 14, 2, '', '', 'F', '0', 'system:file:remove', '#', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 702
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 14
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 14
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 701
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 701
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 702
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 702
);
