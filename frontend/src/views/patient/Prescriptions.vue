<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import PatientPageState from "./PatientPageState.vue";
import {
  currency as money,
  enumName,
  list,
  usePatientPage,
} from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
const { data, loading, error, reload } = usePatientPage(
  () => "/patient/prescriptions",
);
const filter = ref("PENDING");
const prescriptions = computed(() =>
  list(data.value, "prescriptions").filter(
    (rx) =>
      filter.value === "ALL" ||
      (filter.value === "PENDING"
        ? enumName(rx.status) === "PENDING"
        : enumName(rx.status) !== "PENDING"),
  ),
);
const pendingExams = computed(() => list(data.value, "examReportsPendingPay"));
const examName = (id: any, fallback: any) =>
  data.value.examReportExaminationMap?.[id]?.name || fallback;
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="处方与取药"
      description="查看门诊处方，完成费用支付后确认取药。"
    /><PatientPageState :loading="loading" :error="error" @retry="reload" />
    <section v-if="pendingExams.length" class="panel">
      <div class="patient-section-heading">
        <div>
          <h2>待缴费检查</h2>
          <p>支付后可由医生继续填写检查报告</p>
        </div>
        <span class="patient-count">{{ pendingExams.length }} 项待处理</span>
      </div>
      <article
        v-for="rep in pendingExams"
        :key="rep.id"
        class="patient-payable-row"
      >
        <span class="patient-task-icon"><UiIcon name="file" :size="22" /></span>
        <div>
          <strong>{{ examName(rep.id, rep.reportType) }}</strong>
          <p>开单时间 {{ formatDate(rep.reportDate) }}</p>
        </div>
        <strong class="patient-price">{{
          money(data.examReportExaminationMap?.[rep.id]?.price || 0)
        }}</strong
        ><RouterLink
          :to="`/app/patient/payments/report/${rep.id}/alipay`"
          class="button button-primary"
          >去缴费</RouterLink
        >
      </article>
    </section>
    <section class="panel">
      <div class="patient-list-toolbar">
        <div class="patient-tabs">
          <button
            :class="{ active: filter === 'PENDING' }"
            :aria-pressed="filter === 'PENDING'"
            @click="filter = 'PENDING'"
          >
            待处理处方</button
          ><button
            :class="{ active: filter === 'HISTORY' }"
            :aria-pressed="filter === 'HISTORY'"
            @click="filter = 'HISTORY'"
          >
            历史处方</button
          ><button
            :class="{ active: filter === 'ALL' }"
            :aria-pressed="filter === 'ALL'"
            @click="filter = 'ALL'"
          >
            全部
          </button>
        </div>
        <span class="patient-count">{{ prescriptions.length }} 张处方</span>
      </div>
      <EmptyState
        v-if="!loading && !error && !prescriptions.length"
        title="暂无相关处方"
        description="医生开具的药品处方会显示在这里。"
      />
      <div class="patient-rx-grid">
        <article
          v-for="rx in prescriptions"
          :key="rx.id"
          class="patient-rx-card"
        >
          <div class="patient-card-heading">
            <span class="patient-rx-number"
              ><UiIcon name="pill" :size="20" />{{
                text(rx.prescriptionNumber)
              }}</span
            ><StatusBadge :status="rx.status" />
          </div>
          <dl class="patient-facts">
            <div>
              <dt>开方医生</dt>
              <dd>{{ text(data.userMap?.[rx.doctorId]?.realName) }}</dd>
            </div>
            <div>
              <dt>开方时间</dt>
              <dd>{{ formatDate(rx.createdAt) }}</dd>
            </div>
            <div>
              <dt>支付状态</dt>
              <dd>
                <span
                  :class="
                    rx.paid ? 'patient-success-text' : 'patient-warning-text'
                  "
                  >{{ rx.paid ? "已支付" : "未支付" }}</span
                >
              </dd>
            </div>
            <div v-if="rx.patientSignature">
              <dt>取药确认</dt>
              <dd>
                {{ rx.patientSignature }} · {{ formatDate(rx.collectedAt) }}
              </dd>
            </div>
            <div v-if="rx.dispensedAt">
              <dt>发药时间</dt>
              <dd>{{ formatDate(rx.dispensedAt) }}</dd>
            </div>
          </dl>
          <div class="patient-card-actions">
            <RouterLink
              :to="`/app/patient/prescriptions/${rx.id}`"
              class="button"
              :class="
                enumName(rx.status) === 'PENDING'
                  ? 'button-primary'
                  : 'button-secondary'
              "
              >{{
                enumName(rx.status) === "PENDING"
                  ? "查看处方与取药"
                  : "查看处方详情"
              }}<UiIcon name="chevron-right" :size="16"
            /></RouterLink>
          </div>
        </article>
      </div>
    </section>
    <section v-if="list(data, 'examReportsPaidHistory').length" class="panel">
      <div class="patient-section-heading">
        <div>
          <h2>已缴费检查</h2>
          <p>查看检查报告进度与结果</p>
        </div>
      </div>
      <RouterLink
        v-for="rep in list(data, 'examReportsPaidHistory')"
        :key="rep.id"
        :to="`/app/patient/reports/${rep.id}?from=prescriptions`"
        class="patient-report-row"
        ><span class="patient-task-icon"
          ><UiIcon name="file" :size="21"
        /></span>
        <div>
          <strong>{{ examName(rep.id, rep.reportType) }}</strong>
          <p>{{ formatDate(rep.paidAt) }} 付款</p>
        </div>
        <StatusBadge :status="rep.status" /><UiIcon
          name="chevron-right"
          :size="18"
      /></RouterLink>
    </section>
  </div>
</template>
