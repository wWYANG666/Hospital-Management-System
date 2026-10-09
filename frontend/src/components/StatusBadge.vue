<script setup lang="ts">
import { computed } from "vue";
import { statusText, text } from "@/lib/format";
const props = defineProps<{ status?: unknown }>();
const tone = computed(() => {
  const key = text(props.status);
  if (
    ["COMPLETED", "APPROVED", "AVAILABLE", "DISPENSED", "1", "已支付"].includes(
      key,
    )
  )
    return "success";
  if (["CANCELLED", "REJECTED", "STOPPED", "MAINTENANCE", "0"].includes(key))
    return "muted";
  if (["PENDING", "待缴费", "未支付"].includes(key)) return "warning";
  return "info";
});
</script>
<template>
  <span class="status-badge" :class="'status-' + tone"
    ><i></i>{{ statusText(status) }}</span
  >
</template>
