<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import PatientPageState from "./PatientPageState.vue";
import { enumName, list, usePatientPage } from "./usePatientPage";
import { formatDate } from "@/lib/format";
const { data, loading, error, reload } = usePatientPage(
  () => "/patient/reports",
);
const search = ref("");
const filter = ref("ALL");
const reportName = (report: Record<string, any>) =>
  data.value.examinationMap?.[report.id]?.name ||
  report.reportType ||
  "检查报告";
const reports = computed(() =>
  list(data.value, "reports").filter(
    (report) =>
      (filter.value === "ALL" || enumName(report.status) === filter.value) &&
      reportName(report).includes(search.value.trim()),
  ),
);
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="检查报告"
      description="查看检查、检验结果与报告状态，下载或打印报告留存。"
    /><PatientPageState :loading="loading" :error="error" @retry="reload" />
    <section class="panel">
      <div class="patient-list-toolbar">
        <div class="patient-tabs">
          <button :class="{ active: filter === 'ALL' }" :aria-pressed="filter === 'ALL'" @click="filter = 'ALL'">
            全部报告</button
          ><button
            :class="{ active: filter === 'COMPLETED' }"
            :aria-pressed="filter === 'COMPLETED'"
            @click="filter = 'COMPLETED'"
          >
            已出报告</button
          ><button
            :class="{ active: filter === 'PENDING' }"
            :aria-pressed="filter === 'PENDING'"
            @click="filter = 'PENDING'"
          >
            待出报告
          </button>
        </div>
        <label class="patient-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            type="search"
            placeholder="搜索检查名称"
            aria-label="搜索检查报告"
        /></label>
      </div>
      <EmptyState
        v-if="!loading && !error && !reports.length"
        title="暂无符合条件的检查报告"
        description="医生开具检查后，报告进度与结果会在此更新。"
      />
      <div class="patient-reports-grid">
        <article
          v-for="report in reports"
          :key="report.id"
          class="patient-report-card"
        >
          <div class="patient-card-heading">
            <span class="patient-task-icon"
              ><UiIcon name="file" :size="24" /></span
            ><StatusBadge :status="report.status" />
          </div>
          <h3>{{ reportName(report) }}</h3>
          <p>
            {{ data.examinationMap?.[report.id]?.type || "检查" }} · 报告编号
            #{{ report.id }}
          </p>
          <small>{{ formatDate(report.reportDate) }}</small>
          <div class="patient-card-actions">
            <RouterLink
              :to="`/app/patient/reports/${report.id}?from=reports`"
              class="button button-primary"
              >{{ enumName(report.status) === 'COMPLETED' ? '查看报告' : '查看检查进度' }}</RouterLink
            ><a
              v-if="enumName(report.status) === 'COMPLETED' && report.reportContent"
              :href="`/api/patient/reports/${report.id}/download`"
              class="button button-secondary"
              target="_blank"
              rel="noopener"
              ><UiIcon name="download" :size="17" />PDF</a
            >
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
