<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, RotateCcw, Tags, Trash2, UserRound } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const tags = ref([])
const selected = ref([])
const filters = reactive({ keyword: '', tagIds: [], tagOperator: 'AND', includeDeleted: false, pageNo: 1, pageSize: 20 })
const personDialog = ref(false)
const editing = ref(false)
const personForm = reactive(emptyPerson())
const detailOpen = ref(false)
const currentPerson = ref(null)
const currentBindings = ref([])
const currentIndicators = ref([])
const detailLoading = ref(false)
const bindTagId = ref('')
const batchDialog = ref(false)
const batchTagId = ref('')

const enabledTags = computed(() => tags.value.filter(item => item.status === 1))
const tagMap = computed(() => Object.fromEntries(tags.value.map(item => [item.id, item])))
const groupedBindings = computed(() => {
  const groups = new Map()
  for (const binding of currentBindings.value) {
    if (!groups.has(binding.tagId)) groups.set(binding.tagId, { tagId: binding.tagId, bindings: [] })
    groups.get(binding.tagId).bindings.push(binding)
  }
  return Array.from(groups.values()).map(group => ({
    ...group,
    status: group.bindings.some(item => item.status === 'APPROVED')
      ? 'APPROVED'
      : group.bindings.some(item => item.status === 'PENDING') ? 'PENDING' : group.bindings[0].status
  }))
})

function emptyPerson() {
  return { externalId: '', name: '', gender: '', organization: '', occupation: '', address: '', remark: '', sourceUpdatedAt: null }
}

async function load() {
  loading.value = true
  try {
    const data = await api.searchPersons(filters)
    rows.value = data.records
    total.value = data.total
  } finally { loading.value = false }
}

function search() { filters.pageNo = 1; load() }
function reset() { filters.keyword = ''; filters.tagIds = []; filters.tagOperator = 'AND'; filters.includeDeleted = false; search() }

function openCreate() {
  editing.value = false
  Object.assign(personForm, emptyPerson())
  personDialog.value = true
}

function openEdit(row) {
  editing.value = true
  Object.assign(personForm, emptyPerson(), row, { sourceUpdatedAt: row.sourceUpdatedAt || null })
  personDialog.value = true
}

async function savePerson() {
  if (!personForm.externalId.trim() || !personForm.name.trim()) return ElMessage.warning('请填写人员编码和姓名')
  await api.savePerson(personForm)
  ElMessage.success(editing.value ? '人员资料已更新' : '人员已新增')
  personDialog.value = false
  await load()
}

async function openDetail(row) {
  currentPerson.value = row
  currentBindings.value = []
  currentIndicators.value = []
  detailOpen.value = true
  detailLoading.value = true
  try {
    [currentBindings.value, currentIndicators.value] = await Promise.all([
      api.personTags(row.id),
      api.personIndicators(row.id)
    ])
  }
  finally { detailLoading.value = false }
}

async function bindOne() {
  if (!bindTagId.value) return ElMessage.warning('请选择标签')
  await api.bindTag(currentPerson.value.id, { tagId: bindTagId.value, operator: 'WEB_ADMIN' })
  ElMessage.success('标签已绑定')
  bindTagId.value = ''
  currentBindings.value = await api.personTags(currentPerson.value.id)
}

async function unbind(binding) {
  await ElMessageBox.confirm(`确认移除“${tagMap.value[binding.tagId]?.name || '该标签'}”的人工来源？其他来源不会受影响。`, '解绑人工标签', { type: 'warning' })
  await api.unbindTag(currentPerson.value.id, binding.tagId)
  ElMessage.success('标签已解绑')
  currentBindings.value = await api.personTags(currentPerson.value.id)
}

async function removePerson(row) {
  await ElMessageBox.confirm(`确认删除人员“${row.name}”？人员资料将被软删除，历史标签仍会保留。`, '删除人员', { type: 'warning' })
  await api.deletePerson(row.id)
  ElMessage.success('人员已删除')
  await load()
}

async function restorePerson(row) {
  await api.restorePerson(row.id)
  ElMessage.success('人员已恢复')
  await load()
}

function openBatch() {
  if (!selected.value.length) return ElMessage.warning('请先选择人员')
  batchTagId.value = ''
  batchDialog.value = true
}

