<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import AdminFeedback from "./AdminFeedback.vue";
import AdminPagination from "./AdminPagination.vue";
import {
  useAdminPage,
  useAdminPagination,
  items,
  scheduleStatus,
  type AdminData,
} from "./useAdminPage";
import { formatDate } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
import { scheduleState, scheduleTone } from "@/lib/schedule";
const route = useRoute(),
  filter = ref(""),
  selected = ref<any[]>([]);
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () =>
    `/admin/departments/${route.params.departmentId}/doctors/${route.params.doctorId}/schedules`,
);
const schedules = computed(() =>
  items(data.value, "schedules").filter(
    (s) => !filter.value || scheduleState(s) === filter.value,
  ),
);
const { page, totalPages, pageRows } = useAdminPagination(schedules);
const allChecked = computed(
  () =>
    !!pageRows.value.length &&
    pageRows.value.every((s) => selected.value.includes(s.id)),
);
watch([filter, page], () => {
  selected.value = [];
});
const context = computed(() => ({
  departmentId: route.params.departmentId,
  doctorId: route.params.doctorId,
}));
function toggleAll(event: Event) {
  selected.value = (event.target as HTMLInputElement).checked
    ? pageRows.value.map((s) => s.id)
    : [];
}
async function remove() {
  if (
    await confirmAction(`确定删除选中的 ${selected.value.length} 条排班吗？`)
  ) {
    if (
      await action("/admin/schedules/bulk-delete", {
        ids: selected.value,
        ...context.value,
      })
    )
      selected.value = [];
  }
}
async function operate(s: AdminData, op: string) {
  if (
    (op !== "delete" && op !== "reject-leave") ||
    (await confirmAction(
      op === "reject-leave" ? "确定驳回该医生的请假申请？" : "确定删除该排班？",
    ))
  )
    await action(`/admin/schedules/${s.id}/${op}`, context.value);
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      :title="`${data.doctorUser?.realName || '医生'}的出诊排班`"
      :description="`${data.department?.name || '科室'} · ${data.doctor?.title || '医生'} · 普通与专家门诊号源`"
      ><RouterLink
        class="button button-primary"
        :to="{
          path: '/app/admin/schedules/new',
          query: {
            departmentId: String(route.params.departmentId),
            doctorId: String(route.params.doctorId),
          },
        }"
        ><UiIcon name="plus" :size="17" />新增排班</RouterLink
      ><RouterLink
        class="button button-secondary"
        :to="`/app/admin/departments/${route.params.departmentId}/edit`"
        >返回科室</RouterLink
      ></PageHeader
    ><AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <section v-if="!loading" class="panel">
      <div class="admin-panel-heading">
        <div>
          <h2>出诊安排</h2>
          <p>号源使用数按已关联排班的有效预约统计</p>
        </div>
      </div>
      <div class="admin-mini-metrics">
        <div>
          <strong>{{ items(data, "schedules").length }}</strong
          >排班
        </div>
        <div>
          <strong>{{
            items(data, "schedules").filter(
              (s) => scheduleState(s) === "LEAVE_PENDING",
            ).length
          }}</strong
          >待审批请假
        </div>
        <div>
          <strong>{{
            items(data, "schedules").filter(
              (s) => scheduleState(s) === "ACTIVE",
            ).length
          }}</strong
          >有效启用班次
        </div>
      </div>
      <div class="admin-toolbar">
        <select v-model="filter" aria-label="排班状态">
          <option value="">全部状态</option>
          <option value="ACTIVE">启用中</option>
          <option value="LEAVE_PENDING">请假待审批</option>
          <option value="DISABLED">已停用</option>
          <option value="ENDED">已结束</option></select
        ><button
          v-if="selected.length"
          class="button button-danger"
          :disabled="!selected.length || saving"
          @click="remove"
        >
          删除选中 ({{ selected.length }})
        </button>
      </div>
      <EmptyState
        v-if="!schedules.length"
        title="暂无对应排班"
        description="点击新增排班，为医生配置出诊时段与号源。"
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
                  aria-label="选择当前页全部排班"
                />
              </th>
              <th>出诊日期</th>
              <th>班次</th>
              <th>门诊类型</th>
              <th>号源使用</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in pageRows" :key="s.id">
              <td>
                <input
                  v-model="selected"
                  type="checkbox"
                  :value="s.id"
                  :aria-label="`选择排班 ${s.id}`"
                />
              </td>
              <td>{{ formatDate(s.workDate) }}</td>
              <td>
                {{
                  s.workTime === "MORNING"
                    ? "上午"
                    : s.workTime === "AFTERNOON"
                      ? "下午"
                      : "晚上"
                }}
              </td>
              <td>
                {{ s.scheduleType === "EXPERT" ? "专家门诊" : "普通门诊" }}
              </td>
              <td>
                {{ s.currentAppointments || 0 }} / {{ s.maxAppointments }}
              </td>
              <td>
                <span class="status-badge" :class="scheduleTone(s)">{{
                  scheduleStatus(s)
                }}</span>
              </td>
              <td>
                <div class="admin-row-actions">
                  <button
                    v-if="scheduleState(s) === 'LEAVE_PENDING'"
                    class="admin-link"
                    :disabled="saving"
                    @click="operate(s, 'approve-leave')"
                  >
                    同意请假</button
                  ><button
                    v-if="scheduleState(s) === 'LEAVE_PENDING'"
                    class="admin-link"
                    :disabled="saving"
                    @click="operate(s, 'reject-leave')"
                  >
                    驳回申请</button
                  ><button
                    v-if="scheduleState(s) === 'DISABLED'"
                    class="admin-link"
                    :disabled="saving"
                    @click="operate(s, 'restore')"
                  >
                    恢复出诊</button
                  ><button
                    class="admin-link danger"
                    :disabled="saving"
                    @click="operate(s, 'delete')"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AdminPagination
        v-model:page="page"
        :total-pages="totalPages"
        :total="schedules.length"
      />
    </section>
  </div>
</template>
