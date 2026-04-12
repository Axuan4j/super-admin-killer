-- 关联表补充索引，优化按单侧字段查询

ALTER TABLE `sys_user_role`
    ADD INDEX `idx_user_role_role_id` (`role_id`);

ALTER TABLE `sys_role_menu`
    ADD INDEX `idx_role_menu_menu_id` (`menu_id`);
