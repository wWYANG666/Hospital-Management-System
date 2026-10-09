<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { formatDate as displayDate } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, useDoctorPagination, list, mappedName } from "./useDoctorPage";
import "@/styles/doctor.css";
const { data, loading, error, loadFailed, load } = useDoctorPage(() => "/doctor/reports");
const formatDate = (value: unknown) => displayDate(value, true);
const search = ref("");
const status = ref("PENDING");
const all = computed(() => list(data.value, "reports"));
const pending = computed(() =>
  all.value.filter((report) => report.status === "PENDING"),
);
const filtered = computed(() =>
  all.value.filter(
    (report) =>
      (!status.value || report.status === status.value) &&
      `${mappedName(data.value.userMap, report.patientId)} ${data.value.examinationMap?.[report.id]?.name || report.reportType}`.includes(
        search.value.trim(),
      ),
  ),
);
const { page, pages, visible } = useDoctorPagination(filtered);
function resetFilters() { search.value = ""; status.value = ""; }
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="检查报告"
      description="录入检查检验结果，审核并完成患者报告。"
    /><DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <template v-if="!loading && !loadFailed">
    <div class="doctor-inline-metrics">
      <span
        >可审核<strong>{{ data.pendingReviewableCount || 0 }}</strong></span
      ><span
        >待完成<strong>{{ pending.length }}</strong></span
      ><span
        >全部报告<strong>{{ all.length }}</strong></span
      >
    </div>
    <section class="panel">
      <div class="doctor-panel-heading"><div><h2>报告列表</h2><p>默认查看待完成报告，已缴费项目可填写结果。</p></div></div>
      <div class="toolbar doctor-list-toolbar">
        <label class="doctor-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            placeholder="搜索患者或检查项目"
            aria-label="搜索报告" /></label
        ><select v-model="status" class="field" aria-label="报告状态">
          <option value="">全部状态</option>
          <option value="PENDING">待完成</option>
          <option value="COMPLETED">已完成</option></select
        ><span class="doctor-result-count">{{ filtered.length }} 份报告</span>
        <button v-if="search || status" type="button" class="doctor-text-link" @click="resetFilters">查看全部</button>
      </div>
      <EmptyState v-if="!loading && !filtered.length" title="暂无匹配报告" />
      <div v-if="filtered.length" class="table-scroll">
        <table class="doctor-table">
          <thead>
            <tr>
              <th>检查 / 检验项目</th>
              <th>患者</th>
              <th>报告时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="report in visible" :key="report.id">
              <td>
                <strong>{{
                  data.examinationMap?.[report.id]?.name || report.reportType
                }}</strong
                ><small class="doctor-cell-sub">报告 #{{ report.id }}</small>
              </td>
              <td>{{ mappedName(data.userMap, report.patientId) }}</td>
              <td>{{ formatDate(report.reportDate) }}</td>
              <td><span v-if="report.status === 'PENDING' && data.reportNeedsPayment?.[report.id]" class="doctor-tag doctor-tag-warning">待患者缴费</span><StatusBadge v-else :status="report.status" /></td>
              <td>
                <RouterLink
                  v-if="report.status === 'COMPLETED'"
                  :to="`/app/doctor/reports/${report.id}/preview`"
                  class="button button-secondary"
                  >查阅报告</RouterLink
                ><RouterLink
                  v-else-if="!data.reportNeedsPayment?.[report.id]"
                  :to="`/app/doctor/reports/${report.id}/edit`"
                  class="button button-primary"
                  >填写报告</RouterLink
                ><span v-else class="doctor-tag doctor-tag-warning"
                  >待缴费</span
                >
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="pages > 1" class="doctor-pagination" aria-label="报告分页"><span>第 {{ page }} / {{ pages }} 页</span><button class="button button-secondary" :disabled="page === 1" @click="page--">上一页</button><button class="button button-secondary" :disabled="page === pages" @click="page++">下一页</button></nav>
    </section>
    </template>
  </div>
</template>
