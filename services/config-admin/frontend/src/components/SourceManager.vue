<template>
  <div class="source-manager">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>数据源管理</span>
          <el-button type="primary" @click="showCreateDialog">
            <el-icon><Plus /></el-icon> 新增数据源
          </el-button>
        </div>
      </template>

      <!-- 数据源列表 -->
      <el-table :data="sources" stripe>
        <el-table-column prop="sourceId" label="数据源ID" width="180" />
        <el-table-column prop="sourceType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getSourceTypeColor(row.sourceType)">
              {{ row.sourceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="connection" label="连接地址" show-overflow-tooltip />
        <el-table-column label="属性" width="200">
          <template #default="{ row }">
            <el-tag v-for="(value, key) in row.properties" :key="key" size="small" style="margin: 2px;">
              {{ key }}: {{ value }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑数据源' : '新增数据源'"
      width="600px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="数据源ID" prop="sourceId">
          <el-input v-model="form.sourceId" placeholder="请输入数据源ID" :disabled="isEdit" />
        </el-form-item>

        <el-form-item label="类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择类型" style="width: 100%">
            <el-option label="MySQL" value="MYSQL" />
            <el-option label="PostgreSQL" value="POSTGRESQL" />
            <el-option label="Oracle" value="ORACLE" />
            <el-option label="文件" value="FILE" />
          </el-select>
        </el-form-item>

        <el-form-item label="连接地址" prop="connection">
          <el-input v-model="form.connection" placeholder="例如: jdbc:mysql://localhost:3306/dbname" />
        </el-form-item>

        <el-form-item label="属性">
          <el-button size="small" @click="addProperty">添加属性</el-button>
          <div v-for="(prop, index) in form.propertiesList" :key="index" style="margin-top: 8px;">
            <el-input v-model="prop.key" placeholder="键" style="width: 45%; margin-right: 8px;" />
            <el-input v-model="prop.value" placeholder="值" style="width: 45%;" />
            <el-button size="small" type="danger" @click="removeProperty(index)">删除</el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sourceApi } from '../api/config'

const sources = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = ref({
  sourceId: '',
  sourceType: 'MYSQL',
  connection: '',
  propertiesList: []
})

const rules = {
  sourceId: [
    { required: true, message: '请输入数据源ID', trigger: 'blur' }
  ],
  sourceType: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  connection: [
    { required: true, message: '请输入连接地址', trigger: 'blur' }
  ]
}

// 获取数据源列表
const fetchSources = async () => {
  try {
    const response = await sourceApi.getList()
    sources.value = response.data
    // 转换 properties
    sources.value.forEach(s => {
      if (typeof s.properties === 'object') {
        s.propertiesList = Object.entries(s.properties || {}).map(([key, value]) => ({ key, value }))
      } else {
        s.propertiesList = []
      }
    })
  } catch (error) {
    ElMessage.error('获取数据源列表失败')
  }
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    sourceId: '',
    sourceType: 'MYSQL',
    connection: '',
    propertiesList: []
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  form.value = {
    sourceId: row.sourceId,
    sourceType: row.sourceType,
    connection: row.connection,
    propertiesList: row.propertiesList || []
  }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除数据源 "${row.sourceId}"?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await sourceApi.delete(row.sourceId)
    ElMessage.success('删除成功')
    await fetchSources()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 添加属性
const addProperty = () => {
  form.value.propertiesList.push({ key: '', value: '' })
}

// 删除属性
const removeProperty = (index) => {
  form.value.propertiesList.splice(index, 1)
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()

  try {
    const data = {
      ...form.value,
      properties: {}
    }
    form.value.propertiesList.forEach(p => {
      if (p.key) {
        data.properties[p.key] = p.value
      }
    })
    delete data.propertiesList

    if (isEdit.value) {
      await sourceApi.update(form.value.sourceId, data)
      ElMessage.success('更新成功')
    } else {
      await sourceApi.create(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchSources()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

const getSourceTypeColor = (type) => {
  const colors = {
    MYSQL: 'primary',
    POSTGRESQL: 'success',
    ORACLE: 'warning',
    FILE: 'info'
  }
  return colors[type] || ''
}

onMounted(() => {
  fetchSources()
})
</script>

<style scoped>
.source-manager {
  padding: 20px;
}
</style>
