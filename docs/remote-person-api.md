# 远程人员增量接口

标签系统请求：

```http
GET {baseUrl}/api/persons/changes?cursor={cursor}&pageSize=200
Authorization: Bearer {token}
```

建议响应：

```json
{
  "records": [
    {
      "externalId": "SOURCE-0001",
      "name": "示例人员",
      "gender": "未知",
      "organization": "示例机构",
      "occupation": "货运司机",
      "address": "示例地址",
      "remark": "示例备注",
      "updatedAt": "2026-07-27 10:00:00",
      "deleted": false,
      "tagCodes": ["EXTERNAL_FOCUS"],
      "removedTagCodes": []
    }
  ],
  "nextCursor": "opaque-cursor-101",
  "hasMore": false
}
```

约束：

- `externalId` 是稳定且全局唯一的人员编码。
- `cursor` 是上游生成的不透明增量位置，标签系统不解析其内容。
- `tagCodes` 表示本次需要确保存在的外部标签，不代表完整标签快照。
- 撤销外部标签必须显式放入 `removedTagCodes`。
- 删除人员通过 `deleted=true` 软删除，本地保留历史标签关系用于追溯。
- 同一变更可以重复返回，本地依靠唯一键和更新逻辑保证幂等。
- 上游不得返回未知标签编码；未知编码会使当前页事务失败，待配置标签后重试。

