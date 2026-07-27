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
人员增量同步 -> 校验同步结果 -> 后续统计任务（可选）
```

Trie 自动打标已经包含在每页同步事务中，第一版不需要再创建一个独立“计算标签”任务，避免人员数据和规则标签之间出现时间窗口。

