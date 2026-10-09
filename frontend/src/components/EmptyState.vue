<script setup lang="ts">
import UiIcon from "./UiIcon.vue";
withDefaults(
  defineProps<{ title?: string; description?: string; icon?: string }>(),
  { title: "暂无记录", description: "", icon: "clipboard" },
);
</script>
<template>
  <div class="empty-state">
    <span class="empty-icon"><UiIcon :name="icon" :size="30" /></span>
    <h3>{{ title }}</h3>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
