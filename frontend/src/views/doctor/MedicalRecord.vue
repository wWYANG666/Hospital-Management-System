<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import { formatDate } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const { data, loading, error, loadFailed, load } = useDoctorPage(
  () => `/doctor/appointments/${route.params.id}/record`,
);
const print = () => window.print();
const contentText = (value: unknown) => String(value || "暂无记录").replace(/\s*\(null\)/g, "");
const examinationLabel = () =>
  (data.value.examinationLabels || []).join("、") ||
  (data.value.record?.examinationItems ? "检查项目已归档" : "");
</script>
<template>
  <div class="doctor-view doctor-print-view">
    <PageHeader title="电子病历" description="本次门诊诊断、处方与检查记录。"
      ><RouterLink class="button button-secondary" to="/app/doctor/appointments"
        >返回就诊档案</RouterLink
      ><button class="button button-primary" :disabled="loading || loadFailed || !data.record" @click="print">
        <UiIcon name="download" :size="17" /> 打印病历
      </button></PageHeader
    >
    <DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <EmptyState
      v-if="!loading && !loadFailed && !data.record"
      title="暂未生成病历"
      description="完成诊断后即可查阅本次电子病历。"
    />
    <article v-if="data.record" class="panel doctor-document">
      <header class="doctor-document-header">
        <UiIcon name="hospital" :size="26" />
        <h1>门诊电子病历</h1>
        <p>
          挂号编号 #{{ data.appointment?.id || route.params.id }} · 创建于
          {{ formatDate(data.record.createdAt, true) }}
        </p>
      </header>
      <div class="doctor-document-meta">
        <span
          >就诊日期<strong>{{
            formatDate(data.appointment?.appointmentDate)
          }}</strong></span
        ><span
          >预约时间<strong>{{
            data.appointment?.appointmentTime || "未记录"
          }}</strong></span
        >
      </div>
      <section
        v-for="block in [
          { label: '临床诊断', value: data.record.diagnosis },
          { label: '处方与用药', value: data.record.prescription },
          { label: '检查项目记录', value: examinationLabel() },
          { label: '病历内容', value: data.record.medicalRecordContent },
        ]"
        :key="block.label"
        class="doctor-document-section"
      >
        <h2>{{ block.label }}</h2>
        <p class="read-scroll" tabindex="0" role="region" :aria-label="block.label">{{ contentText(block.value) }}</p>
      </section>
    </article>
  </div>
</template>
