-- 文件中心添加文件指纹字段，用于去重
ALTER TABLE `sys_file_record` ADD COLUMN `file_hash` varchar(64) DEFAULT NULL COMMENT '文件SHA256指纹' AFTER `file_size`;

-- 为 file_hash 添加索引，加速查询
ALTER TABLE `sys_file_record` ADD INDEX `idx_sys_file_record_hash` (`file_hash`);
