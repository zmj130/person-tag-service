<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Database, Plus, RefreshCw, Search, Trash2 } from 'lucide-vue-next'
import { api } from '../api'

const loading = ref(false)
const rebuilding = ref(false)
const status = ref({ enabled: false, exists: false, documentCount: 0, index: '' })
const tags = ref([])
const indicators = ref([])
const result = reactive({ total: 0, records: [], aggregations: {} })
const form = reactive({ keyword: '', tagIds: [], tagOperator: 'AND', indicators: [], pageNo: 1, pageSize: 20 })
const indicatorMap = computed(() => Object.fromEntries(indicators.value.map(item => [item.definition.code, item])))

async function loadStatus() { status.value = await api.profileStatus() }
async function rebuild() {
  await ElMessageBox.confirm(`只会重建精确索引 ${status.value.index}，是否继续？`, '重建画像索引', { type: 'warning' })
  rebuilding.value = true
  try { status.value = await api.rebuildProfiles(); ElMessage.success('画像索引已重建'); await search() } finally { rebuilding.value = false }
}
function addFilter() { form.indicators.push({ indicatorCode: '', operator: '', valuesText: '', selectedValues: [] }) }
async function search() {
  if (!status.value.enabled || !status.value.exists) return
  loading.value = true
  try {
    const data = await api.searchProfiles({ ...form, indicators: form.indicators.filter(item => item.indicatorCode && item.operator).map(item => ({ indicatorCode: item.indicatorCode, operator: item.operator, values: indicatorMap.value[item.indicatorCode]?.definition.dataType === 'ENUM' ? item.selectedValues : item.valuesText.split(',').map(value => value.trim()).filter(Boolean) })) })
    Object.assign(result, data)
  } finally { loading.value = false }
}
function runSearch() { form.pageNo = 1; search() }
function changePageSize() { form.pageNo = 1; search() }
onMounted(async () => { [tags.value, indicators.value] = await Promise.all([api.tags(), api.indicators(true)]); await loadStatus(); if (status.value.exists) await search() })
</script>

<template>
  <section class="workspace">
    <div class="index-status"><div class="status-icon"><Database :size="20" /></div><div><b>{{ status.index || 'person_tag_resume_demo_v1' }}</b><span v-if="!status.enabled">ES 未启用，设置 ES_ENABLED=true 后才能建索引和查询</span><span v-else-if="!status.exists">索引尚未创建</span><span v-else>当前 {{ status.documentCount }} 份人员画像</span></div><el-button type="primary" :disabled="!status.enabled" :loading="rebuilding" @click="rebuild"><RefreshCw :size="16" />{{ status.exists ? '重建索引' : '创建索引' }}</el-button></div>
    <template v-if="status.enabled && status.exists">
      <div class="profile-filters"><div class="filter-row"><el-input v-model="form.keyword" clearable class="keyword-input" placeholder="姓名、单位、职业、地址或备注（连续局部匹配）" @keyup.enter="runSearch"><template #prefix><Search :size="16" /></template></el-input><el-select v-model="form.tagIds" multiple clearable collapse-tags class="tag-filter" placeholder="按标签反查人员"><el-option v-for="tag in tags.filter(item => item.status === 1)" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select><el-select v-model="form.tagOperator" class="status-filter"><el-option label="满足全部标签" value="AND" /><el-option label="满足任一标签" value="OR" /></el-select><el-button type="primary" @click="runSearch"><Search :size="16" />查询</el-button></div><el-button @click="addFilter"><Plus :size="16" />指标条件</el-button></div>
      <div v-for="(filter, index) in form.indicators" :key="index" class="condition-editor profile-condition"><el-select v-model="filter.indicatorCode" filterable placeholder="指标" @change="filter.selectedValues = []; filter.valuesText = ''; filter.operator = ''"><el-option v-for="item in indicators" :key="item.definition.code" :label="item.definition.name" :value="item.definition.code" /></el-select><el-select v-model="filter.operator" placeholder="运算符"><el-option v-for="op in indicatorMap[filter.indicatorCode]?.operators || []" :key="op" :label="op" :value="op" /></el-select><el-select v-if="indicatorMap[filter.indicatorCode]?.definition.dataType === 'ENUM'" v-model="filter.selectedValues" multiple collapse-tags placeholder="选择固定值" :disabled="['IS_NULL','IS_NOT_NULL'].includes(filter.operator)"><el-option v-for="option in indicatorMap[filter.indicatorCode]?.options || []" :key="option.code" :label="option.label" :value="option.code" /></el-select><el-select v-else-if="indicatorMap[filter.indicatorCode]?.definition.dataType === 'BOOLEAN'" v-model="filter.valuesText" placeholder="选择布尔值" :disabled="['IS_NULL','IS_NOT_NULL'].includes(filter.operator)"><el-option label="是" value="true" /><el-option label="否" value="false" /></el-select><el-input v-else v-model="filter.valuesText" :disabled="['IS_NULL','IS_NOT_NULL'].includes(filter.operator)" placeholder="比较值；多个值用英文逗号分隔" /><el-button text circle title="删除条件" @click="form.indicators.splice(index, 1)"><Trash2 :size="16" /></el-button></div>
      <div class="profile-layout"><div class="facet-panel"><div v-for="(buckets, name) in result.aggregations" :key="name" class="facet-group"><b>{{ { tags: '关联标签', genders: '性别', occupations: '职业' }[name] }}</b><span v-for="bucket in buckets" :key="bucket.key">{{ bucket.key }} <em>{{ bucket.count }}</em></span></div></div><div class="table-wrap"><el-table v-loading="loading" :data="result.records" height="100%" empty-text="没有符合条件的人员"><el-table-column label="人员" min-width="150"><template #default="{ row }"><div class="primary-cell"><b>{{ row.name }}</b><small>{{ row.externalId }}</small></div></template></el-table-column><el-table-column prop="organization" label="单位" min-width="170" /><el-table-column prop="occupation" label="职业" min-width="120" /><el-table-column label="其他标签" min-width="220"><template #default="{ row }"><span v-for="tag in row.tagNames" :key="tag" class="keyword-chip profile-tag">{{ tag }}</span></template></el-table-column><el-table-column prop="address" label="地址" min-width="170" /></el-table></div></div>
      <div class="pagination-row"><span>共 {{ result.total }} 人</span><el-pagination v-model:current-page="form.pageNo" v-model:page-size="form.pageSize" :page-sizes="[10, 20, 50, 100]" layout="sizes, prev, pager, next" :total="result.total" @current-change="search" @size-change="changePageSize" /></div>
    </template>
    <el-empty v-else description="启用 ES 并显式创建演示索引后，可在这里按标签和指标反查人员" />
  </section>
</template>
