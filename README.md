# Super Admin Killer

一个前后端分离的后台管理项目，包含 Spring Boot 后端服务和 Vue 3 前端页面，当前仓库已经整理为 Maven 多模块结构，支持通过 Maven 统一驱动后端打包与前端构建。

## 项目结构

```text
superkiller-admin/
├── pom.xml                # Maven 父工程
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
- 操作日志
- 系统通知与站内消息
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
- `/system/online-users`：在线用户管理
- `/system/logs`：操作日志
- `/site-messages`：站内消息
- `/ws/site-messages`：WebSocket 站内消息通道

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+ 或更高
- Corepack

说明：

- 前端模块通过 Maven 调用 `corepack pnpm` 执行安装和构建。
- 如果本机没有启用 Corepack，可先执行 `corepack enable`。

## 本地开发

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

存储相关配置：

- 文件根目录：`storage.root-dir`
- 头像目录：`storage.avatar-dir`
- 访问路径：`storage.access-path`

默认情况下，上传文件会写入后端运行目录下的 `storage/`。

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

## 安全说明

当前仓库中部分本地配置包含示例性质的数据库、Redis、JWT 配置。正式部署前建议：

- 使用环境变量覆盖敏感配置
- 更换 JWT 密钥
- 更换数据库与 Redis 凭据
- 根据生产环境收紧 CORS 和 WebSocket 来源配置
- 升级已知存在风险提示的依赖版本

## 后续建议

- 将前端 `dist` 自动并入后端发布包
- 增加数据库初始化与迁移说明
- 增加 Docker / Docker Compose 部署方式
- 增加生产环境示例配置

