<template>
  <div style="max-width: 600px">
    <h2>{{ isEdit ? '编辑分类' : '新建分类' }}</h2>

    <el-form :model="form" label-width="80px" style="margin-top: 20px">
      <el-form-item label="名称">
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
  description: ''
})

const load = async () => {
  if (!isEdit.value) return
  try {
    const res = await categoryApi.adminGetAll()
    const category = res.data.find(c => c.id === Number(id.value))
    if (category) {
      form.value = { name: category.name, description: category.description }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  try {
    if (isEdit.value) {
      await categoryApi.update(id.value, form.value)
    } else {
      await categoryApi.create(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/admin/categories')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

onMounted(load)
</script>
