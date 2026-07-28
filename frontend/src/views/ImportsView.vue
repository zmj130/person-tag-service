<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Upload } from 'lucide-vue-next'
import { api } from '../api'
import StateTag from '../components/StateTag.vue'

const loading = ref(false)
const uploading = ref(false)
const rows = ref([])
const file = ref(null)
const batchNo = ref(`IMPORT_${Date.now()}`)
async function load() { loading.value = true; try { rows.value = await api.importBatches() } finally { loading.value = false } }
function choose(uploadFile) { file.value = uploadFile.raw }
async function upload() {
  if (!file.value) return ElMessage.warning('请先选择 .xlsx 文件')
  uploading.value = true
  try { await api.importPersons(batchNo.value, file.value); ElMessage.success('整批导入成功'); batchNo.value = `IMPORT_${Date.now()}`; file.value = null; await load() } finally { uploading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="workspace">
    <div class="import-band"><div><b>动态人员模板</b><span>模板会包含当前启用的导入型指标；任意一行校验失败，整批数据都不会写入。</span></div><el-button tag="a" href="/api/imports/persons/template"><Download :size="16" />下载模板</el-button></div>
    <div class="action-row import-actions"><div class="filter-row"><el-input v-model="batchNo" class="batch-input" placeholder="导入批次号" /><el-upload :auto-upload="false" :limit="1" accept=".xlsx" :on-change="choose"><el-button><Upload :size="16" />选择 Excel</el-button></el-upload><span class="selected-file">{{ file?.name || '未选择文件' }}</span></div><el-button type="primary" :loading="uploading" @click="upload">开始导入</el-button></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="rows" height="100%" empty-text="暂无导入记录">
      <el-table-column prop="batchNo" label="批次号" min-width="190" /><el-table-column prop="fileName" label="文件" min-width="180" show-overflow-tooltip /><el-table-column label="状态" width="100"><template #default="{ row }"><StateTag :value="row.status" /></template></el-table-column><el-table-column prop="totalCount" label="总行数" width="90" /><el-table-column prop="successCount" label="成功" width="90" /><el-table-column prop="errorMessage" label="错误" min-width="220" show-overflow-tooltip /><el-table-column prop="startedAt" label="开始时间" width="170" />
    </el-table></div>
  </section>
</template>
