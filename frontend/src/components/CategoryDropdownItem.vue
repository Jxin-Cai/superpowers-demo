<template>
  <div class="category-dropdown-item">
    <template v-for="category in categories" :key="category.id">
      <template v-if="category.children && category.children.length > 0">
        <el-sub-menu :index="String(category.id)">
          <template #title>
            <router-link :to="`/category/${category.id}`" @click.stop>
              {{ category.name }}
            </router-link>
          </template>
          <CategoryDropdownItem
            :categories="category.children"
            :level="level + 1"
            @navigate="$emit('navigate', $event)"
          />
        </el-sub-menu>
      </template>
      <template v-else>
        <el-dropdown-item :command="`/category/${category.id}`">
          {{ category.name }}
        </el-dropdown-item>
      </template>
    </template>
  </div>
</template>

<script setup>
const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  level: {
    type: Number,
    default: 0
  }
})

defineEmits(['navigate'])
</script>

<style scoped>
.category-dropdown-item {
  min-width: 200px;
}

/* 根据层级设置不同的样式 */
.category-dropdown-item :deep(.el-dropdown-menu__item) {
  padding-left: calc(20px + (v-bind('level') * 20px));
}
</style>
