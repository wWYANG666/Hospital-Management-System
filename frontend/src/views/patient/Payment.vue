<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import PatientPageState from "./PatientPageState.vue";
import { appPath, list, usePatientPage } from "./usePatientPage";
import { formatDate, money, text } from "@/lib/format";
const route = useRoute();
const paid = ref(false);
watch(() => route.fullPath, () => { paid.value = false; });
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(
    () => `/patient/payments/${route.params.type}/${route.params.id}/alipay`,
  );
watch(data, () => { if (data.value.alreadyPaid === true) paid.value = true; });
const title = computed(
  () =>
    ({
      PRESCRIPTION: "处方药品费用",
      EXAM_REPORT: "检查 / 检验费用",
      HOSPITALIZATION: "住院费用",
    })[String(data.value.paymentType)] || "医疗费用",
);
const cancelUrl = computed(() =>
  appPath(data.value.cancelUrl, "/app/patient/payments/history"),
);
// Once payment succeeds, continue with the business flow instead of sending
// the patient back to the generic payment history page.
const successReturnUrl = computed(() => {
  const id = data.value.businessId;
  if (data.value.paymentType === "PRESCRIPTION" && id) {
    return `/app/patient/prescriptions/${id}`;
  }
  if (data.value.paymentType === "EXAM_REPORT" && id) {
    return `/app/patient/reports/${id}`;
  }
  if (data.value.paymentType === "HOSPITALIZATION" && id) {
    return `/app/patient/hospitalizations/${id}/detail`;
  }
  return cancelUrl.value;
});
const successReturnLabel = computed(() => {
  if (data.value.paymentType === "PRESCRIPTION") return "返回处方，确认取药";
  if (data.value.paymentType === "EXAM_REPORT") return "返回检查报告";
  if (data.value.paymentType === "HOSPITALIZATION") return "返回住院记录";
  return "返回查看详情";
});
async function pay() {
  if (paid.value || loading.value || !data.value.businessId) return;
  const kind =
    data.value.paymentType === "PRESCRIPTION"
      ? "prescriptions"
      : data.value.paymentType === "EXAM_REPORT"
        ? "reports"
        : "hospitalizations";
  if (
    await action(
      `/patient/${kind}/${data.value.businessId}/pay`,
      {},
      "支付已完成",
      false,
    )
  )
    paid.value = true;
}
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      :title="paid ? '支付完成' : '费用确认'"
      :description="paid ? '费用状态已更新，请继续办理后续就医事项。' : '请核对患者、单号与费用明细后完成支付。'"
    /><PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="reload"
    />
    <div v-if="data.paymentType" class="patient-payment-layout">
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <span class="patient-eyebrow">支付订单</span>
            <h2>{{ title }}</h2>
          </div>
          <span class="patient-task-icon"
            ><UiIcon name="wallet" :size="25"
          /></span>
        </div>
        <dl class="patient-facts">
          <div>
            <dt>订单号</dt>
            <dd>{{ data.orderNumber }}</dd>
          </div>
          <div>
            <dt>就诊人</dt>
            <dd>{{ text(data.patientName) }}</dd>
          </div>
        </dl>
        <template v-if="data.paymentType === 'PRESCRIPTION'"
          ><h3 class="patient-detail-subtitle">药品费用明细</h3>
          <div class="table-scroll">
            <table class="patient-table">
              <thead>
                <tr>
                  <th>药品 / 规格</th>
                  <th>数量</th>
                  <th>单价</th>
                  <th>小计</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="item in list(data, 'prescriptionItems')"
                  :key="item.id"
                >
                  <td>
                    {{ item.medicineName
                    }}<small>{{ item.specification }}</small>
                  </td>
                  <td>{{ text(item.quantity) }} {{ item.unit }}</td>
                  <td>¥{{ money(item.price) }}</td>
                  <td>¥{{ money(item.totalPrice) }}</td>
                </tr>
              </tbody>
            </table>
          </div></template
        ><template v-if="data.paymentType === 'EXAM_REPORT'"
          ><h3 class="patient-detail-subtitle">检查项目</h3>
          <div class="patient-record-section">
            <strong>{{ data.examReportName || "检查 / 检验" }}</strong>
            <p class="patient-muted">完成支付后可由医生填写检查报告。</p>
          </div></template
        ><template v-if="data.paymentType === 'HOSPITALIZATION'"
          ><h3 class="patient-detail-subtitle">住院结算信息</h3>
          <dl class="patient-facts">
            <div>
              <dt>入院日期</dt>
              <dd>{{ formatDate(data.hospitalization?.admissionDate) }}</dd>
            </div>
            <div>
              <dt>出院日期</dt>
              <dd>{{ formatDate(data.hospitalization?.dischargeDate) }}</dd>
            </div>
            <div class="patient-fact-full">
              <dt>诊断</dt>
              <dd>{{ text(data.hospitalization?.diagnosis) }}</dd>
            </div>
          </dl>
          <section
            v-if="list(data, 'medicalOrders').length"
            class="patient-record-section"
          >
            <h3>住院期间医嘱概览</h3>
            <ul class="patient-order-list">
              <li v-for="order in list(data, 'medicalOrders')" :key="order.id">
                {{ text(order.orderContent) }}
              </li>
            </ul>
          </section></template
        >
      </section>
      <aside class="panel patient-payment-confirm">
        <span class="patient-eyebrow">{{ paid ? '已支付金额' : '应付金额' }}</span
        ><strong class="patient-payment-amount"
          >¥{{ money(data.amount) }}</strong
        >
        <div v-if="!paid" class="patient-notice">
          <UiIcon name="info" :size="19" />
          <p>确认后更新支付状态，不会发起真实扣款。</p>
        </div>
        <p v-else class="patient-payment-next">{{ data.paymentType === 'PRESCRIPTION' ? '下一步：返回处方填写姓名，完成取药确认。' : data.paymentType === 'EXAM_REPORT' ? '可返回检查报告查看后续检查进度。' : '住院费用已结清，可返回住院记录查看详情。' }}</p>
        <button
          v-if="!paid"
          type="button"
          class="button button-primary"
          :disabled="submitting || loading"
          @click="pay"
        >
          {{ submitting ? "正在确认支付…" : "确认模拟支付" }}</button
        ><RouterLink :to="paid ? successReturnUrl : cancelUrl" class="button" :class="paid ? 'button-primary' : 'button-secondary'"
          >{{ paid ? successReturnLabel : '取消并返回' }}</RouterLink
        >
      </aside>
    </div>
  </div>
</template>
