<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from "vue";
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
  type AdminData,
} from "./useAdminPage";
import { money, formatDate, statusText } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const chosen = reactive<AdminData>({}),
  filter = ref(""),
  department = ref(""),
  search = ref(""),
  selected = ref<any[]>([]),
  selectedBed = ref<AdminData | null>(null),
  dialog = ref<HTMLDialogElement>();
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => "/admin/beds",
  (value) =>
    items(value, "pendingRequests").forEach(
      (req) => (chosen[req.id] = req.bedId || ""),
    ),
);
const beds = computed(() => items(data.value, "beds")),
  available = computed(() =>
    beds.value.filter((b) => b.status === "AVAILABLE"),
  ),
  pending = computed(() => items(data.value, "pendingRequests"));
const filtered = computed(() =>
  beds.value.filter(
    (b) =>
      (!filter.value || b.status === filter.value) &&
      (!department.value || String(b.departmentId) === department.value) &&
      `${b.bedNumber || ""} ${b.roomNumber || ""} ${b.ward || ""}`.includes(
        search.value.trim(),
      ),
  ),
);
const { page, totalPages, pageRows } = useAdminPagination(filtered, 24);
watch([filter, department, search, page], () => { selected.value = []; });
const groups = computed(() => {
  const value: Record<string, AdminData[]> = {};
  pageRows.value.forEach((b) => {
    const label =
      entry(data.value, "departmentMap", b.departmentId).name || "未分配科室";
    (value[label] ||= []).push(b);
  });
  return value;
});
async function viewBed(bed: AdminData) {
  selectedBed.value = bed;
  await nextTick();
  dialog.value?.showModal();
}
function close() {
  dialog.value?.close();
  selectedBed.value = null;
}
async function assign(req: AdminData) {
  await action("/admin/beds/assign", {
    bedId: chosen[req.id],
    hospitalizationId: req.id,
  });
}
async function reject(req: AdminData) {
  if (await confirmAction("确定拒绝此住院申请吗？"))
    await action("/admin/beds/reject", { hospitalizationId: req.id });
}
async function removeSelected() {
  if (await confirmAction(`确定删除选中的 ${selected.value.length} 个床位？`)) {
    if (await action("/admin/beds/bulk-delete", { ids: selected.value }))
      selected.value = [];
  }
}
async function removeBed(bed: AdminData) {
  const wasOpen = !!dialog.value?.open;
  dialog.value?.close();
  await nextTick();
  if (await confirmAction(`确定删除床位 ${bed.bedNumber || bed.id}？正在使用的床位不能删除。`)) {
    selectedBed.value = null;
    await action(`/admin/beds/${bed.id}/delete`);
  } else if (wasOpen) {
    selectedBed.value = bed;
    await nextTick();
    dialog.value?.showModal();
  }
}
const records = computed(() =>
  selectedBed.value
    ? data.value.bedAllHospitalizationsMap?.[selectedBed.value.id] || []
    : [],
);
</script>
<template>
  <div class="admin-view">
    <PageHeader
      title="住院床位看板"
      description="按科室查看床位状态，处理住院申请与床位分配。"
      ><RouterLink class="button button-primary" to="/app/admin/beds/new"
        ><UiIcon name="plus" :size="17" />新增床位</RouterLink
      ><RouterLink
        class="button button-secondary"
        to="/app/admin/hospitalizations"
        >住院记录</RouterLink
      ></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    /><template v-if="!loading"
      ><section class="admin-metrics">
        <div class="admin-metric">
          <span class="admin-metric-icon"><UiIcon name="bed" :size="23" /></span
          ><span>全院床位</span
          ><strong>{{ beds.length }}<small>张</small></strong>
        </div>
        <div class="admin-metric">
          <span class="admin-metric-icon teal"
            ><UiIcon name="check" :size="23" /></span
          ><span>空闲可分配</span
          ><strong>{{ available.length }}<small>张</small></strong>
        </div>
        <div class="admin-metric">
          <span class="admin-metric-icon"
            ><UiIcon name="user" :size="23" /></span
          ><span>使用中</span
          ><strong
            >{{ beds.filter((b) => b.status === "OCCUPIED").length
            }}<small>张</small></strong
          >
        </div>
        <div class="admin-metric">
          <span class="admin-metric-icon amber"
            ><UiIcon name="clipboard" :size="23" /></span
          ><span>住院待审批</span
          ><strong>{{ pending.length }}<small>项</small></strong>
        </div>
      </section>
      <section v-if="pending.length" class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>
              待审批住院申请
              <span class="admin-count">{{ pending.length }}</span>
            </h2>
            <p>核对患者信息、住院原因与床位资源后完成审批</p>
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
              <dt>预计住院</dt>
              <dd>{{ req.expectedDays ?? "待评估" }} 天</dd>
            </div>
            <div>
              <dt>申请时间</dt>
              <dd>{{ formatDate(req.requestTime, true) }}</dd>
            </div>
          </dl>
          <form class="toolbar" @submit.prevent="assign(req)">
            <select v-model="chosen[req.id]" required aria-label="选择住院床位">
              <option value="">请选择空闲床位</option>
              <option v-if="req.bedId" :value="req.bedId">
                {{
                  entry(
                    data,
                    "departmentMap",
                    beds.find((b) => b.id === req.bedId)?.departmentId,
                  ).name
                }}
                ·
                {{
                  beds.find((b) => b.id === req.bedId)?.bedNumber ||
                  `床位 #${req.bedId}`
                }}（医生已选择）
              </option>
              <option
                v-for="bed in available.filter((b) => b.id !== req.bedId)"
                :key="bed.id"
                :value="bed.id"
              >
                {{
                  entry(data, "departmentMap", bed.departmentId).name ||
                  "未分配科室"
                }}
                · {{ bed.bedNumber }} · {{ statusText(bed.bedType) }}
              </option></select
            ><button
              class="button button-primary"
              type="submit"
              :disabled="saving"
            >
              同意并分配床位</button
            ><button
              class="button button-secondary"
              type="button"
              :disabled="saving"
              @click="reject(req)"
            >
              拒绝申请
            </button>
          </form>
        </article>
      </section>
      <section class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>科室床位分布</h2>
            <p>空闲、使用中与维修状态，点击详情查看当前在院患者</p>
          </div>
          <button class="button button-secondary" @click="load">
            刷新床位
          </button>
        </div>
        <div class="admin-toolbar">
          <label class="admin-search"
            ><UiIcon name="search" :size="17" /><input
              v-model="search"
              type="search"
              placeholder="搜索床位号、房间或病区"
              aria-label="搜索床位" /></label
          ><select v-model="department" aria-label="按科室筛选">
            <option value="">全部科室</option>
            <option
              v-for="d in items(data, 'departments')"
              :key="d.id"
              :value="String(d.id)"
            >
              {{ d.name }}
            </option></select
          ><select v-model="filter" aria-label="按床位状态筛选">
            <option value="">全部状态</option>
            <option value="AVAILABLE">空闲</option>
            <option value="OCCUPIED">使用中</option>
            <option value="MAINTENANCE">维修</option></select
          ><button
            v-if="selected.length"
            class="button button-danger"
            :disabled="!selected.length || saving"
            @click="removeSelected"
          >
            删除选中 ({{ selected.length }})
          </button><button v-if="search || filter || department" class="button button-secondary" @click="search = ''; filter = ''; department = ''">清空筛选</button><span class="admin-result-meta">{{ filtered.length }} 张床位</span>
        </div>
        <EmptyState
          v-if="!filtered.length"
          title="暂无对应床位"
          description="调整筛选条件，或新增床位资源。"
        />
        <div v-for="(wardBeds, label) in groups" :key="label">
          <h2 class="admin-ward-title">
            <UiIcon name="building" :size="18" />{{ label }}
            <span class="admin-count">{{ totalPages > 1 ? '本页 ' : '' }}{{ wardBeds.length }} 张</span>
          </h2>
          <div class="admin-ward-grid">
            <article
              v-for="bed in wardBeds"
              :key="bed.id"
              class="admin-bed-card"
              :class="bed.status"
            >
              <input
                v-model="selected"
                type="checkbox"
                :disabled="bed.status === 'OCCUPIED' || saving"
                :value="bed.id"
                :aria-label="`选择床位 ${bed.bedNumber}`"
              />
              <h3>{{ bed.bedNumber }}</h3>
              <StatusBadge :status="bed.status" />
              <p>
                房间 {{ bed.roomNumber || "未设置" }} ·
                {{ statusText(bed.bedType) }}
              </p>
              <p>床位费 ¥{{ money(bed.pricePerDay) }} / 天</p>
              <p v-if="bed.status === 'OCCUPIED'">
                <strong>{{
                  name(
                    data,
                    "userMap",
                    entry(data, "bedHospitalizationMap", bed.id).patientId,
                  )
                }}</strong>
                ·
                {{
                  formatDate(
                    entry(data, "bedHospitalizationMap", bed.id).admissionDate,
                  )
                }}入院
              </p>
              <div class="admin-row-actions">
                <button class="admin-link" @click="viewBed(bed)">
                  查看详情</button
                ><RouterLink
                  class="admin-link"
                  :to="`/app/admin/beds/${bed.id}/edit`"
                  >编辑</RouterLink
                >
              </div>
            </article>
          </div>
        </div>
        <AdminPagination v-model:page="page" :total-pages="totalPages" :total="filtered.length" />
      </section></template
    >
    <dialog
      v-if="selectedBed"
      ref="dialog"
      class="admin-dialog"
      aria-labelledby="admin-bed-dialog-title"
      @cancel="close"
      @close="selectedBed = null"
    >
      <div class="admin-view">
        <div class="admin-panel-heading">
          <div>
            <h2 id="admin-bed-dialog-title">床位 {{ selectedBed.bedNumber }}</h2>
            <p>床位资源与当前在院患者</p>
          </div>
          <button
            class="button button-secondary"
            aria-label="关闭床位详情"
            @click="close"
          >
            <UiIcon name="close" :size="18" />
          </button>
        </div>
        <dl class="admin-info-grid">
          <div>
            <dt>房间号</dt>
            <dd>{{ selectedBed.roomNumber || "未设置" }}</dd>
          </div>
          <div>
            <dt>病区</dt>
            <dd>{{ selectedBed.ward || "未分类" }}</dd>
          </div>
          <div>
            <dt>床位类型</dt>
            <dd>{{ statusText(selectedBed.bedType) }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ statusText(selectedBed.status) }}</dd>
          </div>
          <div>
            <dt>每日费用</dt>
            <dd>¥{{ money(selectedBed.pricePerDay) }}</dd>
          </div>
        </dl>
        <h2 class="admin-ward-title">当前住院记录</h2>
        <EmptyState v-if="!records.length" title="该床位暂无当前住院患者" />
        <div v-else class="table-scroll">
          <table class="admin-table">
            <thead>
              <tr>
                <th>患者</th>
                <th>主治医生</th>
                <th>入院日期</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="h in records" :key="h.id">
                <td>{{ name(data, "userMap", h.patientId) }}</td>
                <td>{{ name(data, "doctorUserMap", h.id) }}</td>
                <td>{{ formatDate(h.admissionDate) }}</td>
                <td><StatusBadge :status="h.status" /></td>
                <td>
                  <RouterLink
                    class="admin-link"
                    :to="`/app/admin/patients/${h.patientId}`"
                    @click="close"
                    >患者档案</RouterLink
                  >
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="admin-form-footer" style="margin-top: 22px">
          <RouterLink
            class="button button-primary"
            :to="`/app/admin/beds/${selectedBed.id}/edit`"
            @click="close"
            >编辑床位</RouterLink
          ><button class="button button-danger" :disabled="saving || selectedBed.status === 'OCCUPIED'" @click="removeBed(selectedBed)">
            删除床位
          </button>
        </div>
      </div>
    </dialog>
  </div>
</template>
