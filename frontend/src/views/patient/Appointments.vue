<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import PatientPageState from "./PatientPageState.vue";
import {
  enumName,
  list,
  usePatientPage,
  type PatientData,
} from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";

const route = useRoute();
const bookingNotice = ref(route.query.booked === "1");
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(() => "/patient/appointments");
const filter = ref("ALL");
const search = ref("");
const tabs = [
  { value: "ALL", label: "全部挂号" },
  { value: "UPCOMING", label: "待就诊" },
  { value: "COMPLETED", label: "已完成" },
  { value: "CANCELLED", label: "已取消" },
];
const doctorName = (apt: PatientData) =>
  data.value.userMap?.[apt.doctorId]?.realName ||
  data.value.doctorMap?.[apt.doctorId]?.doctorCode ||
  "医生信息待完善";
const records = computed(() =>
  list(data.value, "appointments").filter((apt) => {
    const status = enumName(apt.status);
    const matches =
      filter.value === "ALL" ||
      (filter.value === "UPCOMING"
        ? ["PENDING", "CONFIRMED"].includes(status)
        : status === filter.value);
    return (
      matches &&
      `${doctorName(apt)}${data.value.doctorMap?.[apt.doctorId]?.department || ""}${apt.appointmentDate || ""}`.includes(
        search.value.trim(),
      )
    );
  }),
);
async function cancel(appointment: PatientData) {
  if (submitting.value) return;
  if (await confirmAction(
    `${formatDate(appointment.appointmentDate)} ${text(appointment.appointmentTime)} · ${doctorName(appointment)}。取消后需要重新选择号源预约。`,
    "确认取消这次挂号？",
  )) {
    if (await action(`/patient/appointments/${appointment.id}/cancel`, {}, "挂号已取消")) bookingNotice.value = false;
  }
}
const hasRecord = (apt: PatientData) =>
  enumName(apt.status) === "COMPLETED" &&
  (data.value.recordMap?.[apt.id] || apt.diagnosis || apt.prescription);
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="我的挂号"
      description="管理预约安排，查看门诊状态与已完成就诊的病历。"
    />
    <div class="patient-page-actions">
      <RouterLink
        to="/app/patient/appointments/new"
        class="button button-primary"
        ><UiIcon name="plus" :size="18" />预约新的门诊</RouterLink
      >
    </div>
    <PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="reload"
    />
    <div
      v-if="bookingNotice"
      class="patient-notice patient-notice-success"
    >
      预约已成功提交，请留意预约时间与挂号状态。
    </div>
    <section class="panel">
      <div class="patient-list-toolbar">
        <div class="patient-tabs" aria-label="挂号状态">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="{ active: filter === tab.value }"
            :aria-pressed="filter === tab.value"
            @click="filter = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <label class="patient-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            type="search"
            placeholder="搜索医生、科室或日期"
            aria-label="搜索挂号"
        /></label>
      </div>
      <EmptyState
        v-if="!loading && !error && !records.length"
        title="暂无符合条件的挂号"
        description="选择其他状态或预约一次门诊，就医安排会显示在这里。"
      />
      <div class="patient-appointment-list">
        <article
          v-for="apt in records"
          :key="apt.id"
          class="patient-appointment-card"
        >
          <div class="patient-appointment-main">
            <div class="patient-appointment-date">
              <UiIcon name="calendar" :size="23" /><strong>{{
                formatDate(apt.appointmentDate)
              }}</strong
              ><span>{{ text(apt.appointmentTime) }}</span>
            </div>
            <div class="patient-appointment-info">
              <div class="patient-card-heading">
                <h3>{{ doctorName(apt) }}</h3>
                <StatusBadge :status="apt.status" />
              </div>
              <p>
                {{ text(data.doctorMap?.[apt.doctorId]?.department)
                }}<span v-if="data.clinicTypeMap?.[apt.id]">
                  · {{ data.clinicTypeMap[apt.id] }}</span
                >
              </p>
              <p v-if="apt.symptoms" class="patient-muted">
                就诊诉求：{{ apt.symptoms }}
              </p>
              <small
                >预约编号 #{{ apt.id }} ·
                {{ formatDate(apt.createdAt) }} 提交</small
              >
            </div>
          </div>
          <div v-if="hasRecord(apt) || ['PENDING', 'CONFIRMED'].includes(enumName(apt.status))" class="patient-card-actions">
            <RouterLink
              v-if="hasRecord(apt)"
              :to="`/app/patient/appointments/${apt.id}/record`"
              class="button button-primary"
              >查看就诊病历</RouterLink
            ><button
              v-if="['PENDING', 'CONFIRMED'].includes(enumName(apt.status))"
              type="button"
              class="button button-secondary"
              :disabled="submitting || loading"
              @click="cancel(apt)"
            >
            {{ submitting ? "正在处理…" : "取消挂号" }}
            </button>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
