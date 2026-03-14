<template>
  <div class="layout-container">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-logo">
        <svg class="logo-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path d="M4 6C4 4.89543 4.89543 4 6 4H18C19.1046 4 20 4.89543 20 6V8C20 9.10457 19.1046 10 18 10H6C4.89543 10 4 9.10457 4 8V6Z" fill="currentColor" opacity="0.8"/>
          <path d="M4 12C4 10.8954 4.89543 10 6 10H18C19.1046 10 20 10.8954 20 12V14C20 15.1046 19.1046 16 18 16H6C4.89543 16 4 15.1046 4 14V12Z" fill="currentColor" opacity="0.6"/>
          <path d="M4 18C4 16.8954 4.89543 16 6 16H18C19.1046 16 20 16.8954 20 18V20C20 21.1046 19.1046 22 18 22H6C4.89543 22 4 21.1046 4 20V18Z" fill="currentColor" opacity="0.4"/>
        </svg>
        <span v-if="!sidebarCollapsed" class="logo-text">元数据管理</span>
      </div>

      <nav class="sidebar-nav" role="navigation" aria-label="主导航">
        <el-menu
          :default-active="activeMenu"
          :collapse="sidebarCollapsed"
          background-color="#1E3A8A"
          text-color="#CBD5E1"
          active-text-color="#F59E0B"
          router
        >
          <el-menu-item index="/tables">
            <el-icon><Grid /></el-icon>
            <template #title>表元数据</template>
          </el-menu-item>
          <el-menu-item index="/lineage">
            <el-icon><Share /></el-icon>
            <template #title>血缘关系</template>
          </el-menu-item>
          <el-menu-item index="/search">
            <el-icon><Search /></el-icon>
            <template #title>全文搜索</template>
          </el-menu-item>
          <el-menu-item index="/catalog">
            <el-icon><FolderOpened /></el-icon>
            <template #title>数据目录</template>
          </el-menu-item>
          <el-menu-item index="/quality">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>数据质量</template>
          </el-menu-item>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon>
            <template #title>变更历史</template>
          </el-menu-item>
          <el-menu-item index="/import-export">
            <el-icon><Upload /></el-icon>
            <template #title>导入导出</template>
          </el-menu-item>
          <el-menu-item v-if="authStore.isAdmin" index="/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
        </el-menu>
      </nav>

      <button
        class="sidebar-toggle"
        @click="sidebarCollapsed = !sidebarCollapsed"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
      >
        <el-icon>
          <ArrowLeft v-if="!sidebarCollapsed" />
          <ArrowRight v-else />
        </el-icon>
      </button>
    </aside>

    <!-- 主内容区 -->
    <div class="main-wrapper">
      <!-- 顶部导航栏 -->
      <header class="header" role="banner">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 快速搜索 -->
          <el-input
            v-model="quickSearch"
            placeholder="快速搜索..."
            prefix-icon="Search"
            size="small"
            class="header-search"
            @keyup.enter="handleQuickSearch"
            clearable
          />

          <!-- 用户菜单 -->
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-avatar" role="button" tabindex="0" :aria-label="`用户: ${authStore.user?.username}`">
              <el-avatar :size="32" :style="{ backgroundColor: '#1E40AF' }">
                {{ authStore.user?.username?.charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ authStore.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <span class="user-role-badge">{{ roleLabel }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <main class="main-content" role="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)
const quickSearch = ref('')

// 当前激活菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/tables')) return '/tables'
  if (path.startsWith('/lineage')) return '/lineage'
  if (path.startsWith('/search')) return '/search'
  if (path.startsWith('/catalog')) return '/catalog'
  if (path.startsWith('/quality')) return '/quality'
  if (path.startsWith('/history')) return '/history'
  if (path.startsWith('/import-export')) return '/import-export'
  if (path.startsWith('/users')) return '/users'
  return path
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  return matched.map(r => ({ path: r.path, title: r.meta?.title as string }))
})

// 角色标签
const roleLabel = computed(() => {
  const roleMap: Record<string, string> = {
    ADMIN: '管理员',
    DEVELOPER: '开发者',
    GUEST: '访客'
  }
  return roleMap[authStore.user?.role || ''] || '未知角色'
})

// 快速搜索
function handleQuickSearch() {
  if (quickSearch.value.trim()) {
    router.push({ path: '/search', query: { keyword: quickSearch.value } })
    quickSearch.value = ''
  }
}

// 用户菜单命令
async function handleUserCommand(command: string) {
  if (command === 'logout') {
    await authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  width: 220px;
  min-width: 220px;
  background-color: #1E3A8A;
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow), min-width var(--transition-slow);
  overflow: hidden;
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: white;
  min-height: 60px;
}

.logo-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  color: #F59E0B;
}

.logo-text {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 15px;
  white-space: nowrap;
  color: white;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background: none;
  border: none;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #94A3B8;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.sidebar-toggle:hover {
  color: white;
}

/* 主内容区 */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* 顶部导航栏 */
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  background: white;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-search {
  width: 200px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: background-color var(--transition-fast);
}

.user-avatar:hover {
  background-color: var(--color-border-light);
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.user-role-badge {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* 主内容 */
.main-content {
  flex: 1;
  overflow-y: auto;
  background-color: var(--color-bg);
}
</style>
