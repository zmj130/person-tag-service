<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Play, RefreshCw } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const running = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ status: '', pageNo: 1, pageSize: 20 })
const dialog = ref(false)
const form = reactive({ batchNo: '', startCursor: '', token: '' })

function defaultBatchNo() {
  const now = new Date()
  const pad = value => String(value).padStart(2, '0')
  return `WEB-${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}
async function load() { loading.value = true; try { const data = await api.syncBatches(query); rows.value = data.records; total.value = data.total } finally { loading.value = false } }
function filter() { query.pageNo = 1; load() }
function openRun() { Object.assign(form, { batchNo: defaultBatchNo(), startCursor: '', token: '' }); dialog.value = true }
async function run() {
  if (!form.batchNo.trim() || !form.token.trim()) return ElMessage.warning('请填写批次号和调度 Token')
  running.value = true
  try {
    await api.runSync({ batchNo: form.batchNo.trim(), startCursor: form.startCursor.trim() || null }, form.token.trim())
    ElMessage.success('同步执行完成')
    form.token = ''
    dialog.value = false
    await load()
  } finally { running.value = false }
}
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="action-row"><div class="filter-row"><el-select v-model="query.status" placeholder="全部状态" clearable class="status-filter" @change="filter"><el-option label="执行中" value="RUNNING" /><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILED" /></el-select><el-button aria-label="刷新" title="刷新" @click="load"><RefreshCw :size="16" /></el-button></div><el-button type="primary" @click="openRun"><Play :size="16" />执行增量同步</el-button></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="rows" height="100%" empty-text="暂无同步记录">
      <el-table-column prop="batchNo" label="批次号" min-width="210"><template #default="{ row }"><span class="mono">{{ row.batchNo }}</span></template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column>
      <el-table-column label="处理结果" min-width="190"><template #default="{ row }"><div class="count-group"><span>接收 <b>{{ row.receivedCount }}</b></span><span class="success-text">成功 <b>{{ row.successCount }}</b></span><span :class="{ 'danger-text': row.failureCount }">失败 <b>{{ row.failureCount }}</b></span></div></template></el-table-column>
      <el-table-column prop="cursorBefore" label="起始游标" min-width="140" show-overflow-tooltip />
      <el-table-column prop="cursorAfter" label="结束游标" min-width="140" show-overflow-tooltip />
      <el-table-column label="开始时间" width="165"><template #default="{ row }">{{ row.startedAt }}</template></el-table-column>
      <el-table-column label="结束时间" width="165"><template #default="{ row }">{{ row.finishedAt || '-' }}</template></el-table-column>
      <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
    </el-table></div>
    <div class="pagination-row"><span>共 {{ total }} 个批次</span><el-pagination v-model:current-page="query.pageNo" layout="prev, pager, next" :total="total" @current-change="load" /></div>
  </section>

  <el-dialog v-model="dialog" title="执行增量同步" width="min(520px, 92vw)" :close-on-click-modal="!running">
    <el-alert title="同步会读取上游人员变更并重新计算规则标签。请使用唯一批次号，调度 Token 仅用于本次请求。" type="info" :closable="false" show-icon />
    <el-form label-position="top" class="sync-form"><el-form-item label="批次号" required><el-input v-model="form.batchNo" /></el-form-item><el-form-item label="起始游标"><el-input v-model="form.startCursor" placeholder="留空则沿用上次成功游标" /></el-form-item><el-form-item label="调度 Token" required><el-input v-model="form.token" type="password" show-password autocomplete="off" /></el-form-item></el-form>
    <template #footer><el-button :disabled="running" @click="dialog = false">取消</el-button><el-button type="primary" :loading="running" @click="run">开始同步</el-button></template>
  </el-dialog>
</template>
