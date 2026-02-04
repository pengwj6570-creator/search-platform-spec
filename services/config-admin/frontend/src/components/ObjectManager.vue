<template>
  <div class="object-manager">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>搜索对象配置</span>
          <div>
            <el-select v-model="filterAppKey" placeholder="筛选应用" clearable style="width: 150px; margin-right: 10px;" @change="fetchObjects">
              <el-option label="全部" value="" />
              <el-option label="电商" value="ecommerce" />
              <el-option label="内容" value="content" />
            </el-select>
            <el-button type="primary" @click="showCreateDialog">
              <el-icon><Plus /></el-icon> 新增搜索对象
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索对象列表 -->
      <el-table :data="objects" stripe>
        <el-table-column prop="objectId" label="对象ID" width="150" />
        <el-table-column prop="sourceId" label="数据源" width="150" />
        <el-table-column prop="table" label="表名" width="150" />
        <el-table-column prop="appKey" label="应用" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.appKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字段配置" width="300">
          <template #default="{ row }">
            <el-tag v-for="field in row.fields" :key="field.name" size="small" style="margin: 2px;">
              {{ field.name }}: {{ field.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleViewFields(row)">查看字段</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑搜索对象' : '新增搜索对象'"
      width="800px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="对象ID" prop="objectId">
              <el-input v-model="form.objectId" placeholder="请输入对象ID" :disabled="isEdit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源" prop="sourceId">
              <el-select v-model="form.sourceId" placeholder="请选择数据源" style="width: 100%">
                <el-option v-for="s in sources" :key="s.sourceId" :label="s.sourceId" :value="s.sourceId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="表名" prop="table">
              <el-input v-model="form.table" placeholder="请输入表名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主键" prop="primaryKey">
              <el-input v-model="form.primaryKey" placeholder="请输入主键字段名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="应用Key" prop="appKey">
          <el-select v-model="form.appKey" placeholder="请选择应用" style="width: 100%">
            <el-option label="ecommerce" value="ecommerce" />
            <el-option label="content" value="content" />
          </el-select>
        </el-form-item>

        <el-form-item label="字段配置">
          <el-button size="small" @click="addField">添加字段</el-button>
          <el-table :data="form.fields" stripe style="margin-top: 10px;" max-height="300">
            <el-table-column prop="name" label="字段名" width="120" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option label="TEXT" value="TEXT" />
                  <el-option label="KEYWORD" value="KEYWORD" />
                  <el-option label="INTEGER" value="INTEGER" />
                  <el-option label="LONG" value="LONG" />
                  <el-option label="DOUBLE" value="DOUBLE" />
                  <el-option label="DATE" value="DATE" />
                  <el-option label="BOOLEAN" value="BOOLEAN" />
                  <el-option label="DENSE_VECTOR" value="DENSE_VECTOR" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="可搜索" width="80">
              <template #default="{ row }">
                <el-checkbox v-model="row.searchable" />
              </template>
            </el-table-column>
            <el-table-column label="可过滤" width="80">
              <template #default="{ row }">
                <el-checkbox v-model="row.filterable" />
              </template>
            </el-table-column>
            <el-table-column label="可排序" width="80">
              <template #default="{ row }">
                <el-checkbox v-model="row.sortable" />
              </template>
            </el-table-column>
            <el-table-column label="分词器" width="120">
              <template #default="{ row }">
                <el-input v-model="row.analyzer" placeholder="ik_max_word" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="权重" width="80">
              <template #default="{ row }">
                <el-input-number v-model="row.boost" :min="0" :max="10" :step="0.1" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default="{ $index }">
                <el-button size="small" type="danger" @click="removeField($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 字段查看对话框 -->
    <el-dialog v-model="fieldsDialogVisible" title="字段配置详情" width="700px">
      <el-table :data="currentObject.fields" stripe>
        <el-table-column prop="name" label="字段名" />
        <el-table-column prop="type" label="类型" />
        <el-table-column label="可搜索">
          <template #default="{ row }">
            <el-tag :type="row.searchable ? 'success' : 'info'">{{ row.searchable ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分词器">
          <template #default="{ row }">
            {{ row.analyzer || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="boost" label="权重" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { objectApi, sourceApi } from '../api/config'

const objects = ref([])
const sources = ref([])
const filterAppKey = ref('')
const dialogVisible = ref(false)
const fieldsDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const currentObject = ref({ fields: [] })

const form = ref({
  objectId: '',
  sourceId: '',
  table: '',
  primaryKey: 'id',
  appKey: '',
  fields: []
})

const rules = {
  objectId: [{ required: true, message: '请输入对象ID', trigger: 'blur' }],
  sourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  table: [{ required: true, message: '请输入表名', trigger: 'blur' }],
  primaryKey: [{ required: true, message: '请输入主键', trigger: 'blur' }],
  appKey: [{ required: true, message: '请选择应用', trigger: 'change' }]
}

// 获取搜索对象列表
const fetchObjects = async () => {
  try {
    const response = filterAppKey.value
      ? await objectApi.getByAppKey(filterAppKey.value)
      : await objectApi.getList()
    objects.value = response.data
  } catch (error) {
    ElMessage.error('获取搜索对象失败')
  }
}

// 获取数据源列表
const fetchSources = async () => {
  try {
    const response = await sourceApi.getList()
    sources.value = response.data
  } catch (error) {
    ElMessage.error('获取数据源失败')
  }
}

// 显示创建对话框
const showCreateDialog = async () => {
  await fetchSources()
  isEdit.value = false
  form.value = {
    objectId: '',
    sourceId: '',
    table: '',
    primaryKey: 'id',
    appKey: '',
    fields: []
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row) => {
  await fetchSources()
  isEdit.value = true
  form.value = {
    objectId: row.objectId,
    sourceId: row.sourceId,
    table: row.table,
    primaryKey: row.primaryKey,
    appKey: row.appKey,
    fields: [...row.fields]
  }
  dialogVisible.value = true
}

// 查看字段
const handleViewFields = (row) => {
  currentObject.value = row
  fieldsDialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除搜索对象 "${row.objectId}"?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await objectApi.delete(row.objectId)
    ElMessage.success('删除成功')
    await fetchObjects()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 添加字段
const addField = () => {
  form.value.fields.push({
    name: '',
    type: 'TEXT',
    searchable: false,
    filterable: false,
    sortable: false,
    analyzer: '',
    boost: 1.0
  })
}

// 删除字段
const removeField = (index) => {
  form.value.fields.splice(index, 1)
}

// 提交表单
const handleSubmit = async () => {
  await formRef.value.validate()

  try {
    if (isEdit.value) {
      await objectApi.update(form.value.objectId, form.value)
      ElMessage.success('更新成功')
    } else {
      await objectApi.create(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchObjects()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

onMounted(() => {
  fetchObjects()
})
</script>

<style scoped>
.object-manager {
  padding: 20px;
}
</style>
