# Super Admin Killer

一个前后端分离的后台管理项目，包含 Spring Boot 后端服务和 Vue 3 前端页面。当前仓库已经整理为 Maven 多模块结构，支持通过 Maven 统一驱动后端打包与前端构建，同时内置了一套偏“脚手架平台化”的后台基础能力。

## 项目结构

```text
superkiller-admin/
├── pom.xml                # Maven 父工程
├── README.md              # 项目说明
├── docs/                  # 补充文档
├── scripts/               # 环境变量导入脚本等辅助脚本
├── sak-service/           # Spring Boot 后端
└── sak-page/              # Vue 3 + Vite 前端
```

## 技术栈

### 后端 `sak-service`

- Java 17
- Spring Boot 3
- Spring Security
- MyBatis-Plus
- MySQL
- Redis
- JWT
- WebSocket

### 前端 `sak-page`

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Arco Design Vue

## 核心功能

- 登录鉴权与验证码
- JWT Access Token / Refresh Token
- 用户信息、头像上传、密码修改
- 用户管理、角色管理、菜单管理
- 系统配置
- 在线用户管理
- 消息推送中心：站内信 / 邮件 / WxPusher
- 站内消息与 WebSocket 消息提醒
- 下载中心与异步导出模板
- 文件中心
- 审计中心：操作日志 / 登录日志
- 调度中心：固定频率 / 固定延迟 / Cron
- 硬件监控：系统 / JVM / 磁盘 / 数据库 / Redis 聚合监控
- WebSocket 实时消息推送

## 主要接口与能力

后端控制器当前主要围绕以下能力组织：

- `/auth`：验证码、登录入口
- `/user`：当前用户信息、资料修改、密码修改、头像上传、令牌刷新与退出
- `/system/users`：用户管理
- `/system/roles`：角色管理
- `/system/menus`：后台菜单管理
- `/system/config`：系统配置
- `/system/notifications`：通知管理
- `/system/exports`：下载中心 / 导出记录
- `/system/files`：文件中心
- `/system/monitor`：系统监控
- `/system/schedules`：调度中心
- `/system/online-users`：在线用户管理
- `/system/logs`：操作日志
- `/system/login-logs`：登录日志
- `/site-messages`：站内消息
- `/ws/site-messages`：WebSocket 站内消息通道

## 模块概览

当前后台已经形成了几条比较清晰的能力线：

- 系统管理：用户、角色、权限、通知、下载中心、硬件监控、文件中心、调度中心
- 审计中心：操作日志、登录日志
- 消息能力：站内信、邮件、WxPusher、WebSocket 到站提醒
- 导出能力：异步导出任务、导出记录、ZIP 打包下载、导出模板抽象
- 平台能力：环境变量导入脚本、`ip2region` 属地解析、菜单/权限缓存刷新

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+ 或更高
- Corepack

说明：

- 前端模块通过 Maven 调用 `corepack pnpm` 执行安装和构建。
- 如果本机没有启用 Corepack，可先执行 `corepack enable`。

## 本地开发

建议先准备数据库、Redis，并执行初始化 SQL。

### 0. 初始化数据库

全新部署时，执行以下两个脚本即可：

1. [schema.sql](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/resources/schema/schema.sql)
2. [seed.sql](/Users/wuxuan/work/super-admin-killer/sak-service/src/main/resources/schema/seed.sql)

说明：

- `schema.sql` 负责建表
- `seed.sql` 负责插入当前版本的初始化菜单、角色、用户和示例数据
- `schema/upgrade/*.sql` 主要用于旧库增量升级，不是新库必跑项

### 1. 启动后端

进入后端模块运行：

```bash
cd sak-service
mvn spring-boot:run
```

### 2. 启动前端

进入前端模块运行：

```bash
cd sak-page
corepack pnpm install
corepack pnpm dev
```

## 环境变量与配置

项目支持通过环境变量覆盖本地配置，推荐先看：

- [environment-variables.md](/Users/wuxuan/work/super-admin-killer/docs/environment-variables.md)

仓库里已经准备了导入脚本与模板：

- [superkiller.env.example](/Users/wuxuan/work/super-admin-killer/scripts/env/superkiller.env.example)
- [import-env.sh](/Users/wuxuan/work/super-admin-killer/scripts/import-env.sh)
- [import-env.ps1](/Users/wuxuan/work/super-admin-killer/scripts/import-env.ps1)
- [import-env.bat](/Users/wuxuan/work/super-admin-killer/scripts/import-env.bat)

Linux / macOS 常用方式：

```bash
cp scripts/env/superkiller.env.example scripts/env/superkiller.env
source scripts/import-env.sh
```

