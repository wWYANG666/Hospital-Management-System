<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import AdminFeedback from "./AdminFeedback.vue";
import PrescriptionItems from "./PrescriptionItems.vue";
import { useAdminPage, items, appPath } from "./useAdminPage";
import { formatDate } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const route = useRoute();
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => `/admin/pharmacy/${route.params.id}`,
);
const prescription = computed(() => data.value.prescription || {});
const canDispense = computed(
  () =>
    prescription.value.status === "PENDING" &&
    prescription.value.paid &&
    !!String(prescription.value.patientSignature || "").trim(),
);
async function dispense() {
  if (await confirmAction("已核对处方与药品，确认发药并扣减库存？"))
    await action(`/admin/pharmacy/${route.params.id}/dispense`);
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      title="处方核对与发药"
      description="核对药品明细、患者付款与取药签字后办理发药。"
      ><RouterLink
        class="button button-secondary"
        :to="appPath(data.adminBackUrl || '/admin/patients')"
        ><UiIcon name="chevron-left" :size="17" />返回档案</RouterLink
      ></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    /><template v-if="!loading"
      ><section class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>
              {{
                prescription.prescriptionNumber || `处方 #${route.params.id}`
              }}
            </h2>
            <p>开具时间 {{ formatDate(prescription.createdAt, true) }}</p>
          </div>
          <StatusBadge :status="prescription.status" />
        </div>
        <dl class="admin-info-grid">
          <div>
            <dt>患者姓名</dt>
            <dd>{{ data.patientUser?.realName || "未登记" }}</dd>
          </div>
          <div>
            <dt>开方医生</dt>
            <dd>{{ data.doctorUser?.realName || "未登记" }}</dd>
          </div>
          <div>
            <dt>所属科室</dt>
            <dd>{{ data.doctor?.department || "未登记" }}</dd>
          </div>
        </dl>
        <div class="admin-status-flow">
          <div>
            <strong
              ><UiIcon
                :name="prescription.paid ? 'check' : 'clock'"
                :size="17"
              />费用支付</strong
            >{{ prescription.paid ? "患者已完成支付" : "等待患者支付" }}
            <p>{{ formatDate(prescription.paidAt, true) }}</p>
          </div>
          <div>
            <strong
              ><UiIcon
                :name="prescription.patientSignature ? 'check' : 'clock'"
                :size="17"
              />患者取药确认</strong
            >{{
              prescription.patientSignature
                ? `已签字：${prescription.patientSignature}`
                : "等待患者签字确认"
            }}
            <p>{{ formatDate(prescription.collectedAt, true) }}</p>
          </div>
          <div>
            <strong
              ><UiIcon
                :name="prescription.status === 'DISPENSED' ? 'check' : 'clock'"
                :size="17"
              />药房发药</strong
            >{{
              prescription.status === "DISPENSED" ? "已完成发药" : "尚未发药"
            }}
            <p>{{ formatDate(prescription.dispensedAt, true) }}</p>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>处方药品明细</h2>
            <p>核对名称、规格、数量和用法用量</p>
          </div>
        </div>
        <PrescriptionItems
          :items="items(data, 'items')"
          :total="data.totalAmount"
        />
      </section>
      <section v-if="prescription.status === 'PENDING'" class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>药房处理</h2>
            <p>
              {{
                canDispense
                  ? "患者已完成付款与签字，核对药品后可办理发药。"
                  : "患者完成支付并签字确认取药后，可办理发药。"
              }}
            </p>
          </div>
        </div>
        <button
          class="button button-primary"
          :disabled="!canDispense || saving"
          @click="dispense"
        >
          <UiIcon name="pill" :size="17" />{{ saving ? "处理中…" : "确认发药" }}
        </button>
      </section></template
    >
  </div>
</template>
