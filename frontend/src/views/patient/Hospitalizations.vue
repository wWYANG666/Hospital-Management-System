<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import UiIcon from "@/components/UiIcon.vue";
import PatientPageState from "./PatientPageState.vue";
import {
  currency as money,
  list,
  usePatientPage,
  type PatientData,
} from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(() => "/patient/hospitalizations");
const tab = ref("CURRENT");
const records = computed(() =>
  list(
    data.value,
    tab.value === "CURRENT"
      ? "currentHospitalizations"
      : "historyHospitalizations",
  ),
);
const requested = (id: any) => !!data.value.dischargeRequestedMap?.[id];
const paid = (id: any) => !!data.value.paidMap?.[id];
async function choose(hosp: PatientData, endpoint: string) {
  if (submitting.value) return;
  const cancelling = endpoint === "cancel";
  if (await confirmAction(
    `住院记录 #${hosp.id}。${cancelling ? "取消后将终止本次待安排的住院申请。" : "申请提交后，医生将根据您的病情确认是否可以出院。"}`,
    cancelling ? "确认取消住院？" : "确认提交出院申请？",
  )) await action(
      `/patient/hospitalizations/${hosp.id}/${endpoint}`,
      {},
      cancelling
        ? "住院记录已取消"
        : "出院申请已提交，请等待医生处理",
    );
}
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="住院服务"
      description="查看住院安排、医嘱与病历，办理出院申请及费用支付。"
    /><PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="reload"
    />
    <div class="patient-bottom-note">
      <UiIcon name="info" :size="21" />
      <div>
        <strong>住院办理流程</strong>
        <p>
          住院申请由医生诊断后发起，床位安排完成后可查看住院医嘱；出院须由医生确认。
        </p>
      </div>
    </div>
    <section class="panel">
      <div class="patient-list-toolbar">
        <div class="patient-tabs">
          <button
            :class="{ active: tab === 'CURRENT' }"
            :aria-pressed="tab === 'CURRENT'"
            @click="tab = 'CURRENT'"
          >
            当前住院
            <span>{{
              list(data, "currentHospitalizations").length
            }}</span></button
          ><button
            :class="{ active: tab === 'HISTORY' }"
            :aria-pressed="tab === 'HISTORY'"
            @click="tab = 'HISTORY'"
          >
            历史住院
            <span>{{ list(data, "historyHospitalizations").length }}</span>
          </button>
        </div>
      </div>
      <EmptyState
        v-if="!loading && !error && !records.length"
        :title="tab === 'CURRENT' ? '当前没有住院记录' : '暂无历史住院记录'"
        description="住院信息将在医生发起住院流程后显示。"
      />
      <article
        v-for="hosp in records"
        :key="hosp.id"
        class="patient-hospital-card"
      >
        <div class="patient-card-heading">
          <div class="patient-inline-heading">
            <span class="patient-task-icon"
              ><UiIcon name="building" :size="23"
            /></span>
            <div>
              <h3>住院记录 #{{ hosp.id }}</h3>
              <p>{{ formatDate(hosp.admissionDate) }} 入院</p>
            </div>
          </div>
          <StatusBadge :status="hosp.status" />
        </div>
        <dl class="patient-facts">
          <div>
            <dt>主治医生</dt>
            <dd>{{ text(data.doctorDisplayMap?.[hosp.id]) }}</dd>
          </div>
          <div>
            <dt>病房与床位</dt>
            <dd v-if="data.bedDisplayMap?.[hosp.id]">
              {{ data.bedDisplayMap[hosp.id].ward }} ·
              {{ data.bedDisplayMap[hosp.id].roomNumber }} 房 ·
              {{ data.bedDisplayMap[hosp.id].bedNumber }} 床
            </dd>
            <dd v-else>{{ hosp.bedId ? "床位信息待完善" : "等待床位安排" }}</dd>
          </div>
          <div v-if="hosp.dischargeDate">
            <dt>出院日期</dt>
            <dd>{{ formatDate(hosp.dischargeDate) }}</dd>
          </div>
          <div v-if="hosp.totalCost != null">
            <dt>住院总费用</dt>
            <dd>
              {{ money(hosp.totalCost) }} ·
              <span
                :class="
                  paid(hosp.id)
                    ? 'patient-success-text'
                    : 'patient-warning-text'
                "
                >{{ paid(hosp.id) ? "已支付" : "待支付" }}</span
              >
            </dd>
          </div>
          <div class="patient-fact-full">
            <dt>诊断</dt>
            <dd class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="住院诊断完整内容">
              {{ hosp.diagnosis || hosp.dischargeDiagnosis || "暂无诊断记录" }}
            </dd>
          </div>
        </dl>
        <div
          v-if="requested(hosp.id) && tab === 'CURRENT'"
          class="patient-notice"
        >
          出院申请已提交，等待医生处理。
        </div>
        <div class="patient-card-actions">
          <RouterLink
            :to="`/app/patient/hospitalizations/${hosp.id}/detail`"
            class="button button-primary"
            >查看医嘱与病历</RouterLink
          ><button
            v-if="tab === 'CURRENT' && hosp.bedId"
            class="button button-secondary"
            :disabled="submitting || loading || requested(hosp.id)"
            @click="choose(hosp, 'request-discharge')"
          >
            {{ requested(hosp.id) ? "出院申请处理中" : "申请出院" }}</button
          ><button
            v-if="tab === 'CURRENT' && !hosp.bedId"
            class="button button-secondary"
            :disabled="submitting || loading"
            @click="choose(hosp, 'cancel')"
          >
            取消住院</button
          ><RouterLink
            v-if="tab === 'HISTORY' && hosp.totalCost != null && !paid(hosp.id)"
            :to="`/app/patient/payments/hospitalization/${hosp.id}/alipay`"
            class="button button-primary"
            >缴纳住院费用</RouterLink
          >
        </div>
      </article>
    </section>
  </div>
</template>