## Maven 多模块构建

仓库根目录是父工程，可以统一构建全部模块：

```bash
mvn clean package
```

只构建后端：

```bash
mvn clean package -pl sak-service
```

只构建前端：

```bash
mvn clean package -pl sak-page
```

如果前端依赖已经安装过，希望跳过 `pnpm install`：

```bash
mvn clean package -pl sak-page -Dfrontend.skip.install=true
```

如果只想跳过前端构建：

```bash
mvn clean package -pl sak-page -Dfrontend.skip.build=true
```

## 后端配置说明

后端当前默认激活本地配置：

```yaml
spring:
  profiles:
    active: local
```

本地主要配置文件位于：

- `sak-service/src/main/resources/application.yml`
- `sak-service/src/main/resources/application-local.yml`

当前涉及的主要配置项：

- `SERVER.PORT`
- `DB.HOST`
- `DB.PORT`
- `DB.USERNAME`
- `DB.PASSWORD`
- `REDIS.HOST`
- `REDIS.PORT`
- `REDIS.PASSWORD`
- `JWT.*`
- `MAIL.*`
- `WXPUSHER.*`
- `IP2REGION.*`

存储相关配置：

- 文件根目录：`storage.root-dir`
- 头像目录：`storage.avatar-dir`
- 导出目录：`storage.export-dir`
- 文件中心目录：`storage.file-dir`
- 访问路径：`storage.access-path`

默认情况下，上传文件会写入后端运行目录下的 `storage/`。

## ip2region 属地库

登录日志支持记录登录 IP 属地，默认优先从运行目录外部文件加载 `ip2region` 数据库，不要求把资源打进 `classpath`。

建议查看：

- [ip2region-usage.md](/Users/wuxuan/work/super-admin-killer/docs/ip2region-usage.md)

默认路径示例：

```text
./ip2region/ip2region_v4.xdb
```

如果未提供 `xdb` 文件，系统仍可正常启动，只是登录日志中的“登录属地”会为空。

## 后端发布与打包

`sak-service` 已调整为依赖与主程序分离的发布模式，不再使用默认 fat jar。

执行：

```bash
cd sak-service
mvn clean package
```

会生成类似如下结构的发行包：

```text
target/sak-service-1.0.0-dist/
├── app/      # 业务主 jar
├── lib/      # 运行依赖
├── config/   # 配置文件
├── sql/      # SQL 脚本
├── static/   # 静态资源
└── bin/      # 启动脚本
```

同时会生成：

- Linux 启动脚本：`bin/start.sh`
- Windows 启动脚本：`bin/start.bat`

## 前端构建说明

前端模块 `sak-page` 使用 Vite 构建，产物默认输出到：

```text
sak-page/dist/
```

当前 Maven 会在前端模块生命周期中执行：

- `corepack pnpm install --frozen-lockfile`
- `corepack pnpm run build`

并且已为非交互环境自动注入：

- `CI=true`
- `COREPACK_HOME=${project.build.directory}/corepack`

用于避免 CI / Maven 执行时的交互式中断问题。

## 调度中心扩展

调度中心支持通过实现 `ScheduledTaskDefinitionHandler` 来扩展新的任务类型。当前模式是：

- 新增一个 handler 实现类
- 部署新代码
- 重启后端
- 启动时自动同步任务类型元数据到 `sys_scheduled_task_type`

详细说明见：

- [schedule-task-extension.md](/Users/wuxuan/work/super-admin-killer/docs/schedule-task-extension.md)

## 相关文档

- [environment-variables.md](/Users/wuxuan/work/super-admin-killer/docs/environment-variables.md)
- [ip2region-usage.md](/Users/wuxuan/work/super-admin-killer/docs/ip2region-usage.md)
- [schedule-task-extension.md](/Users/wuxuan/work/super-admin-killer/docs/schedule-task-extension.md)
- [scaffold-gap-analysis.md](/Users/wuxuan/work/super-admin-killer/docs/scaffold-gap-analysis.md)
- [scaffold-roadmap.md](/Users/wuxuan/work/super-admin-killer/docs/scaffold-roadmap.md)

## 安全说明

当前仓库中部分本地配置包含示例性质的数据库、Redis、JWT 配置。正式部署前建议：

- 使用环境变量覆盖敏感配置
- 更换 JWT 密钥
- 更换数据库与 Redis 凭据
- 根据生产环境收紧 CORS 和 WebSocket 来源配置
- 升级已知存在风险提示的依赖版本

## 后续建议

- 增加 Docker / Docker Compose 部署方式
- 增加生产环境示例配置
- 增加导入中心与调度执行日志
