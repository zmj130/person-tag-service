# person-tag-service

一个可独立运行和讲解的人员标签系统第一版。项目使用 JDK 8、Spring Boot 2.7、MyBatis 和达梦，覆盖标签管理、人员标签绑定、HTTP 增量同步、Trie 规则打标及 AND/OR 组合查询。

## 第一版能力

- 标签创建、修改、启停，关键词规则创建和启停。
- 人员资料维护，人工单条/批量绑定、解绑及候选标签审核。
- 远程人员接口游标分页同步，批次号幂等，单页事务和失败重试。
- 外部标签直接确认；规则标签按配置进入候选或自动确认。
- Trie 在规则事务提交后重建，通过原子引用切换快照。
- 人员资料变化后重新对账规则标签，避免旧标签长期残留。
- 达梦初始化脚本和基于 H2 的自动化测试。

当前刻意不引入 Redis、Elasticsearch、Spark 和 HBase。它们应在数据规模或检索需求证明有必要后再增加。

## 环境

- JDK 8（已使用 `D:\Jdk\jdk-8u151` 验证）
- Maven 3.9.x
- 达梦 8，具体服务端版本待确认

## 初始化达梦

1. 使用独立用户/模式登录达梦。
2. 执行 `src/main/resources/db/dm/V1__init.sql`。
3. 如需演示数据，再执行 `V2__demo_data.sql`，生产环境不要执行演示脚本。
4. 设置环境变量：`DM_URL`、`DM_USERNAME`、`DM_PASSWORD`、`SCHEDULER_TOKEN`。

达梦版本确认 SQL 和兼容性检查见 [docs/dameng.md](docs/dameng.md)。

## 构建与测试

```powershell
& 'D:\Maven\apache-maven-3.9.9\bin\mvn.cmd' test
& 'D:\Maven\apache-maven-3.9.9\bin\mvn.cmd' package
```

启动后访问：

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

## 同步演示

默认 `REMOTE_PERSON_MODE=mock`，会生成两条虚构人员数据。调用：

```http
POST /internal/sync/persons/incremental
X-Scheduler-Token: local-demo-token
Content-Type: application/json

{"batchNo":"LOCAL-20260727-001"}
```

接入真实上游时设置 `REMOTE_PERSON_MODE=http` 和远程地址、令牌。协议见 [docs/remote-person-api.md](docs/remote-person-api.md)，DolphinScheduler 配置方式见 [docs/dolphinscheduler.md](docs/dolphinscheduler.md)。

## 设计资料

- [架构与关键取舍](docs/architecture.md)
- [远程人员接口协议](docs/remote-person-api.md)
- [DolphinScheduler 调用方式](docs/dolphinscheduler.md)
- [达梦检查与部署](docs/dameng.md)

# person-tag-service
# person-tag-service
