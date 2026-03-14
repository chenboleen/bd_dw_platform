<template>
  <div class="login-page" role="main">
    <div class="login-bg">
      <!-- 背景装饰 -->
      <div class="bg-grid" aria-hidden="true"></div>
    </div>

    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="login-brand" aria-hidden="true">
        <div class="brand-content">
          <div class="brand-logo">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="8" width="40" height="10" rx="3" fill="#F59E0B"/>
              <rect x="4" y="22" width="40" height="10" rx="3" fill="#3B82F6" opacity="0.8"/>
              <rect x="4" y="36" width="40" height="8" rx="3" fill="#1E40AF" opacity="0.6"/>
            </svg>
          </div>
          <h1 class="brand-title">数据仓库元数据管理系统</h1>
          <p class="brand-subtitle">统一管理企业数据资产，追踪数据血缘，保障数据质量</p>

          <div class="brand-features">
            <div class="feature-item" v-for="feature in features" :key="feature.title">
              <div class="feature-icon">
                <component :is="feature.icon" />
              </div>
              <div>
                <div class="feature-title">{{ feature.title }}</div>
                <div class="feature-desc">{{ feature.desc }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-form-wrapper">
        <div class="login-form-card">
          <div class="form-header">
            <h2>欢迎登录</h2>
            <p>请输入您的账号信息</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            @submit.prevent="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                prefix-icon="User"
                autocomplete="username"
                :disabled="loading"
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
                autocomplete="current-password"
                :disabled="loading"
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                size="large"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
                native-type="submit"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <p class="hint-text">
              <el-icon><InfoFilled /></el-icon>
              默认管理员账号: admin / admin123
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { FormInstance, FormRules } from 'element-plus'
import { Grid, Share, Search, DataAnalysis } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度为 2-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
  ]
}

const features = [
  {
    icon: Grid,
    title: '元数据管理',
    desc: '统一管理表、字段等元数据信息'
  },
  {
    icon: Share,
    title: '血缘追踪',
    desc: '可视化数据流转和依赖关系'
  },
  {
    icon: Search,
    title: '全文搜索',
    desc: '快速定位所需数据资产'
  },
  {
    icon: DataAnalysis,
    title: '质量监控',
    desc: '实时监控数据质量指标'
  }
]

async function handleLogin() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  const success = await authStore.login(form)
  loading.value = false

  if (success) {
    router.push('/')
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: fixed;
  inset: 0;
  background: linear-gradient(135deg, #1E3A8A 0%, #1E40AF 50%, #2563EB 100%);
  z-index: 0;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.05) 1px, transparent 1px);
  background-size: 40px 40px;
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  min-height: 100vh;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  color: white;
}

.brand-content {
  max-width: 480px;
}

.brand-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
  line-height: 1.3;
}

.brand-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: 40px;
  line-height: 1.6;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.feature-icon {
  width: 40px;
  height: 40px;
  background: rgba(245, 158, 11, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #F59E0B;
  font-size: 20px;
  flex-shrink: 0;
}

.feature-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 2px;
}

.feature-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.65);
}

/* 右侧登录表单 */
.login-form-wrapper {
  width: 480px;
  min-width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  padding: 48px;
}

.login-form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
}

.form-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.form-header p {
  color: var(--color-text-muted);
  font-size: 14px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.login-btn:hover {
  background-color: var(--color-primary-light);
  border-color: var(--color-primary-light);
}

.form-footer {
  margin-top: 20px;
}

.hint-text {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-muted);
  background: #F8FAFC;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
}

/* 响应式 */
@media (max-width: 768px) {
  .login-brand {
    display: none;
  }

  .login-form-wrapper {
    width: 100%;
    min-width: unset;
    padding: 24px;
  }
}
</style>
