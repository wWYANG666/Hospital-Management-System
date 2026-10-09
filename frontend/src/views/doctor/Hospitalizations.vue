<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import { formatDate, money } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import {
  useDoctorPage,
  useDoctorPagination,
  list,
  mappedName,
  type DoctorData,
} from "./useDoctorPage";
import "@/styles/doctor.css";
const { data, loading, error, loadFailed, load } = useDoctorPage(
  () => "/doctor/hospitalizations",
);
const search = ref("");
const filter = ref("all");
const all = computed(() => list(data.value, "hospitalizations"));
const pendingDischarge = computed(() =>
  list(data.value, "dischargeRequestHospitalizations"),
);
function stage(hosp: DoctorData) {
  if (hosp.status === "DISCHARGED") return "DISCHARGED";
  if (hosp.requestStatus === "REJECTED") return "REJECTED";
  if (!hosp.bedId) return "UNASSIGNED";
  if (hosp.requestStatus === "PENDING") return "PENDING";
  return "ADMITTED";
}
const filtered = computed(() =>
  all.value.filter(
    (hosp) =>
      (filter.value === "all" || stage(hosp) === filter.value) &&
      `${mappedName(data.value.userMap, hosp.patientId)} ${data.value.bedMap?.[hosp.bedId]?.bedNumber || ""} ${hosp.id}`.includes(
        search.value.trim(),
      ),
  ),
);
const { page, pages, visible } = useDoctorPagination(filtered);
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="住院患者"
      description="处理住院申请与床位分配，管理在院医嘱和出院手续。"
      ><RouterLink to="/app/doctor/appointments" class="button button-primary"
        ><UiIcon name="plus" :size="17" /> 从就诊档案开立申请</RouterLink
      ></PageHeader
    ><DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <template v-if="!loading && !loadFailed">
    <div class="doctor-inline-metrics">
      <span
        >在院患者<strong>{{
          all.filter((h) => stage(h) === "ADMITTED").length
        }}</strong></span
      ><span
        >待分床<strong>{{
          all.filter((h) => stage(h) === "UNASSIGNED").length
        }}</strong></span
      ><span
        >出院待处理<strong>{{ pendingDischarge.length }}</strong></span
      ><span
        >住院记录<strong>{{ all.length }}</strong></span
      >
    </div>
    <section
      v-if="pendingDischarge.length"
      class="panel doctor-discharge-requests"
    >
      <div class="doctor-panel-heading">
        <div>
          <h2><UiIcon name="clock" :size="18" /> 患者出院申请</h2>
          <p>核对病情与医嘱后办理出院</p>
        </div>
        <span class="doctor-count">{{ pendingDischarge.length }}</span>
      </div>
      <div class="doctor-review-grid">
        <article
          v-for="hosp in pendingDischarge"
          :key="hosp.id"
          class="doctor-review-card"
        >
          <h3>{{ mappedName(data.userMap, hosp.patientId) }}</h3>
          <p>
            {{ data.bedMap?.[hosp.bedId]?.bedNumber || "床位未分配" }} ·
            {{ formatDate(hosp.admissionDate) }} 入院
          </p>
          <div class="doctor-row-actions">
            <RouterLink
              :to="`/app/doctor/hospitalizations/${hosp.id}/manage`"
              class="button button-secondary"
              >查阅医嘱</RouterLink
            ><RouterLink
              :to="`/app/doctor/hospitalizations/${hosp.id}/discharge`"
              class="button button-primary"
              >办理出院</RouterLink
            >
          </div>
        </article>
      </div>
    </section>
    <section class="panel">
      <div class="toolbar doctor-list-toolbar">
        <label class="doctor-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            placeholder="搜索患者、床位号或住院编号"
            aria-label="搜索住院患者" /></label
        ><select v-model="filter" class="field" aria-label="住院状态">
          <option value="all">全部状态</option>
          <option value="ADMITTED">住院中</option>
          <option value="UNASSIGNED">待分配床位</option>
          <option value="PENDING">待审批</option>
          <option value="DISCHARGED">已出院</option>
          <option value="REJECTED">已拒绝</option></select
        ><span class="doctor-result-count">{{ filtered.length }} 条记录</span>
        <button v-if="search || filter !== 'all'" type="button" class="doctor-text-link" @click="search = ''; filter = 'all'">重置筛选</button>
      </div>
      <EmptyState
        v-if="!loading && !filtered.length"
        title="暂无匹配住院记录"
      />
      <div v-if="filtered.length" class="table-scroll">
        <table class="doctor-table">
          <thead>
            <tr>
              <th>患者</th>
              <th>入院 / 申请日期</th>
              <th>床位</th>
              <th>诊断</th>
              <th>状态</th>
              <th>累计医嘱费用</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="hosp in visible" :key="hosp.id">
              <td>
                <strong>{{ mappedName(data.userMap, hosp.patientId) }}</strong
                ><small class="doctor-cell-sub">住院 #{{ hosp.id }}</small>
              </td>
              <td>
                {{ formatDate(hosp.admissionDate)
                }}<small class="doctor-cell-sub"
                  >申请 {{ formatDate(hosp.createdAt) }}</small
                >
              </td>
              <td>{{ data.bedMap?.[hosp.bedId]?.bedNumber || "待分配" }}</td>
              <td class="doctor-symptom-cell">
                {{ hosp.diagnosis || hosp.admissionReason || "暂无诊断" }}
              </td>
              <td>
                <span
                  v-if="stage(hosp) === 'UNASSIGNED'"
                  class="doctor-tag doctor-tag-warning"
                  >待分配床位</span
                ><StatusBadge v-else :status="stage(hosp)" /><small
                  v-if="data.dischargeRequestHospIds?.includes(hosp.id)"
                  class="doctor-cell-sub doctor-amber-text"
                  >申请出院</small
                >
              </td>
              <td>¥{{ money(hosp.totalCost) }}</td>
              <td>
                <div class="doctor-row-actions">
                  <RouterLink
                    v-if="stage(hosp) === 'UNASSIGNED'"
                    :to="`/app/doctor/hospitalizations/${hosp.id}/assign-bed`"
                    class="button button-primary"
                    >分配床位</RouterLink
                  ><RouterLink
                    v-else-if="hosp.bedId || hosp.status === 'DISCHARGED'"
                    :to="`/app/doctor/hospitalizations/${hosp.id}/manage`"
                    class="button button-secondary"
                    >{{
                      hosp.status === "DISCHARGED" ? "查阅记录" : "管理医嘱"
                    }}</RouterLink
                  >
                  <span v-else class="doctor-muted">{{ stage(hosp) === 'REJECTED' ? '申请未通过' : '等待审批' }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <nav v-if="pages > 1" class="doctor-pagination" aria-label="住院记录分页"><span>第 {{ page }} / {{ pages }} 页</span><button class="button button-secondary" :disabled="page === 1" @click="page--">上一页</button><button class="button button-secondary" :disabled="page === pages" @click="page++">下一页</button></nav>
    </section>
    </template>
  </div>
</template>
