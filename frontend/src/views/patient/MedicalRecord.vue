<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import PatientPageState from "./PatientPageState.vue";
import { usePatientPage } from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
const route = useRoute();
const { data, loading, error, reload } = usePatientPage(
  () => `/patient/appointments/${route.params.id}/record`,
);
const print = () => window.print();
const blocks = [
  { key: "diagnosis", title: "诊断结果" },
  { key: "prescription", title: "处方与治疗" },
  { key: "examinationItems", title: "检查项目" },
  { key: "medicalRecordContent", title: "病历内容" },
];
const recordContent = (key: string) => {
  if (key === "examinationItems")
    return (
      data.value.examinationDisplay || data.value.record?.[key] || "暂无记录"
    );
  return String(data.value.record?.[key] || "暂无记录").replace(
    /\s*\(null\)/g,
    "",
  );
};
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="就诊病历"
      description="查看本次门诊的诊断、检查与治疗记录。"
    />
    <div class="patient-page-actions">
      <RouterLink to="/app/patient/appointments" class="button button-secondary"
        >返回我的挂号</RouterLink
      ><button v-if="data.record && !loading" class="button button-primary" @click="print">打印病历</button>
    </div>
    <PatientPageState :loading="loading" :error="error" @retry="reload" />
    <section v-if="data.record" class="panel patient-document">
      <header class="patient-document-header">
        <span class="patient-eyebrow">门诊病历</span>
        <h2>本次就诊记录</h2>
        <p>病历编号 #{{ data.record.id }}</p>
      </header>
      <dl class="patient-facts">
        <div>
          <dt>就诊医生</dt>
          <dd>
            {{ data.doctorUser?.realName || data.doctor?.doctorCode || "—" }}
          </dd>
        </div>
        <div>
          <dt>就诊科室</dt>
          <dd>{{ text(data.doctor?.department) }}</dd>
        </div>
        <div>
          <dt>预约日期</dt>
          <dd>{{ formatDate(data.appointment?.appointmentDate) }}</dd>
        </div>
        <div>
          <dt>记录时间</dt>
          <dd>{{ formatDate(data.record.createdAt) }}</dd>
        </div>
      </dl>
      <section
        v-for="block in blocks"
        :key="block.key"
        class="patient-record-section"
      >
        <h3>{{ block.title }}</h3>
        <div class="patient-preformatted read-scroll" tabindex="0" role="region" :aria-label="block.title">
          {{ recordContent(block.key) }}
        </div>
      </section>
    </section>
    <EmptyState
      v-else-if="!loading && !error"
      title="暂无就诊病历"
      description="医生完成诊疗后可查看对应记录。"
    />
  </div>
</template>
