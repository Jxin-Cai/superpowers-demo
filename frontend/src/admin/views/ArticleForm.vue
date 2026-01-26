<template>
  <div style="max-width: 900px; margin: 0 auto; padding: 20px;">
    <h2>{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-width="100px" style="margin-top: 20px">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>

      <el-form-item label="分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="内容">
        <MdEditor
          v-model="form.content"
          @onUploadImg="handleImageUpload"
          :toolbars="toolbars"
          style="height: 500px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="save">{{ isEdit ? '更新' : '创建草稿' }}</el-button>
        <el-button @click="publish" v-if="!isEdit || form.status === 'DRAFT'">发布</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const form = ref({
  title: '',
  content: '',
  categoryId: null,
  status: 'DRAFT'
})

const categories = ref([])

const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough',
  '-', 'title', 'sub', 'sup', 'quote',
  '-', 'unorderedList', 'orderedList', 'task',
  '-', 'codeRow', 'code',
  '-', 'link', 'image', 'table',
  '-', 'revoke', 'next', 'save',
  '=', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview'
]

const handleImageUpload = async (files, callback) => {
  const base64Images = await Promise.all(
    files.map(file => fileToBase64(file))
  )
  callback(base64Images)
}

const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.adminGetAll()
    categories.value = res.data
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const load = async () => {
  if (!isEdit.value) return
  try {
    const res = await articleApi.adminGetAll()
    const article = res.data.find(a => a.id === Number(id.value))
    if (article) {
      form.value = {
        title: article.title,
        content: article.content,
        categoryId: article.categoryId,
        status: article.status
      }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  try {
    if (isEdit.value) {
      await articleApi.update(id.value, form.value)
    } else {
      await articleApi.create(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/admin/articles')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

const publish = async () => {
  try {
    const data = { ...form.value, status: 'PUBLISHED' }
    if (isEdit.value) {
      await articleApi.update(id.value, data)
    } else {
      await articleApi.create(data)
    }
    ElMessage.success('发布成功')
    router.push('/admin/articles')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>
