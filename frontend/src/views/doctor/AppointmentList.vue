<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, useDoctorPagination, list, mappedName } from "./useDoctorPage";
import { formatDate } from "@/lib/format";
import "@/styles/doctor.css";

const route = useRoute();
const history = computed(() => route.path.endsWith("/appointments"));
const { data, loading, error, loadFailed, load } = useDoctorPage(() =>
  history.value ? "/doctor/appointments" : "/doctor/diagnose",
);
const search = ref("");
const status = ref("");
const all = computed(() => list(data.value, "appointments"));
const filtered = computed(() =>
  all.value.filter(
    (apt) =>
      (!status.value || apt.status === status.value) &&
      `${mappedName(data.value.userMap, apt.patientId)} ${apt.symptoms || ""} ${apt.id}`
        .toLowerCase()
        .includes(search.value.trim().toLowerCase()),
  ),
);
const completed = computed(
  () => all.value.filter((row) => row.status === "COMPLETED").length,
);
const { page, pages, visible } = useDoctorPagination(filtered);
</script>

<template>
  <div class="doctor-view">
    <PageHeader
      :title="history ? '就诊档案' : '门诊接诊'"
      :description="
        history
          ? '回顾患者就诊记录，查阅病历与处方。'
          : '按预约时段接诊，完成病历记录、处方与检查医嘱。'
      "
    />
    <DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <template v-if="!loading && !loadFailed">
    <div class="doctor-inline-metrics">
      <span
        >{{ history ? "挂号记录" : "今日待诊"
        }}<strong>{{ all.length }}</strong></span
      ><span v-if="history"
        >已完成<strong>{{ completed }}</strong></span
      ><span v-if="history"
        >病历档案<strong>{{
          Object.keys(data.recordMap || {}).length
        }}</strong></span
      ><span v-else class="doctor-muted"
        ><UiIcon name="clock" :size="16" /> 按预约顺序接诊</span
      >
    </div>
    <section class="panel">
      <div class="toolbar doctor-list-toolbar">
        <label class="doctor-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            aria-label="搜索患者"
            placeholder="搜索患者姓名、主诉或挂号编号" /></label
        ><select
          v-if="history"
          v-model="status"
          class="field"
          aria-label="筛选挂号状态"
        >
          <option value="">全部状态</option>
          <option value="COMPLETED">已完成</option>
          <option value="PENDING">待诊</option>
          <option value="CONFIRMED">已确认</option>
          <option value="CANCELLED">已取消</option></select
        ><span class="doctor-result-count">共 {{ filtered.length }} 条</span>
        <button v-if="search || status" type="button" class="doctor-text-link" @click="search = ''; status = ''">重置筛选</button>
      </div>
      <EmptyState
        v-if="!loading && !filtered.length"
        :title="
          search || status
            ? '没有匹配的患者'
            : history
              ? '暂无就诊记录'
              : '今日暂无待诊患者'
        "
        description="可调整搜索条件，或稍后重新加载。"
      />
      <div v-if="filtered.length" class="table-scroll">
        <table class="doctor-table">
          <thead>
            <tr>
              <th>序号 / 编号</th>
              <th>患者姓名</th>
              <th>预约时间</th>
              <th>挂号状态</th>
              <th>主诉</th>
              <th v-if="history">病历</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(apt, index) in visible" :key="apt.id">
              <td>
                <span class="doctor-table-index">{{
                  history ? `#${apt.id}` : String((page - 1) * 10 + index + 1).padStart(2, "0")
                }}</span>
              </td>
              <td>
                <strong>{{ mappedName(data.userMap, apt.patientId) }}</strong>
              </td>
              <td>
                {{ formatDate(apt.appointmentDate)
                }}<small class="doctor-cell-sub">{{
                  apt.appointmentTime || "时间待定"
                }}</small>
              </td>
              <td><StatusBadge :status="apt.status" /></td>
              <td class="doctor-symptom-cell">
                {{ apt.symptoms || "未填写主诉" }}
              </td>
              <td v-if="history">
                <span
                  :class="
                    data.recordMap?.[apt.id]
                      ? 'doctor-green-text'
                      : 'doctor-muted'
                  "
                  >{{ data.recordMap?.[apt.id] ? "已归档" : "未生成" }}</span
                >
              </td>
              <td>
                <div class="doctor-row-actions">
                  <RouterLink
                    v-if="history && data.recordMap?.[apt.id]"
                    :to="`/app/doctor/appointments/${apt.id}/record`"
                    class="button button-secondary"
                    >病历</RouterLink
                  ><RouterLink
                    v-if="apt.status !== 'CANCELLED'"
                    :to="`/app/doctor/diagnose/${apt.id}`"
                    class="button"
                    :class="history ? 'button-secondary' : 'button-primary'"
                    >{{ history ? "补充诊断" : "开始接诊" }}</RouterLink
                  >
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="pages > 1" class="doctor-pagination" aria-label="患者列表分页"><span>第 {{ page }} / {{ pages }} 页</span><button class="button button-secondary" :disabled="page === 1" @click="page--">上一页</button><button class="button button-secondary" :disabled="page === pages" @click="page++">下一页</button></nav>
    </section>
    </template>
  </div>
</template>
