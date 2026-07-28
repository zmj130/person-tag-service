<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, Users, Tags, ClipboardCheck, RefreshCw, Database, ListFilter, FileSpreadsheet, SearchCheck, Workflow } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const mobileNav = ref(false)
const navItems = [
  { path: '/persons', label: '人员管理', icon: Users },
  { path: '/profiles', label: '用户画像', icon: SearchCheck },
  { path: '/tags', label: '标签管理', icon: Tags },
  { path: '/indicators', label: '指标管理', icon: ListFilter },
  { path: '/rules', label: '规则管理', icon: Workflow },
  { path: '/reviews', label: '标签审核', icon: ClipboardCheck },
  { path: '/imports', label: '人员导入', icon: FileSpreadsheet },
  { path: '/sync', label: '同步管理', icon: RefreshCw }
]
const title = computed(() => route.meta.title || '人员标签管理')

function navigate(path) {
  router.push(path)
  mobileNav.value = false
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark"><Database :size="20" /></span><span>人员标签管理</span></div>
      <nav class="nav-list" aria-label="主导航">
        <button v-for="item in navItems" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="navigate(item.path)">
          <component :is="item.icon" :size="18" /><span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="sidebar-foot"><span class="health-dot"></span>服务管理端</div>
    </aside>

    <section class="main-shell">
      <header class="topbar">
        <el-button class="mobile-menu" text circle aria-label="打开导航" @click="mobileNav = true"><Menu :size="21" /></el-button>
        <div><h1>{{ title }}</h1><p>人员标签服务</p></div>
      </header>
      <main class="page-content"><router-view /></main>
    </section>

    <el-drawer v-model="mobileNav" direction="ltr" size="260px" :with-header="false" class="mobile-drawer">
      <div class="brand"><span class="brand-mark"><Database :size="20" /></span><span>人员标签管理</span></div>
      <nav class="nav-list">
        <button v-for="item in navItems" :key="item.path" class="nav-item" :class="{ active: route.path === item.path }" @click="navigate(item.path)">
          <component :is="item.icon" :size="18" /><span>{{ item.label }}</span>
        </button>
      </nav>
    </el-drawer>
  </div>
</template>
