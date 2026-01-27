<template>
  <div class="category-tree">
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="defaultProps"
      draggable
      node-key="id"
      default-expand-all
      :allow-drop="allowDrop"
      @node-drop="handleDrop"
    >
      <template #default="{ node, data }">
        <div class="tree-node">
          <span class="node-label">{{ node.label }}</span>
          <span class="node-actions">
            <el-button
              size="small"
              text
              type="primary"
              @click.stop="edit(data)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              text
              type="danger"
              @click.stop="remove(data)"
            >
              删除
            </el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isEdit ? '编辑分类' : '新建分类'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>

        <el-form-item label="父分类">
          <el-tree-select
            v-model="form.parentId"
            :data="treeDataForSelect"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择父分类"
            clearable
            check-strictly
            :render-after-expand="false"
          />
        </el-form-item>

        <el-form-item label="排序序号">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'

const emit = defineEmits(['refresh'])

const treeRef = ref()
const treeData = ref([])
const editDialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)

const defaultProps = {
  label: 'name',
  children: 'children'
}

const form = ref({
  name: '',
  description: '',
  parentId: null,
  sortOrder: 0
})

// 用于选择器的树形数据（排除当前编辑节点及其子节点）
const treeDataForSelect = computed(() => {
  const excludeIds = new Set()

  const collectChildren = (node) => {
    excludeIds.add(node.id)
    if (node.children) {
      node.children.forEach(collectChildren)
    }
  }

  if (currentId.value) {
    const findAndCollect = (nodes) => {
      for (const node of nodes) {
        if (node.id === currentId.value) {
          collectChildren(node)
          return true
        }
        if (node.children && findAndCollect(node.children)) {
          return true
        }
      }
      return false
    }
    findAndCollect(treeData.value)
  }

  const filterTree = (nodes) => {
    return nodes
      .filter(node => !excludeIds.has(node.id))
      .map(node => ({
        ...node,
        children: node.children ? filterTree(node.children) : undefined
      }))
  }

  return filterTree(JSON.parse(JSON.stringify(treeData.value)))
})

// 加载树形数据
const load = async () => {
  try {
    const res = await categoryApi.getTree()
    // 后端已经返回嵌套的树形结构 { tree: [...] }
    treeData.value = cleanEmptyChildren(res.data.tree || [])
  } catch (e) {
    console.error('加载分类树失败:', e)
    ElMessage.error('加载分类树失败')
  }
}

// 清理空 children 数组
const cleanEmptyChildren = (nodes) => {
  return nodes.map(node => {
    const cleaned = { ...node }
    if (cleaned.children && cleaned.children.length > 0) {
      cleaned.children = cleanEmptyChildren(cleaned.children)
    } else {
      delete cleaned.children
    }
    return cleaned
  })
}

// 防止循环引用和非法拖拽
const allowDrop = (draggingNode, dropNode, type) => {
  // 不允许拖拽到自己的子节点中
  if (type === 'inner') {
    const isDescendant = (node, targetId) => {
      if (node.id === targetId) return true
      if (node.children) {
        return node.children.some(child => isDescendant(child, targetId))
      }
      return false
    }
    return !isDescendant(draggingNode, dropNode.id)
  }
  return true
}

// 处理节点拖拽
const handleDrop = async (draggingNode, dropNode, dropType) => {
  try {
    // 跨级移动：移动到其他节点下
    if (dropType === 'inner') {
      await categoryApi.moveToCategory(draggingNode.data.id, dropNode.data.id)
      ElMessage.success('移动成功')
    } else {
      // 同级排序：调用排序接口
      const siblings = dropNode.parent.childNodes
      const orderData = siblings.map((node, index) => ({
        id: node.data.id,
        sortOrder: index
      }))
      await categoryApi.reorder(orderData)
      ElMessage.success('排序成功')
    }
    await load()
    emit('refresh')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
    await load() // 刷新恢复原状
  }
}

// 编辑节点
const edit = (data) => {
  isEdit.value = true
  currentId.value = data.id
  form.value = {
    name: data.name,
    description: data.description || '',
    parentId: data.parentId,
    sortOrder: data.sortOrder || 0
  }
  editDialogVisible.value = true
}

// 删除节点
const remove = async (data) => {
  const hasChildren = data.children && data.children.length > 0

  try {
    let message = '确定删除该分类吗？'
    let cascade = false

    if (hasChildren) {
      const result = await ElMessageBox.confirm(
        `该分类包含 ${data.children.length} 个子分类，是否同时删除？`,
        '确认删除',
        {
          distinguishCancelAndClose: true,
          confirmButtonText: '全部删除',
          cancelButtonText: '仅删除当前',
          type: 'warning'
        }
      )
      cascade = result === 'confirm'
    } else {
      await ElMessageBox.confirm(message, '确认删除', { type: 'warning' })
    }

    await categoryApi.delete(data.id, cascade)
    ElMessage.success('删除成功')
    await load()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

// 保存表单
const save = async () => {
  try {
    const data = {
      name: form.value.name,
      description: form.value.description,
      parentId: form.value.parentId || null,
      sortOrder: form.value.sortOrder
    }

    if (isEdit.value) {
      await categoryApi.update(currentId.value, data)
      ElMessage.success('更新成功')
    } else {
      await categoryApi.create(data)
      ElMessage.success('创建成功')
    }

    editDialogVisible.value = false
    await load()
    emit('refresh')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

// 新建节点
const create = () => {
  isEdit.value = false
  currentId.value = null
  form.value = {
    name: '',
    description: '',
    parentId: null,
    sortOrder: 0
  }
  editDialogVisible.value = true
}

// 暴露方法
defineExpose({
  load,
  create
})
</script>

<style scoped>
.category-tree {
  padding: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  padding-right: 20px;
}

.node-label {
  flex: 1;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node:hover .node-actions {
  opacity: 1;
}

:deep(.el-tree-node__content) {
  height: 36px;
}
</style>
