<script setup lang="ts">
import { computed } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, list, mappedName } from "./useDoctorPage";
import { formatDate } from "@/lib/format";
import "@/styles/doctor.css";

const { data, loading, error, loadFailed, load } = useDoctorPage(() => "/doctor/dashboard");
const queue = computed(() => list(data.value, "todayPendingAppointments"));
const schedules = computed(() => list(data.value, "scheduleSummaryList"));
const recent = computed(() => list(data.value, "recentCompletedAppointments"));
const dateLabel = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
</script>

<template>
  <div class="doctor-view">
    <PageHeader
      title="门诊工作台"
      description="聚焦今日接诊、检查报告与住院患者，及时处理诊疗任务。"
    />
    <DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <section v-if="data.accountUnbound" class="panel doctor-unbound">
      <UiIcon name="shield" :size="36" />
      <h2>账号尚未关联医生档案</h2>
      <p>请联系管理员完善所属科室和医生档案后进入工作台。</p>
    </section>
    <template v-else-if="!loading && !loadFailed">
      <div class="doctor-inline-metrics doctor-workload" aria-label="今日任务概览">
        <span>今日待诊<strong>{{ queue.length }}<small>人</small></strong></span>
        <span>待完成报告<strong>{{ list(data, "pendingReports").length }}<small>份</small></strong></span>
        <span>住院待办<strong>{{ data.totalPendingHospTasks ?? 0 }}<small>项</small></strong></span>
        <span>今日门诊时段<strong>{{ schedules.length }}<small>个</small></strong></span>
      </div>
      <div class="doctor-dashboard-grid">
        <section class="panel doctor-queue-panel">
          <div class="doctor-panel-heading">
            <div>
              <h2>
                今日候诊队列
                <span class="doctor-count">{{ queue.length }}</span>
              </h2>
              <p>{{ dateLabel }} · 按预约时段接诊</p>
            </div>
            <RouterLink class="doctor-text-link" to="/app/doctor/diagnose"
              >完整队列 <UiIcon name="arrow-right" :size="15"
            /></RouterLink>
          </div>
          <EmptyState
            v-if="!queue.length"
            title="今日暂无待诊患者"
            description="新的预约会显示在这里，可前往就诊档案查阅已完成病历。"
          />
          <div v-else class="doctor-patient-queue">
            <article
              v-for="(apt, index) in queue.slice(0, 8)"
              :key="apt.id"
              class="doctor-queue-row"
            >
              <span
                class="doctor-queue-number"
                :class="{ 'is-next': index === 0 }"
                >{{ String(index + 1).padStart(2, "0") }}</span
              >
              <div class="doctor-queue-person">
                <strong>{{
                  mappedName(data.userMapForDashboard, apt.patientId)
                }}</strong>
                <p>{{ apt.symptoms || "患者暂未填写主诉" }}</p>
              </div>
              <div class="doctor-queue-time">
                <strong>{{
                  data.allTimeSlotMap?.[apt.id] ||
                  apt.appointmentTime ||
                  "时间待定"
                }}</strong
                ><StatusBadge :status="apt.status" />
              </div>
              <RouterLink
                class="button"
                :class="index === 0 ? 'button-primary' : 'button-secondary'"
                :to="`/app/doctor/diagnose/${apt.id}`"
                >{{ index === 0 ? "开始接诊" : "接诊" }}</RouterLink
              >
            </article>
          </div>
        </section>
        <section class="panel doctor-schedule-panel">
          <div class="doctor-panel-heading">
            <div>
              <h2>今日出诊安排</h2>
              <p>号源使用情况与班次</p>
            </div>
            <UiIcon name="calendar" :size="19" />
          </div>
          <EmptyState
            v-if="!schedules.length"
            title="今日暂无排班"
            description="可在排班管理中查看其他日期的安排。"
          />
          <article
            v-for="(summary, index) in schedules"
            :key="index"
            class="doctor-shift"
          >
            <div class="doctor-shift-heading">
              <strong>{{ summary.workTimeName }}</strong
              ><span
                >{{ summary.totalUsed }} / {{ summary.totalMax }} 号源</span
              >
            </div>
            <div class="doctor-capacity">
              <span
                :style="{
                  width: `${Math.min(100, (Number(summary.totalUsed || 0) / Math.max(1, Number(summary.totalMax || 0))) * 100)}%`,
                }"
              ></span>
            </div>
            <p>{{ summary.appointments?.length || 0 }} 位预约患者</p>
          </article>
          <RouterLink
            to="/app/doctor/schedules"
            class="button button-secondary doctor-full-width"
            >查看全部排班</RouterLink
          >
        </section>
      </div>
      <details class="panel doctor-history-details">
        <summary class="doctor-panel-heading">
          <div>
            <h2>最近完成的诊疗</h2>
            <p>{{ recent.length }} 条近期记录 · 展开查阅</p>
          </div>
          <UiIcon name="chevron-down" :size="18" />
        </summary>
        <EmptyState v-if="!recent.length" title="暂无近期诊疗记录" />
        <div v-else class="table-scroll read-scroll read-scroll--list" tabindex="0" role="region" aria-label="最近完成的诊疗记录">
          <table class="doctor-table">
            <thead>
              <tr>
                <th>患者</th>
                <th>就诊日期</th>
                <th>主诉</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="apt in recent.slice(0, 6)" :key="apt.id">
                <td>
                  <strong>{{
                    mappedName(data.recentUserMap, apt.patientId)
                  }}</strong>
                </td>
                <td>
                  {{ formatDate(apt.appointmentDate) }}
                  {{ apt.appointmentTime }}
                </td>
                <td class="doctor-symptom-cell">
                  {{ apt.symptoms || "未填写" }}
                </td>
                <td>
                  <RouterLink
                    :to="`/app/doctor/appointments/${apt.id}/record`"
                    class="doctor-text-link"
                    >查阅病历</RouterLink
                  >
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <RouterLink to="/app/doctor/appointments" class="doctor-text-link">查看全部就诊档案 <UiIcon name="arrow-right" :size="15" /></RouterLink>
      </details>
    </template>
  </div>
</template>
