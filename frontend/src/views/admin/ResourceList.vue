<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import AdminFeedback from "./AdminFeedback.vue";
import AdminPagination from "./AdminPagination.vue";
import {
  useAdminPage,
  useAdminPagination,
  items,
  entry,
  name,
  scheduleStatus,
  type AdminData,
} from "./useAdminPage";
import { formatDate, money, statusText } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
import {
  scheduleState,
  scheduleTone,
  type ScheduleStatus,
} from "@/lib/schedule";

type Column = {
  title: string;
  key: string;
  kind?: string;
  value?: (row: AdminData) => any;
};
const route = useRoute();
const search = ref(""),
  filter = ref(""),
  selected = ref<any[]>([]);
const resource = computed(() => String(route.meta.resource));
const settings: Record<
  string,
  { title: string; sub: string; key: string; create?: string; filter?: string }
> = {
  departments: {
    title: "科室与医生管理",
    sub: "维护科室信息、管理医生账号与科室排班。",
    key: "departments",
    create: "新增科室",
  },
  medicines: {
    title: "药品与库存管理",
    sub: "维护药品目录与零售价格，按批次入库补充库存。",
    key: "medicines",
    create: "新增药品",
    filter: "type",
  },
  examinations: {
    title: "检查项目管理",
    sub: "维护检查与检验项目、归属科室及收费标准。",
    key: "examinations",
    create: "新增项目",
    filter: "type",
  },
  inbounds: {
    title: "药品入库记录",
    sub: "追溯药品入库批次、数量、采购价格与有效期。",
    key: "inbounds",
  },
  schedules: {
    title: "全院排班与号源",
    sub: "统一配置出诊班次及号源，审批医生请假申请。",
    key: "schedules",
    create: "新增排班",
    filter: "status",
  },
  hospitalizations: {
    title: "住院记录管理",
    sub: "跟进住院审批、入院状态及出院结算。",
    key: "hospitalizations",
    filter: "status",
  },
  reports: {
    title: "检查报告中心",
    sub: "查询患者检查报告、审阅状态与结果内容。",
    key: "reports",
    filter: "status",
  },
  appointments: {
    title: "全院挂号管理",
    sub: "统一查看各科室预约记录，维护挂号状态并进入报告与处方详情。",
    key: "appointments",
    filter: "status",
  },
};
const config = computed(() => settings[resource.value] || settings.departments);
const path = computed(() =>
  resource.value === "inbounds"
    ? "/admin/medicines/inbounds"
    : `/admin/${resource.value}`,
);
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => path.value,
);
const allRows = computed(() => items(data.value, config.value.key));
const pending = computed(() => items(data.value, "pendingRequests"));
const columns = computed<Column[]>(() => {
  const d = data.value;
  switch (resource.value) {
    case "departments":
      return [
        { title: "科室编号", key: "id", kind: "id" },
        { title: "科室名称", key: "name", kind: "strong" },
        { title: "科室介绍", key: "description", kind: "description" },
        {
          title: "关联医生",
          key: "doctors",
          value: (r) => `${d.departmentDoctorCountMap?.[r.id] || 0} 人`,
        },
      ];
    case "medicines":
      return [
        { title: "药品编码", key: "code", kind: "id" },
        { title: "药品名称 / 规格", key: "name", kind: "medicine" },
        { title: "分类", key: "type" },
        { title: "零售价", key: "price", kind: "money" },
        { title: "当前库存", key: "stock", kind: "stock" },
        { title: "生产厂家", key: "manufacturer", kind: "description" },
      ];
    case "examinations":
      return [
        { title: "项目编号", key: "id", kind: "id" },
        { title: "项目名称", key: "name", kind: "strong" },
        {
          title: "归属科室",
          key: "departmentId",
          value: (r) =>
            items(d, "departments").find((x) => x.id === r.departmentId)
              ?.name || "未分配",
        },
        { title: "类型", key: "type" },
        { title: "收费", key: "price", kind: "money" },
        { title: "状态", key: "status", kind: "enabled" },
      ];
    case "inbounds":
      return [
        {
          title: "入库时间",
          key: "inboundTime",
          kind: "date",
          value: (r) => r.inboundTime || r.inboundDate || r.createdAt,
        },
        {
          title: "药品",
          key: "medicineId",
          value: (r) => name(d, "medicineMap", r.medicineId),
        },
        { title: "批次号", key: "batchNumber" },
        { title: "有效期至", key: "expiryDate", kind: "date" },
        { title: "入库数量", key: "quantity" },
        { title: "采购单价", key: "purchasePrice", kind: "money" },
        { title: "存放位置", key: "location" },
        { title: "经办人", key: "operator" },
        {
          title: "审核",
          key: "status",
          value: (r) =>
            r.status === 1 ? "已审核" : r.status === -1 ? "已取消" : "待审核",
        },
      ];
    case "schedules":
      return [
        { title: "出诊日期", key: "workDate", kind: "date" },
        {
          title: "医生",
          key: "doctorId",
          value: (r) =>
            entry(d, "doctorUserMap", r.doctorId).realName ||
            items(d, "doctors").find((x) => x.id === r.doctorId)?.doctorCode ||
            `医生 #${r.doctorId}`,
        },
        {
          title: "科室",
          key: "departmentId",
          value: (r) =>
            items(d, "departments").find((x) => x.id === r.departmentId)
              ?.name || `科室 #${r.departmentId}`,
        },
        {
          title: "班次",
          key: "workTime",
          value: (r) =>
            ({ MORNING: "上午", AFTERNOON: "下午", EVENING: "晚上" })[
              String(r.workTime)
            ] || r.workTime,
        },
        {
          title: "门诊类型",
          key: "scheduleType",
          value: (r) => (r.scheduleType === "EXPERT" ? "专家门诊" : "普通门诊"),
        },
        { title: "号源使用", key: "capacity", kind: "capacity" },
        {
          title: "状态",
          key: "status",
          kind: "schedule",
          value: (row) => scheduleStatus(row),
        },
      ];
    case "hospitalizations":
      return [
        {
          title: "患者",
          key: "patientId",
          value: (r) => name(d, "patientUserMap", r.patientId),
        },
        {
          title: "主治医生",
          key: "doctorId",
          value: (r) => name(d, "doctorUserMap", r.id),
        },
        {
          title: "床位",
          key: "bedId",
          value: (r) => entry(d, "bedMap", r.bedId).bedNumber || "未分配",
        },
        { title: "入院日期", key: "admissionDate", kind: "date" },
        { title: "出院日期", key: "dischargeDate", kind: "date" },
        {
          title: "状态",
          key: "status",
          kind: "status",
          value: (r) => r.status || r.requestStatus,
        },
        { title: "住院费用", key: "totalCost", kind: "money" },
      ];
    case "reports":
      return [
        { title: "报告编号", key: "id", kind: "id", value: (r) => `#${r.id}` },
        {
          title: "患者",
          key: "patientId",
          value: (r) => name(d, "patientUserMap", r.patientId),
        },
        {
          title: "检查项目",
          key: "reportType",
          value: (r) =>
            entry(d, "examinationMap", r.id).name || r.reportType || "诊疗报告",
        },
        {
          title: "开单医生",
          key: "doctorId",
          value: (r) => name(d, "doctorUserMap", r.doctorId),
        },
        { title: "出具日期", key: "reportDate", kind: "date" },
        { title: "报告状态", key: "status", kind: "status" },
        {
          title: "费用状态",
          key: "paid",
          value: (r) => (r.paid ? "已支付" : "未支付"),
        },
      ];
    case "appointments":
      return [
        { title: "挂号编号", key: "id", kind: "id", value: (r) => `#${r.id}` },
        {
          title: "患者",
          key: "patientId",
          value: (r) => name(d, "patientUserMap", r.patientId),
        },
        {
          title: "医生",
          key: "doctorId",
          value: (r) => name(d, "doctorUserMap", r.doctorId),
        },
        {
          title: "预约日期 / 时段",
          key: "appointmentDate",
          value: (r) =>
            `${r.appointmentDate || "待定"} · ${r.appointmentTime || "时段待定"}`,
        },
        { title: "患者主诉", key: "symptoms", kind: "description" },
        { title: "状态", key: "status", kind: "status" },
      ];
    default:
      return [];
  }
});
function cell(row: AdminData, col: Column) {
  return col.value ? col.value(row) : row[col.key];
}
const filteredRows = computed(() =>
  allRows.value.filter((row) => {
    const haystack = columns.value
      .map((col) => String(cell(row, col) ?? ""))
      .join(" ")
      .toLowerCase();
    const query = search.value.trim().toLowerCase();
    const matches =
      !filter.value ||
      (filter.value === "__low"
        ? Number(row.stock || 0) < 10
        : (resource.value === "schedules"
            ? scheduleState(row)
            : String(row[config.value.filter || ""])) === filter.value);
    return haystack.includes(query) && matches;
  }),
);
const filterOptions = computed(() =>
  config.value.filter
    ? [
        ...new Set(
          allRows.value
            .map((r) =>
              resource.value === "schedules"
                ? scheduleState(r)
                : String(r[config.value.filter!]),
            )
            .filter((v) => v !== "null" && v !== "undefined"),
        ),
      ]
    : [],
);
const { page, totalPages, pageRows } = useAdminPagination(filteredRows);
function filterLabel(value: string) {
  return resource.value === "schedules"
    ? scheduleStatus(value as ScheduleStatus)
    : config.value.filter === "status"
      ? statusText(value)
      : value;
}
const allChecked = computed(
  () =>
    !!pageRows.value.length &&
    pageRows.value.every((r) => selected.value.includes(r.id)),
);
function toggleAll(event: Event) {
  selected.value = (event.target as HTMLInputElement).checked
    ? pageRows.value.map((r) => r.id)
    : [];
}
function toggle(id: any, event: Event) {
  selected.value = (event.target as HTMLInputElement).checked
    ? [...selected.value, id]
    : selected.value.filter((x) => x !== id);
}
async function removeSelected() {
  if (
    selected.value.length &&
    (await confirmAction(
      `确定删除选中的 ${selected.value.length} 条记录吗？删除后无法恢复。`,
    ))
  ) {
    if (await action(`${path.value}/bulk-delete`, { ids: selected.value }))
      selected.value = [];
  }
}
async function scheduleAction(row: AdminData, op: string) {
  if (
    op === "reject-leave" &&
    !(await confirmAction("确定驳回该医生的请假申请？"))
  )
    return;
  await action(`/admin/schedules/${row.id}/${op}`);
}
async function appointmentAction(row: AdminData, op: string, event?: Event) {
  const status = event ? (event.target as HTMLSelectElement).value : undefined;
  if (
    op === "cancel" &&
    !(await confirmAction("确定取消该患者的预约吗？取消后将释放预约号源。"))
  )
    return;
  if (
    op === "status" &&
    !(await confirmAction(
      `将挂号 #${row.id} 的状态改为「${statusText(status)}」？`,
    ))
  ) {
    if (event) (event.target as HTMLSelectElement).value = row.status;
    return;
  }
  await action(`/admin/appointments/${row.id}/${op}`, {
    status,
    patientId: row.patientId,
  });
}
async function rejectRequest(id: number) {
  if (await confirmAction("确定拒绝此住院申请吗？"))
    await action("/admin/beds/reject", { hospitalizationId: id });
}
async function remove(row: AdminData) {
  if (await confirmAction("确定删除这条记录吗？删除后无法恢复。"))
    await action(`${path.value}/${row.id}/delete`);
}
watch(resource, () => {
  search.value = "";
  filter.value = "";
  selected.value = [];
});
watch([search, filter, page], () => {
  selected.value = [];
});
</script>
<template>
  <div class="admin-view">
    <PageHeader :title="config.title" :description="config.sub"
      ><RouterLink
        v-if="config.create"
        class="button button-primary"
        :to="`/app/admin/${resource}/new`"
        ><UiIcon name="plus" :size="17" />{{ config.create }}</RouterLink
      ><RouterLink
        v-if="resource === 'medicines'"
        class="button button-secondary"
        to="/app/admin/medicines/inbounds"
        >入库记录</RouterLink
      ><RouterLink
        v-if="resource === 'hospitalizations'"
        class="button button-secondary"
        to="/app/admin/beds"
        >床位看板</RouterLink
      ></PageHeader
    >
    <AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <template v-if="!loading">
      <section
        v-if="resource === 'hospitalizations' && pending.length"
        class="panel"
      >
        <div class="admin-panel-heading">
          <div>
            <h2>
              住院待审批 <span class="admin-count">{{ pending.length }}</span>
            </h2>
            <p>核对住院原因与床位信息后处理</p>
          </div>
        </div>
        <article v-for="req in pending" :key="req.id" class="admin-review-card">
          <h3>{{ name(data, "requestUserMap", req.patientId) }} · 住院申请</h3>
          <dl class="admin-info-grid">
            <div>
              <dt>住院原因</dt>
              <dd>{{ req.admissionReason || "未填写" }}</dd>
            </div>
            <div>
              <dt>申请时间</dt>
              <dd>{{ formatDate(req.requestTime) }}</dd>
            </div>
            <div>
              <dt>拟分配床位</dt>
              <dd>
                {{ entry(data, "bedMap", req.bedId).bedNumber || "尚未分配" }}
              </dd>
            </div>
          </dl>
          <div class="toolbar">
            <button
              v-if="req.bedId"
              class="button button-primary"
              :disabled="saving"
              @click="
                action('/admin/beds/assign', {
                  bedId: req.bedId,
                  hospitalizationId: req.id,
                })
              "
            >
              同意床位分配</button
            ><RouterLink
              v-else
              class="button button-primary"
              to="/app/admin/beds"
              >选择可用床位</RouterLink
            ><button
              class="button button-secondary"
              :disabled="saving"
              @click="rejectRequest(req.id)"
            >
              拒绝申请
            </button>
          </div>
        </article>
      </section>
      <section class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>
              {{ config.title.replace("管理", "") }}列表
              <span class="admin-count">{{ allRows.length }}</span>
            </h2>
            <p>数据来自实际业务记录，可检索当前列表</p>
          </div>
          <button
            class="button button-secondary"
            :disabled="saving"
            @click="load"
          >
            刷新列表
          </button>
        </div>
        <div v-if="resource === 'medicines'" class="admin-mini-metrics">
          <div>
            <strong>{{ allRows.length }}</strong
            >药品品种
          </div>
          <div>
            <strong>{{
              allRows.filter((m) => Number(m.stock || 0) < 10).length
            }}</strong
            >低库存品种
          </div>
          <div>
            <strong>{{
              allRows.filter((m) => Number(m.stock || 0) === 0).length
            }}</strong
            >零库存品种
          </div>
        </div>
        <div v-if="resource === 'schedules'" class="admin-mini-metrics">
          <div>
            <strong>{{ allRows.length }}</strong
            >排班记录
          </div>
          <div>
            <strong>{{
              allRows.filter((m) => scheduleState(m) === "LEAVE_PENDING").length
            }}</strong
            >请假待审批
          </div>
          <div>
            <strong>{{
              allRows.filter((m) => scheduleState(m) === "ACTIVE").length
            }}</strong
            >有效启用排班
          </div>
        </div>
        <div v-if="resource === 'appointments'" class="admin-mini-metrics">
          <div>
            <strong>{{ data.totalAppointments ?? allRows.length }}</strong
            >全部挂号
          </div>
          <div>
            <strong>{{ data.pendingAppointments ?? 0 }}</strong
            >待确认
          </div>
          <div>
            <strong>{{ data.confirmedAppointments ?? 0 }}</strong
            >已确认
          </div>
          <div>
            <strong>{{ data.completedAppointments ?? 0 }}</strong
            >已完成
          </div>
        </div>
        <div v-if="resource === 'departments'" class="admin-mini-metrics">
          <div>
            <strong>{{ allRows.length }}</strong
            >科室
          </div>
          <div>
            <strong>{{
              Object.values(data.departmentDoctorCountMap || {}).reduce(
                (a: number, b: any) => a + Number(b),
                0,
              )
            }}</strong
            >关联医生
          </div>
          <div>
            <strong>{{ allRows.filter((m) => !m.description).length }}</strong
            >待补充介绍
          </div>
        </div>
        <div class="admin-toolbar">
          <label class="admin-search"
            ><UiIcon name="search" :size="17" /><input
              v-model="search"
              type="search"
              placeholder="搜索当前列表…"
              aria-label="搜索列表" /></label
          ><select
            v-if="config.filter"
            v-model="filter"
            aria-label="筛选类型或状态"
          >
            <option value="">
              全部{{ config.filter === "status" ? "状态" : "类型" }}
            </option>
            <option v-if="resource === 'medicines'" value="__low">
              仅低库存药品
            </option>
            <option
              v-for="option in filterOptions"
              :key="option"
              :value="option"
            >
              {{ filterLabel(option) }}
            </option></select
          ><button
            v-if="resource !== 'inbounds' && selected.length"
            class="button button-danger"
            :disabled="saving || !selected.length"
            @click="removeSelected"
          >
            删除选中{{ selected.length ? ` (${selected.length})` : "" }}</button
          ><button
            v-if="search || filter"
            class="button button-secondary"
            @click="
              search = '';
              filter = '';
            "
          >
            清空筛选</button
          ><span class="admin-result-meta"
            >显示 {{ filteredRows.length }} 条记录</span
          >
        </div>
        <EmptyState
          v-if="!filteredRows.length"
          :title="allRows.length ? '未找到匹配记录' : '暂无业务数据'"
          :description="
            allRows.length
              ? '请调整关键词或筛选条件。'
              : config.create
                ? '点击右上角新增，建立第一条记录。'
                : '新的业务记录会显示在这里。'
          "
        />
        <div v-else class="table-scroll">
          <table class="admin-table">
            <thead>
              <tr>
                <th v-if="resource !== 'inbounds'">
                  <input
                    type="checkbox"
                    :checked="allChecked"
                    @change="toggleAll"
                    aria-label="选择当前页全部记录"
                  />
                </th>
                <th v-for="col in columns" :key="col.key">{{ col.title }}</th>
                <th v-if="resource !== 'inbounds'">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pageRows" :key="row.id">
                <td v-if="resource !== 'inbounds'">
                  <input
                    type="checkbox"
                    :checked="selected.includes(row.id)"
                    @change="toggle(row.id, $event)"
                    :aria-label="`选择记录 ${row.id}`"
                  />
                </td>
                <td
                  v-for="col in columns"
                  :key="col.key"
                  :class="{
                    'admin-id': col.kind === 'id',
                    'admin-description': col.kind === 'description',
                  }"
                >
                  <template v-if="col.kind === 'medicine'"
                    ><strong>{{ row.name }}</strong
                    ><small
                      >{{ row.specification }} · {{ row.unit }}</small
                    ></template
                  ><strong v-else-if="col.kind === 'strong'">{{
                    cell(row, col) || "未填写"
                  }}</strong
                  ><StatusBadge
                    v-else-if="col.kind === 'status'"
                    :status="cell(row, col)"
                  /><span
                    v-else-if="col.kind === 'schedule'"
                    class="status-badge"
                    :class="scheduleTone(row)"
                    >{{ scheduleStatus(row) }}</span
                  ><span
                    v-else-if="col.kind === 'enabled'"
                    class="status-badge"
                    >{{ row.status === 1 ? "启用" : "停用" }}</span
                  ><span v-else-if="col.kind === 'date'">{{
                    formatDate(cell(row, col))
                  }}</span
                  ><span v-else-if="col.kind === 'money'">{{
                    cell(row, col) == null ? "未维护" : money(cell(row, col))
                  }}</span
                  ><span
                    v-else-if="col.kind === 'stock'"
                    :class="{ 'admin-stock-low': Number(row.stock || 0) < 10 }"
                    >{{ row.stock ?? 0 }} {{ row.unit }}</span
                  ><template v-else-if="col.kind === 'capacity'"
                    ><span
                      >{{ row.currentAppointments ?? 0 }} /
                      {{ row.maxAppointments ?? 0 }}</span
                    >
                    <div class="admin-capacity">
                      <span
                        :style="{
                          width: `${Math.min(100, (Number(row.currentAppointments || 0) / Math.max(1, Number(row.maxAppointments || 0))) * 100)}%`,
                        }"
                      ></span></div></template
                  ><span v-else>{{ cell(row, col) ?? "—" }}</span>
                </td>
                <td v-if="resource !== 'inbounds'">
                  <div class="admin-row-actions">
                    <template v-if="resource === 'departments'"
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/departments/${row.id}/edit`"
                        >科室 / 医生管理</RouterLink
                      ></template
                    ><template v-else-if="resource === 'medicines'"
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/medicines/${row.id}/inbound`"
                        >入库</RouterLink
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/medicines/${row.id}/edit`"
                        >编辑</RouterLink
                      ></template
                    ><RouterLink
                      v-else-if="resource === 'examinations'"
                      class="admin-link"
                      :to="`/app/admin/examinations/${row.id}/edit`"
                      >编辑</RouterLink
                    ><template v-else-if="resource === 'schedules'"
                      ><button
                        v-if="scheduleState(row) === 'LEAVE_PENDING'"
                        class="admin-link"
                        :disabled="saving"
                        @click="scheduleAction(row, 'approve-leave')"
                      >
                        同意请假</button
                      ><button
                        v-if="scheduleState(row) === 'LEAVE_PENDING'"
                        class="admin-link"
                        :disabled="saving"
                        @click="scheduleAction(row, 'reject-leave')"
                      >
                        驳回</button
                      ><button
                        v-if="scheduleState(row) === 'DISABLED'"
                        class="admin-link"
                        :disabled="saving"
                        @click="scheduleAction(row, 'restore')"
                      >
                        恢复出诊</button
                      ><button
                        class="admin-link danger"
                        :disabled="saving"
                        @click="remove(row)"
                      >
                        删除
                      </button></template
                    ><template v-else-if="resource === 'appointments'"
                      ><select
                        :value="row.status"
                        :disabled="saving"
                        aria-label="修改挂号状态"
                        @change="appointmentAction(row, 'status', $event)"
                      >
                        <option value="PENDING">待确认</option>
                        <option value="CONFIRMED">已确认</option>
                        <option value="COMPLETED">已完成</option>
                        <option value="CANCELLED">已取消</option></select
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/appointments/${row.id}/reports`"
                        >报告</RouterLink
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
                        @click="appointmentAction(row, 'cancel')"
                      >
                        取消
                      </button></template
                    ><template v-else-if="resource === 'hospitalizations'"
                      ><RouterLink
                        class="admin-link"
                        :to="`/app/admin/patients/${row.patientId}`"
                        >患者档案</RouterLink
                      ><RouterLink
                        v-if="
                          row.status === 'DISCHARGED' && !Number(row.totalCost)
                        "
                        class="admin-link"
                        :to="`/app/admin/hospitalizations/${row.id}/settle`"
                        >结算</RouterLink
                      ></template
                    ><RouterLink
                      v-else-if="resource === 'reports'"
                      class="admin-link"
                      :to="`/app/admin/reports/${row.id}/preview`"
                      >查看报告</RouterLink
                    >
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <AdminPagination
          v-model:page="page"
          :total-pages="totalPages"
          :total="filteredRows.length"
        />
      </section>
    </template>
  </div>
</template>
