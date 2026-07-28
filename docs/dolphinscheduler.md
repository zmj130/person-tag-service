# DolphinScheduler 调用方式

标签项目不依赖 DolphinScheduler 源码，也不需要引入其 SDK。生产工作流使用 HTTP Task；若现有版本没有 HTTP Task，可使用 Shell Task 调用同一个接口。

## HTTP Task

- Method：`POST`
- URL：`http://person-tag-service:8080/internal/sync/persons/incremental`
- Header：`X-Scheduler-Token: ${schedulerToken}`
- Body：`{"batchNo":"PERSON_${system.biz.date}_${system.task.instance.id}"}`
- 成功条件：HTTP 200 且响应 `success=true`
- 最大并行实例：`1`
- 失败重试：建议 3 次，间隔按现场网络情况配置

批次号在重试时必须保持不变。不要把随机数或重试次数放进批次号，否则服务端无法识别幂等重试。

## 推荐工作流

```text
人员增量同步 -> 结构化规则重算（按需或夜间） -> ES 画像重建（启用 ES 时）
```

人员增量同步会立即计算该批发生变化的人员，包括兼容 Trie 和所有已发布结构化规则。规则发布后，即使人员没有变化，也可在规则管理页点击“重算”，或由调度调用下面的接口扫描存量人员：

```http
POST /internal/sync/rules/recalculate
X-Scheduler-Token: ${schedulerToken}
Content-Type: application/json

{"batchNo":"RULE_${system.biz.date}_${system.task.instance.id}"}
```

ES 采用显式全量重建。只有设置 `ES_ENABLED=true` 后才配置这一步；未启用时接口会返回 `ES_DISABLED`，不会连接远端集群。

```http
POST /internal/sync/profiles/rebuild
X-Scheduler-Token: ${schedulerToken}
```

规则重算会对每个已发布规则生成子批次，重试必须复用同一个父批次号。演示环境建议人员同步按业务频率运行、全部规则重算每天一次或规则发布后运行、ES 重建放在规则任务成功之后；三个任务最大并行度均设为 1。
