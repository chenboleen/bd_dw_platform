<template>
  <slot v-if="hasPermission" />
  <slot v-else name="fallback">
    <el-tooltip :content="tooltipContent" placement="top">
      <span class="permission-disabled">
        <slot />
      </span>
    </el-tooltip>
  </slot>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = withDefaults(defineProps<{
  action: 'read' | 'update' | 'delete' | 'create_catalog'
  tooltipContent?: string
}>(), {
  tooltipContent: '权限不足，无法执行此操作'
})

const authStore = useAuthStore()
const hasPermission = computed(() => authStore.hasPermission(props.action))
</script>

<style scoped>
.permission-disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}
</style>
