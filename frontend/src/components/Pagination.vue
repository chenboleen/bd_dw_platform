<template>
  <div class="pagination-wrapper" role="navigation" aria-label="分页导航">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="currentPageSize"
      :total="total"
      :page-sizes="pageSizes"
      layout="total, sizes, prev, pager, next, jumper"
      background
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  total: number
  page?: number
  pageSize?: number
  pageSizes?: number[]
}>(), {
  page: 1,
  pageSize: 20,
  pageSizes: () => [10, 20, 50, 100]
})

const emit = defineEmits<{
  change: [page: number, pageSize: number]
}>()

const currentPage = ref(props.page)
const currentPageSize = ref(props.pageSize)

watch(() => props.page, (val) => { currentPage.value = val })
watch(() => props.pageSize, (val) => { currentPageSize.value = val })

function handleSizeChange(size: number) {
  currentPageSize.value = size
  currentPage.value = 1
  emit('change', 1, size)
}

function handleCurrentChange(page: number) {
  currentPage.value = page
  emit('change', page, currentPageSize.value)
}
</script>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
