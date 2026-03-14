// 路由配置
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 懒加载路由组件
const Login = () => import('@/views/Login.vue')
const Layout = () => import('@/components/Layout.vue')
const TableList = () => import('@/views/TableList.vue')
const TableDetail = () => import('@/views/TableDetail.vue')
const LineageGraph = () => import('@/views/LineageGraph.vue')
const Search = () => import('@/views/Search.vue')
const Catalog = () => import('@/views/Catalog.vue')
const Quality = () => import('@/views/Quality.vue')
const History = () => import('@/views/History.vue')
const ImportExport = () => import('@/views/ImportExport.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/',
      component: Layout,
      redirect: '/tables',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'tables',
          name: 'TableList',
          component: TableList,
          meta: { title: '表元数据', requiresAuth: true }
        },
        {
          path: 'tables/:id',
          name: 'TableDetail',
          component: TableDetail,
          meta: { title: '表详情', requiresAuth: true }
        },
        {
          path: 'lineage',
          name: 'LineageGraph',
          component: LineageGraph,
          meta: { title: '血缘关系', requiresAuth: true }
        },
        {
          path: 'search',
          name: 'Search',
          component: Search,
          meta: { title: '全文搜索', requiresAuth: true }
        },
        {
          path: 'catalog',
          name: 'Catalog',
          component: Catalog,
          meta: { title: '数据目录', requiresAuth: true }
        },
        {
          path: 'quality',
          name: 'Quality',
          component: Quality,
          meta: { title: '数据质量', requiresAuth: true }
        },
        {
          path: 'history',
          name: 'History',
          component: History,
          meta: { title: '变更历史', requiresAuth: true }
        },
        {
          path: 'import-export',
          name: 'ImportExport',
          component: ImportExport,
          meta: { title: '导入导出', requiresAuth: true }
        }
      ]
    },
    // 404 重定向
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

// 路由守卫 - 认证检查
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 元数据管理系统` : '元数据管理系统'

  // 不需要认证的页面直接放行
  if (to.meta.requiresAuth === false) {
    // 已登录用户访问登录页，重定向到首页
    if (authStore.isAuthenticated && to.path === '/login') {
      next('/')
    } else {
      next()
    }
    return
  }

  // 需要认证
  if (!authStore.isAuthenticated) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已认证但没有用户信息，尝试获取
  if (!authStore.user) {
    await authStore.fetchCurrentUser()
    if (!authStore.user) {
      next({ path: '/login' })
      return
    }
  }

  next()
})

export default router
