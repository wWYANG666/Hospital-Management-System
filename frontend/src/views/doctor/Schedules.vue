<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import { formatDate } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
import { scheduleState, scheduleLabel } from "@/lib/schedule";
import DoctorFeedback from "./DoctorFeedback.vue";
import {
  useDoctorPage,
  useDoctorPagination,
  list,
  type DoctorData,
} from "./useDoctorPage";
import "@/styles/doctor.css";
const { data, loading, saving, error, notice, loadFailed, load, action } =
  useDoctorPage(() => "/doctor/schedules");
const filter = ref("all");
const all = computed(() =>
  list(data.value, "schedules")
    .slice()
    .sort((a, b) => {
      const aEnded = scheduleState(a) === "ENDED",
        bEnded = scheduleState(b) === "ENDED";
      if (aEnded !== bEnded) return aEnded ? 1 : -1;
      const order = String(a.workDate || "").localeCompare(
        String(b.workDate || ""),
      );
      return aEnded ? -order : order;
    }),
);
const filtered = computed(() =>
  all.value.filter(
    (s) => filter.value === "all" || scheduleState(s) === filter.value,
  ),
);
const summary = computed(() => ({
  active: all.value.filter((s) => scheduleState(s) === "ACTIVE").length,
  pending: all.value.filter((s) => scheduleState(s) === "LEAVE_PENDING").length,
  ended: all.value.filter((s) => scheduleState(s) === "ENDED").length,
  booked: all.value.reduce((n, s) => n + Number(s.currentAppointments || 0), 0),
}));
const { page, pages, visible } = useDoctorPagination(filtered);
const names: Record<string, string> = {
  MORNING: "上午门诊",
  AFTERNOON: "下午门诊",
  EVENING: "晚间门诊",
};
async function submitLeave(schedule: DoctorData) {
  if (scheduleState(schedule) !== "ACTIVE") return;
  if (
    saving.value ||
    !(await confirmAction(
      `${formatDate(schedule.workDate)} · ${names[schedule.workTime] || schedule.workTime}，已有 ${schedule.currentAppointments || 0} 位预约患者。提交后由管理员审批，请确认已安排就诊衔接。`,
      "确认申请请假",
    ))
  )
    return;
  await action(`/doctor/schedules/${schedule.id}/leave`, {});
}
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="排班管理"
      description="查看门诊班次、号源配置与请假审批。"
    />
    <DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <template v-if="!loading && !loadFailed">
      <div class="doctor-inline-metrics">
        <span
          >排班总数<strong>{{ all.length }}<small>班次</small></strong></span
        >
        <span
          >有效启用班次<strong>{{ summary.active }}</strong></span
        >
        <span
          >请假审批中<strong>{{ summary.pending }}</strong></span
        >
        <span
          >已结束班次<strong>{{ summary.ended }}</strong></span
        >
        <span
          >累计已预约<strong
            >{{ summary.booked }}<small>人</small></strong
          ></span
        >
      </div>
      <section class="panel">
        <div class="doctor-panel-heading">
          <div>
            <h2>门诊排班</h2>
            <p>可对未结束的启用班次发起请假申请；历史记录显示为已结束</p>
          </div>
          <select v-model="filter" class="field" aria-label="筛选班次状态">
            <option value="all">全部班次</option>
            <option value="ACTIVE">启用中</option>
            <option value="LEAVE_PENDING">审批中</option>
            <option value="DISABLED">已停用</option>
            <option value="ENDED">已结束</option>
          </select>
        </div>
        <EmptyState
          v-if="!loading && !filtered.length"
          title="暂无符合条件的排班"
        />
        <div v-if="filtered.length" class="table-scroll">
          <table class="doctor-table">
            <thead>
              <tr>
                <th>出诊日期</th>
                <th>门诊班次</th>
                <th>预约 / 总号源</th>
                <th>号源使用</th>
                <th>班次状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="s in visible" :key="s.id">
                <td>
                  <strong>{{ formatDate(s.workDate) }}</strong>
                </td>
                <td>{{ names[s.workTime] || s.workTime }}</td>
                <td>
                  {{ s.currentAppointments || 0 }} /
                  {{ s.maxAppointments || 0 }}
                </td>
                <td>
                  <div class="doctor-capacity doctor-table-capacity">
                    <span
                      :style="{
                        width: `${Math.min(100, (Number(s.currentAppointments || 0) / Math.max(1, Number(s.maxAppointments || 0))) * 100)}%`,
                      }"
                    ></span>
                  </div>
                </td>
                <td>
                  <span
                    class="doctor-tag"
                    :class="
                      scheduleState(s) === 'ACTIVE'
                        ? 'doctor-tag-success'
                        : scheduleState(s) === 'LEAVE_PENDING'
                          ? 'doctor-tag-warning'
                          : ''
                    "
                    >{{ scheduleLabel(s) }}</span
                  >
                </td>
                <td>
                  <button
                    v-if="scheduleState(s) === 'ACTIVE'"
                    class="button button-secondary"
                    :disabled="saving"
                    @click="submitLeave(s)"
                  >
                    申请请假</button
                  ><span v-else class="doctor-muted">{{
                    scheduleState(s) === "LEAVE_PENDING"
                      ? "等待管理员审批"
                      : "—"
                  }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <nav v-if="pages > 1" class="doctor-pagination" aria-label="排班分页">
          <span>第 {{ page }} / {{ pages }} 页</span
          ><button
            class="button button-secondary"
            :disabled="page === 1"
            @click="page--"
          >
            上一页</button
          ><button
            class="button button-secondary"
            :disabled="page === pages"
            @click="page++"
          >
            下一页
          </button>
        </nav>
      </section>
    </template>
    <section class="panel">
      <div class="doctor-panel-heading">
        <div>
          <h2>今日班次预约详情</h2>
          <p>查看各时段已预约患者</p>
        </div>
      </div>
      <div class="doctor-shift-links">
        <RouterLink
          v-for="(label, time) in names"
          :key="time"
          :to="`/app/doctor/schedule-detail?workTime=${time}`"
          class="doctor-shift-link"
          ><UiIcon name="clock" :size="19" /><strong>{{ label }}</strong
          ><UiIcon name="chevron-right" :size="16"
        /></RouterLink>
      </div>
    </section>
  </div>
</template>
