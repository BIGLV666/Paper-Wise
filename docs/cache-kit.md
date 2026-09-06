# PaperWise × cache-kit 接入说明

PaperWise 接入了自研三级缓存组件 `cache-kit-spring-boot-starter`（Caffeine → Redis → DB read-through）。
组件源码：`../cache-kit-spring-boot-starter`，设计文档：`../cache-kit-proposal.md`。

## 当前接入状态（cache-kit 0.2.0）

| 接入点 | 方式 | 说明 |
|---|---|---|
| `UserMapper.getUserById` | `@CachedQuery` | 用户公开信息按 ID 读取（个人主页高频），自定义 XML 方法 |
| `UserMapper.updateById` 等 BaseMapper 方法 | 零注解自动接入 | MP BaseMapper 内置名单（selectById/updateById/deleteById），写后自动失效 + 广播 |
| 其他 `xxxMapper.selectById` | 零注解自动接入 | 对应实体有 `@TableName`/`@TableId` 即生效（`cache-kit.mp.auto-cache-base-methods`，默认开） |

自定义方法要接入缓存时加 `@CachedQuery`（仅 by-ID 查询，列表/分页不支持）；对应写方法用
`@CacheInvalidate(entity = Xxx.class)` 或走 BaseMapper 的 `updateById/deleteById` 自动失效。

## 重要约定

1. **写必须走应用路径**。直接改 MySQL（DBA/其他服务）不会触发失效广播，脏读会持续到
   TTL（L1 30s / L2 10min）。这类场景需要开启 binlog 直连失效（见下）。
2. **强一致数据不要进缓存**（余额、库存等）。需要临时强一致读时：
   `CacheKit.withDb(() -> mapper.selectById(id))`。
3. **不要在 service 和 mapper 两层重复加注解**：重复包装无害但浪费。
4. 列表/分页查询（`selectList` 等）不缓存——MVP 边界，见组件 README。

## 部署注意：mapper-locations 大小写（已修复）

mapper XML 位于 `resources/Mapper/`（大写 M）。`application.yml` 原配置为小写
`classpath*:/mapper/*.xml`：IDE 目录模式下 NTFS 大小写不敏感能碰巧匹配，**打成 jar 后
大小写敏感，全部 XML 加载失败**（登录报 `Invalid bound statement`）。已改为大写路径。
如用命令行启动，等价参数：`--mybatis-plus.mapper-locations=classpath*:/Mapper/*.xml`。

## binlog 直连失效（可选，当前未开启）

如需覆盖"绕过应用的写"（本系统目前没有该场景，保持关闭）：

```yaml
cache-kit:
  binlog:
    enabled: true        # 需 MySQL 开启 log_bin + binlog_format=ROW，账号有 REPLICATION SLAVE 权限
    server-id: 18365     # 与 MySQL server-id 及其他副本唯一
```

host/port/database/账号缺省从 `spring.datasource.url` 解析。要求 cache-kit 0.2.0+
（MySQL 8.4 需 mysql-binlog-connector 0.30.0+，组件已内置 0.31.0）。

## 压测工具与历史数据

- `src/test/java/org/example/paperwise/cachekit/`：集成测试 + 集群/极限压测（无 Redis 自动跳过）；
- `loadtest/LoadDriver.java`：外部 HTTP 压测驱动（Java 21 虚拟线程单文件），
  `java LoadDriver.java read <ports> <token> <ms> <threads>` 读压测 / `probe` 脏读观测；
- 关键实测（详见组件 README"一致性机制与性能实测"）：20k HTTP 请求 DB 零读、
  10 实例 4.6 万次广播零丢失、binlog 直写失效 p99 14.2ms。
