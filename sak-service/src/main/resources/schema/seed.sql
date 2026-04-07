-- 开发库初始化数据脚本
-- 用途：插入当前项目运行所需的基础演示数据
-- 注意：会清空当前表中的示例数据，仅适合开发环境

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `sys_role_menu`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_site_message`;
DELETE FROM `sys_oper_log`;
DELETE FROM `sys_dict_item`;
DELETE FROM `sys_config`;
DELETE FROM `sys_menu`;
DELETE FROM `sys_role`;
DELETE FROM `sys_user`;

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`) VALUES
(1, '首页', 0, 1, '/layout/dashboard', '/dashboard', 'C', '0', 'dashboard:view', 'fa-solid fa-chart-line', '首页'),
(2, '系统管理', 0, 6, '/layout/system', NULL, 'M', '0', NULL, 'fa-solid fa-users', '系统管理目录'),
(3, '用户管理', 2, 1, '/layout/system/users', '/system/users', 'C', '0', 'system:user:view', 'fa-solid fa-user', ''),
(4, '角色管理', 2, 2, '/layout/system/roles', '/system/roles', 'C', '0', 'system:role:view', 'fa-solid fa-user-shield', ''),
(5, '权限管理', 2, 3, '/layout/system/permissions', '/system/permissions', 'C', '0', 'system:permission:view', 'fa-solid fa-key', ''),
(9, '通知管理', 2, 4, '/layout/system/notifications', '/system/notifications', 'C', '0', 'system:notification:view', 'fa-solid fa-bullhorn', ''),
(6, '操作日志', 0, 3, '/layout/logs', '/system/logs', 'C', '0', 'system:log:view', 'fa-solid fa-file-lines', ''),
(7, '在线用户', 0, 4, '/layout/online', '/online', 'C', '0', 'system:online:view', 'fa-solid fa-user-lock', ''),
(8, '个人中心', 0, 5, '/layout/profile', '/account/profile', 'C', '0', 'system:profile:view', 'fa-solid fa-user', ''),
(101, '用户新增', 3, 1, '', '', 'F', '0', 'system:user:add', '#', ''),
(102, '用户编辑', 3, 2, '', '', 'F', '0', 'system:user:edit', '#', ''),
(103, '用户删除', 3, 3, '', '', 'F', '0', 'system:user:remove', '#', ''),
(201, '角色新增', 4, 1, '', '', 'F', '0', 'system:role:add', '#', ''),
(202, '角色编辑', 4, 2, '', '', 'F', '0', 'system:role:edit', '#', ''),
(203, '角色删除', 4, 3, '', '', 'F', '0', 'system:role:remove', '#', ''),
(301, '权限新增', 5, 1, '', '', 'F', '0', 'system:permission:add', '#', ''),
(302, '权限编辑', 5, 2, '', '', 'F', '0', 'system:permission:edit', '#', ''),
(303, '权限删除', 5, 3, '', '', 'F', '0', 'system:permission:remove', '#', ''),
(401, '在线用户强退', 7, 1, '', '', 'F', '0', 'system:online:forceLogout', '#', ''),
(501, '发送通知', 9, 1, '', '', 'F', '0', 'system:notification:send', '#', '');

INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `role_sort`, `status`, `remark`) VALUES
(1, '超级管理员', 'admin', 1, '0', '默认管理员角色'),
(2, '普通用户', 'user', 2, '0', '默认普通用户角色');

-- BCrypt("123456")
INSERT INTO `sys_user` (`id`, `username`, `password`, `nick_name`, `email`, `phone`, `sex`, `avatar`, `status`, `remark`) VALUES
(1, 'admin', '$2a$10$p4DUvR06FLq7G1yFqqqG4.C8DRUmqRH2.SxIdXQjbMzRO4Ueledbu', '管理员', 'admin@example.com', '15888888888', '0', '', '0', '管理员'),
(2, 'user', '$2a$10$p4DUvR06FLq7G1yFqqqG4.C8DRUmqRH2.SxIdXQjbMzRO4Ueledbu', '普通用户', 'user@example.com', '15666666666', '0', '', '0', '普通用户');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9),
(1, 101), (1, 102), (1, 103),
(1, 201), (1, 202), (1, 203),
(1, 301), (1, 302), (1, 303),
(1, 401), (1, 501),
(2, 1), (2, 8);

INSERT INTO `sys_config` (`id`, `config_key`, `config_value`, `config_type`, `remark`) VALUES
(1, 'site.name', 'SuperKiller Admin', 'string', '站点名称'),
(2, 'site.notice', '欢迎使用后台管理系统', 'string', '站点公告');

INSERT INTO `sys_dict_item`
(`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`) VALUES
(1, 'sys_normal_disable', '启用', '0', 'success', 'green', 1, '0', '通用状态'),
(2, 'sys_normal_disable', '禁用', '1', 'danger', 'red', 2, '0', '通用状态'),
(3, 'sys_oper_success', '成功', '1', 'success', 'green', 1, '0', '操作日志结果'),
(4, 'sys_oper_success', '失败', '0', 'danger', 'red', 2, '0', '操作日志结果'),
(5, 'sys_user_sex', '男', '0', 'arcoblue', 'arcoblue', 1, '0', '用户性别'),
(6, 'sys_user_sex', '女', '1', 'pinkpurple', 'pinkpurple', 2, '0', '用户性别'),
(7, 'sys_user_sex', '未知', '2', 'gray', 'gray', 3, '0', '用户性别');

INSERT INTO `sys_site_message` (`id`, `user_id`, `title`, `content`, `sender_name`, `read_status`, `read_time`, `create_time`, `update_time`) VALUES
(1, 1, '系统升级通知', '后台管理系统将在今晚 23:00 进行例行维护，请提前保存操作内容。', '系统', 0, NULL, NOW(), NOW()),
(2, 1, '权限变更提醒', '您收到一条新的角色权限调整，请进入权限管理页面核对配置。', '系统', 1, NOW(), NOW(), NOW()),
(3, 2, '欢迎使用', '欢迎进入 SuperKiller Admin，您可以在这里查看自己的站内消息。', '系统', 0, NULL, NOW(), NOW());

INSERT INTO `sys_oper_log` (`id`, `biz_no`, `log_type`, `sub_type`, `operator`, `action`, `extra`, `ip`, `method`, `request_url`, `request_method`, `execution_time`, `success`, `err_msg`, `create_time`) VALUES
(1, '1', 'USER', 'CREATE', 'admin', '新增用户：admin', '初始化示例日志', '127.0.0.1', 'USER', '/system/users', 'POST', 36, 1, NULL, NOW()),
(2, '1', 'ROLE', 'GRANT', 'admin', '更新角色权限：1', '初始化示例日志', '127.0.0.1', 'ROLE', '/system/roles/1/menu-ids', 'PUT', 42, 1, NULL, NOW());

SET FOREIGN_KEY_CHECKS = 1;
