<script setup lang="ts">
import { computed, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import AdminFeedback from './AdminFeedback.vue'
import { useAdminPage, items, type AdminData } from './useAdminPage'
import { submitAction } from '@/lib/api'

const { data, loading, error, load } = useAdminPage(() => '/admin/dashboard')
const summary = ref(''), summaryLoading = ref(false), summaryError = ref('')
const lowStock = computed(() => [...items(data.value, 'lowStockMedicines')].sort((a, b) => Number(a.stock || 0) - Number(b.stock || 0)))
const occupancy = computed(() => Math.max(0, Math.min(100, Number(data.value.bedOccupancyRate || 0))))
const pendingCount = computed(() => Number(data.value.pendingHospitalizations || 0))
const dateLabel = new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
async function generate() {
  if (summaryLoading.value) return
  summaryLoading.value = true; summaryError.value = ''
  try { const result = await submitAction<AdminData>('/admin/ai/summary'); summary.value = String(result.summary || '') }
  catch (cause) { summaryError.value = cause instanceof Error ? cause.message : '摘要生成失败' }
  finally { summaryLoading.value = false }
}
</script>
<template>
  <div class="admin-view admin-dashboard">
    <PageHeader title="运营工作台" description="先处理待办，再查看今日运行情况。">
      <span class="admin-date"><UiIcon name="calendar" :size="16" />{{ dateLabel }}</span>
      <button class="button button-secondary" :disabled="loading" @click="load"><UiIcon name="refresh" :size="16" />刷新</button>
    </PageHeader>
    <AdminFeedback :loading="loading" :error="error" @retry="load" />
    <template v-if="!loading && !error">
      <section class="admin-metrics admin-dashboard-summary" aria-label="今日运营概况">
        <div class="admin-metric">
          <span class="admin-metric-icon"><UiIcon name="calendar" :size="22" /></span>
          <span>今日预约门诊</span><strong>{{ data.todayAppointments ?? 0 }}<small>人次</small></strong><p>按预约日期统计</p>
        </div>
        <div class="admin-metric">
          <span class="admin-metric-icon teal"><UiIcon name="users" :size="22" /></span>
          <span>当前在院患者</span><strong>{{ data.inHospitalCount ?? 0 }}<small>人</small></strong><p>已办理入院的患者</p>
        </div>
        <div class="admin-metric">
          <span class="admin-metric-icon"><UiIcon name="bed" :size="22" /></span>
          <span>床位占用率</span><strong>{{ occupancy.toFixed(1) }}<small>%</small></strong>
          <p>已占用 {{ data.occupiedBeds ?? 0 }} / {{ data.totalBeds ?? 0 }} 张</p>
        </div>
      </section>
      <section class="panel admin-dashboard-worklist" aria-labelledby="admin-worklist-title">
        <div class="admin-panel-heading"><div><h2 id="admin-worklist-title">待处理事项</h2><p>住院审批与药品供应</p></div></div>
        <div class="admin-dashboard-task">
          <span class="admin-task-icon" :class="pendingCount ? 'amber' : 'teal'"><UiIcon :name="pendingCount ? 'clipboard' : 'check'" :size="20" /></span>
          <div><h3>住院申请</h3><p>{{ pendingCount ? '核对住院原因，为患者分配床位。' : '暂无待审批申请。' }}</p></div>
          <b :class="{ 'has-tasks': pendingCount }">{{ pendingCount }}<small>项待审批</small></b>
          <RouterLink :class="pendingCount ? 'button button-primary button-small' : 'admin-link'" to="/app/admin/beds">{{ pendingCount ? '处理申请' : '查看申请' }}<UiIcon name="arrow-right" :size="15" /></RouterLink>
        </div>
        <div class="admin-dashboard-task">
          <span class="admin-task-icon" :class="lowStock.length ? 'red' : 'teal'"><UiIcon :name="lowStock.length ? 'pill' : 'check'" :size="20" /></span>
          <div><h3>药品库存</h3><p>{{ lowStock.length ? '以下药品库存少于 10，请及时核对并补库。' : '暂无低库存药品。' }}</p></div>
          <b :class="{ 'has-tasks': lowStock.length }">{{ lowStock.length }}<small>种低库存</small></b>
          <RouterLink class="admin-link" to="/app/admin/medicines">查看库存<UiIcon name="arrow-right" :size="15" /></RouterLink>
        </div>
        <div v-if="lowStock.length" class="admin-dashboard-stock">
          <p class="admin-stock-caption">优先补库药品<span>按库存从低到高，最多显示 5 种</span></p>
          <ul class="admin-stock-list" aria-label="优先补库药品">
            <li v-for="medicine in lowStock.slice(0, 5)" :key="medicine.id">
              <div><strong>{{ medicine.name }}</strong><small>{{ medicine.specification || '规格待维护' }}</small></div>
              <span class="admin-stock-value"><small>现有库存</small><strong>{{ medicine.stock }} {{ medicine.unit }}</strong></span>
              <RouterLink class="admin-link" :to="`/app/admin/medicines/${medicine.id}/inbound`" :aria-label="`为${medicine.name}办理入库`">办理入库<UiIcon name="arrow-right" :size="14" /></RouterLink>
            </li>
          </ul>
        </div>
        <RouterLink to="/app/admin/schedules" class="admin-link admin-dashboard-schedule">查看排班与请假申请<UiIcon name="arrow-right" :size="15" /></RouterLink>
      </section>
      <details class="panel admin-disclosure">
        <summary><span><UiIcon name="sparkles" :size="18" />运营辅助摘要</span><small>按需生成</small></summary>
        <div class="admin-disclosure-content">
          <p class="admin-muted">根据当前业务数据生成运营摘要，并保留审计记录。</p>
          <button class="button button-secondary" :disabled="summaryLoading" @click="generate">{{ summaryLoading ? '生成中…' : '生成摘要' }}</button>
          <p v-if="summaryError" class="admin-error-text" role="alert">{{ summaryError }}</p>
          <textarea v-if="summary" class="admin-summary" :value="summary" readonly aria-label="运营辅助摘要"></textarea>
          <RouterLink class="admin-link" to="/app/admin/ai-logs">查看生成审计<UiIcon name="arrow-right" :size="15" /></RouterLink>
        </div>
      </details>
    </template>
  </div>
</template>
