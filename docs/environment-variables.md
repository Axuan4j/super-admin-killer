# 环境变量与一键导入脚本

## 1. 当前项目实际使用到的环境变量

### 后端配置

- `SERVER_PORT`
- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `JWT_SECRET`
- `JWT_AT_EXPIRE`
- `JWT_RT_EXPIRE`
- `WXPUSHER_ENABLED`
- `WXPUSHER_APP_TOKEN`
- `WXPUSHER_BASE_URL`
- `WXPUSHER_CONTENT_TYPE`
- `IP2REGION_ENABLED`
- `IP2REGION_IPV4_DB`
- `IP2REGION_IPV6_DB`
- `IP2REGION_IPV4_CLASSPATH`
- `IP2REGION_IPV6_CLASSPATH`

### 前端配置

- `VITE_API_BASE_URL`
- `VITE_APP_TITLE`

### 启动脚本相关

- `SPRING_PROFILES_ACTIVE`
- `JAVA_HOME`
- `JAVA_OPTS`
- `SPRING_OPTS`

## 2. 为什么把变量名改成下划线风格

原来后端配置里有类似 `JWT.AT.EXPIRE`、`SERVER.PORT` 这样的占位写法。  
这类名字在 Spring 里能解析，但在 Linux shell 里不能直接 `export`，拿来做跨平台导入脚本会很别扭。

现在已经统一成更适合脚本和 CI 的形式：

- `SERVER_PORT`
- `JWT_AT_EXPIRE`
- `WXPUSHER_APP_TOKEN`

这样 Linux、macOS、Windows CMD、PowerShell 都能直接使用。

## 3. 模板文件

环境变量模板：

- [superkiller.env.example](/Users/wuxuan/work/super-admin-killer/scripts/env/superkiller.env.example)

建议复制为：

- `scripts/env/superkiller.env`

## 4. 一键导入脚本

### Linux / macOS

脚本：

- [import-env.sh](/Users/wuxuan/work/super-admin-killer/scripts/import-env.sh)

推荐用法：

```bash
cp scripts/env/superkiller.env.example scripts/env/superkiller.env
source scripts/import-env.sh
```

如果你想导入后立刻执行命令：

```bash
scripts/import-env.sh -- mvn -pl sak-service spring-boot:run
scripts/import-env.sh -- pnpm --dir sak-page dev
```

### Windows PowerShell

脚本：

- [import-env.ps1](/Users/wuxuan/work/super-admin-killer/scripts/import-env.ps1)

用法：

```powershell
Copy-Item scripts/env/superkiller.env.example scripts/env/superkiller.env
. .\scripts\import-env.ps1
```

导入后直接执行命令：

```powershell
.\scripts\import-env.ps1 mvn -pl sak-service spring-boot:run
.\scripts\import-env.ps1 pnpm --dir sak-page dev
```

### Windows CMD

脚本：

- [import-env.bat](/Users/wuxuan/work/super-admin-killer/scripts/import-env.bat)

用法：

```bat
copy scripts\env\superkiller.env.example scripts\env\superkiller.env
call scripts\import-env.bat
```

导入后直接执行命令：

```bat
scripts\import-env.bat -- mvn -pl sak-service spring-boot:run
scripts\import-env.bat -- pnpm --dir sak-page dev
```

## 5. 建议

- 开发环境优先维护 `scripts/env/superkiller.env`
- 服务器部署时可以把同样的变量放进系统环境或服务管理器
- `ip2region` 默认优先走外部文件：`./ip2region/ip2region_v4.xdb`
- 前端如果只是本地开发，也可以继续使用 [sak-page/.env](/Users/wuxuan/work/super-admin-killer/sak-page/.env)
