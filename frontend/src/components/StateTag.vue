<script setup>
defineProps({ value: { type: [String, Number], required: true }, kind: { type: String, default: 'status' } })

const labels = {
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝',
  RUNNING: '执行中', SUCCESS: '成功', FAILED: '失败',
  DRAFT: '草稿', PUBLISHED: '已发布', DISABLED: '已停用',
  ACTIVE: '有效', EXPIRED: '已失效',
  NORMAL: '正常', DELETED: '已删除',
  MANUAL: '人工', REMOTE: '上游', RULE: '规则', 1: '启用', 0: '停用'
}
</script>

<template><span class="state-tag" :class="`state-${String(value).toLowerCase()}`">{{ labels[value] ?? value }}</span></template>
