<script setup lang="ts">
import UiIcon from "@/components/UiIcon.vue";
import { isPatientPortal } from "@/lib/portal";
</script>
<template>
  <main class="not-found">
    <span>404</span>
    <h1>页面未找到</h1>
    <p>
      地址可能已经变更，请从{{
        isPatientPortal ? "患者服务首页" : "医务首页"
      }}重新进入。
    </p>
    <RouterLink class="button button-primary" to="/app"
      ><UiIcon name="hospital" :size="18" />返回服务首页</RouterLink
    >
  </main>
</template>
