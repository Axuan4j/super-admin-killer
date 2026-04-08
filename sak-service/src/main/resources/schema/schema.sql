-- 开发库建表脚本
-- 用途：重建项目当前所需的表结构
-- 注意：会 DROP 并重建表，只适合开发环境

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_export_record`;
DROP TABLE IF EXISTS `sys_file_record`;
DROP TABLE IF EXISTS `sys_site_message`;
DROP TABLE IF EXISTS `sys_login_log`;
DROP TABLE IF EXISTS `sys_oper_log`;
DROP TABLE IF EXISTS `sys_dict_item`;
DROP TABLE IF EXISTS `sys_config`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;

CREATE TABLE `sys_menu`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   varchar(50) NOT NULL COMMENT '菜单名称',
    `parent_id`   bigint       DEFAULT 0 COMMENT '父菜单ID',
    `order_num`   int          DEFAULT 0 COMMENT '显示顺序',
    `path`        varchar(200) DEFAULT '' COMMENT '路由地址',
    `component`   varchar(255) DEFAULT NULL COMMENT '组件路径',
    `menu_type`   char(1)      DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮)',
    `visible`     char(1)      DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
    `perms`       varchar(100) DEFAULT NULL COMMENT '权限标识',
    `icon`        varchar(100) DEFAULT '#' COMMENT '菜单图标',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜单权限表';

CREATE TABLE `sys_role`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   varchar(30)  NOT NULL COMMENT '角色名称',
    `role_key`    varchar(100) NOT NULL COMMENT '角色权限字符串',
    `role_sort`   int          NOT NULL COMMENT '显示顺序',
    `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '角色状态（0正常 1停用）',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

CREATE TABLE `sys_user`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    varchar(30) NOT NULL COMMENT '用户账号',
    `password`    varchar(100)         DEFAULT '' COMMENT '密码',
    `nick_name`   varchar(30)          DEFAULT '' COMMENT '用户昵称',
    `email`       varchar(50)          DEFAULT '' COMMENT '用户邮箱',
    `wx_pusher_uid` varchar(100)       DEFAULT '' COMMENT 'WxPusher UID',
    `phone`       varchar(11)          DEFAULT '' COMMENT '手机号码',
    `sex`         char(1)              DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
    `avatar`      varchar(100)         DEFAULT '' COMMENT '头像地址',
    `status`      char(1)     NOT NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
    `create_time` datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `remark`      varchar(500)         DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE `sys_user_role`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`),
    KEY `idx_user_role_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户和角色关联表';

CREATE TABLE `sys_role_menu`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `menu_id` bigint NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`),
    KEY `idx_role_menu_menu_id` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色和菜单关联表';

CREATE TABLE `sys_config`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`   varchar(100) NOT NULL COMMENT '参数键',
    `config_value` varchar(500) NOT NULL COMMENT '参数值',
    `config_type`  varchar(20)  DEFAULT 'string' COMMENT '参数类型',
    `remark`       varchar(200) DEFAULT NULL COMMENT '备注',
    `create_time`  datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统参数表';

CREATE TABLE `sys_dict_item`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
    `dict_type`   varchar(100) NOT NULL COMMENT '字典类型',
    `dict_label`  varchar(100) NOT NULL COMMENT '字典标签',
    `dict_value`  varchar(100) NOT NULL COMMENT '字典键值',
    `tag_type`    varchar(30)           DEFAULT NULL COMMENT '标签类型',
    `tag_color`   varchar(30)           DEFAULT NULL COMMENT '标签颜色',
    `order_num`   int                   DEFAULT 0 COMMENT '显示顺序',
    `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `remark`      varchar(500)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dict_type` (`dict_type`),
    KEY `idx_dict_type_status` (`dict_type`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统字典表';

CREATE TABLE `sys_site_message`
(
    `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '站内消息ID',
    `user_id`     bigint        NOT NULL COMMENT '接收用户ID',
    `title`       varchar(120)  NOT NULL COMMENT '消息标题',
    `content`     varchar(1000) NOT NULL COMMENT '消息内容',
    `sender_name` varchar(50)            DEFAULT '系统' COMMENT '发送人',
    `read_status` tinyint(1)    NOT NULL DEFAULT '0' COMMENT '读取状态（0未读 1已读）',
    `read_time`   datetime               DEFAULT NULL COMMENT '读取时间',
    `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_site_message_user_id` (`user_id`),
    KEY `idx_site_message_read_status` (`read_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内消息表';

CREATE TABLE `sys_export_record`
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
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='导出记录表';

CREATE TABLE `sys_file_record`
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

CREATE TABLE `sys_login_log`
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

CREATE TABLE `sys_oper_log`
(
    `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `biz_no`         varchar(100)           DEFAULT NULL COMMENT '业务标识',
    `log_type`       varchar(50)            DEFAULT NULL COMMENT '日志类型',
    `sub_type`       varchar(50)            DEFAULT NULL COMMENT '日志子类型',
    `operator`       varchar(100)           DEFAULT NULL COMMENT '操作人',
    `action`         varchar(1000)          DEFAULT NULL COMMENT '操作描述',
    `extra`          varchar(2000)          DEFAULT NULL COMMENT '附加信息',
    `ip`             varchar(64)            DEFAULT NULL COMMENT 'IP地址',
    `method`         varchar(200)           DEFAULT NULL COMMENT '方法标识',
    `request_url`    varchar(500)           DEFAULT NULL COMMENT '请求地址',
    `request_method` varchar(20)            DEFAULT NULL COMMENT '请求方式',
    `execution_time` int                    DEFAULT NULL COMMENT '执行耗时',
    `success`        tinyint(1)             DEFAULT '1' COMMENT '是否成功（1成功 0失败）',
    `err_msg`        varchar(1000)          DEFAULT NULL COMMENT '错误信息',
    `create_time`    datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_oper_log_type` (`log_type`),
    KEY `idx_oper_log_operator` (`operator`),
    KEY `idx_oper_log_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
