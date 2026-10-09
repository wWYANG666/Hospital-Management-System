<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { formatDate as displayDate } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const formatDate = (value: unknown) => displayDate(value, true);
const { data, loading, error, loadFailed, load } = useDoctorPage(
  () => `/doctor/reports/${route.params.id}/preview`,
);
const print = () => window.print();
</script>
<template>
  <div class="doctor-view doctor-print-view">
    <PageHeader title="报告预览" description="患者检查结果与医生审核记录。"
      ><RouterLink to="/app/doctor/reports" class="button button-secondary"
        >返回报告列表</RouterLink
      ><button class="button button-primary" :disabled="loading || loadFailed || !data.report" @click="print">
        <UiIcon name="download" :size="16" /> 打印报告
      </button></PageHeader
    ><DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <article v-if="data.report" class="panel doctor-document">
      <header class="doctor-document-header">
        <UiIcon name="hospital" :size="27" />
        <h1>检验 / 检查报告单</h1>
        <p>
          {{ data.examination?.name || data.report.reportType }} · 报告 #{{
            data.report.id
          }}
        </p>
      </header>
      <div class="doctor-document-meta">
        <span
          >患者姓名<strong>{{
            data.patientUser?.realName || "未维护"
          }}</strong></span
        ><span
          >检查类型<strong>{{ data.examination?.type || "检查" }}</strong></span
        ><span
          >报告医生<strong>{{
            data.doctorUser?.realName || "未维护"
          }}</strong></span
        ><span
          >报告时间<strong>{{
            formatDate(data.report.reportDate)
          }}</strong></span
        >
      </div>
      <section class="doctor-document-section">
        <h2>检查结果与建议</h2>
        <p class="read-scroll" tabindex="0" role="region" aria-label="检查报告完整内容">{{ data.report.reportContent || "暂无报告内容" }}</p>
      </section>
      <footer class="doctor-document-footer">
        <StatusBadge :status="data.report.status" /><span
          >报告结果供临床诊疗参考</span
        >
      </footer>
    </article>
  </div>
</template>
