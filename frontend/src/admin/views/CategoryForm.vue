<template>
  <div style="max-width: 600px">
    <h2>{{ isEdit ? '编辑分类' : '新建分类' }}</h2>

    <el-form :model="form" label-width="100px" style="margin-top: 20px">
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
          placeholder="请选择父分类（不选则为根分类）"
          clearable
          check-strictly
          :render-after-expand="false"
        />
      </el-form-item>

      <el-form-item label="排序序号">
        <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const form = ref({
  name: '',
  description: '',
  parentId: null,
  sortOrder: 0
})

const allCategories = ref([])

// 用于选择器的树形数据（排除当前编辑节点及其子节点）
const treeDataForSelect = computed(() => {
  const buildTree = (parentId = null) => {
    return allCategories.value
      .filter(c => c.parentId === parentId)
      .map(c => ({
        id: c.id,
        name: c.name,
        children: buildTree(c.id)
      }))
  }
  return buildTree()
})

const loadCategories = async () => {
  try {
    const res = await categoryApi.adminGetAll()
    allCategories.value = res || []
  } catch (e) {
    console.error('加载分类列表失败', e)
  }
}

const load = async () => {
  await loadCategories()
  if (!isEdit.value) return

  try {
    const res = await categoryApi.adminGetAll()
    const category = res.find(c => c.id === Number(id.value))
    if (category) {
      form.value = {
        name: category.name,
        description: category.description || '',
        parentId: category.parentId,
        sortOrder: category.sortOrder || 0
      }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  try {
    const data = {
      name: form.value.name,
      description: form.value.description,
      parentId: form.value.parentId || null,
      sortOrder: form.value.sortOrder
    }

    if (isEdit.value) {
      await categoryApi.update(id.value, data)
    } else {
      await categoryApi.create(data)
    }
    ElMessage.success('保存成功')
    router.push('/admin/categories')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

onMounted(load)
</script>
