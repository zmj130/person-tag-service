<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, SlidersHorizontal } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const rows = ref([])
const dialog = ref(false)
const optionDialog = ref(false)
const current = ref(null)
const form = reactive({ code: '', name: '', dataType: 'TEXT', sourceType: 'IMPORT', unit: '' })
const option = reactive({ code: '', label: '', sortNo: 0 })
const types = ['TEXT', 'NUMBER', 'DATE', 'DATETIME', 'BOOLEAN', 'ENUM']
const typeLabels = { TEXT: '文本', NUMBER: '数值', DATE: '日期', DATETIME: '日期时间', BOOLEAN: '布尔', ENUM: '枚举' }
const flattened = computed(() => rows.value.map(item => ({ ...item.definition, options: item.options, operators: item.operators })))

async function load() { loading.value = true; try { rows.value = await api.indicators() } finally { loading.value = false } }
function openCreate() { Object.assign(form, { code: '', name: '', dataType: 'TEXT', sourceType: 'IMPORT', unit: '' }); dialog.value = true }
async function save() {
  if (!form.code.trim() || !form.name.trim()) return ElMessage.warning('请填写指标编码和名称')
  await api.createIndicator(form); dialog.value = false; ElMessage.success('指标已创建'); await load()
}
async function toggle(row) { await api.setIndicatorStatus(row.id, row.status !== 1); await load() }
function openOption(row) { current.value = row; Object.assign(option, { code: '', label: '', sortNo: row.options.length }); optionDialog.value = true }
async function saveOption() {
  if (!option.code.trim() || !option.label.trim()) return ElMessage.warning('请填写选项编码和名称')
  await api.addIndicatorOption(current.value.id, option); optionDialog.value = false; ElMessage.success('枚举项已添加'); await load()
}
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="action-row"><span class="page-note">规则条件和导入模板都从这里读取字段定义</span><el-button type="primary" @click="openCreate"><Plus :size="16" />新增指标</el-button></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="flattened" height="100%" empty-text="暂无指标">
      <el-table-column label="指标" min-width="180"><template #default="{ row }"><div class="primary-cell"><b>{{ row.name }}</b><small>{{ row.code }}</small></div></template></el-table-column>
      <el-table-column label="类型" width="100"><template #default="{ row }">{{ typeLabels[row.dataType] || row.dataType }}</template></el-table-column>
      <el-table-column prop="sourceType" label="来源" width="100" />
      <el-table-column label="可用运算" min-width="210"><template #default="{ row }">{{ row.operators.join(' / ') }}</template></el-table-column>
      <el-table-column label="枚举值" min-width="180"><template #default="{ row }">{{ row.options.map(item => item.label).join('、') || '-' }}</template></el-table-column>
      <el-table-column label="状态" width="90"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column>
      <el-table-column label="操作" width="170" fixed="right"><template #default="{ row }"><el-button v-if="row.dataType === 'ENUM'" link type="primary" @click="openOption(row)"><SlidersHorizontal :size="15" />选项</el-button><el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button></template></el-table-column>
    </el-table></div>
  </section>

  <el-dialog v-model="dialog" title="新增指标" width="min(600px, 92vw)">
    <el-form label-position="top" class="form-grid"><el-form-item label="指标编码" required><el-input v-model="form.code" placeholder="如 ANNUAL_FLOW" /></el-form-item><el-form-item label="指标名称" required><el-input v-model="form.name" placeholder="如 年度银行流水" /></el-form-item><el-form-item label="数据类型"><el-select v-model="form.dataType" class="full-width"><el-option v-for="type in types" :key="type" :label="typeLabels[type]" :value="type" /></el-select></el-form-item><el-form-item label="数据来源"><el-select v-model="form.sourceType" class="full-width"><el-option label="Excel 导入" value="IMPORT" /><el-option label="远程同步" value="REMOTE" /><el-option label="派生计算" value="DERIVED" /></el-select></el-form-item><el-form-item label="单位"><el-input v-model="form.unit" placeholder="可选，如 元" /></el-form-item></el-form>
    <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="optionDialog" :title="`${current?.name || ''} · 添加枚举项`" width="min(480px, 92vw)">
    <el-form label-position="top"><el-form-item label="选项编码" required><el-input v-model="option.code" /></el-form-item><el-form-item label="显示名称" required><el-input v-model="option.label" /></el-form-item></el-form>
    <template #footer><el-button @click="optionDialog = false">取消</el-button><el-button type="primary" @click="saveOption">添加</el-button></template>
  </el-dialog>
</template>
