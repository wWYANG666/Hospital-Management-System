<script setup lang="ts">
import UiIcon from "@/components/UiIcon.vue";
import "@/styles/staff-portal.css";
</script>

<template>
  <div class="staff-portal">
    <a class="staff-skip-link" href="#staff-main">跳到主要内容</a>
    <header class="staff-header">
      <div class="staff-header-inner">
        <RouterLink to="/app" class="staff-brand" aria-label="医院医务管理平台首页">
          <span class="staff-brand-icon"><UiIcon name="hospital" :size="24" /></span>
          <span><strong>医院医务管理平台</strong><small>院内业务系统</small></span>
        </RouterLink>
      </div>
    </header>
    <RouterView />
    <footer class="staff-footer">
      <span>仅供医院授权人员使用</span>
      <span>医院医务管理平台</span>
    </footer>
  </div>
</template>
