<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search, Settings2 } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const rows = ref([])
const ruleSets = ref([])
const router = useRouter()
const keyword = ref('')
const dialog = ref(false)
const editing = ref(false)
const form = reactive({ id: '', code: '', name: '', category: '', description: '', autoApprove: false })
const rulesByTag = computed(() => {
  const result = {}
  for (const rule of ruleSets.value) {
    const current = result[rule.tagId]
    if (!current || rule.status === 'PUBLISHED' || (current.status !== 'PUBLISHED' && rule.version > current.version)) result[rule.tagId] = rule
  }
  return result
})

const filtered = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return !text ? rows.value : rows.value.filter(item => [item.name, item.code, item.category].some(value => value?.toLowerCase().includes(text)))
})

async function load() { loading.value = true; try { [rows.value, ruleSets.value] = await Promise.all([api.tags(), api.ruleSets()]) } finally { loading.value = false } }
function openCreate() { editing.value = false; Object.assign(form, { id: '', code: '', name: '', category: '', description: '', autoApprove: false }); dialog.value = true }
function openEdit(row) { editing.value = true; Object.assign(form, row, { autoApprove: row.autoApprove === 1 }); dialog.value = true }

async function save() {
  if (!form.code.trim() || !form.name.trim() || !form.category.trim()) return ElMessage.warning('请填写编码、名称和分类')
  const data = { name: form.name, category: form.category, description: form.description, autoApprove: form.autoApprove }
  if (editing.value) await api.updateTag(form.id, data)
  else await api.createTag({ code: form.code, ...data })
  ElMessage.success(editing.value ? '标签已更新' : '标签已创建')
  dialog.value = false
  await load()
}

async function toggleTag(row) { await api.setTagStatus(row.id, row.status !== 1); ElMessage.success(row.status === 1 ? '标签已停用' : '标签已启用'); await load() }

function openRules(row) { router.push({ path: '/rules', query: { tagId: row.id } }) }
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="action-row"><el-input v-model="keyword" clearable placeholder="搜索名称、编码或分类" class="keyword-input"><template #prefix><Search :size="16" /></template></el-input><el-button type="primary" @click="openCreate"><Plus :size="16" />新增标签</el-button></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="filtered" height="100%" empty-text="暂无标签">
      <el-table-column label="标签" min-width="180"><template #default="{ row }"><div class="primary-cell"><b>{{ row.name }}</b><small>{{ row.code }}</small></div></template></el-table-column>
      <el-table-column prop="category" label="分类" width="130" />
      <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column label="规则审核" width="120"><template #default="{ row }">{{ row.autoApprove === 1 ? '自动通过' : '人工审核' }}</template></el-table-column>
      <el-table-column label="结构化规则" width="120"><template #default="{ row }"><span v-if="rulesByTag[row.id]">{{ rulesByTag[row.id].status === 'PUBLISHED' ? '已发布' : '草稿' }} v{{ rulesByTag[row.id].version }}</span><span v-else>未配置</span></template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column>
      <el-table-column label="操作" width="260" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openRules(row)"><Settings2 :size="15" />{{ rulesByTag[row.id] ? '查看规则' : '配置规则' }}</el-button><el-button link @click="openEdit(row)">编辑</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleTag(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button></template></el-table-column>
    </el-table></div>
    <div class="pagination-row"><span>共 {{ filtered.length }} 个标签</span></div>
  </section>

  <el-dialog v-model="dialog" :title="editing ? '编辑标签' : '新增标签'" width="min(620px, 92vw)">
    <el-form label-position="top" class="form-grid"><el-form-item label="标签编码" required><el-input v-model="form.code" :disabled="editing" placeholder="如 LOGISTICS_WORKER" /></el-form-item><el-form-item label="标签名称" required><el-input v-model="form.name" /></el-form-item><el-form-item label="分类" required><el-input v-model="form.category" /></el-form-item><el-form-item label="规则命中后"><el-radio-group v-model="form.autoApprove"><el-radio-button :value="false">进入审核</el-radio-button><el-radio-button :value="true">自动通过</el-radio-button></el-radio-group></el-form-item><el-form-item label="说明" class="form-span"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></el-form>
    <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
</template>
