<script setup lang="ts">
defineProps<{ loading?: boolean; error?: string; notice?: string }>()
defineEmits<{ retry: [] }>()
</script>
<template>
  <div v-if="error" class="admin-feedback admin-feedback-error" role="alert"><span>{{ error }}</span><button type="button" class="button button-secondary" @click="$emit('retry')">重试</button></div>
  <div v-if="notice" class="admin-feedback admin-feedback-success" role="status">{{ notice }}</div>
  <div v-if="loading" class="admin-loading" role="status"><span></span>正在加载业务数据…</div>
</template>
