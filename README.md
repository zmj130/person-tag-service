# person-tag-service

一个可独立运行和讲解的人员标签系统。项目使用 Vue 3、JDK 8、Spring Boot 2.7、MyBatis、达梦和可选的 Elasticsearch 7.16.2，覆盖动态指标、结构化规则、批量导入、审核留痕和标签画像反查。

## 当前能力

- 标签创建、修改、启停，关键词规则创建和启停。
- 人员资料维护，人工单条/批量绑定、解绑及候选标签审核。
- 远程人员接口游标分页同步，批次号幂等，单页事务和失败重试。
- 外部标签直接确认；规则标签按配置进入候选或自动确认。
- Trie 在规则事务提交后重建，通过原子引用切换快照。
- 人员资料变化后重新对账规则标签，避免旧标签长期残留。
- Vue 3 管理端覆盖人员维护、标签规则、候选审核和同步记录，静态资源随 Spring Boot 打入同一个 JAR。
- 达梦初始化脚本和基于 H2 的自动化测试。
- 动态指标及类型约束，支持文本、数值、日期、日期时间、布尔和枚举。
- 人员详情动态展示当前指标值，统一转换布尔、枚举、日期和单位，便于人工核验规则命中结果。
- 标签规则集版本、ALL/ANY 条件、类型化比较、存量人员重算和命中证据。
- 根据当前指标动态生成双语 Excel 模板和可直接导入的示例文件；第一行是字段编码、第二行是中文说明、第三行开始是数据，校验失败时整批不写入。
- ES 人员画像副本，支持全文搜索、标签 AND/OR、指标过滤及标签/性别/职业聚合。
- ES 主配置已启用、自动化测试强制关闭；只能显式重建固定前缀索引，演示数据上限默认 5000 人。

当前刻意不引入 Redis、Spark 和 HBase，也不使用 ES 的集群级配置、通配符删除和自动建索引。

## 开发约束

- 修复历史错误逻辑时，不在正常业务代码中兼容旧 bug 的行为或其错误数据，也不保留新旧两套处理分支。
- 历史错误数据经确认范围后，通过人工操作或独立的一次性脚本清理；清理逻辑不得混入长期运行的业务方法。
- 方法严格遵守单一职责：业务处理、数据迁移、历史纠错和运维清理分别实现，禁止以“顺带兼容”为由合并职责。

## 环境

- JDK 8（已使用 `D:\Jdk\jdk-8u151` 验证）
- Maven 3.9.x
- Node.js 20+（仅构建前端需要，已使用 `D:\Node` 下的 Node.js 22 验证）
- 达梦 8，具体服务端版本待确认

## 初始化达梦

1. 使用独立用户/模式登录达梦。
2. 新库依次执行 `V1__init.sql`、`V2__indicator_rule_profile.sql` 和 `V3__multi_source_tag_binding.sql`；已有 V2 数据库只需补执行 V3。
3. 如需完整演示数据，再执行 `db/dm/demo/full_flow_demo_data.sql`；生产环境不要执行 `demo` 目录脚本。
4. 设置环境变量：`DM_URL`、`DM_USERNAME`、`DM_PASSWORD`、`SCHEDULER_TOKEN`。

达梦版本确认 SQL 和兼容性检查见 [docs/dameng.md](docs/dameng.md)。

## 构建与测试

```powershell
cd frontend
& 'D:\Node\npm.cmd' ci
& 'D:\Node\npm.cmd' run build
cd ..
& 'D:\Maven\apache-maven-3.9.9\bin\mvn.cmd' test
& 'D:\Maven\apache-maven-3.9.9\bin\mvn.cmd' package
```

`npm ci` 只需在首次拉取或依赖锁变化后执行。Maven 会把 `frontend/dist` 复制到 JAR 的 `static` 目录，部署和运行时不需要 Node.js，也不会额外启动前端进程。

前端开发时可在 `frontend` 目录运行 `& 'D:\Node\npm.cmd' run dev`，Vite 会把 `/api` 和 `/internal` 请求代理到本机 `8080` 端口。

启动后访问：

- 管理端：`http://localhost:8080/`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

当前版本按演示和内网管理工具定位，不包含登录、RBAC 和数据权限。除带 Token 的内部同步接口外，管理接口默认不鉴权，因此不要直接暴露到公网。

## 同步演示

默认 `REMOTE_PERSON_MODE=mock`，会生成两条虚构人员数据。调用：

```http
POST /internal/sync/persons/incremental
X-Scheduler-Token: local-demo-token
Content-Type: application/json

{"batchNo":"LOCAL-20260727-001"}
```

接入真实上游时设置 `REMOTE_PERSON_MODE=http` 和远程地址、令牌。协议见 [docs/remote-person-api.md](docs/remote-person-api.md)，DolphinScheduler 配置方式见 [docs/dolphinscheduler.md](docs/dolphinscheduler.md)。

## Elasticsearch

当前主配置默认 `ES_ENABLED=true`，测试配置强制关闭。部署时建议显式设置 `ES_ENABLED=true`，并通过 `ES_URL`、`ES_INDEX`、`ES_MAX_DOCUMENTS` 指定共享集群边界；索引必须以 `person_tag_resume_` 开头。启用不会自动建索引，首次使用仍要在“用户画像”页或内部调度接口显式创建。

共享服务器的检查、重建、精确清理命令和本项目主动舍弃的 ES 能力见 [docs/elasticsearch.md](docs/elasticsearch.md)。

## 设计资料

- [代码、全部接口与面试讲解手册](docs/interview-project-guide.md)
- [架构与关键取舍](docs/architecture.md)
- [远程人员接口协议](docs/remote-person-api.md)
- [DolphinScheduler 调用方式](docs/dolphinscheduler.md)
- [达梦检查与部署](docs/dameng.md)
- [Elasticsearch 使用与清理](docs/elasticsearch.md)
- [全流程演示数据与验证步骤](docs/full-flow-test.md)
