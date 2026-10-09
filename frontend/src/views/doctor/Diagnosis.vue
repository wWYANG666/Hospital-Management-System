<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import EmptyState from "@/components/EmptyState.vue";
import { submitAction } from "@/lib/api";
import { money as formatMoney, formatDate } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, list } from "./useDoctorPage";
import "@/styles/doctor.css";

const route = useRoute();
const money = (value: unknown) => `¥${formatMoney(value)}`;
const form = reactive({
  diagnosis: "",
  prescription: "",
  medicines: [] as string[],
  examinationItems: [] as string[],
});
const quantities = reactive<Record<string, number>>({});
const medicineSearch = ref("");
const examSearch = ref("");
const aiDraft = ref("");
const aiBusy = ref(false);
const aiError = ref("");
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => `/doctor/diagnose/${route.params.id}`,
  (value) => {
    form.diagnosis =
      value.existingRecord?.diagnosis || value.appointment?.diagnosis || "";
    form.prescription =
      value.existingRecord?.prescription ||
      value.appointment?.prescription ||
      "";
    form.medicines = Object.keys(value.appointmentMedicineQty || {});
    Object.keys(quantities).forEach((key) => delete quantities[key]);
    Object.assign(quantities, value.appointmentMedicineQty || {});
    form.examinationItems = (value.selectedExamIds || []).map(String);
  },
  () => ({ ...form, quantities: Object.fromEntries(form.medicines.map((id) => [id, quantities[id] || 1])) }),
);
const medicines = computed(() =>
  list(data.value, "medicines").filter((med) =>
    `${med.name} ${med.specification || ""}`.includes(
      medicineSearch.value.trim(),
    ),
  ),
);
const exams = computed(() =>
  list(data.value, "examinations").filter((exam) =>
    `${exam.name} ${exam.type || ""}`.includes(examSearch.value.trim()),
  ),
);
const medTotal = computed(() =>
  list(data.value, "medicines").reduce(
    (sum, med) =>
      sum +
      (form.medicines.includes(String(med.id))
        ? Number(med.price || 0) * Math.max(1, quantities[String(med.id)] || 1)
        : 0),
    0,
  ),
);
const examTotal = computed(() =>
  list(data.value, "examinations").reduce(
    (sum, exam) =>
      sum +
      (form.examinationItems.includes(String(exam.id))
        ? Number(exam.price || 0)
        : 0),
    0,
  ),
);
async function save() {
  const payload: Record<string, unknown> = { ...form };
  form.medicines.forEach(
    (id) => (payload[`medicineQty_${id}`] = quantities[id] || 1),
  );
  await action(
    `/doctor/diagnose/${route.params.id}`,
    payload,
    "/app/doctor/appointments",
  );
}
async function generate() {
  if (aiBusy.value) return;
  aiError.value = "";
  aiBusy.value = true;
  try {
    aiDraft.value = await submitAction<string>(
      "/doctor/ai/diagnosis-suggestion",
      {
        appointmentId: route.params.id,
        symptoms: data.value.appointment?.symptoms,
        existingDiagnosis: form.diagnosis,
        medicineIds: form.medicines,
        examinationIds: form.examinationItems,
        gender: data.value.patient?.gender,
        department: data.value.currentDoctorDepartment,
        historySummary: data.value.existingRecord?.medicalRecordContent || "",
      },
    );
  } catch (cause) {
    aiError.value = cause instanceof Error ? cause.message : "草稿生成失败";
  } finally {
    aiBusy.value = false;
  }
}
function appendDraft() {
  if (aiDraft.value.trim()) {
    form.diagnosis = [form.diagnosis, aiDraft.value]
      .filter(Boolean)
      .join("\n\n");
    aiDraft.value = "";
  }
}
</script>

