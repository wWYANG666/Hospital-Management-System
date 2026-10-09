<script setup lang="ts">
defineProps<{ loading: boolean; error?: string; message?: string }>();
defineEmits<{ retry: [] }>();
</script>
<template>
  <div v-if="loading" class="patient-loading" role="status">
    <span class="patient-spinner"></span>正在加载就医信息…
  </div>
  <div v-if="error" class="patient-notice patient-notice-error" role="alert">
    <span>{{ error }}</span
    ><button
      type="button"
      class="button button-secondary"
      @click="$emit('retry')"
    >
      重新加载
    </button>
  </div>
  <div
    v-if="message"
    class="patient-notice patient-notice-success"
    role="status"
  >
    {{ message }}
  </div>
</template>
