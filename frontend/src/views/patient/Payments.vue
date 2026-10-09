<script setup lang="ts">
import { computed, ref } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import UiIcon from "@/components/UiIcon.vue";
import PatientPageState from "./PatientPageState.vue";
import { appPath, list, usePatientPage } from "./usePatientPage";
import { formatDate, money, text } from "@/lib/format";
const { data, loading, error, reload } = usePatientPage(
  () => "/patient/payments/history",
);
const search = ref("");
const filter = ref("ALL");
const types = computed(() => [
  ...new Set(list(data.value, "paymentRecords").map((row) => String(row.type))),
]);
const records = computed(() =>
  list(data.value, "paymentRecords").filter(
    (row) =>
      (filter.value === "ALL" || row.type === filter.value) &&
      `${row.orderNo || ""}${row.description || ""}`.includes(
        search.value.trim(),
      ),
  ),
);
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="缴费记录"
      description="查看已支付的门诊处方、检查与住院费用明细。"
    /><PatientPageState :loading="loading" :error="error" @retry="reload" />
    <section v-if="data.paymentRecords" class="patient-payment-summary">
      <div>
        <span class="patient-task-icon"
          ><UiIcon name="wallet" :size="24"
        /></span>
        <div>
          <p>累计已支付金额</p>
          <strong>¥{{ money(data.totalPaid || 0) }}</strong>
        </div>
      </div>
      <div>
        <span>支付记录</span
        ><strong>{{ list(data, "paymentRecords").length }} 笔</strong>
      </div>
      <p>统计范围为当前就诊人的支付记录。</p>
    </section>
    <section class="panel">
      <div class="patient-list-toolbar">
        <div class="patient-filter">
          <label for="payment-type">付款类型</label
          ><select id="payment-type" v-model="filter">
            <option value="ALL">全部类型</option>
            <option v-for="type in types" :key="type" :value="type">
              {{ type }}
            </option>
          </select>
        </div>
        <label class="patient-search"
          ><UiIcon name="search" :size="18" /><input
            v-model="search"
            type="search"
            placeholder="搜索单号或费用说明"
            aria-label="搜索账单"
        /></label>
      </div>
      <EmptyState
        v-if="!loading && !error && !records.length"
        title="暂无符合条件的缴费记录"
        description="完成费用支付后，对应账单会显示在这里。"
      />
      <div v-else-if="records.length" class="table-scroll">
        <table class="patient-table">
          <thead>
            <tr>
              <th>付款类型</th>
              <th>单号</th>
              <th>金额</th>
              <th>付款时间</th>
              <th>费用说明</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in records" :key="row.orderNo + row.type">
              <td>
                <span class="patient-type-label">{{ row.type }}</span>
              </td>
              <td>{{ row.orderNo }}</td>
              <td class="patient-price">¥{{ money(row.amount) }}</td>
              <td>{{ formatDate(row.payTime, true) }}</td>
              <td>{{ text(row.description) }}</td>
              <td>
                <RouterLink :to="appPath(row.detailUrl)" class="patient-link"
                  >查看单据 <UiIcon name="chevron-right" :size="16"
                /></RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
