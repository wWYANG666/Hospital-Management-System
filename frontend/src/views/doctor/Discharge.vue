<script setup lang="ts">
import { reactive } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import { formatDate } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const form = reactive({ dischargeDiagnosis: "", dischargeNotes: "" });
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => `/doctor/hospitalizations/${route.params.id}/discharge`,
  (value) => (form.dischargeDiagnosis = value.hospitalization?.diagnosis || ""),
  () => ({ ...form }),
);
async function discharge() {
  if (saving.value || !await confirmAction(
    `${data.value.patientUser?.realName || "患者"} 的住院记录将标记为已出院。系统会结算费用、释放床位并完成执行中的医嘱。请确认出院诊断与后续治疗建议已核对。`,
    "确认办理出院",
  )) return;
  await action(
      `/doctor/hospitalizations/${route.params.id}/discharge`,
      { ...form },
      "/app/doctor/hospitalizations",
  );
}
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="办理出院"
      description="核对出院诊断并记录后续治疗与复诊建议。"
      ><RouterLink
        :to="`/app/doctor/hospitalizations/${route.params.id}/manage`"
        class="button button-secondary"
        >返回住院管理</RouterLink
      ></PageHeader
    ><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.hospitalization"
      @retry="load"
    /><template v-if="data.hospitalization"
      ><section class="panel doctor-patient-banner">
        <span class="doctor-patient-avatar"
          ><UiIcon name="user" :size="26"
        /></span>
        <div class="doctor-patient-banner-main">
          <h2>{{ data.patientUser?.realName || "患者" }}</h2>
          <p>
            {{ formatDate(data.hospitalization.admissionDate) }} 入院 · 住院 #{{
              data.hospitalization.id
            }}
          </p>
        </div>
        <div class="doctor-chief-complaint">
          <span>住院诊断</span>
          <p>{{ data.hospitalization.diagnosis || "暂无诊断" }}</p>
        </div>
      </section>
      <form
        class="panel doctor-form-panel"
        @submit.prevent="discharge"
      >
        <fieldset class="doctor-form-lock" :disabled="saving">
        <div class="doctor-panel-heading">
          <div>
            <h2>出院记录</h2>
            <p>办理后自动完成费用结算，释放床位并生成出院报告</p>
          </div>
        </div>
        <label class="doctor-form-field"
          ><span>出院诊断 <b>*</b></span
          ><textarea
            v-model="form.dischargeDiagnosis"
            required
            rows="5"
            placeholder="记录本次住院明确诊断及出院时病情"
          ></textarea></label
        ><label class="doctor-form-field"
          ><span>出院注意事项 <b>*</b></span
          ><textarea
            v-model="form.dischargeNotes"
            required
            rows="6"
            placeholder="记录出院用药、生活指导、复查及复诊安排"
          ></textarea>
        </label>
        <div class="doctor-form-actions">
          <RouterLink
            to="/app/doctor/hospitalizations"
            class="button button-secondary"
            >取消</RouterLink
          ><button class="button button-primary" :disabled="saving">
            {{ saving ? "办理中…" : "核对并办理出院" }}
          </button>
        </div>
        </fieldset>
      </form></template
    >
  </div>
</template>
