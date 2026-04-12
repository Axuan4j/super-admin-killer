SET NAMES utf8mb4;

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 13, '硬件监控', 2, 6, '/layout/system/monitor', '/system/monitor', 'C', '0', 'system:monitor:view', 'fa-solid fa-server', ''
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 13
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 13
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 13
);
