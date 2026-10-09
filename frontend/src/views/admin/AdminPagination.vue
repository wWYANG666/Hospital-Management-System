<script setup lang="ts">
defineProps<{ page: number; totalPages: number; total: number }>()
defineEmits<{ 'update:page': [value: number] }>()
</script>
<template>
  <nav v-if="totalPages > 1" class="admin-pagination" aria-label="列表分页">
    <span>共 {{ total }} 条 · 第 {{ page }} / {{ totalPages }} 页</span>
    <div>
      <button type="button" class="button button-secondary" :disabled="page <= 1" @click="$emit('update:page', page - 1)">上一页</button>
      <button type="button" class="button button-secondary" :disabled="page >= totalPages" @click="$emit('update:page', page + 1)">下一页</button>
    </div>
  </nav>
</template>
