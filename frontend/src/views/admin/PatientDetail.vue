<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import AdminFeedback from "./AdminFeedback.vue";
import AdminPagination from "./AdminPagination.vue";
import { useAdminPage, useAdminPagination, items, name, entry } from "./useAdminPage";
import { formatDate, money, statusText } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const route = useRoute(),
  tab = ref("appointments"),
  selected = ref<any[]>([]),
  keyword = ref("");
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => `/admin/patients/${route.params.id}`,
);
const tabs = [
  { key: "appointments", title: "挂号记录" },
  { key: "reports", title: "检查报告" },
  { key: "prescriptions", title: "处方与发药" },
  { key: "hospitalizations", title: "住院记录" },
];
const filtered = computed(() =>
  items(data.value, tab.value).filter((r) =>
    JSON.stringify(r)
      .toLowerCase()
      .includes(keyword.value.trim().toLowerCase()),
  ),
);
const { page, totalPages, pageRows } = useAdminPagination(filtered);
const allChecked = computed(() => !!pageRows.value.length && pageRows.value.every(r => selected.value.includes(r.id)));
watch([keyword, page], () => { selected.value = []; });
function setTab(value: string) {
  tab.value = value;
  selected.value = [];
  keyword.value = "";
}
function toggleAll(event: Event) {
  selected.value = (event.target as HTMLInputElement).checked
    ? pageRows.value.map((r) => r.id)
    : [];
}
async function removeSelected() {
  if (
    !selected.value.length ||
    !(await confirmAction(
      `确定删除选中的 ${selected.value.length} 条记录？删除后无法恢复。`,
    ))
  )
    return;
  const path =
    tab.value === "prescriptions"
      ? "/admin/pharmacy/bulk-delete"
      : `/admin/patients/${route.params.id}/${tab.value}/bulk-delete`;
  if (await action(path, { ids: selected.value, patientId: route.params.id }))
    selected.value = [];
}
async function changeAppointment(id: any, event: Event) {
  const select = event.target as HTMLSelectElement;
  const row = filtered.value.find(r => r.id === id);
  if (!(await confirmAction(`将该预约状态改为「${statusText(select.value)}」？`))) {
    select.value = row?.status || '';
    return;
  }
  await action(`/admin/appointments/${id}/status`, {
    status: select.value,
    patientId: route.params.id,
  });
}
async function changeHospitalization(id: any, event: Event) {
  const select = event.target as HTMLSelectElement;
  const row = filtered.value.find(r => r.id === id);
  if (!(await confirmAction(`将该患者的住院状态改为「${statusText(select.value)}」？请核对实际入院与出院情况。`))) {
    select.value = row?.status || '';
    return;
  }
  await action(
    `/admin/patients/${route.params.id}/hospitalizations/${id}/status`,
    { status: select.value },
  );
}
async function cancelAppointment(id: any) {
  if (await confirmAction("确定取消该患者的预约吗？"))
    await action(`/admin/appointments/${id}/cancel`, {
      patientId: route.params.id,
    });
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      title="患者就诊档案"
      description="集中查看并处理该患者的门诊、检查、处方与住院业务。"
      ><RouterLink
        v-if="data.patientUser?.id"
        class="button button-secondary"
        :to="{
          path: `/app/admin/users/${data.patientUser.id}/change-password`,
          query: { returnTo: `/admin/patients/${route.params.id}` },
        }"
        >修改账号密码</RouterLink
      ><RouterLink class="button button-secondary" to="/app/admin/patients"
        ><UiIcon name="chevron-left" :size="17" />返回患者列表</RouterLink
      ></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    /><template v-if="!loading"
      ><section class="panel">
        <div class="admin-person-summary">
          <span class="admin-person-avatar">{{
            String(data.patientUser?.realName || "患").slice(0, 1)
          }}</span>
          <div>
            <h2>{{ data.patientUser?.realName || "姓名待登记" }}</h2>
            <p>
              {{ data.patient?.patientCode || `患者 #${route.params.id}` }} ·
              {{ statusText(data.patient?.gender) }}
            </p>
          </div>
        </div>
        <dl class="admin-info-grid">
          <div>
            <dt>身份证号</dt>
            <dd>{{ data.patient?.idCard || "未登记" }}</dd>
          </div>
          <div>
            <dt>联系手机</dt>
            <dd>{{ data.patientUser?.phone || "未登记" }}</dd>
          </div>
          <div>
            <dt>出生日期</dt>
            <dd>{{ formatDate(data.patient?.birthday) }}</dd>
          </div>
          <div>
            <dt>联系邮箱</dt>
            <dd>{{ data.patientUser?.email || "未登记" }}</dd>
          </div>
          <div>
            <dt>家庭住址</dt>
            <dd>{{ data.patient?.address || "未登记" }}</dd>
          </div>
          <div>
            <dt>登录账号</dt>
            <dd>{{ data.patientUser?.username || "未关联" }}</dd>
          </div>
        </dl>
      </section>
      <section class="panel">
        <div class="admin-tabs" role="tablist" aria-label="就诊档案分类">
          <button
            v-for="item in tabs"
            :key="item.key"
            type="button"
            role="tab"
            :aria-selected="tab === item.key"
            :class="{ active: tab === item.key }"
            @click="setTab(item.key)"
          >
            {{ item.title }}
            <span class="admin-count">{{ items(data, item.key).length }}</span>
          </button>
        </div>
        <div class="admin-toolbar">
          <label class="admin-search"
            ><UiIcon name="search" :size="17" /><input
              v-model="keyword"
              type="search"
              placeholder="检索当前记录…"
              aria-label="检索档案记录" /></label
          ><button
            v-if="selected.length"
            class="button button-danger"
            :disabled="!selected.length || saving"
            @click="removeSelected"
          >
            删除选中{{ selected.length ? ` (${selected.length})` : "" }}</button
          ><button v-if="keyword" class="button button-secondary" @click="keyword = ''">清空搜索</button><span class="admin-result-meta">{{ filtered.length }} 条记录</span>
        </div>
        <EmptyState
          v-if="!filtered.length"
          title="暂无对应记录"
          description="新的就诊记录会归档到这里。"
        />
        <div v-else class="table-scroll">
          <table class="admin-table">
            <thead>
              <tr>
                <th>
                  <input
                    type="checkbox"
                    :checked="allChecked"
                    @change="toggleAll"
                    aria-label="选择当前页全部记录"
                  />
                </th>
                <template v-if="tab === 'appointments'"
                  ><th>挂号编号</th>
                  <th>就诊日期 / 时段</th>
                  <th>医生</th>
                  <th>患者主诉</th>
                  <th>状态</th>
                  <th>操作</th></template
                ><template v-else-if="tab === 'reports'"
                  ><th>报告编号</th>
                  <th>检查项目</th>
                  <th>报告日期</th>
                  <th>报告状态</th>
                  <th>费用状态</th>
                  <th>操作</th></template
                ><template v-else-if="tab === 'prescriptions'"
                  ><th>处方编号</th>
                  <th>开具医生</th>
                  <th>开具时间</th>
                  <th>费用 / 取药</th>
                  <th>处方状态</th>
                  <th>操作</th></template
                ><template v-else
                  ><th>入院日期</th>
                  <th>主治医生</th>
                  <th>床位</th>
                  <th>出院日期</th>
                  <th>状态</th>
                  <th>总费用</th>
                  <th>操作</th></template
                >
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pageRows" :key="row.id">
                <td>
                  <input
                    v-model="selected"
                    type="checkbox"
                    :value="row.id"
                    :aria-label="`选择记录 ${row.id}`"
                  />
                </td>
                <template v-if="tab === 'appointments'"
                  ><td class="admin-id">#{{ row.id }}</td>
                  <td>
                    {{ formatDate(row.appointmentDate)
                    }}<small>{{ row.appointmentTime }}</small>
                  </td>
                  <td>{{ name(data, "doctorUserMap", row.id) }}</td>
                  <td class="admin-description">
                    <span>{{ row.symptoms || "未填写" }}</span>
                  </td>
                  <td>
                    <select
                      :value="row.status"
                      aria-label="修改挂号状态"
                      :disabled="saving"
                      @change="changeAppointment(row.id, $event)"
                    >
                      <option value="PENDING">待确认</option>
                      <option value="CONFIRMED">已确认</option>
                      <option value="COMPLETED">已完成</option>
                      <option value="CANCELLED">已取消</option>
                    </select>
                  </td>
                  <td>
                    <div class="admin-row-actions">
                      <RouterLink
                        class="admin-link"
                        :to="`/app/admin/appointments/${row.id}/reports`"
                        >检查报告</RouterLink
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/appointments/${row.id}/prescriptions`"
                        >处方</RouterLink
                      ><button
                        v-if="
                          row.status !== 'CANCELLED' &&
                          row.status !== 'COMPLETED'
                        "
                        class="admin-link danger"
                        :disabled="saving"
                        @click="cancelAppointment(row.id)"
                      >
                        取消预约
                      </button>
                    </div>
                  </td></template
                ><template v-else-if="tab === 'reports'"
                  ><td class="admin-id">#{{ row.id }}</td>
                  <td>
                    <strong>{{
                      entry(data, "examinationMap", row.id).name ||
                      row.reportType ||
                      "诊疗报告"
                    }}</strong>
                  </td>
                  <td>{{ formatDate(row.reportDate, true) }}</td>
                  <td><StatusBadge :status="row.status" /></td>
                  <td>{{ row.paid ? "已支付" : "未支付" }}</td>
                  <td>
                    <RouterLink
                      class="admin-link"
                      :to="{
                        path: `/app/admin/reports/${row.id}/preview`,
                        query: { returnPatientId: String(route.params.id) },
                      }"
                      >查看报告</RouterLink
                    >
                  </td></template
                ><template v-else-if="tab === 'prescriptions'"
                  ><td class="admin-id">
                    {{ row.prescriptionNumber || `#${row.id}` }}
                  </td>
                  <td>{{ name(data, "prescriptionDoctorUserMap", row.id) }}</td>
                  <td>{{ formatDate(row.createdAt, true) }}</td>
                  <td>
                    {{ row.paid ? "已支付" : "未支付"
                    }}<small>{{
                      row.patientSignature ? "患者已确认取药" : "尚未确认取药"
                    }}</small>
                  </td>
                  <td><StatusBadge :status="row.status" /></td>
                  <td>
                    <RouterLink
                      class="admin-link"
                      :to="{
                        path: `/app/admin/pharmacy/${row.id}`,
                        query: { returnPatientId: String(route.params.id) },
                      }"
                      >{{
                        row.status === "PENDING" ? "药房处理" : "处方详情"
                      }}</RouterLink
                    >
                  </td></template
                ><template v-else
                  ><td>{{ formatDate(row.admissionDate) }}</td>
                  <td>
                    {{ name(data, "hospitalizationDoctorUserMap", row.id) }}
                  </td>
                  <td>
                    {{
                      entry(data, "hospitalizationBedMap", row.id).bedNumber ||
                      "未分配"
                    }}
                  </td>
                  <td>{{ formatDate(row.dischargeDate) }}</td>
                  <td>
                    <select
                      v-if="row.status"
                      :value="row.status"
                      :disabled="saving"
                      aria-label="修改住院状态"
                      @change="changeHospitalization(row.id, $event)"
                    >
                      <option value="ADMITTED">住院中</option>
                      <option value="DISCHARGED">已出院</option></select
                    ><StatusBadge v-else :status="row.requestStatus" />
                  </td>
                  <td>
                    {{
                      row.totalCost == null
                        ? "未结算"
                        : `¥${money(row.totalCost)}`
                    }}<small>{{ row.paid ? "已支付" : "未支付" }}</small>
                  </td>
                  <td>
                    <RouterLink
                      v-if="
                        row.status === 'DISCHARGED' && !Number(row.totalCost)
                      "
                      class="admin-link"
                      :to="`/app/admin/hospitalizations/${row.id}/settle`"
                      >办理结算</RouterLink
                    ><span v-else class="admin-id">—</span>
                  </td></template
                >
              </tr>
            </tbody>
          </table>
        </div>
        <AdminPagination v-model:page="page" :total-pages="totalPages" :total="filtered.length" />
      </section></template
    >
  </div>
</template>
