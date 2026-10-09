<script setup lang="ts">
withDefaults(defineProps<{ loading?: boolean; error?: string; notice?: string; reloadAvailable?: boolean }>(), { reloadAvailable: true });
defineEmits<{ retry: [] }>();
</script>

<template>
  <div v-if="error" class="doctor-alert doctor-alert-error" role="alert">
    <span>{{ error }}</span>
    <button
      v-if="reloadAvailable"
      type="button"
      class="button button-secondary"
      @click="$emit('retry')"
    >
      重新加载
    </button>
  </div>
  <div v-if="notice" class="doctor-alert doctor-alert-success" role="status">
    {{ notice }}
  </div>
  <div v-if="loading" class="doctor-loading" role="status">
    <span class="doctor-loading-dot"></span> 正在加载诊疗数据
  </div>
</template>