<template>
  <div class="doctor-view">
    <PageHeader title="门诊诊疗" description="记录诊断，开立处方与检查项目。"
      ><RouterLink class="button button-secondary" to="/app/doctor/diagnose"
        ><UiIcon name="chevron-left" :size="17" /> 返回待诊队列</RouterLink
      ></PageHeader
    >
    <DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.appointment"
      @retry="load"
    />
    <template v-if="data.appointment && !loading">
      <section class="panel doctor-patient-banner">
        <span class="doctor-patient-avatar"
          ><UiIcon name="user" :size="27"
        /></span>
        <div class="doctor-patient-banner-main">
          <div>
            <h2>{{ data.patientUser?.realName || "患者" }}</h2>
            <StatusBadge :status="data.appointment.status" /><span
              class="doctor-muted"
              >挂号 #{{ data.appointment.id }}</span
            >
          </div>
          <p>
            {{ data.currentDoctorDepartment || "门诊" }} <span>·</span>
            {{ formatDate(data.appointment.appointmentDate) }}
            {{ data.appointment.appointmentTime }}
          </p>
        </div>
        <div class="doctor-chief-complaint">
          <span>本次主诉</span>
          <p>{{ data.appointment.symptoms || "未填写，请接诊时补充了解" }}</p>
        </div>
      </section>
      <form @submit.prevent="save">
        <fieldset class="doctor-form-lock" :disabled="saving">
        <div class="doctor-diagnosis-grid">
          <section class="panel doctor-record-editor">
            <div class="doctor-panel-heading">
              <div>
                <h2><UiIcon name="clipboard" :size="18" /> 门诊病历</h2>
                <p>正式诊断与用药说明</p>
              </div>
              <span class="doctor-required-note">* 必填</span>
            </div>
            <label class="doctor-form-field"
              ><span>诊断结果 <b>*</b></span
              ><textarea
                v-model="form.diagnosis"
                required
                rows="7"
                placeholder="记录临床诊断、分型及重要依据"
              ></textarea></label
            ><label class="doctor-form-field"
              ><span>处方与用药说明</span
              ><textarea
                v-model="form.prescription"
                rows="6"
                placeholder="记录用法用量、疗程与注意事项；药品数量在右侧选择"
              ></textarea>
            </label>
            <details class="doctor-ai-panel">
              <summary>
                <UiIcon name="sparkles" :size="17" /> AI 辅助诊断草稿
              </summary>
              <p>生成内容需要医生审阅，确认后追加到正式诊断。</p>
              <p v-if="aiError" class="doctor-amber-text" role="alert">{{ aiError }}</p>
              <button
                type="button"
                class="button button-secondary"
                :disabled="aiBusy"
                @click="generate"
              >
                {{ aiBusy ? "生成中…" : "生成诊断草稿" }}</button
              ><textarea
                v-if="aiDraft"
                v-model="aiDraft"
                rows="6"
                aria-label="AI 诊断草稿"
              ></textarea
              ><button
                v-if="aiDraft"
                type="button"
                class="doctor-text-link"
                @click="appendDraft"
              >
                追加到诊断结果 <UiIcon name="arrow-right" :size="15" />
              </button>
            </details>
          </section>
          <section class="panel doctor-order-editor">
            <div class="doctor-panel-heading">
              <div>
                <h2><UiIcon name="pill" :size="18" /> 开立医嘱</h2>
                <p>选择药品数量与检查项目</p>
              </div>
            </div>
            <div class="doctor-order-section">
              <div class="doctor-order-title">
                <h3>处方药品</h3>
                <span
                  >{{ form.medicines.length }} 种 · {{ money(medTotal) }}</span
                >
              </div>
              <label class="doctor-search"
                ><UiIcon name="search" :size="16" /><input
                  v-model="medicineSearch"
                  placeholder="查找药品或规格"
                  aria-label="查找药品"
              /></label>
              <div class="doctor-catalog">
                <EmptyState v-if="!medicines.length" title="暂无匹配药品" />
                <div
                  v-for="med in medicines"
                  :key="med.id"
                  class="doctor-catalog-row"
                >
                  <label class="doctor-choice"
                    ><input
                      v-model="form.medicines"
                      type="checkbox"
                      :value="String(med.id)"
                    />
                    <div>
                      <strong>{{ med.name }}</strong
                      ><small
                        >{{ med.specification || "规格未维护" }} ·
                        {{ money(med.price) }}/单位</small
                      >
                    </div></label
                  ><input
                    v-if="form.medicines.includes(String(med.id))"
                    v-model.number="quantities[String(med.id)]"
                    type="number"
                    min="1"
                    max="9999"
                    step="1"
                    :aria-label="`${med.name}数量`"
                    class="doctor-quantity"
                    placeholder="1"
                  />
                </div>
              </div>
            </div>
            <div class="doctor-order-section">
              <div class="doctor-order-title">
                <h3>检查 / 检验</h3>
                <span
                  >{{ form.examinationItems.length }} 项 ·
                  {{ money(examTotal) }}</span
                >
              </div>
              <label class="doctor-search"
                ><UiIcon name="search" :size="16" /><input
                  v-model="examSearch"
                  placeholder="查找检查项目"
                  aria-label="查找检查"
              /></label>
              <div class="doctor-catalog">
                <EmptyState
                  v-if="!exams.length"
                  title="暂无匹配检查项目"
                /><label
                  v-for="exam in exams"
                  :key="exam.id"
                  class="doctor-catalog-row doctor-choice"
                  ><input
                    v-model="form.examinationItems"
                    type="checkbox"
                    :value="String(exam.id)"
                  />
                  <div>
                    <strong>{{ exam.name }}</strong
                    ><small>{{ exam.type || "检查" }}</small>
                  </div>
                  <span class="doctor-catalog-price">{{
                    money(exam.price)
                  }}</span></label
                >
              </div>
            </div>
            <div class="doctor-cost-summary">
              <span>本次所选项目费用</span
              ><strong>{{ money(medTotal + examTotal) }}</strong
              ><small>按所选药品单价 × 数量与检查项目合计</small>
            </div>
          </section>
        </div>
        <footer class="doctor-sticky-actions">
          <span
            ><UiIcon name="check" :size="16" />
            提交后生成病历、处方与检查报告任务</span
          >
          <div>
            <RouterLink
              v-if="data.appointment.status === 'COMPLETED'"
              class="button button-secondary"
              :to="`/app/doctor/hospitalizations/request/${data.appointment.id}`"
              >开立住院申请</RouterLink
            ><button
              class="button button-primary"
              type="submit"
              :disabled="saving || aiBusy"
            >
              {{ saving ? "正在提交…" : "提交诊断" }}
            </button>
          </div>
        </footer>
        </fieldset>
      </form>
    </template>
  </div>
</template>
