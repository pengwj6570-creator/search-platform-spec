<template>
  <div class="index-manager">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>索引管理</span>
          <el-button @click="refreshIndices">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <!-- OpenSearch 集群状态 -->
      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="12">
          <el-statistic title="集群状态" :value="clusterStatus" />
        </el-col>
        <el-col :span="12">
          <el-statistic title="索引数量" :value="indices.length" />
        </el-col>
      </el-row>

      <!-- 索引列表 -->
      <el-table :data="indices" stripe>
        <el-table-column prop="index" label="索引名" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'open' ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="docsCount" label="文档数" width="100" />
        <el-table-column prop="storeSize" label="存储大小" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewMapping(row)">查看Mapping</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Mapping 查看对话框 -->
    <el-dialog v-model="mappingDialogVisible" title="索引 Mapping" width="700px">
      <pre style="background: #f5f7fa; padding: 15px; border-radius: 4px; overflow: auto;">{{ currentMapping }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { searchApi } from '../api/config'

const indices = ref([])
const clusterStatus = ref('unknown')
const mappingDialogVisible = ref(false)
const currentMapping = ref('')

// 刷新索引列表
const refreshIndices = async () => {
  try {
    // 获取集群健康状态
    const healthRes = await searchApi.getHealth()
    clusterStatus.value = healthRes.data.status || 'unknown'

    // 获取索引列表
    const indicesRes = await searchApi.getIndices()
    // 解析索引列表
    const lines = indicesRes.data.split('\n').filter(line => line.trim())
    indices.value = lines.map(line => {
      const parts = line.split(/\s+/)
      return {
        index: parts[0],
        status: parts[1] || 'unknown',
        docsCount: parts[4] || '0',
        storeSize: parts[6] || '0b'
      }
    })
  } catch (error) {
    ElMessage.error('获取索引列表失败')
  }
}

// 查看 Mapping
const viewMapping = async (row) => {
  try {
    const response = await axios.get(`http://localhost:9200/${row.index}/_mapping`)
    currentMapping.value = JSON.stringify(response.data, null, 2)
    mappingDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取Mapping失败')
  }
}

// 删除索引
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除索引 "${row.index}"?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await searchApi.deleteIndex(row.index)
    ElMessage.success('删除成功')
    await refreshIndices()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  refreshIndices()
})
</script>

<style scoped>
.index-manager {
  padding: 20px;
}
</style>
