<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import AdminFeedback from "./AdminFeedback.vue";
import AdminPagination from "./AdminPagination.vue";
import { useAdminPage, useAdminPagination, items } from "./useAdminPage";
import { formatDate } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const route = useRoute(),
  router = useRouter();
const filters = reactive<Record<string, any>>({
  whoUser: String(route.query.whoUser || ""),
  whereScene: String(route.query.whereScene || ""),
  fromTime: String(route.query.fromTime || ""),
  toTime: String(route.query.toTime || ""),
  limit: Number(route.query.limit || 200),
});
const selected = ref<any[]>([]),
  expanded = ref<Set<string>>(new Set());
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () => "/admin/ai-logs",
);
const logs = computed(() => items(data.value, "logs"));
const { page, totalPages, pageRows } = useAdminPagination(logs);
const allChecked = computed(() => !!pageRows.value.length && pageRows.value.every(log => selected.value.includes(log.id)));
const quickRange = ref(filters.fromTime || filters.toTime ? 'custom' : 'all');
watch(page, () => { selected.value = []; });
const scenes = [
  { value: "doctor.diagnosis", label: "医生 · 诊断建议" },
  { value: "doctor.report", label: "医生 · 报告草稿" },
  { value: "patient.triage", label: "患者 · 智能导诊" },
  { value: "admin.summary", label: "管理 · 运营摘要" },
];
async function query() {
  const fields: Record<string, string> = {};
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== "" && value != null) fields[key] = String(value);
  });
  selected.value = [];
  if (router.resolve({ path: "/app/admin/ai-logs", query: fields }).fullPath === route.fullPath) await load();
  else await router.replace({ path: "/app/admin/ai-logs", query: fields });
}
function localTime(date: Date) {
  const pad = (v: number) => String(v).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
async function range(hours: number) {
  const now = new Date(),
    start =
      hours === 0
        ? new Date(now.getFullYear(), now.getMonth(), now.getDate())
        : new Date(now.getTime() - hours * 3600000);
  filters.fromTime = localTime(start);
  filters.toTime = localTime(now);
}
async function changeRange() {
  if (quickRange.value === 'custom') return;
  if (quickRange.value === 'all') {
    filters.fromTime = ''; filters.toTime = '';
    return;
  }
  await range(Number(quickRange.value));
}
function expand(id: string) {
  const copy = new Set(expanded.value);
  copy.has(id) ? copy.delete(id) : copy.add(id);
  expanded.value = copy;
}
function toggleAll(event: Event) {
  selected.value = (event.target as HTMLInputElement).checked
    ? pageRows.value.map((log) => log.id)
    : [];
}
async function remove() {
  if (
    await confirmAction(
      `确定删除选中的 ${selected.value.length} 条审计记录？删除后无法恢复。`,
    )
  ) {
    if (
      await action("/admin/ai-logs/bulk-delete", {
        ...filters,
        ids: selected.value,
      })
    )
      selected.value = [];
  }
}
async function reset() {
  quickRange.value = 'all';
  Object.assign(filters, {
    whoUser: "",
    whereScene: "",
    fromTime: "",
    toTime: "",
    limit: 200,
  });
  await query();
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      title="AI 生成审计"
      description="追溯智能导诊、诊断建议、报告草稿与运营摘要的触发人、输入和输出。"
      ><button class="button button-secondary" :disabled="loading || saving" @click="load">
        刷新日志
      </button></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <section class="panel">
      <div class="admin-panel-heading">
        <div>
          <h2>审计检索</h2>
          <p>按触发人、业务场景与时间区间组合查询</p>
        </div>
        <UiIcon name="shield" :size="21" />
      </div>
      <form @submit.prevent="query">
        <div class="admin-form-grid admin-log-filters">
          <label class="admin-field"
            ><span>触发人</span
            ><select v-model="filters.whoUser">
              <option value="">全部触发人</option>
              <option
                v-for="user in data.triggerUsers || []"
                :key="user"
                :value="user"
              >
                {{ user }}
              </option>
            </select></label
          ><label class="admin-field"
            ><span>业务场景</span
            ><select v-model="filters.whereScene">
              <option value="">全部场景</option>
              <option
                v-for="scene in scenes"
                :key="scene.value"
                :value="scene.value"
              >
                {{ scene.label }}
              </option>
            </select></label
          ><label class="admin-field"
            ><span>时间范围</span><select v-model="quickRange" @change="changeRange"><option value="all">全部时间</option><option value="0">今天</option><option value="24">近 24 小时</option><option value="72">近 3 天</option><option value="custom">自定义时间</option></select></label>
          <label v-if="quickRange === 'custom'" class="admin-field"
            ><span>开始时间</span
            ><input v-model="filters.fromTime" type="datetime-local" /></label
          ><label v-if="quickRange === 'custom'" class="admin-field"
            ><span>结束时间</span
            ><input
              v-model="filters.toTime"
              type="datetime-local"
              :min="filters.fromTime" /></label
          ><label v-if="quickRange === 'custom'" class="admin-field"
            ><span>返回记录上限</span
            ><input
              v-model="filters.limit"
              type="number"
              min="1"
              max="500"
              required
          /></label>
        </div>
        <div class="admin-toolbar" style="margin-top: 20px">
          <button class="button button-primary" type="submit" :disabled="loading">查询日志</button
          ><button class="button button-secondary" type="button" @click="reset">
            清空筛选</button>
        </div>
      </form>
    </section>
    <section v-if="!loading" class="panel">
      <div class="admin-panel-heading">
        <div>
          <h2>
            生成记录 <span class="admin-count">{{ logs.length }}</span>
          </h2>
          <p>展开后可在内容区滚动查看完整输入与生成结果</p>
        </div>
        <button
          v-if="selected.length"
          class="button button-danger"
          :disabled="saving || !selected.length"
          @click="remove"
        >
          删除选中{{ selected.length ? ` (${selected.length})` : "" }}
        </button>
      </div>
      <EmptyState
        v-if="!logs.length"
        title="暂无匹配审计记录"
        description="调整查询条件，或完成一次 AI 辅助生成后查看记录。"
      />
      <div v-else class="table-scroll">
        <table class="admin-table admin-ai-log-table">
          <thead>
            <tr>
              <th>
                <input
                  type="checkbox"
                  :checked="allChecked"
                  @change="toggleAll"
                  aria-label="选择当前页全部日志"
                />
              </th>
              <th>触发人 / 场景</th>
              <th>输入内容</th>
              <th>生成结果</th>
              <th>生成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in pageRows" :key="log.id">
              <td>
                <input
                  v-model="selected"
                  type="checkbox"
                  :value="log.id"
                  :aria-label="`选择日志 ${log.id}`"
                />
              </td>
              <td>
                <strong>{{ log.whoUserZh || log.whoUser }}</strong
                ><small>{{ log.whereSceneZh || log.whereScene }}</small>
              </td>
              <td>
                <div
                  :id="`ai-log-input-${log.id}`"
                  class="admin-log-text"
                  :class="{ clamped: !expanded.has(`in_${log.id}`), 'read-scroll read-scroll--compact': expanded.has(`in_${log.id}`) }"
                  :tabindex="expanded.has(`in_${log.id}`) ? 0 : undefined"
                  :role="expanded.has(`in_${log.id}`) ? 'region' : undefined"
                  :aria-label="expanded.has(`in_${log.id}`) ? `日志 ${log.id} 完整输入内容` : undefined"
                >
                  {{ log.inputPayloadZh || "未记录输入" }}
                </div>
                <button
                  type="button"
                  :aria-controls="`ai-log-input-${log.id}`"
                  :aria-expanded="expanded.has(`in_${log.id}`)"
                  class="admin-link admin-log-expand"
                  @click="expand(`in_${log.id}`)"
                >
                  {{ expanded.has(`in_${log.id}`) ? "收起" : "展开" }}
                </button>
              </td>
              <td>
                <div
                  :id="`ai-log-output-${log.id}`"
                  class="admin-log-text"
                  :class="{ clamped: !expanded.has(`out_${log.id}`), 'read-scroll read-scroll--compact': expanded.has(`out_${log.id}`) }"
                  :tabindex="expanded.has(`out_${log.id}`) ? 0 : undefined"
                  :role="expanded.has(`out_${log.id}`) ? 'region' : undefined"
                  :aria-label="expanded.has(`out_${log.id}`) ? `日志 ${log.id} 完整生成结果` : undefined"
                >
                  {{ log.outputText || "无输出" }}
                </div>
                <button
                  type="button"
                  :aria-controls="`ai-log-output-${log.id}`"
                  :aria-expanded="expanded.has(`out_${log.id}`)"
                  class="admin-link admin-log-expand"
                  @click="expand(`out_${log.id}`)"
                >
                  {{ expanded.has(`out_${log.id}`) ? "收起" : "展开" }}
                </button>
              </td>
              <td class="admin-id">{{ formatDate(log.createdAt, true) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <AdminPagination v-model:page="page" :total-pages="totalPages" :total="logs.length" />
    </section>
  </div>
</template>
