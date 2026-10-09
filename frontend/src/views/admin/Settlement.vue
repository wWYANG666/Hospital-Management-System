<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import AdminFeedback from "./AdminFeedback.vue";
import { useAdminPage } from "./useAdminPage";
import { formatDate, money } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const route = useRoute();
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => `/admin/hospitalizations/${route.params.id}/settle`,
);
async function settle() {
  if (await confirmAction("确认结算该住院记录，并释放床位？"))
    await action(
      `/admin/hospitalizations/${route.params.id}/settle`,
      {},
      "/admin/hospitalizations",
    );
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      title="出院结算"
      description="核对出院患者与床位信息，提交结算后更新住院费用并释放床位。"
      ><RouterLink
        class="button button-secondary"
        to="/app/admin/hospitalizations"
        ><UiIcon name="chevron-left" :size="17" />返回住院记录</RouterLink
      ></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <div v-if="!loading" class="admin-form-layout">
      <section class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>患者与住院信息</h2>
            <p>住院记录 #{{ data.hospitalization?.id }}</p>
          </div>
        </div>
        <dl class="admin-info-grid">
          <div>
            <dt>患者</dt>
            <dd>{{ data.patientUser?.realName || "未登记" }}</dd>
          </div>
          <div>
            <dt>床位</dt>
            <dd>{{ data.bed?.bedNumber || "未分配" }}</dd>
          </div>
          <div>
            <dt>床位费 / 天</dt>
            <dd>¥{{ money(data.bed?.pricePerDay) }}</dd>
          </div>
          <div>
            <dt>入院日期</dt>
            <dd>{{ formatDate(data.hospitalization?.admissionDate) }}</dd>
          </div>
          <div>
            <dt>出院日期</dt>
            <dd>{{ formatDate(data.hospitalization?.dischargeDate) }}</dd>
          </div>
          <div>
            <dt>出院诊断</dt>
            <dd>{{ data.hospitalization?.dischargeDiagnosis || "未填写" }}</dd>
          </div>
        </dl>
        <h2 class="admin-ward-title">费用预览</h2>
        <div class="table-scroll">
          <table class="admin-table admin-table-compact">
            <thead>
              <tr>
                <th>费用类型</th>
                <th>金额（元）</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>床位费用</td>
                <td>{{ money(data.bedCost) }}</td>
              </tr>
              <tr>
                <td>药品费用</td>
                <td>{{ money(data.medicineCost) }}</td>
              </tr>
              <tr>
                <td>检查费用</td>
                <td>{{ money(data.examinationCost) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="admin-total">
          <span>费用预览合计</span><strong>¥{{ money(data.totalCost) }}</strong>
        </div>
        <p class="admin-result-meta" style="margin: 0 0 20px">
          最终金额以提交后生成的住院结算记录为准。
        </p>
        <div class="admin-form-footer">
          <button
            class="button button-primary"
            :disabled="saving"
            @click="settle"
          >
            {{ saving ? "结算中…" : "确认结算并释放床位" }}
          </button>
        </div>
      </section>
      <aside class="panel admin-help-panel">
        <h2>结算流程</h2>
        <p>仅已出院记录可结算。核对费用后提交结算，更新费用并释放床位。</p>
        <p>患者可在个人账单中查看并支付结算费用。</p>
      </aside>
    </div>
  </div>
</template>
