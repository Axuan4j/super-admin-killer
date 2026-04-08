# ip2region 使用说明

项目默认优先从当前运行目录下的外部文件加载 `ip2region` 数据库，不要求把 `xdb` 文件打包进 `classpath`。

默认路径：

- `./ip2region/ip2region_v4.xdb`
- `./ip2region/ip2region_v6.xdb`

对应配置：

```yaml
ip2region:
  enabled: true
  ipv4-db: ./ip2region/ip2region_v4.xdb
  ipv6-db: ./ip2region/ip2region_v6.xdb
  ipv4-classpath: ""
  ipv6-classpath: ""
```

说明：

- 系统会优先尝试外部路径 `ipv4-db / ipv6-db`
- 只有当外部路径不存在，并且显式配置了 `ipv4-classpath / ipv6-classpath` 时，才会回退到 `classpath`
- 如果都没有配置到可用文件，系统仍可正常启动，只是登录日志中的“登录属地”为空

推荐部署方式：

1. 在应用运行目录创建 `ip2region/`
2. 放入官方 `ip2region_v4.xdb`
3. 如需 IPv6，再放入 `ip2region_v6.xdb`

环境变量示例：

```bash
IP2REGION_ENABLED=true
IP2REGION_IPV4_DB=./ip2region/ip2region_v4.xdb
IP2REGION_IPV6_DB=
IP2REGION_IPV4_CLASSPATH=
IP2REGION_IPV6_CLASSPATH=
```
