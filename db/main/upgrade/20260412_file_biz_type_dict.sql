INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 12, 'sys_file_biz_type', '通用文件', 'COMMON', 'arcoblue', 'arcoblue', 1, '0', '文件中心业务类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `id` = 12
);

INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 13, 'sys_file_biz_type', '通知附件', 'NOTICE', 'orange', 'orange', 2, '0', '文件中心业务类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `id` = 13
);

INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 14, 'sys_file_biz_type', '系统资源', 'RESOURCE', 'cyan', 'cyan', 3, '0', '文件中心业务类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `id` = 14
);

INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 15, 'sys_file_biz_type', '博客封面图', 'BLOG_POST_COVER', 'green', 'green', 4, '0', '文件中心业务类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `id` = 15
);

INSERT INTO `sys_dict_item` (`id`, `dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 16, 'sys_file_biz_type', '博客正文图', 'BLOG_POST_IMAGE', 'purple', 'purple', 5, '0', '文件中心业务类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `id` = 16
);
