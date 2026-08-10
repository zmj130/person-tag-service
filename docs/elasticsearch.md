# Elasticsearch 使用与清理

## 已确认环境

- 服务端：Elasticsearch `7.16.2`
- 集群名：`ddp_es`
- 已访问节点名：`ddp1`
- 地址：`http://10.3.7.225:9200`
- 项目索引：`person_tag_resume_demo_v1`

节点名不等于集群只有一个节点。使用以下只读命令确认集群和分片状态：

```bash
curl "http://10.3.7.225:9200/_cluster/health?pretty"
curl "http://10.3.7.225:9200/_cat/nodes?v&h=name,ip,node.role,heap.percent,ram.percent,cpu,load_1m"
curl "http://10.3.7.225:9200/_cat/indices/person_tag_resume_demo_v1?v&h=index,health,status,docs.count,store.size"
curl "http://10.3.7.225:9200/_cat/shards/person_tag_resume_demo_v1?v"
```

索引不存在时 `_cat/indices/person_tag_resume_demo_v1` 返回 404 是正常状态，不代表集群故障。

## 项目实际使用的能力

- 倒排索引：按姓名、单位、职业、地址和备注做连续局部匹配。例如“张一”可命中“演示张一”，但不会因单个相同汉字召回无关人员。
- `keyword` 精确值：标签组合 AND/OR 和枚举筛选。
- `nested` 类型化指标：同一固定 mapping 存储动态指标，避免每个指标产生一个新字段。
- `terms` 聚合：从筛选结果统计其他标签、性别和职业，完成标签反推人员。
- 单人增量刷新：人员、标签关系或规则结果提交后，更新或删除对应的画像文档。
- Bulk 全量重建：从达梦恢复全部画像，不把 ES 当成事实库。

索引固定为 1 个主分片、0 个副本、`30s` 刷新间隔，默认最多 5000 个文档。0 副本意味着该演示索引不提供副本容灾，但能减少共享服务器资源；数据可从达梦重建。

## 为共享服务器稳定性主动不用的能力

- 不在应用启动时自动建索引或重建索引。
- 不修改集群设置、索引模板、ILM、ingest pipeline 或其他项目索引。
- 不用通配符删除，不提供应用内删除接口。
- 不在数据库事务内双写 ES，不做 CDC 或消息队列补偿；事务提交后只做尽力而为的单人刷新。
- 不启用副本、分词插件、同义词词典、脚本评分、向量检索和高亮。
- 不使用 aliases/rollover 多代索引；演示环境只维护一个精确命名索引。

这些能力在生产平台化时有价值：副本提升容灾，别名和 rollover 支持无停机切换，中文分词和同义词改善召回，增量索引降低全量刷新延迟，向量检索可扩展语义搜索。但它们都会增加资源、运维或一致性成本，不适合当前简历演示目标。

## 启用和重建

当前主配置默认 `ES_ENABLED=true`，测试配置强制关闭。为了避免服务器环境变量覆盖造成误判，部署进程仍建议显式设置：

```bash
export ES_ENABLED=true
export ES_URL=http://10.3.7.225:9200
export ES_INDEX=person_tag_resume_demo_v1
export ES_MAX_DOCUMENTS=5000
```

环境变量只在 Java 进程启动时读取，修改后必须重启后端。启动后先检查状态，再从页面点击“创建索引/重建索引”，或调用受调度令牌保护的内部接口。重建会先确认达梦有效人员不超过上限，然后只删除并重建 `ES_INDEX` 指定的精确索引。

只修改搜索查询逻辑不需要重建索引。新版本部署后，人员、标签关系和规则结果的变更会在数据库事务提交后自动刷新对应画像；索引尚未创建时增量刷新会跳过。升级前遗留了陈旧数据、日志提示增量刷新失败，或需要做全量校准时，再从画像页面点击“重建索引”，或调用：

```bash
curl --fail --show-error -X POST "http://localhost:8080/api/profiles/rebuild"
```

演示数据统一使用 `src/main/resources/db/dm/demo/cleanup_demo_data.sql` 清理。脚本包含旧版“示例甲/示例乙”的 `DEMO-001`、`DEMO-002`，先删除人员侧证据、指标值和标签关系，再删除人员，不删除共享标签和规则配置。

## 精确清理

清理前必须先列出精确索引并核对名称：

```bash
curl "http://10.3.7.225:9200/_cat/indices/person_tag_resume_demo_v1?v&h=index,health,status,docs.count,store.size"
```

确认输出中的索引名完全等于 `person_tag_resume_demo_v1` 后再执行：

```bash
curl --fail --show-error -X DELETE "http://10.3.7.225:9200/person_tag_resume_demo_v1"
```

最后再次执行精确查询，返回 404 即表示演示索引已清理。禁止使用 `DELETE /*`、`DELETE /person_tag_resume_*` 或修改 `action.destructive_requires_name`。
