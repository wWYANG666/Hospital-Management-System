<script setup lang="ts">
import { computed, ref, watch } from "vue";
import StatusBadge from "@/components/StatusBadge.vue";
import UiIcon from "@/components/UiIcon.vue";
import { loadPage } from "@/lib/api";
import { formatDate, statusText } from "@/lib/format";
import { useAuth } from "@/stores/auth";

interface Dashboard {
  nextAppointment: {
    id: number;
    appointmentDate: string;
    appointmentTime: string;
    status: string;
    doctorName: string;
    department: string;
    clinicType: string;
  } | null;
  pendingPayments: { prescriptions: number; reports: number; hospitalizations: number };
  readyReportCount: number;
}
const { user } = useAuth();
const dashboard = ref<Dashboard | null>(null);
const loading = ref(false);
const error = ref("");
let requestVersion = 0;
const appointment = computed(() => dashboard.value?.nextAppointment);
const reminders = computed(() => {
  const items: { label: string; to: string; icon: string }[] = [];
  const pending = dashboard.value?.pendingPayments;
  if (pending?.prescriptions) items.push({ label: `${pending.prescriptions} 张处方待缴费`, to: "/app/patient/prescriptions", icon: "pill" });
  const reports: string[] = [];
  if (pending?.reports) reports.push(`${pending.reports} 项检查待缴费`);
  if (dashboard.value?.readyReportCount) reports.push(`${dashboard.value.readyReportCount} 份报告可查看`);
  if (reports.length) items.push({ label: reports.join(" · "), to: "/app/patient/reports", icon: "file" });
  if (pending?.hospitalizations) items.push({ label: `${pending.hospitalizations} 笔住院费用待缴费`, to: "/app/patient/hospitalizations", icon: "bed" });
  return items;
});
async function loadDashboard() {
  const version = ++requestVersion;
  dashboard.value = null;
  loading.value = true;
  error.value = "";
  try {
    const result = await loadPage<Dashboard>("/patient/dashboard");
    if (version === requestVersion) dashboard.value = result;
  } catch (cause) {
    if (version === requestVersion) error.value = cause instanceof Error ? cause.message : "就医安排加载失败，请重试。";
  } finally {
    if (version === requestVersion) loading.value = false;
  }
}
watch(() => user.value?.id, loadDashboard, { immediate: true });
</script>

<template>
  <div class="patient-portal-home patient-health-dashboard">
    <header class="patient-home-heading patient-health-heading">
      <div>
        <span class="patient-eyebrow"><span></span>个人就医中心</span>
        <h1>我的就医</h1>
        <p>{{ user?.realName || user?.username }}，查看您的预约、待缴费用与检查进度。</p>
      </div>
      <RouterLink to="/app/booking" class="button button-primary"><UiIcon name="calendar" :size="18" />预约新的门诊</RouterLink>
    </header>
    <section class="patient-home-plan" :class="{ 'patient-home-plan-single': loading || error || !reminders.length }" aria-label="就医安排">
      <div v-if="loading" class="patient-home-next patient-home-feedback" role="status">正在加载您的就医安排…</div>
      <div v-else-if="error" class="patient-home-next patient-home-feedback" role="alert">
        <UiIcon name="alert" :size="20" /><p>{{ error }}</p>
        <button type="button" class="button button-secondary" @click="loadDashboard">重新加载</button>
      </div>
      <article v-else-if="appointment" class="patient-home-next patient-next-appointment">
        <div class="patient-home-next-heading"><h2>下一次门诊</h2><StatusBadge :status="appointment.status" /></div>
        <div class="patient-home-next-details">
          <div class="patient-home-appointment-date"><UiIcon name="calendar" :size="22" /><strong>{{ formatDate(appointment.appointmentDate) }}</strong><span>{{ appointment.appointmentTime }}</span></div>
          <div class="patient-home-appointment-doctor"><strong>{{ appointment.department }} · {{ appointment.doctorName }}</strong><span>{{ statusText(appointment.clinicType) }}</span></div>
          <RouterLink to="/app/patient/appointments" class="patient-home-inline-link">查看预约<UiIcon name="chevron-right" :size="17" /></RouterLink>
        </div>
        <p class="patient-home-appointment-note">请按预约时间到院，并携带本人有效证件。</p>
      </article>
      <div v-else class="patient-home-next patient-home-empty">
        <UiIcon name="calendar" :size="24" /><div><h2>暂无待就诊预约</h2><p>已完成的就诊记录可在“我的挂号”中查看。</p></div>
      </div>
      <aside v-if="!loading && !error && reminders.length" class="patient-home-reminders" aria-labelledby="patient-reminder-title">
        <h2 id="patient-reminder-title">就诊提醒</h2>
        <RouterLink v-for="reminder in reminders" :key="reminder.to" :to="reminder.to" class="patient-home-reminder">
          <UiIcon :name="reminder.icon" :size="19" /><span>{{ reminder.label }}</span><UiIcon name="chevron-right" :size="16" />
        </RouterLink>
      </aside>
    </section>
    <p v-if="!loading && !error && !reminders.length" class="patient-health-clear"><UiIcon name="check" :size="18" />暂无待处理的缴费事项或近期已完成报告。</p>
  </div>
</template>
