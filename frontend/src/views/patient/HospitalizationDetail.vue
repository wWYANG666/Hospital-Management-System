<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import PatientPageState from "./PatientPageState.vue";
import { list, usePatientPage } from "./usePatientPage";
import { formatDate, statusText, text } from "@/lib/format";
const route = useRoute();
const { data, loading, error, reload } = usePatientPage(
  () => `/patient/hospitalizations/${route.params.id}/detail`,
);
const showRecordContent = (record: Record<string, any>) => {
  const update = String(record.conditionUpdate || "").trim();
  const content = String(record.medicalRecordContent || "").trim();
  return (
    content &&
    ![update, "病情更新：" + update, "病情更新:" + update].includes(content)
  );
};
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="住院详情"
      description="查看本次住院信息、医嘱与病情记录。"
    />
    <div class="patient-page-actions">
      <RouterLink
        to="/app/patient/hospitalizations"
        class="button button-secondary"
        >返回住院列表</RouterLink
      >
    </div>
    <PatientPageState
      :loading="loading"
      :error="error"
      @retry="reload"
    /><template v-if="data.hospitalization"
      ><section class="panel">
        <div class="patient-section-heading">
          <h2>住院记录 #{{ data.hospitalization.id }}</h2>
          <StatusBadge :status="data.hospitalization.status" />
        </div>
        <dl class="patient-facts">
          <div>
            <dt>主治医生</dt>
            <dd>{{ text(data.doctorDisplay) }}</dd>
          </div>
          <div>
            <dt>病房与床位</dt>
            <dd v-if="data.bed">
              {{ data.bed.ward }} · {{ data.bed.roomNumber }} 房 ·
              {{ data.bed.bedNumber }} 床
            </dd>
            <dd v-else>
              {{
                data.hospitalization.bedId ? "床位信息待完善" : "等待床位安排"
              }}
            </dd>
          </div>
          <div>
            <dt>入院日期</dt>
            <dd>{{ formatDate(data.hospitalization.admissionDate) }}</dd>
          </div>
          <div v-if="data.hospitalization.dischargeDate">
            <dt>出院日期</dt>
            <dd>{{ formatDate(data.hospitalization.dischargeDate) }}</dd>
          </div>
          <div class="patient-fact-full">
            <dt>诊断</dt>
            <dd class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="住院诊断完整内容">
              {{
                data.hospitalization.diagnosis ||
                data.hospitalization.dischargeDiagnosis ||
                "暂无诊断"
              }}
            </dd>
          </div>
        </dl>
      </section>
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>住院医嘱</h2>
            <p>用药、检查、护理与其他医嘱</p>
          </div>
          <span class="patient-count"
            >{{ list(data, "orders").length }} 条</span
          >
        </div>
        <EmptyState v-if="!list(data, 'orders').length" title="暂无医嘱记录" />
        <div v-else class="table-scroll">
          <table class="patient-table">
            <thead>
              <tr>
                <th>医嘱类型</th>
                <th>医嘱内容</th>
                <th>状态</th>
                <th>开具时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in list(data, 'orders')" :key="order.id">
                <td>{{ statusText(order.orderType) }}</td>
                <td>
                  <div class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="医嘱完整内容">{{ text(order.orderContent) }}</div><small v-if="order.quantity"
                    >数量：{{ order.quantity
                    }}<span v-if="order.dosage"> · {{ order.dosage }}</span
                    ><span v-if="order.frequency">
                      · {{ order.frequency }}</span
                    ></small
                  >
                </td>
                <td><StatusBadge :status="order.status" /></td>
                <td>{{ formatDate(order.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>住院病历</h2>
            <p>医生记录的病情变化与诊疗情况</p>
          </div>
        </div>
        <EmptyState v-if="!list(data, 'records').length" title="暂无病历记录" />
        <article
          v-for="record in list(data, 'records')"
          :key="record.id"
          class="patient-record-timeline"
        >
          <div class="patient-record-time">
            {{ formatDate(record.createdAt) }}
          </div>
          <section v-if="record.diagnosis" class="patient-record-section">
            <h3>诊断</h3>
            <p class="patient-preformatted read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="住院诊断完整内容">{{ record.diagnosis }}</p>
          </section>
          <section v-if="record.conditionUpdate" class="patient-record-section">
            <h3>病情更新</h3>
            <p class="patient-preformatted read-scroll" tabindex="0" role="region" aria-label="病情更新完整内容">{{ record.conditionUpdate }}</p>
          </section>
          <section
            v-if="showRecordContent(record)"
            class="patient-record-section"
          >
            <h3>病历内容</h3>
            <p class="patient-preformatted read-scroll" tabindex="0" role="region" aria-label="住院病历完整内容">
              {{ record.medicalRecordContent }}
            </p>
          </section>
          <p
            v-if="
              !record.conditionUpdate &&
              !record.medicalRecordContent &&
              !record.diagnosis
            "
            class="patient-muted"
          >
            暂无详细内容
          </p>
        </article>
      </section></template
    >
  </div>
</template>
