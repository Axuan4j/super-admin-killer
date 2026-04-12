-- 消息发送记录中心升级脚本
-- 用途：给现有数据库补充通知发送记录表

CREATE TABLE IF NOT EXISTS `sys_notification_record`
(
    `id`                          bigint        NOT NULL AUTO_INCREMENT COMMENT '通知记录ID',
    `sender_name`                 varchar(100)  NOT NULL COMMENT '发送人',
    `title`                       varchar(120)  NOT NULL COMMENT '通知标题',
    `content`                     varchar(2000) NOT NULL COMMENT '通知内容',
    `channels_json`               varchar(500)  NOT NULL COMMENT '发送渠道JSON',
    `send_all`                    tinyint(1)    NOT NULL DEFAULT '0' COMMENT '是否发送全部用户',
    `recipient_count`             int           NOT NULL DEFAULT 0 COMMENT '目标人数',
    `success_user_count`          int           NOT NULL DEFAULT 0 COMMENT '成功触达人数',
    `status`                      varchar(20)   NOT NULL DEFAULT 'SUCCESS' COMMENT '发送状态',
    `channel_success_counts_json` varchar(1000) DEFAULT NULL COMMENT '渠道成功数JSON',
    `channel_skip_counts_json`    varchar(1000) DEFAULT NULL COMMENT '渠道跳过数JSON',
    `recipient_details_json`      text COMMENT '接收人详情JSON',
    `create_time`                 datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`                 datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_notification_record_status` (`status`),
    KEY `idx_notification_record_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='通知发送记录表';
