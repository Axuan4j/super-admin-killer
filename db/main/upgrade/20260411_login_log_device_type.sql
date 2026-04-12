ALTER TABLE `sys_login_log`
    ADD COLUMN `device_type` varchar(32) DEFAULT NULL COMMENT '设备类型' AFTER `os`;

INSERT INTO `sys_dict_item` (`dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 'sys_device_type', '网页端', 'web', 'arcoblue', 'arcoblue', 1, '0', '设备类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `dict_type` = 'sys_device_type' AND `dict_value` = 'web'
);

INSERT INTO `sys_dict_item` (`dict_type`, `dict_label`, `dict_value`, `tag_type`, `tag_color`, `order_num`, `status`, `remark`)
SELECT 'sys_device_type', '移动端', 'mobile', 'green', 'green', 2, '0', '设备类型'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dict_item` WHERE `dict_type` = 'sys_device_type' AND `dict_value` = 'mobile'
);
