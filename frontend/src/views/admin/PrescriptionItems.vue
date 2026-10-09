<script setup lang="ts">
import EmptyState from '@/components/EmptyState.vue'
import { money } from '@/lib/format'
import type { AdminData } from './useAdminPage'
defineProps<{ items: AdminData[]; total?: unknown }>()
</script>
<template><EmptyState v-if="!items.length" title="暂无药品明细" /><div v-else class="table-scroll"><table class="admin-table"><thead><tr><th>药品 / 规格</th><th>数量</th><th>用法用量</th><th>单价（元）</th><th>金额（元）</th></tr></thead><tbody><tr v-for="item in items" :key="item.id"><td><strong>{{ item.medicineName }}</strong><small>{{ item.specification }}</small></td><td>{{ item.quantity }} {{ item.unit }}</td><td class="admin-description">{{ [item.dosage,item.frequency,item.usage].filter(Boolean).join('，') || '未填写' }}</td><td>{{ money(item.price) }}</td><td>{{ money(item.totalPrice) }}</td></tr></tbody></table></div><div class="admin-total"><span>处方合计</span><strong>¥{{ money(total) }}</strong></div></template>
