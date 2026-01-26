<template>
  <div>
    <h2>{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-width="80px" style="margin-top: 20px; max-width: 800px">
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
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="20"
          placeholder="请输入 Markdown 内容"
        />
      </el-form-item>

      <el-form-item label="关键词">
        <el-input
          v-model="form.keywords"
          placeholder="请输入关键词，用逗号分隔（如：Java,Spring,编程）"
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
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const categories = ref([])

const form = ref({
  title: '',
  content: '',
  categoryId: null,
  keywords: ''
})

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
    const res = await articleApi.adminGetById(id.value)
    const article = res.data
    form.value = {
      title: article.title,
      content: article.content,
      categoryId: article.categoryId,
      keywords: article.keywords || ''
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  if (!form.value.title || !form.value.content || !form.value.categoryId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  const data = {
    title: form.value.title,
    content: form.value.content,
    categoryId: form.value.categoryId,
    keywords: form.value.keywords
  }
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

onMounted(() => {
  loadCategories()
  load()
})
</script>
