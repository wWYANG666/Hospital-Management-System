<script setup lang="ts">
import { notices, dismiss } from "@/lib/notifications";
import UiIcon from "./UiIcon.vue";
</script>
<template>
  <div class="toast-host" aria-live="polite">
    <div
      v-for="notice in notices"
      :key="notice.id"
      class="toast"
      :class="'toast-' + notice.type"
    >
      <UiIcon
        :name="notice.type === 'error' ? 'alert' : 'check'"
        :size="18"
      /><span>{{ notice.text }}</span
      ><button @click="dismiss(notice.id)" aria-label="关闭消息">
        <UiIcon name="close" :size="16" />
      </button>
    </div>
  </div>
</template>
