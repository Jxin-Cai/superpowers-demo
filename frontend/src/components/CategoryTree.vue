<template>
  <div class="category-tree-container">
    <div class="toolbar">
      <el-button type="primary" @click="handleAddRoot">
        <el-icon><Plus /></el-icon>
        添加顶级分类
      </el-button>
      <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedNodes.length">
        <el-icon><Delete /></el-icon>
        删除选中
      </el-button>
    </div>

    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="defaultProps"
      node-key="id"
      :expand-on-click-node="false"
      :check-on-click-node="true"
      :highlight-current="true"
      :allow-drop="allowDrop"
      :allow-drag="allowDrag"
      draggable
      @node-drop="handleDrop"
      @node-click="handleNodeClick"
      @check="handleCheck"
    >
      <template #default="{ node, data }">
        <span class="custom-tree-node">
          <span class="node-label">{{ node.label }}</span>
          <span class="node-actions">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleAddChild(data)"
            >
              <el-icon><Plus /></el-icon>
              添加子分类
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="handleEdit(data)"
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click.stop="handleDelete(data)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 分类编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'edit' ? '编辑分类' : '新建分类'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
        <el-form-item
          v-if="dialogMode === 'create' && !isRootCategory"
          label="父分类"
          prop="parentId"
        >
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="treeSelectProps"
            node-key="id"
            placeholder="请选择父分类"
            :render-after-expand="false"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import { categoryApi } from '@/api/category'
import { useRouter } from 'vue-router'

const router = useRouter()

const treeRef = ref(null)
const treeData = ref([])
const selectedNodes = ref([])
const dialogVisible = ref(false)
const dialogMode = ref('create') // 'create' or 'edit'
const editingNodeId = ref(null)
const isRootCategory = ref(false)
const formRef = ref(null)

const form = ref({
  name: '',
  description: '',
  parentId: null
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择父分类', trigger: 'change' }]
}

const defaultProps = {
  label: 'name',
  children: 'children'
}

const treeSelectProps = {
  label: 'name',
  children: 'children',
  disabled: (data) => {
    // 禁用自身及其所有子孙节点作为父分类
    if (editingNodeId.value && data.id === editingNodeId.value) {
      return true
    }
    // 禁用超过3级的节点作为父分类
    return data.level >= 3
  }
}

const loadTreeData = async () => {
  try {
    const res = await categoryApi.getTree()
    // axios 拦截器已返回 response.data.data，所以 res 直接是 { tree: [...] }
    treeData.value = res.tree || []
  } catch (error) {
    ElMessage.error('加载分类树失败')
    console.error(error)
  }
}

const handleAddRoot = () => {
  dialogMode.value = 'create'
  isRootCategory.value = true
  form.value = {
    name: '',
    description: '',
    parentId: null
  }
  dialogVisible.value = true
}

const handleAddChild = (data) => {
  // 检查是否超过最大层级（4层）
  if (data.level >= 3) {
    ElMessage.warning('最多支持4级分类，当前节点已是第4级')
    return
  }

  dialogMode.value = 'create'
  isRootCategory.value = false
  editingNodeId.value = null
  form.value = {
    name: '',
    description: '',
    parentId: data.id
  }
  dialogVisible.value = true
}

const handleEdit = (data) => {
  dialogMode.value = 'edit'
  editingNodeId.value = data.id
  form.value = {
    name: data.name,
    description: data.description || '',
    parentId: data.parentId || null
  }
  dialogVisible.value = true
}

const handleDelete = async (data) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类"${data.name}"吗？删除后将无法恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await categoryApi.delete(data.id)
    ElMessage.success('删除成功')
    await loadTreeData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedNodes.value.length === 0) {
    ElMessage.warning('请先选择要删除的分类')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedNodes.value.length} 个分类吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const promises = selectedNodes.value.map((node) =>
      categoryApi.delete(node.id)
    )
    await Promise.all(promises)
    ElMessage.success('批量删除成功')
    selectedNodes.value = []
    await loadTreeData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '批量删除失败')
    }
  }
}

const handleSave = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      if (dialogMode.value === 'edit') {
        await categoryApi.update(editingNodeId.value, form.value)
        ElMessage.success('更新成功')
      } else {
        await categoryApi.create(form.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      await loadTreeData()
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '保存失败')
    }
  })
}

const resetForm = () => {
  form.value = {
    name: '',
    description: '',
    parentId: null
  }
  editingNodeId.value = null
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const handleNodeClick = (data) => {
  console.log('Node clicked:', data)
}

const handleCheck = (data, checkedInfo) => {
  selectedNodes.value = checkedInfo.checkedNodes
}

// 拖拽相关方法
const allowDrop = (draggingNode, dropNode, type) => {
  // 只允许拖拽到节点内部（作为子节点）或之前之后（同级排序）
  if (type === 'inner') {
    // 检查目标节点是否已经有3层以上
    if (dropNode.data.level >= 3) {
      return false
    }
    // 检查拖拽节点是否会形成循环引用
    if (isDescendant(draggingNode.data.id, dropNode.data.id)) {
      return false
    }
    return true
  }
  // type 为 'prev' 或 'after' 时，只能在同级节点之间排序
  return draggingNode.data.parentId === dropNode.data.parentId
}

const allowDrag = (draggingNode) => {
  // 所有节点都可以拖拽
  return true
}

const handleDrop = async (draggingNode, dropNode, dropType) => {
  try {
    if (dropType === 'inner') {
      // 拖拽到节点内部，更新父分类
      await categoryApi.moveCategory(draggingNode.data.id, {
        newParentId: dropNode.data.id
      })
    } else {
      // 同级排序，使用批量更新排序
      const nodes = dropNode.parent.childNodes.map((node, index) => ({
        id: node.data.id,
        sortOrder: index + 1
      }))
      await categoryApi.batchUpdateSortOrder({ updates: nodes })
    }
    ElMessage.success('操作成功')
    await loadTreeData()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
    await loadTreeData()
  }
}

// 检查是否为子孙节点
const isDescendant = (descendantId, ancestorId) => {
  const findNode = (nodes) => {
    for (const node of nodes) {
      if (node.id === descendantId) {
        return true
      }
      if (node.children && node.children.length > 0) {
        if (findNode(node.children)) {
          return true
        }
      }
    }
    return false
  }

  const ancestorNode = findNodeInTree(treeData.value, ancestorId)
  if (ancestorNode && ancestorNode.children) {
    return findNode(ancestorNode.children)
  }
  return false
}

const findNodeInTree = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeInTree(node.children, id)
      if (found) return found
    }
  }
  return null
}

onMounted(() => {
  loadTreeData()
})
</script>

<style scoped>
.category-tree-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-right: 8px;
  font-size: 14px;
}

.node-label {
  font-weight: 500;
}

.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.3s;
}

.el-tree-node__content:hover .node-actions {
  opacity: 1;
}
</style>
