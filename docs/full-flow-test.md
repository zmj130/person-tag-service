# 全流程功能测试

## 前提

1. 当前达梦模式已经执行 `V1__init.sql` 和 `V2__indicator_rule_profile.sql`。
2. 后端是包含动态指标、结构化规则和 ES 画像代码的新版本。
3. 后端服务器能够访问 `http://10.3.7.225:9200`。
4. 测试数据仅用于本项目测试模式，不要在生产模式执行。

## 一、导入演示数据

使用达梦客户端执行：

```text
src/main/resources/db/dm/demo/full_flow_demo_data.sql
```

脚本使用固定 ID 和 `DEMO_*` 业务编码，重复执行不会重复插入。执行后检查：

```sql
SELECT COUNT(*) AS PERSON_COUNT FROM PT_PERSON
WHERE EXTERNAL_ID IN ('DEMO-P001','DEMO-P002','DEMO-P003','DEMO-P004','DEMO-P005','DEMO-P006');

SELECT COUNT(*) AS INDICATOR_COUNT FROM PT_INDICATOR_DEFINITION
WHERE CODE IN ('DEMO_AGE','DEMO_ANNUAL_FLOW','DEMO_RISK_LEVEL','DEMO_ACTIVE','DEMO_REGISTERED_AT');

SELECT COUNT(*) AS TAG_COUNT FROM PT_TAG_DEFINITION
WHERE CODE IN ('DEMO_YOUNG_HIGH_FLOW','DEMO_RISK_OR_HIGH_FLOW','DEMO_ACTIVE_RISK','DEMO_RECENT_REGISTERED','DEMO_STUDENT','DEMO_HIGH_EDUCATION','DEMO_TECH_EMPLOYEE');

SELECT COUNT(*) AS RULE_COUNT FROM PT_TAG_RULE_SET
WHERE ID IN ('45000000000000000000000000000001','45000000000000000000000000000002','45000000000000000000000000000003','45000000000000000000000000000004');

SELECT COUNT(*) AS VALUE_COUNT FROM PT_PERSON_INDICATOR
WHERE PERSON_ID IN (SELECT ID FROM PT_PERSON WHERE EXTERNAL_ID IN ('DEMO-P001','DEMO-P002','DEMO-P003','DEMO-P004','DEMO-P005','DEMO-P006'));
```

预期依次为：人员 `6`、自定义指标 `5`、标签 `7`、已发布规则 `4`、人员指标值 `30`。

## 二、启用 Elasticsearch

当前 `application.yml` 使用 `${ES_ENABLED:true}`，没有外部覆盖时已经启用；测试配置 `application-test.yml` 仍强制关闭，因此 Maven 测试不会连接公司 ES。

Linux 直接启动 JAR 时，在启动同一个 Java 进程前设置：

```bash
export ES_ENABLED=true
export ES_URL=http://10.3.7.225:9200
export ES_INDEX=person_tag_resume_demo_v1
export ES_MAX_DOCUMENTS=5000
java -jar person-tag-service-1.0.0-SNAPSHOT.jar
```

如果由 systemd 管理，把这些变量放进对应服务的 `[Service]` 环境配置并重启服务。环境变量在 Java 进程启动后再执行 `export` 不会生效，必须重启后端。

先做只读验证：

```bash
curl "http://10.3.7.225:9200/?pretty"
curl "http://10.3.0.181:8080/api/profiles/status"
```

第二个响应应为 `enabled=true`。首次测试 `exists=false` 是正常的，此时还没有创建索引。

## 三、重算规则

调用 DolphinScheduler 使用的内部接口，也可以直接在 Linux 上执行：

```bash
curl --fail --show-error \
  -X POST "http://10.3.0.181:8080/internal/sync/rules/recalculate" \
  -H "Content-Type: application/json" \
  -H "X-Scheduler-Token: ${SCHEDULER_TOKEN}" \
  --data '{"batchNo":"DEMO_RULE_20260728_001"}'
```

每次真正重新计算必须换一个批次号；相同成功批次号用于验证幂等，会直接返回旧结果。

四个标签的演示人员预期命中关系：

| 标签 | 模式 | 命中人员 | 数量 |
|---|---|---|---:|
| 演示-青年高流水 | ALL | P001、P005 | 2 |
| 演示-高风险或高流水 | ANY | P001、P002、P004、P005 | 4 |
| 演示-活跃风险人员 | ALL | P001、P002、P005 | 3 |
| 演示-近年登记人员 | ALL | P001、P003、P005、P006 | 4 |

