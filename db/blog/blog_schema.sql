-- 个人博客业务独立建表脚本
-- 说明：
-- 1. 这份脚本只包含博客业务表，不修改脚手架基础表
-- 2. 建议在独立数据库或现有业务库中按需执行
-- 3. 当前设计面向“个人博客网站 + 后台内容管理 + 前台纯展示”

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 文章分类
CREATE TABLE IF NOT EXISTS `blog_category`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `category_name` varchar(64)  NOT NULL COMMENT '分类名称',
    `slug`          varchar(128) NOT NULL COMMENT '分类别名，用于URL',
    `description`   varchar(255)          DEFAULT NULL COMMENT '分类描述',
    `cover_image`   varchar(255)          DEFAULT NULL COMMENT '分类封面图',
    `order_num`     int                   DEFAULT 0 COMMENT '排序',
    `status`        char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `remark`        varchar(255)          DEFAULT NULL COMMENT '备注',
    `create_time`   datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_category_slug` (`slug`),
    KEY `idx_blog_category_status_order` (`status`, `order_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客分类表';

-- 文章标签
CREATE TABLE IF NOT EXISTS `blog_tag`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `tag_name`    varchar(64)  NOT NULL COMMENT '标签名称',
    `slug`        varchar(128) NOT NULL COMMENT '标签别名，用于URL',
    `color`       varchar(32)           DEFAULT NULL COMMENT '标签颜色',
    `description` varchar(255)          DEFAULT NULL COMMENT '标签描述',
    `order_num`   int                   DEFAULT 0 COMMENT '排序',
    `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `remark`      varchar(255)          DEFAULT NULL COMMENT '备注',
    `create_time` datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_tag_slug` (`slug`),
    UNIQUE KEY `uk_blog_tag_name` (`tag_name`),
    KEY `idx_blog_tag_status_order` (`status`, `order_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客标签表';

-- 博客文章主表
CREATE TABLE IF NOT EXISTS `blog_post`
(
    `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `title`               varchar(200)  NOT NULL COMMENT '文章标题',
    `slug`                varchar(220)  NOT NULL COMMENT '文章别名，用于URL',
    `summary`             varchar(1000)          DEFAULT NULL COMMENT '文章摘要',
    `cover_image`         varchar(255)           DEFAULT NULL COMMENT '封面图片',
    `category_id`         bigint                 DEFAULT NULL COMMENT '分类ID',
    `author_user_id`      bigint                 DEFAULT NULL COMMENT '作者后台用户ID，关联sys_user.id',
    `content_markdown`    longtext      NOT NULL COMMENT 'Markdown正文',
    `content_html`        longtext               DEFAULT NULL COMMENT '渲染后的HTML正文',
    `seo_title`           varchar(255)           DEFAULT NULL COMMENT 'SEO标题',
    `seo_keywords`        varchar(255)           DEFAULT NULL COMMENT 'SEO关键词',
    `seo_description`     varchar(500)           DEFAULT NULL COMMENT 'SEO描述',
    `source_type`         varchar(20)   NOT NULL DEFAULT 'ORIGINAL' COMMENT '文章来源（ORIGINAL/REPOST/TRANSLATION）',
    `source_url`          varchar(500)           DEFAULT NULL COMMENT '转载原文地址',
    `allow_comment`       tinyint(1)    NOT NULL DEFAULT 1 COMMENT '是否允许评论',
    `is_top`              tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否置顶',
    `is_recommend`        tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否推荐',
    `status`              varchar(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT/PUBLISHED/OFFLINE）',
    `publish_time`        datetime               DEFAULT NULL COMMENT '发布时间',
    `last_comment_time`   datetime               DEFAULT NULL COMMENT '最后评论时间',
    `word_count`          int                    DEFAULT 0 COMMENT '字数',
    `reading_minutes`     int                    DEFAULT 0 COMMENT '预计阅读分钟数',
    `version_no`          int           NOT NULL DEFAULT 1 COMMENT '版本号',
    `create_time`         datetime               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         datetime               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_post_slug` (`slug`),
    KEY `idx_blog_post_status_publish` (`status`, `publish_time`),
    KEY `idx_blog_post_category` (`category_id`),
    KEY `idx_blog_post_top_recommend` (`is_top`, `is_recommend`, `publish_time`),
    KEY `idx_blog_post_author` (`author_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客文章表';

-- 文章与标签关联
CREATE TABLE IF NOT EXISTS `blog_post_tag`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `post_id`     bigint   NOT NULL COMMENT '文章ID',
    `tag_id`      bigint   NOT NULL COMMENT '标签ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_post_tag` (`post_id`, `tag_id`),
    KEY `idx_blog_post_tag_tag_id` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章标签关联表';

-- 文章统计
CREATE TABLE IF NOT EXISTS `blog_post_stat`
(
    `post_id`            bigint     NOT NULL COMMENT '文章ID',
    `view_count`         bigint     NOT NULL DEFAULT 0 COMMENT '浏览数',
    `unique_view_count`  bigint     NOT NULL DEFAULT 0 COMMENT '独立访客浏览数',
    `comment_count`      bigint     NOT NULL DEFAULT 0 COMMENT '评论数',
    `like_count`         bigint     NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favorite_count`     bigint     NOT NULL DEFAULT 0 COMMENT '收藏数',
    `share_count`        bigint     NOT NULL DEFAULT 0 COMMENT '分享数',
    `last_view_time`     datetime            DEFAULT NULL COMMENT '最后浏览时间',
    `create_time`        datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`post_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='文章统计表';

-- 评论表
CREATE TABLE IF NOT EXISTS `blog_comment`
(
    `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`               bigint        NOT NULL COMMENT '文章ID',
    `root_comment_id`       bigint        NOT NULL DEFAULT 0 COMMENT '根评论ID，一级评论为0，二级评论指向所属一级评论',
    `reply_comment_id`      bigint                 DEFAULT 0 COMMENT '回复目标评论ID，仅二级评论使用',
    `reply_to_name`         varchar(64)            DEFAULT NULL COMMENT '被回复人昵称，冗余存储便于展示',
    `visitor_name`          varchar(64)   NOT NULL COMMENT '访客昵称',
    `visitor_email`         varchar(128)           DEFAULT NULL COMMENT '访客邮箱',
    `visitor_website`       varchar(255)           DEFAULT NULL COMMENT '访客网站',
    `visitor_ip`            varchar(64)            DEFAULT NULL COMMENT '访客IP',
    `visitor_location`      varchar(128)           DEFAULT NULL COMMENT '访客归属地',
    `content`               varchar(2000) NOT NULL COMMENT '评论内容',
    `status`                varchar(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING/APPROVED/REJECTED/SPAM）',
    `comment_level`         tinyint       NOT NULL DEFAULT 1 COMMENT '评论层级（1一级评论 2二级评论）',
    `children_count`        int           NOT NULL DEFAULT 0 COMMENT '二级回复数量，仅一级评论使用',
    `is_admin_reply`        tinyint(1)    NOT NULL DEFAULT 0 COMMENT '是否博主回复',
    `like_count`            int           NOT NULL DEFAULT 0 COMMENT '点赞数',
    `create_time`           datetime               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`           datetime               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_comment_post_status` (`post_id`, `status`, `create_time`),
    KEY `idx_blog_comment_root` (`root_comment_id`, `status`, `create_time`),
    KEY `idx_blog_comment_reply` (`reply_comment_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客评论表（仅支持二级评论）';

-- 站点配置，按key-value存放，便于后台统一管理
CREATE TABLE IF NOT EXISTS `blog_site_config`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`   varchar(100) NOT NULL COMMENT '配置键',
    `config_value` text                  DEFAULT NULL COMMENT '配置值',
    `config_group` varchar(50)           DEFAULT 'site' COMMENT '配置分组',
    `config_type`  varchar(30)           DEFAULT 'string' COMMENT '配置类型',
    `remark`       varchar(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_site_config_key` (`config_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客站点配置表';

-- 访问日志，用于统计来源、PV、UV 等
CREATE TABLE IF NOT EXISTS `blog_visit_log`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
    `post_id`        bigint                DEFAULT NULL COMMENT '文章ID，首页等公共页可为空',
    `page_type`      varchar(30)  NOT NULL DEFAULT 'POST' COMMENT '页面类型（HOME/POST/CATEGORY/TAG/ABOUT/ARCHIVE）',
    `page_path`      varchar(255) NOT NULL COMMENT '访问路径',
    `visitor_ip`     varchar(64)           DEFAULT NULL COMMENT '访客IP',
    `visitor_ua`     varchar(500)          DEFAULT NULL COMMENT '访客UA',
    `referrer`       varchar(500)          DEFAULT NULL COMMENT '来源页',
    `device_type`    varchar(30)           DEFAULT NULL COMMENT '设备类型',
    `os_name`        varchar(64)           DEFAULT NULL COMMENT '操作系统',
    `browser_name`   varchar(64)           DEFAULT NULL COMMENT '浏览器',
    `session_id`     varchar(100)          DEFAULT NULL COMMENT '会话ID',
    `visitor_hash`   varchar(128)          DEFAULT NULL COMMENT '访客唯一摘要，用于UV统计',
    `stay_seconds`   int                   DEFAULT 0 COMMENT '停留时长（秒）',
    `create_time`    datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_visit_post_time` (`post_id`, `create_time`),
    KEY `idx_blog_visit_page_type_time` (`page_type`, `create_time`),
    KEY `idx_blog_visit_visitor_hash` (`visitor_hash`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客访问日志表';

-- 可选：友情链接
CREATE TABLE IF NOT EXISTS `blog_friend_link`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '友链ID',
    `site_name`    varchar(100) NOT NULL COMMENT '站点名称',
    `site_url`     varchar(255) NOT NULL COMMENT '站点地址',
    `site_avatar`  varchar(255)          DEFAULT NULL COMMENT '站点头像',
    `description`  varchar(255)          DEFAULT NULL COMMENT '站点描述',
    `order_num`    int                   DEFAULT 0 COMMENT '排序',
    `status`       char(1)      NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    `create_time`  datetime              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_blog_friend_link_status_order` (`status`, `order_num`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='博客友情链接表';

SET FOREIGN_KEY_CHECKS = 1;
