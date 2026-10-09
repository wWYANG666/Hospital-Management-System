<script setup lang="ts">
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import AdminFeedback from './AdminFeedback.vue'
import {useAdminPage,items,appPath} from './useAdminPage'
import {formatDate,money,statusText} from '@/lib/format'
const route=useRoute()
const {data,loading,error,load}=useAdminPage(()=>`/admin/reports/${route.params.id}/preview`)
function printReport(){window.print()}
</script>
<template><div class="admin-view"><PageHeader title="报告单详情" description="检查结果与关联处方，按临床原始记录展示。"><button class="button button-primary" @click="printReport"><UiIcon name="file" :size="17" />打印报告</button><RouterLink class="button button-secondary" :to="appPath(data.adminBackUrl || '/admin/reports')">返回列表</RouterLink></PageHeader><AdminFeedback :loading="loading" :error="error" @retry="load" /><article v-if="!loading && data.report" class="panel admin-paper"><header class="admin-paper-title"><span>HOSPITAL MEDICAL REPORT</span><h2>医院检验 / 检查报告单</h2><p>{{ data.examination?.name || data.report.reportType || '诊疗报告' }}</p></header><dl class="admin-info-grid"><div><dt>患者姓名</dt><dd>{{ data.patientUser?.realName || '未登记' }}</dd></div><div><dt>患者编号</dt><dd>{{ data.patient?.patientCode || `#${data.report.patientId}` }}</dd></div><div><dt>性别</dt><dd>{{ statusText(data.patient?.gender) }}</dd></div><div><dt>开具医生</dt><dd>{{ data.doctorUser?.realName || '未登记' }}</dd></div><div><dt>报告时间</dt><dd>{{ formatDate(data.report.reportDate,true) }}</dd></div><div><dt>报告状态</dt><dd><StatusBadge :status="data.report.status" /></dd></div></dl><h2 class="admin-ward-title">报告内容</h2><div class="admin-report-content read-scroll" tabindex="0" role="region" aria-label="检查报告完整内容">{{ data.report.reportContent || '报告内容尚未出具。' }}</div><p class="admin-result-meta" style="margin:16px 0 0">本报告仅供临床参考，不作为唯一诊断依据。</p><h2 class="admin-ward-title">本次就诊关联处方</h2><EmptyState v-if="!items(data,'prescriptions').length" title="无关联处方" /><div v-else class="table-scroll"><table class="admin-table"><thead><tr><th>处方编号</th><th>开具时间</th><th>状态</th><th>金额（元）</th><th>操作</th></tr></thead><tbody><tr v-for="p in items(data,'prescriptions')" :key="p.id"><td>{{ p.prescriptionNumber || `#${p.id}` }}</td><td>{{ formatDate(p.createdAt,true) }}</td><td><StatusBadge :status="p.status" /></td><td>{{ money(data.totalAmountMap?.[p.id]) }}</td><td><RouterLink class="admin-link" :to="{path:`/app/admin/pharmacy/${p.id}`,query:{returnPatientId:String(data.report.patientId)}}">查看处方</RouterLink></td></tr></tbody></table></div></article></div></template>