async function bindBatch() {
  if (!batchTagId.value) return ElMessage.warning('请选择标签')
  await api.bindTagBatch({ personIds: selected.value.map(item => item.id), tagId: batchTagId.value, operator: 'WEB_ADMIN' })
  ElMessage.success(`已为 ${selected.value.length} 人绑定标签`)
  batchDialog.value = false
  await load()
}

onMounted(async () => { tags.value = await api.tags(); await load() })
</script>

<template>
  <section class="workspace">
    <div class="action-row">
      <div class="filter-row">
        <el-input v-model="filters.keyword" clearable placeholder="姓名、编码、机构或职业" class="keyword-input" @keyup.enter="search"><template #prefix><Search :size="16" /></template></el-input>
        <el-select v-model="filters.tagIds" multiple collapse-tags collapse-tags-tooltip clearable placeholder="按标签筛选" class="tag-filter">
          <el-option v-for="tag in enabledTags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
        <el-segmented v-model="filters.tagOperator" :options="[{ label: '满足全部', value: 'AND' }, { label: '满足任一', value: 'OR' }]" />
        <el-checkbox v-model="filters.includeDeleted" @change="search">含已删除</el-checkbox>
        <el-button type="primary" @click="search"><Search :size="16" />查询</el-button>
        <el-button aria-label="重置筛选" title="重置筛选" @click="reset"><RotateCcw :size="16" /></el-button>
      </div>
      <div class="command-row">
        <el-button :disabled="!selected.length" @click="openBatch"><Tags :size="16" />批量打标<span v-if="selected.length">（{{ selected.length }}）</span></el-button>
        <el-button type="primary" @click="openCreate"><Plus :size="16" />新增人员</el-button>
      </div>
    </div>

    <div class="table-wrap">
      <el-table v-loading="loading" :data="rows" row-key="id" height="100%" empty-text="暂无人员数据" @selection-change="selected = $event">
        <el-table-column type="selection" width="44" />
        <el-table-column label="人员" min-width="170">
          <template #default="{ row }"><button class="person-link" @click="openDetail(row)"><span class="avatar">{{ row.name.slice(0, 1) }}</span><span><b>{{ row.name }}</b><small>{{ row.externalId }}</small></span></button></template>
        </el-table-column>
        <el-table-column prop="organization" label="所属机构" min-width="180" show-overflow-tooltip />
        <el-table-column prop="occupation" label="职业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="address" label="地区/地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90"><template #default="{ row }"><StateTag :value="row.deleted === 1 ? 'DELETED' : 'NORMAL'" /></template></el-table-column>
        <el-table-column label="更新时间" width="165"><template #default="{ row }">{{ row.updatedAt || '-' }}</template></el-table-column>
        <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">查看</el-button><el-button v-if="row.deleted !== 1" link @click="openEdit(row)">编辑</el-button><el-button v-if="row.deleted !== 1" link type="danger" title="软删除人员" @click="removePerson(row)"><Trash2 :size="15" />删除</el-button><el-button v-else link type="success" @click="restorePerson(row)"><RotateCcw :size="15" />恢复</el-button></template></el-table-column>
      </el-table>
    </div>
    <div class="pagination-row"><span>共 {{ total }} 人</span><el-pagination v-model:current-page="filters.pageNo" v-model:page-size="filters.pageSize" layout="prev, pager, next" :total="total" @current-change="load" /></div>
  </section>

  <el-dialog v-model="personDialog" :title="editing ? '编辑人员' : '新增人员'" width="min(680px, 92vw)" destroy-on-close>
    <el-form label-position="top" class="form-grid">
      <el-form-item label="人员编码" required><el-input v-model="personForm.externalId" :disabled="editing" placeholder="如 EMP-001" /></el-form-item>
      <el-form-item label="姓名" required><el-input v-model="personForm.name" /></el-form-item>
      <el-form-item label="性别"><el-select v-model="personForm.gender" clearable><el-option label="男" value="男" /><el-option label="女" value="女" /><el-option label="未知" value="未知" /></el-select></el-form-item>
      <el-form-item label="所属机构"><el-input v-model="personForm.organization" /></el-form-item>
      <el-form-item label="职业"><el-input v-model="personForm.occupation" /></el-form-item>
      <el-form-item label="地区/地址"><el-input v-model="personForm.address" /></el-form-item>
      <el-form-item label="备注" class="form-span"><el-input v-model="personForm.remark" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="personDialog = false">取消</el-button><el-button type="primary" @click="savePerson">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="batchDialog" title="批量打标" width="min(460px, 92vw)">
    <el-form label-position="top"><el-form-item :label="`为已选 ${selected.length} 人绑定标签`"><el-select v-model="batchTagId" filterable class="full-width" placeholder="选择启用中的标签"><el-option v-for="tag in enabledTags" :key="tag.id" :label="`${tag.name} · ${tag.category}`" :value="tag.id" /></el-select></el-form-item></el-form>
    <template #footer><el-button @click="batchDialog = false">取消</el-button><el-button type="primary" @click="bindBatch">确认绑定</el-button></template>
  </el-dialog>

  <el-drawer v-model="detailOpen" size="min(560px, 96vw)" :title="currentPerson?.name || '人员详情'">
    <template v-if="currentPerson">
      <div class="identity-block"><span class="avatar avatar-large"><UserRound :size="24" /></span><div><h2>{{ currentPerson.name }}</h2><p>{{ currentPerson.externalId }} · {{ currentPerson.organization || '未填写机构' }}</p></div></div>
      <dl class="detail-grid"><div><dt>职业</dt><dd>{{ currentPerson.occupation || '-' }}</dd></div><div><dt>性别</dt><dd>{{ currentPerson.gender || '-' }}</dd></div><div class="detail-span"><dt>地区/地址</dt><dd>{{ currentPerson.address || '-' }}</dd></div><div class="detail-span"><dt>备注</dt><dd>{{ currentPerson.remark || '-' }}</dd></div></dl>
      <div class="section-heading"><h3>动态指标</h3><span>{{ currentIndicators.length }} 项</span></div>
      <div v-loading="detailLoading" class="indicator-section">
        <dl v-if="currentIndicators.length" class="indicator-kv-grid">
          <div v-for="indicator in currentIndicators" :key="indicator.indicatorId"><dt><span>{{ indicator.name }}</span><small>{{ indicator.code }}</small></dt><dd>{{ indicator.value || '-' }}</dd></div>
        </dl>
        <p v-else-if="!detailLoading" class="section-empty">暂无动态指标</p>
      </div>
      <div class="section-heading"><h3>人员标签</h3><span>{{ groupedBindings.length }} 个</span></div>
      <div class="inline-form"><el-select v-model="bindTagId" filterable placeholder="选择标签"><el-option v-for="tag in enabledTags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select><el-button type="primary" @click="bindOne"><Plus :size="16" />绑定</el-button></div>
      <div v-loading="detailLoading" class="binding-list">
        <div v-for="group in groupedBindings" :key="group.tagId" class="binding-row"><div><el-popover trigger="click" placement="left" :width="360"><template #reference><button class="tag-detail-trigger"><b>{{ tagMap[group.tagId]?.name || group.tagId }}</b><small>{{ group.bindings.length }} 个来源 · 点击查看</small></button></template><div class="source-detail-list"><div v-for="binding in group.bindings" :key="binding.id" class="source-detail-row"><div><p><StateTag :value="binding.source" /><StateTag :value="binding.status" /></p><small v-if="binding.matchedKeyword">命中“{{ binding.matchedKeyword }}”</small><small v-else>{{ binding.reviewedBy || binding.batchNo || '系统记录' }}</small></div><el-button v-if="binding.source === 'MANUAL'" link type="danger" title="解绑人工标签" @click="unbind(binding)"><Trash2 :size="15" />解绑</el-button></div></div></el-popover><p><StateTag :value="group.status" /><span>{{ group.bindings.map(item => item.source === 'MANUAL' ? '人工' : item.source === 'REMOTE' ? '上游' : '规则').join('、') }}</span></p></div></div>
        <el-empty v-if="!detailLoading && !groupedBindings.length" description="暂未绑定标签" :image-size="70" />
      </div>
    </template>
  </el-drawer>
</template>
