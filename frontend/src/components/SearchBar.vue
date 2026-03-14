<template>
  <div class="search-bar">
    <el-input
      v-model="keyword"
      :placeholder="placeholder"
      size="large"
      clearable
      @keyup.enter="handleSearch"
      @clear="handleClear"
      @input="handleInput"
    >
      <template #prefix>
        <el-icon class="search-icon"><Search /></el-icon>
      </template>
      <template #append>
        <el-button type="primary" @click="handleSearch" :loading="loading">
          搜索
        </el-button>
      </template>
    </el-input>

    <!-- 搜索建议 -->
    <div v-if="showSuggestions && suggestions.length > 0" class="suggestions-panel" role="listbox" aria-label="搜索建议">
      <div
        v-for="(suggestion, index) in suggestions"
        :key="index"
        class="suggestion-item"
        role="option"
        :aria-selected="false"
        @click="selectSuggestion(suggestion)"
        @keydown.enter="selectSuggestion(suggestion)"
        tabindex="0"
      >
        <el-icon><Search /></el-icon>
        <span>{{ suggestion }}</span>
      </div>
    </div>

    <!-- 搜索历史 -->
    <div v-if="showHistory && searchHistory.length > 0 && !keyword" class="history-panel">
      <div class="history-header">
        <span>搜索历史</span>
        <el-button link size="small" @click="clearHistory">清除</el-button>
      </div>
      <div class="history-tags">
        <el-tag
          v-for="(item, index) in searchHistory.slice(0, 8)"
          :key="index"
          size="small"
          class="history-tag"
          @click="selectSuggestion(item)"
          style="cursor: pointer"
        >
          {{ item }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useSearchStore } from '@/stores/search'
import { useDebounceFn } from '@vueuse/core'

const props = withDefaults(defineProps<{
  placeholder?: string
  loading?: boolean
  showHistory?: boolean
}>(), {
  placeholder: '搜索表名、字段名、描述...',
  loading: false,
  showHistory: true
})

const emit = defineEmits<{
  search: [keyword: string]
  clear: []
}>()

const searchStore = useSearchStore()
const keyword = ref('')
const showSuggestions = ref(false)
const suggestions = ref<string[]>([])
const searchHistory = computed(() => searchStore.searchHistory)

// 防抖获取建议
const debouncedFetchSuggestions = useDebounceFn(async (value: string) => {
  if (value.length >= 2) {
    await searchStore.fetchSuggestions(value)
    suggestions.value = searchStore.suggestions
    showSuggestions.value = true
  } else {
    showSuggestions.value = false
  }
}, 300)

function handleInput(value: string) {
  debouncedFetchSuggestions(value)
}

function handleSearch() {
  showSuggestions.value = false
  emit('search', keyword.value)
}

function handleClear() {
  showSuggestions.value = false
  emit('clear')
}

function selectSuggestion(value: string) {
  keyword.value = value
  showSuggestions.value = false
  emit('search', value)
}

function clearHistory() {
  searchStore.searchHistory.splice(0)
}

// 点击外部关闭建议
function handleClickOutside() {
  showSuggestions.value = false
}

// 暴露方法
defineExpose({ keyword })
</script>

<style scoped>
.search-bar {
  position: relative;
  width: 100%;
}

.search-icon {
  color: var(--color-text-muted);
}

.suggestions-panel,
.history-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 1000;
  overflow: hidden;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 13px;
  transition: background-color var(--transition-fast);
}

.suggestion-item:hover {
  background-color: #EFF6FF;
  color: var(--color-primary);
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid var(--color-border-light);
  font-size: 12px;
  color: var(--color-text-muted);
}

.history-tags {
  padding: 8px 16px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.history-tag {
  cursor: pointer;
}

.history-tag:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
</style>
