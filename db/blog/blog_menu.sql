INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 17, '博客管理', 0, 2, '/layout/blog', NULL, 'M', '0', NULL, 'fa-solid fa-pen-nib', '博客管理目录'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 17
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1701, '文章管理', 17, 1, '/layout/blog/posts', '/blog/posts', 'C', '0', 'blog:post:view', 'fa-solid fa-newspaper', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1701
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1702, '发布文章', 17, 2, '/layout/blog/editor', '/blog/editor', 'C', '0', 'blog:post:view', 'fa-solid fa-pen-to-square', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1702
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170101, '文章新增', 1701, 1, '', '', 'F', '0', 'blog:post:add', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170101
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170102, '文章编辑', 1701, 2, '', '', 'F', '0', 'blog:post:edit', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170102
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170103, '文章删除', 1701, 3, '', '', 'F', '0', 'blog:post:remove', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170103
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170104, '文章发布', 1701, 4, '', '', 'F', '0', 'blog:post:publish', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170104
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 17
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 17
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1701
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1701
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1702
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1702
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170101
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170101
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170102
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170102
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170103
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170103
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170104
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170104
);

-- 分类管理
INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 1703, '分类管理', 17, 3, '/layout/blog/categories', '/blog/categories', 'C', '0', 'blog:category:view', 'fa-solid fa-folder-open', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 1703
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170301, '分类新增', 1703, 1, '', '', 'F', '0', 'blog:category:add', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170301
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170302, '分类编辑', 1703, 2, '', '', 'F', '0', 'blog:category:edit', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170302
);

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `perms`, `icon`, `remark`)
SELECT 170303, '分类删除', 1703, 3, '', '', 'F', '0', 'blog:category:remove', '#', ''
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_menu` WHERE `id` = 170303
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 1703
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 1703
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170301
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170301
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170302
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170302
);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 170303
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 170303
);
