INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 16, '字典配置', 2, 9, '/layout/system/dicts', '/system/dicts', 'C', '0', 'system:dict:view', 'fa-solid fa-book-bookmark', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 16
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1601, '字典新增', 16, 1, '', '', 'F', '0', 'system:dict:add', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1601
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1602, '字典编辑', 16, 2, '', '', 'F', '0', 'system:dict:edit', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1602
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1603, '字典删除', 16, 3, '', '', 'F', '0', 'system:dict:remove', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1603
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 16
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 16
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1601
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1601
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1602
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1602
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1603
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1603
);
