<script setup lang="ts">
import { ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { submitAction } from "@/lib/api";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const content = ref("");
const aiDraft = ref("");
const aiBusy = ref(false);
const aiError = ref("");
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => `/doctor/reports/${route.params.id}/edit`,
  (value) => (content.value = value.report?.reportContent || ""),
  () => content.value,
);
async function generate() {
  if (aiBusy.value) return;
  aiError.value = "";
  aiBusy.value = true;
  try {
    aiDraft.value = await submitAction<string>("/doctor/ai/report-suggestion", {
      examinationId: data.value.report?.examinationId,
      rawResult: content.value,
    });
  } catch (cause) {
    aiError.value = cause instanceof Error ? cause.message : "草稿生成失败";
  } finally {
    aiBusy.value = false;
  }
}
const append = () => {
  if (aiDraft.value) {
    content.value = [content.value, aiDraft.value].filter(Boolean).join("\n\n");
    aiDraft.value = "";
  }
};
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="检查报告审核"
      description="核对患者与检查项目，录入结果及临床建议。"
      ><RouterLink to="/app/doctor/reports" class="button button-secondary"
        >返回报告列表</RouterLink
      ></PageHeader
    ><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.report"
      @retry="load"
    /><template v-if="data.report"
      ><section class="panel doctor-patient-banner">
        <span class="doctor-patient-avatar"
          ><UiIcon name="file" :size="26"
        /></span>
        <div class="doctor-patient-banner-main">
          <div>
            <h2>{{ data.examination?.name || data.report.reportType }}</h2>
            <StatusBadge :status="data.report.status" />
          </div>
          <p>
            {{ data.patientUser?.realName || "患者信息未维护" }} ·
            {{ data.examination?.type || "检查" }} · 报告 #{{ data.report.id }}
          </p>
        </div>
      </section>
      <form
        class="doctor-form-panel"
        @submit.prevent="
          action(
            `/doctor/reports/${route.params.id}`,
            { reportContent: content },
            '/app/doctor/reports',
          )
        "
      >
        <fieldset class="doctor-form-lock" :disabled="saving">
        <section class="panel">
          <div class="doctor-panel-heading">
            <div>
              <h2>正式报告内容</h2>
              <p>记录检查所见、结果、判断及建议</p>
            </div>
          </div>
          <label class="doctor-form-field"
            ><span>报告内容 <b>*</b></span
            ><textarea
              v-model="content"
              required
              rows="18"
              placeholder="检查所见：&#10;&#10;结果与判断：&#10;&#10;临床建议："
            ></textarea>
          </label>
          <div class="doctor-form-actions">
            <RouterLink to="/app/doctor/reports" class="button button-secondary"
              >取消</RouterLink
            ><button class="button button-primary" :disabled="saving || aiBusy">
              {{ saving ? "保存中…" : "保存并完成报告" }}
            </button>
          </div>
        </section>
        <details class="panel doctor-ai-panel doctor-report-assistant">
          <summary><UiIcon name="sparkles" :size="18" /> AI 辅助报告草稿</summary>
          <p class="doctor-muted">审阅并确认草稿内容后，可追加到正式报告。</p>
          <p v-if="aiError" class="doctor-amber-text" role="alert">{{ aiError }}</p>
          <button
            type="button"
            class="button button-secondary doctor-full-width"
            :disabled="aiBusy"
            @click="generate"
          >
            {{ aiBusy ? "生成中…" : "生成报告草稿" }}</button
          ><textarea
            v-if="aiDraft"
            v-model="aiDraft"
            rows="13"
            aria-label="报告草稿"
          ></textarea
          ><button
            v-if="aiDraft"
            type="button"
            class="button button-secondary"
            @click="append"
          >
            追加到正式报告
          </button>
        </details>
        </fieldset>
      </form></template
    >
  </div>
</template>
