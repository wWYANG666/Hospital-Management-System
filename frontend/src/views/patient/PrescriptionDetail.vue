<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import PatientPageState from "./PatientPageState.vue";
import {
  appPath,
  currency as money,
  enumName,
  list,
  usePatientPage,
} from "./usePatientPage";
import { formatDate, text } from "@/lib/format";
const route = useRoute();
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(
    () => `/patient/prescriptions/${route.params.id}`,
    () => ({ returnTo: route.query.returnTo }),
  );
const rx = computed(() => data.value.prescription || {});
const signature = ref("");
async function collect() {
  if (!signature.value.trim() || !rx.value.paid || loading.value) return;
  await action(
    `/patient/prescriptions/${route.params.id}/collect`,
    { signature: signature.value.trim() },
    "已确认取药，请到药房领取药品。",
  );
}
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="处方详情"
      description="核对药品明细与用法，完成缴费和取药确认。"
    />
    <div class="patient-page-actions">
      <RouterLink
        :to="appPath(data.returnTo, '/app/patient/prescriptions')"
        class="button button-secondary"
        >返回处方列表</RouterLink
      >
    </div>
    <PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="reload"
    /><template v-if="data.prescription"
      ><section class="panel">
        <div class="patient-section-heading">
          <div>
            <span class="patient-eyebrow">门诊处方</span>
            <h2>{{ rx.prescriptionNumber }}</h2>
          </div>
          <StatusBadge :status="rx.status" />
        </div>
        <dl class="patient-facts">
          <div>
            <dt>开方医生</dt>
            <dd>
              {{ data.doctorUser?.realName || data.doctor?.doctorCode || "—" }}
            </dd>
          </div>
          <div>
            <dt>开方时间</dt>
            <dd>{{ formatDate(rx.createdAt) }}</dd>
          </div>
          <div>
            <dt>支付状态</dt>
            <dd>{{ rx.paid ? "已支付" : "未支付" }}</dd>
          </div>
          <div v-if="rx.paidAt">
            <dt>付款时间</dt>
            <dd>{{ formatDate(rx.paidAt) }}</dd>
          </div>
          <div v-if="rx.collectedAt">
            <dt>取药确认时间</dt>
            <dd>{{ formatDate(rx.collectedAt) }}</dd>
          </div>
          <div v-if="rx.dispensedAt">
            <dt>发药完成时间</dt>
            <dd>{{ formatDate(rx.dispensedAt) }}</dd>
          </div>
        </dl>
      </section>
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>药品明细</h2>
            <p>请遵医嘱用药，疑问请咨询开方医生</p>
          </div>
        </div>
        <EmptyState v-if="!list(data, 'items').length" title="暂无药品明细" />
        <div v-else class="table-scroll">
          <table class="patient-table">
            <thead>
              <tr>
                <th>药品名称 / 规格</th>
                <th>数量</th>
                <th>用法用量</th>
                <th>单价</th>
                <th>小计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in list(data, 'items')" :key="item.id">
                <td>
                  <strong>{{ item.medicineName }}</strong
                  ><small>{{ text(item.specification) }}</small>
                </td>
                <td>{{ text(item.quantity) }} {{ item.unit }}</td>
                <td>
                  {{
                    [item.dosage, item.frequency, item.usage]
                      .filter(Boolean)
                      .join("，") || "—"
                  }}
                </td>
                <td>{{ money(item.price || 0) }}</td>
                <td class="patient-price">{{ money(item.totalPrice || 0) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="patient-total">
          <span>处方费用合计</span
          ><strong>{{ money(data.totalAmount || 0) }}</strong>
        </div>
      </section>
      <section v-if="enumName(rx.status) === 'PENDING'" class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>缴费与取药</h2>
            <p>先完成处方费用支付，再填写姓名确认取药。</p>
          </div>
        </div>
        <div class="patient-collection">
          <div class="patient-collection-step">
            <span class="patient-step-number">1</span>
            <div>
              <strong>完成费用支付</strong>
              <p>
                {{
                  rx.paid
                    ? "处方费用已支付，可继续确认取药。"
                    : "请核对药品与费用后前往支付。"
                }}
              </p>
            </div>
            <RouterLink
              v-if="!rx.paid"
              :to="`/app/patient/payments/prescription/${rx.id}/alipay`"
              class="button button-primary"
              >去缴费</RouterLink
            ><span v-else class="patient-success-text">已支付</span>
          </div>
          <div class="patient-collection-step">
            <span class="patient-step-number">2</span>
            <div>
              <strong>签字确认取药</strong>
              <p v-if="rx.patientSignature">
                {{ rx.patientSignature }} 已确认 ·
                {{ formatDate(rx.collectedAt) }}
              </p>
              <p v-else>填写您的姓名，确认取药后由药房工作人员发药。</p>
            </div>
          </div>
          <form
            v-if="!rx.patientSignature"
            class="patient-signature-form"
            @submit.prevent="collect"
          >
            <label class="field"
              ><span>患者签名</span
              ><input
                v-model="signature"
                name="signature"
                type="text"
                required
                placeholder="请输入您的姓名"
                maxlength="50"
                autocomplete="name"
                :disabled="!rx.paid || submitting || loading" /></label
            ><button
              type="submit"
              class="button button-primary"
              :disabled="!rx.paid || submitting || loading || !signature.trim()"
            >
              {{
                submitting ? "正在确认…" : rx.paid ? "确认取药" : "请先完成缴费"
              }}
            </button>
          </form>
        </div>
      </section></template
    >
  </div>
</template>
