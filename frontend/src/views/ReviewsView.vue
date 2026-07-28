<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, RefreshCw, Trash2, X } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const reviewer = ref(localStorage.getItem('person-tag-reviewer') || '管理员')
const query = reactive({ status: 'PENDING', pageNo: 1, pageSize: 20 })
const statusOptions = [{ label: '待审核', value: 'PENDING' }, { label: '已通过', value: 'APPROVED' }, { label: '已拒绝', value: 'REJECTED' }]

async function load() { loading.value = true; try { const data = await api.reviews(query); rows.value = data.records; total.value = data.total } finally { loading.value = false } }
function changeStatus() { query.pageNo = 1; load() }
async function review(row, status) {
  if (!reviewer.value.trim()) return ElMessage.warning('请填写审核人')
  if (status === 'REJECTED') await ElMessageBox.confirm(`确认拒绝 ${row.personName} 的“${row.tagName}”标签？`, '拒绝标签', { type: 'warning' })
  localStorage.setItem('person-tag-reviewer', reviewer.value.trim())
  await api.review(row.id, { status, reviewer: reviewer.value.trim() })
  ElMessage.success(status === 'APPROVED' ? '标签已通过' : '标签已拒绝')
  await load()
}
function evidence(row) {
  if (!row.matchDetail) return []
  try { return JSON.parse(row.matchDetail) } catch (_) { return [] }
}
function evidenceText(item) {
  const expected = Array.isArray(item.expected) ? item.expected.join('、') : item.expected
  return `${item.indicatorName || item.indicatorCode} ${item.operator}${expected === undefined || expected === null || expected === '' ? '' : ` ${expected}`}（实际 ${item.actual ?? '空'}）`
}
async function remove(row) {
  await ElMessageBox.confirm('仅删除这条规则结果及其结构化证据。若规则仍已发布且人员仍满足条件，下次重算会重新产生，是否继续？', '删除规则结果', { type: 'warning' })
  await api.deleteRuleResult(row.id)
  ElMessage.success('规则结果已删除')
  await load()
}
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="action-row">
      <div class="filter-row"><el-segmented v-model="query.status" :options="statusOptions" @change="changeStatus" /><el-input v-model="reviewer" class="reviewer-input" placeholder="审核人" /></div>
      <el-button aria-label="刷新" title="刷新" @click="load"><RefreshCw :size="16" /></el-button>
    </div>
    <div class="table-wrap"><el-table v-loading="loading" :data="rows" height="100%" empty-text="当前没有审核记录">
      <el-table-column label="人员" min-width="155"><template #default="{ row }"><div class="primary-cell"><b>{{ row.personName }}</b><small>{{ row.externalId }}</small></div></template></el-table-column>
      <el-table-column label="候选标签" min-width="160"><template #default="{ row }"><div class="primary-cell"><b>{{ row.tagName }}</b><small>{{ row.tagCategory }}</small></div></template></el-table-column>
      <el-table-column label="命中依据" min-width="260"><template #default="{ row }"><div v-if="evidence(row).length" class="condition-list"><span v-for="(item, index) in evidence(row)" :key="index">{{ evidenceText(item) }}</span></div><span v-else-if="row.matchedKeyword" class="keyword-chip">关键词：{{ row.matchedKeyword }}</span><span v-else>-</span></template></el-table-column>
      <el-table-column label="来源" width="90"><template #default="{ row }"><StateTag :value="row.source" /></template></el-table-column>
      <el-table-column prop="batchNo" label="同步批次" min-width="170" show-overflow-tooltip />
      <el-table-column label="状态" width="100"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column>
      <el-table-column label="产生时间" width="165"><template #default="{ row }">{{ row.createdAt }}</template></el-table-column>
      <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><template v-if="query.status === 'PENDING'"><el-button link type="success" @click="review(row, 'APPROVED')"><Check :size="15" />通过</el-button><el-button link type="danger" @click="review(row, 'REJECTED')"><X :size="15" />拒绝</el-button></template><el-button v-if="row.source === 'RULE'" link type="danger" title="删除规则结果" @click="remove(row)"><Trash2 :size="15" />删除</el-button></template></el-table-column>
    </el-table></div>
    <div class="pagination-row"><span>共 {{ total }} 条</span><el-pagination v-model:current-page="query.pageNo" layout="prev, pager, next" :total="total" @current-change="load" /></div>
  </section>
</template>
