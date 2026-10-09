<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import UiIcon from "@/components/UiIcon.vue";
import PatientPageState from "./PatientPageState.vue";
import { appPath, enumName, usePatientPage } from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
const route = useRoute();
const { data, loading, error, reload } = usePatientPage(
  () => `/patient/reports/${route.params.id}`,
  () => ({ from: route.query.from }),
);
const print = () => window.print();
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="报告详情"
      description="查看本次检查结果，支持 PDF 下载与打印。"
    />
    <div class="patient-page-actions">
      <RouterLink
        :to="appPath(data.backUrl, '/app/patient/reports')"
        class="button button-secondary"
        >返回列表</RouterLink
      ><a
        v-if="enumName(data.report?.status) === 'COMPLETED' && data.report?.reportContent"
        :href="`/api/patient/reports/${data.report.id}/download`"
        target="_blank"
        rel="noopener"
        class="button button-primary"
        ><UiIcon name="download" :size="18" />下载 PDF</a
      ><button
        v-if="enumName(data.report?.status) === 'COMPLETED' && data.report?.reportContent"
        class="button button-secondary"
        @click="print"
      >
        打印报告
      </button>
    </div>
    <PatientPageState :loading="loading" :error="error" @retry="reload" />
    <section v-if="data.report" class="panel patient-document">
      <header class="patient-document-header">
        <span class="patient-eyebrow">检查 / 检验报告</span>
        <h2>
          {{ data.examination?.name || data.report.reportType || "检查报告" }}
        </h2>
        <p>
          报告编号 #{{ data.report.id }} ·
          {{ formatDate(data.report.reportDate) }}
        </p>
        <StatusBadge :status="data.report.status" />
      </header>
      <dl class="patient-facts">
        <div>
          <dt>患者姓名</dt>
          <dd>{{ text(data.patientUser?.realName) }}</dd>
        </div>
        <div>
          <dt>开单医生</dt>
          <dd>{{ text(data.doctorUser?.realName) }}</dd>
        </div>
        <div>
          <dt>检查类型</dt>
          <dd>{{ data.examination?.type || "检查" }}</dd>
        </div>
        <div>
          <dt>报告日期</dt>
          <dd>{{ formatDate(data.report.reportDate) }}</dd>
        </div>
      </dl>
      <section class="patient-record-section">
        <h3>检查结果</h3>
        <div
          v-if="data.report.reportContent"
          class="patient-preformatted patient-report-content read-scroll"
          tabindex="0" role="region" aria-label="检查报告完整内容"
        >
          {{ data.report.reportContent }}
        </div>
        <EmptyState
          v-else
          :title="data.report.paid === false ? '检查项目待缴费' : '报告尚未填写'"
          :description="data.report.paid === false ? '请先完成检查费用支付，之后可继续办理检查。' : '检查报告生成后，您可以在这里查看结果。'"
        />
      </section>
      <div v-if="data.report.paid === false && enumName(data.report.status) === 'PENDING'" class="patient-form-footer">
        <RouterLink :to="`/app/patient/payments/report/${data.report.id}/alipay`" class="button button-primary">缴纳检查费用</RouterLink>
      </div>
      <div class="patient-bottom-note">
        <UiIcon name="info" :size="21" />
        <div>
          <strong>报告解读</strong>
          <p>检查结果请结合临床情况，由就诊医生进行解读。</p>
        </div>
      </div>
    </section>
  </div>
</template>
