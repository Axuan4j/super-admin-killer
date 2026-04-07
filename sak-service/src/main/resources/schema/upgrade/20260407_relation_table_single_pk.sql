-- 关联表改造：联合主键改为自增ID主键 + 唯一索引

ALTER TABLE `sys_user_role`
    DROP PRIMARY KEY,
    ADD COLUMN `id` bigint NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `uk_user_role_user_id_role_id` (`user_id`, `role_id`);

ALTER TABLE `sys_role_menu`
    DROP PRIMARY KEY,
    ADD COLUMN `id` bigint NOT NULL AUTO_INCREMENT FIRST,
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `uk_role_menu_role_id_menu_id` (`role_id`, `menu_id`);
