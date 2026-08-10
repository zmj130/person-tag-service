<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Play, Plus, Rocket, Trash2 } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const route = useRoute()
const router = useRouter()
const rows = ref([])
const tags = ref([])
const indicators = ref([])
const dialog = ref(false)
const form = reactive({ tagId: '', matchMode: 'ALL', conditions: [] })
const tagMap = computed(() => Object.fromEntries(tags.value.map(item => [item.id, item])))
const indicatorMap = computed(() => Object.fromEntries(indicators.value.map(item => [item.definition.id, item])))
const selectedTagId = computed(() => typeof route.query.tagId === 'string' ? route.query.tagId : '')
const filteredRows = computed(() => selectedTagId.value ? rows.value.filter(item => item.tagId === selectedTagId.value) : rows.value)
const selectedTag = computed(() => tagMap.value[selectedTagId.value])

async function load() { loading.value = true; try { [rows.value, tags.value, indicators.value] = await Promise.all([api.ruleSets(), api.tags(), api.indicators(true)]) } finally { loading.value = false } }
function emptyCondition() { return { indicatorId: '', operator: '', valuesText: '', selectedValues: [] } }
function openCreate() { form.tagId = selectedTagId.value; form.matchMode = 'ALL'; form.conditions = [emptyCondition()]; dialog.value = true }
function clearTagFilter() { router.push('/rules') }
function addCondition() { form.conditions.push(emptyCondition()) }
function indicatorChanged(condition) { condition.operator = ''; condition.valuesText = ''; condition.selectedValues = [] }
function values(condition) {
  const type = indicatorMap.value[condition.indicatorId]?.definition.dataType
  return type === 'ENUM' ? condition.selectedValues : condition.valuesText.split(',').map(item => item.trim()).filter(Boolean)
}
async function save() {
  if (!form.tagId || form.conditions.some(item => !item.indicatorId || !item.operator)) return ElMessage.warning('请完整选择标签、指标和运算符')
  await api.createRuleSet({ tagId: form.tagId, matchMode: form.matchMode, conditions: form.conditions.map(item => ({ indicatorId: item.indicatorId, operator: item.operator, values: values(item) })) })
  dialog.value = false; ElMessage.success('规则草稿已创建'); await load()
}
async function publish(row) { await ElMessageBox.confirm('发布后会停用该标签的上一版本规则，是否继续？', '发布规则'); await api.publishRuleSet(row.id); ElMessage.success('规则已发布'); await load() }
async function recalculate(row) { await api.recalculateRuleSet(row.id, `RULE_${Date.now()}`); ElMessage.success('全量重算完成，命中结果已进入审核') }
async function removeDraft(row) { await ElMessageBox.confirm('确认删除该规则草稿？此操作不可恢复。', '删除草稿', { type: 'warning' }); await api.deleteRuleSet(row.id); ElMessage.success('规则草稿已删除'); await load() }
function conditionText(condition) {
  const detail = indicatorMap.value[condition.indicatorId]
  let expected = condition.expectedValues
  try { expected = JSON.parse(expected).join('、') } catch (_) { /* keep raw value */ }
  return `${detail?.definition.name || condition.indicatorId} ${condition.operator}${expected ? ` ${expected}` : ''}`
}
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="action-row"><span class="page-note"><template v-if="selectedTag">当前标签：{{ selectedTag.name }}；</template>一个标签一个生效规则集；条件只允许一层 ALL/ANY，不支持规则套规则</span><div><el-button v-if="selectedTagId" @click="clearTagFilter">查看全部</el-button><el-button type="primary" @click="openCreate"><Plus :size="16" />新建规则</el-button></div></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="filteredRows" height="100%" empty-text="当前标签暂无结构化规则">
      <el-table-column label="目标标签" min-width="150"><template #default="{ row }"><div class="primary-cell"><b>{{ tagMap[row.tagId]?.name || row.tagId }}</b><small>版本 v{{ row.version }}</small></div></template></el-table-column>
      <el-table-column label="条件关系" width="110"><template #default="{ row }">{{ row.matchMode === 'ALL' ? '满足全部' : '满足任一' }}</template></el-table-column>
      <el-table-column label="规则条件" min-width="320"><template #default="{ row }"><div class="condition-list"><span v-for="condition in row.conditions" :key="condition.id">{{ conditionText(condition) }}</span></div></template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column>
      <el-table-column label="操作" width="220" fixed="right"><template #default="{ row }"><el-button v-if="row.status === 'DRAFT'" link type="primary" @click="publish(row)"><Rocket :size="15" />发布</el-button><el-button v-if="row.status === 'DRAFT'" link type="danger" @click="removeDraft(row)"><Trash2 :size="15" />删除</el-button><el-button v-if="row.status === 'PUBLISHED'" link type="primary" @click="recalculate(row)"><Play :size="15" />重算</el-button></template></el-table-column>
    </el-table></div>
  </section>

  <el-dialog v-model="dialog" title="新建标签规则" width="min(760px, 94vw)">
    <el-form label-position="top"><div class="form-grid"><el-form-item label="目标标签" required><el-select v-model="form.tagId" filterable class="full-width"><el-option v-for="tag in tags.filter(item => item.status === 1)" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item><el-form-item label="条件关系" required><el-select v-model="form.matchMode" class="full-width"><el-option label="满足全部条件" value="ALL" /><el-option label="满足任一条件" value="ANY" /></el-select></el-form-item></div>
      <div class="section-heading"><h3>条件</h3><el-button @click="addCondition"><Plus :size="15" />添加条件</el-button></div>
      <div v-for="(condition, index) in form.conditions" :key="index" class="condition-editor"><el-select v-model="condition.indicatorId" filterable placeholder="指标" @change="indicatorChanged(condition)"><el-option v-for="item in indicators" :key="item.definition.id" :label="item.definition.name" :value="item.definition.id" /></el-select><el-select v-model="condition.operator" placeholder="运算符"><el-option v-for="op in indicatorMap[condition.indicatorId]?.operators || []" :key="op" :label="op" :value="op" /></el-select><el-select v-if="indicatorMap[condition.indicatorId]?.definition.dataType === 'ENUM'" v-model="condition.selectedValues" multiple collapse-tags placeholder="选择固定值" :disabled="['IS_NULL','IS_NOT_NULL'].includes(condition.operator)"><el-option v-for="option in indicatorMap[condition.indicatorId]?.options || []" :key="option.code" :label="option.label" :value="option.code" /></el-select><el-select v-else-if="indicatorMap[condition.indicatorId]?.definition.dataType === 'BOOLEAN'" v-model="condition.valuesText" placeholder="选择布尔值" :disabled="['IS_NULL','IS_NOT_NULL'].includes(condition.operator)"><el-option label="是" value="true" /><el-option label="否" value="false" /></el-select><el-input v-else v-model="condition.valuesText" :disabled="['IS_NULL','IS_NOT_NULL'].includes(condition.operator)" placeholder="比较值；多个值用英文逗号分隔" /><el-button text circle title="删除条件" aria-label="删除条件" :disabled="form.conditions.length === 1" @click="form.conditions.splice(index, 1)"><Trash2 :size="16" /></el-button></div>
    </el-form>
    <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存草稿</el-button></template>
  </el-dialog>
</template>
