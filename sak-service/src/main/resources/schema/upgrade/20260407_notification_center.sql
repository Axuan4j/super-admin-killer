-- 消息推送中心升级脚本
-- 用途：给现有数据库补充 WxPusher 用户标识字段

ALTER TABLE `sys_user`
    ADD COLUMN `wx_pusher_uid` varchar(100) DEFAULT '' COMMENT 'WxPusher UID' AFTER `email`;