合计应产生 `13` 条待审核规则标签。若测试库还有其他已发布规则，接口响应中会出现额外规则批次，但 `DEMO_*` 规则只会命中拥有演示指标的六名人员。

## 四、审核规则标签

打开管理端“标签审核”，筛选 `PENDING`。逐条通过上述 `13` 条演示候选。人工辅助标签已经是 `APPROVED`，不会出现在待审核列表。

审核后可用 SQL核对：

```sql
SELECT T.CODE, PT.STATUS, COUNT(*) AS PERSON_COUNT
FROM PT_PERSON_TAG PT
JOIN PT_TAG_DEFINITION T ON T.ID = PT.TAG_ID
JOIN PT_PERSON P ON P.ID = PT.PERSON_ID
WHERE P.EXTERNAL_ID IN ('DEMO-P001','DEMO-P002','DEMO-P003','DEMO-P004','DEMO-P005','DEMO-P006')
  AND T.CODE IN ('DEMO_YOUNG_HIGH_FLOW','DEMO_RISK_OR_HIGH_FLOW','DEMO_ACTIVE_RISK','DEMO_RECENT_REGISTERED')
GROUP BY T.CODE, PT.STATUS
ORDER BY T.CODE, PT.STATUS;
```

四行状态都应为 `APPROVED`，数量分别为 `2、4、3、4`。

## 五、创建或重建 ES 画像

规则审核完成后再执行：

```bash
curl --fail --show-error \
  -X POST "http://10.3.0.181:8080/internal/sync/profiles/rebuild" \
  -H "X-Scheduler-Token: ${SCHEDULER_TOKEN}"
```

该接口只会删除并重建精确索引 `person_tag_resume_demo_v1`。然后检查：

```bash
curl "http://10.3.7.225:9200/_cat/indices/person_tag_resume_demo_v1?v&h=index,health,status,docs.count,store.size"
```

`docs.count` 等于达梦中所有未删除人员数，不一定只等于 6；演示数据只新增 6 人。

## 六、验证画像反查

按“演示-青年高流水”反查：

```bash
curl --fail --show-error \
  -X POST "http://10.3.0.181:8080/api/profiles/search" \
  -H "Content-Type: application/json" \
  --data '{"keyword":"","tagIds":["41000000000000000000000000000001"],"tagOperator":"AND","indicators":[],"pageNo":1,"pageSize":20}'
```

预期返回 P001、P005 两人；P001 的其他标签包含“演示-学生”，P005 包含“演示-高学历”和“演示-科技从业”。页面左侧聚合应出现这些其他标签。

还可以在画像页测试：

- 关键词输入“货运司机”：命中 P003，验证全文检索。
- 同时选择“青年高流水”和“近年登记人员”，关系选择“满足全部”：命中 P001、P005。
- 指标选择“演示年度银行流水”，运算符 `GE`，值 `10000000`：命中 P001、P002、P005。
- 指标选择“演示风险等级”，运算符 `IN`，选择“中、高”：命中 P001、P002、P004、P005。

## 七、验证规则失效

把 P001 的年度流水改为 500 元：

```sql
UPDATE PT_PERSON_INDICATOR
SET NUMBER_VALUE = 500, SOURCE_UPDATED_AT = CURRENT_TIMESTAMP, UPDATED_AT = CURRENT_TIMESTAMP
WHERE PERSON_ID = '43000000000000000000000000000001'
  AND INDICATOR_ID = '42000000000000000000000000000002';
COMMIT;
```

使用新批次号再次执行规则重算。P001 应退出“青年高流水”，但仍因风险等级 `HIGH` 命中“高风险或高流水”。再次重建 ES 后，画像查询结果同步变化；达梦中的旧规则证据保留为失效状态。

## 八、清理

先按 [Elasticsearch 使用与清理](elasticsearch.md) 精确删除 `person_tag_resume_demo_v1`，再执行：

```text
src/main/resources/db/dm/demo/cleanup_demo_data.sql
```

清理脚本只使用精确人员编码、标签编码、指标编码和规则集 ID，不使用 `DEMO_%` 通配符删除；同时会清理旧版 `DEMO-001`、`DEMO-002` 人员侧数据。
