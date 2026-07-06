<template>
  <div class="main-layout">
    <header class="main-header">
      <router-link to="/" class="header-brand">
        <span class="brand-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <rect width="24" height="24" rx="5" fill="#3451b2" />
            <path d="M7 8l5 4-5 4V8z" fill="#fff" opacity="0.9" />
            <path d="M13 8l4 4-4 4" stroke="#fff" stroke-width="1.8" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="0.7" />
          </svg>
        </span>
        <span class="brand-text">CodeReviewBot</span>
      </router-link>

      <nav class="header-nav">
        <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
          <el-icon :size="16"><Document /></el-icon>
          <span>审查代码</span>
        </router-link>
        <router-link to="/history" class="nav-item" :class="{ active: route.path === '/history' }">
          <el-icon :size="16"><Clock /></el-icon>
          <span>历史记录</span>
        </router-link>
      </nav>
    </header>

    <main class="main-body">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Document, Clock } from '@element-plus/icons-vue'

const route = useRoute()
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
}

.brand-icon {
  display: flex;
  align-items: center;
}

.brand-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.15s;
}

.nav-item:hover {
  background: #f5f5f7;
  color: var(--text-primary);
}

.nav-item.active {
  background: #edf0ff;
  color: var(--brand-color);
  font-weight: 500;
}

.main-body {
  flex: 1;
  max-width: 1440px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 24px;
}

@media (max-width: 768px) {
  .main-header {
    padding: 0 16px;
  }
  .main-body {
    padding: 16px;
  }
}
</style>
