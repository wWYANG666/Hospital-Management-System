<script setup lang="ts">
import UiIcon from "./UiIcon.vue";
defineProps<{ message: string }>();
defineEmits<{ retry: [] }>();
</script>
<template>
  <div class="error-state" role="alert">
    <UiIcon name="alert" :size="28" />
    <h3>暂时无法加载</h3>
    <p>{{ message }}</p>
    <button class="button button-secondary" @click="$emit('retry')">
      <UiIcon name="refresh" :size="16" />重新加载
    </button>
  </div>
</template>
