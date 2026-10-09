<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import AdminFeedback from "./AdminFeedback.vue";
import AdminPagination from "./AdminPagination.vue";
import { useAdminPage, useAdminPagination, items, entry, type AdminData } from "./useAdminPage";
import { confirmAction } from "@/lib/confirm";

const route = useRoute(),
  departmentForm = reactive<AdminData>({}),
  doctorForm = reactive<AdminData>({});
const creating = computed(() => !route.params.id),
  showCreate = ref(false),
  search = ref(""),
  onlyPending = ref(false);
const { data, loading, saving, error, notice, load, action } = useAdminPage(
  () =>
    creating.value
      ? "/admin/departments/new"
      : `/admin/departments/${route.params.id}/edit`,
  (value) => {
    Object.assign(departmentForm, value.department || {});
    doctorForm.department = value.department?.name || "";
  },
);
const doctors = computed(() =>
  items(data.value, "departmentDoctors").filter((d) => {
    const user = entry(data.value, "doctorUserMap", d.id);
    return (
      (!onlyPending.value || user.status === 0) &&
      `${user.realName || ""} ${user.username || ""} ${d.doctorCode || ""} ${d.title || ""}`
        .toLowerCase()
        .includes(search.value.trim().toLowerCase())
    );
  }),
);
const { page, totalPages, pageRows } = useAdminPagination(doctors);
const pendingCount = computed(
  () =>
    items(data.value, "departmentDoctors").filter(
      (d) => entry(data.value, "doctorUserMap", d.id).status === 0,
    ).length,
);
async function saveDepartment() {
  await action(
    creating.value
      ? "/admin/departments"
      : `/admin/departments/${route.params.id}`,
    { ...departmentForm },
    "/admin/departments",
  );
}
async function createDoctor() {
  if (
    await action(`/admin/departments/${route.params.id}/doctors`, {
      ...doctorForm,
      department: data.value.department.name,
    })
  ) {
    Object.keys(doctorForm).forEach((key) => {
      if (key !== "department") delete doctorForm[key];
    });
    showCreate.value = false;
  }
}
async function deleteDoctor(id: any) {
  if (await confirmAction("删除此医生及账号、关联排班？删除后无法恢复。"))
    await action(`/admin/doctors/${id}/delete`, {
      departmentId: route.params.id,
    });
}
</script>
<template>
  <div class="admin-view">
    <PageHeader
      :title="creating ? '新增科室' : `${data.department?.name || '科室'}管理`"
      description="维护科室介绍，管理所属医生账号、注册审核与出诊安排。"
      ><RouterLink class="button button-secondary" to="/app/admin/departments"
        ><UiIcon name="chevron-left" :size="17" />返回科室列表</RouterLink
      ></PageHeader
    >
    <AdminFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <template v-if="!loading">
      <form class="panel" @submit.prevent="saveDepartment">
        <div class="admin-panel-heading">
          <div>
            <h2>科室基础信息</h2>
            <p>科室名称修改后会同步关联医生的所属科室</p>
          </div>
          <UiIcon name="building" :size="22" />
        </div>
        <div class="admin-form-grid">
          <label class="admin-field"
            ><span>科室名称 <span class="admin-required">*</span></span
            ><input
              v-model="departmentForm.name"
              required
              placeholder="例如：呼吸内科" /></label
          ><label class="admin-field full"
            ><span>科室介绍</span
            ><textarea
              v-model="departmentForm.description"
              rows="3"
              placeholder="填写诊疗范围、科室特点与就诊说明"
            ></textarea>
          </label>
        </div>
        <div class="admin-form-footer" style="margin-top: 22px">
          <button
            class="button button-primary"
            type="submit"
            :disabled="saving"
          >
            {{ saving ? "保存中…" : "保存科室信息" }}
          </button>
        </div>
      </form>
      <section v-if="!creating" class="panel">
        <div class="admin-panel-heading">
          <div>
            <h2>
              科室医生
              <span class="admin-count">{{
                items(data, "departmentDoctors").length
              }}</span>
            </h2>
            <p>审核注册账号，维护访问权限与排班</p>
          </div>
          <button
            class="button button-primary"
            @click="showCreate = !showCreate"
          >
            <UiIcon :name="showCreate ? 'close' : 'plus'" :size="17" />{{
              showCreate ? "收起表单" : "添加医生"
            }}
          </button>
        </div>
        <form
          v-if="showCreate"
          class="admin-review-card"
          @submit.prevent="createDoctor"
        >
          <h3>建立医生档案与账号</h3>
          <div class="admin-form-grid">
            <label class="admin-field"
              ><span>登录账号 *</span
              ><input
                v-model="doctorForm.username"
                required
                autocomplete="off" /></label
            ><label class="admin-field"
              ><span>初始密码 *</span
              ><input
                v-model="doctorForm.password"
                type="password"
                required
                autocomplete="new-password"
                minlength="6" /></label
            ><label class="admin-field"
              ><span>医生姓名 *</span
              ><input v-model="doctorForm.realName" required /></label
            ><label class="admin-field"
              ><span>所属科室</span
              ><input :value="data.department.name" readonly /></label
            ><label class="admin-field"
              ><span>职称</span
              ><input
                v-model="doctorForm.title"
                placeholder="例如：副主任医师" /></label
            ><label class="admin-field"
              ><span>手机号</span
              ><input v-model="doctorForm.phone" type="tel" /></label
            ><label class="admin-field"
              ><span>邮箱</span
              ><input v-model="doctorForm.email" type="email" /></label
            ><label class="admin-field"
              ><span>诊疗专长</span
              ><input v-model="doctorForm.specialty" /></label
            ><label class="admin-field full"
              ><span>医生介绍</span
              ><textarea v-model="doctorForm.introduction" rows="3"></textarea>
            </label>
          </div>
          <div class="admin-form-footer" style="margin-top: 20px">
            <button
              class="button button-primary"
              type="submit"
              :disabled="saving"
            >
              创建医生账号</button
            ><button
              class="button button-secondary"
              type="button"
              @click="showCreate = false"
            >
              取消
            </button>
          </div>
        </form>
        <div class="admin-toolbar">
          <label class="admin-search"
            ><UiIcon name="search" :size="17" /><input
              v-model="search"
              type="search"
              placeholder="检索姓名、工号或职称"
              aria-label="检索医生" /></label
          ><button
            class="button"
            :class="onlyPending ? 'button-primary' : 'button-secondary'"
            :aria-pressed="onlyPending"
            @click="onlyPending = !onlyPending"
          >
            待审核 {{ pendingCount }}</button
          ><button v-if="search || onlyPending" class="button button-secondary" @click="search = ''; onlyPending = false">清空筛选</button><span class="admin-result-meta"
            >显示 {{ doctors.length }} 位医生</span
          >
        </div>
        <EmptyState
          v-if="!doctors.length"
          :title="onlyPending ? '暂无待审核医生' : '暂无匹配的医生'"
          description="添加医生后，可配置出诊安排和号源。"
        />
        <div v-else class="table-scroll">
          <table class="admin-table">
            <thead>
              <tr>
                <th>医生 / 工号</th>
                <th>登录账号</th>
                <th>职称 / 专长</th>
                <th>联系方式</th>
                <th>账号状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="doctor in pageRows" :key="doctor.id">
                <td>
                  <strong>{{
                    entry(data, "doctorUserMap", doctor.id).realName ||
                    "姓名待维护"
                  }}</strong
                  ><small>{{
                    doctor.doctorCode || `医生 #${doctor.id}`
                  }}</small>
                </td>
                <td>
                  {{
                    entry(data, "doctorUserMap", doctor.id).username || "未关联"
                  }}
                </td>
                <td>
                  {{ doctor.title || "待补充职称"
                  }}<small>{{ doctor.specialty || "专长待维护" }}</small>
                </td>
                <td>
                  {{ entry(data, "doctorUserMap", doctor.id).phone || "未登记"
                  }}<small>{{
                    entry(data, "doctorUserMap", doctor.id).email
                  }}</small>
                </td>
                <td>
                  <span
                    class="status-badge"
                    :class="
                      entry(data, 'doctorUserMap', doctor.id).status === 0
                        ? 'status-warning'
                        : 'status-success'
                    "
                    >{{
                      entry(data, "doctorUserMap", doctor.id).status === 0
                        ? "注册待审核"
                        : "已启用"
                    }}</span
                  >
                </td>
                <td>
                  <div class="admin-row-actions">
                    <button
                      v-if="
                        entry(data, 'doctorUserMap', doctor.id).status === 0
                      "
                      class="admin-link"
                      :disabled="saving"
                      @click="
                        action(`/admin/doctors/${doctor.id}/approve`, {
                          departmentId: route.params.id,
                        })
                      "
                    >
                      审核通过</button
                    ><RouterLink
                      class="admin-link"
                      :to="`/app/admin/departments/${route.params.id}/doctors/${doctor.id}/schedules`"
                      >出诊排班</RouterLink
                    ><details class="admin-more-actions"><summary>更多操作</summary><div><RouterLink
                      v-if="doctor.userId"
                      class="admin-link"
                      :to="{
                        path: `/app/admin/users/${doctor.userId}/change-password`,
                        query: { departmentId: String(route.params.id) },
                      }"
                      >修改密码</RouterLink
                    ><button
                      class="admin-link danger"
                      :disabled="saving"
                      @click="deleteDoctor(doctor.id)"
                    >
                      删除
                    </button></div></details>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <AdminPagination v-model:page="page" :total-pages="totalPages" :total="doctors.length" />
      </section>
    </template>
  </div>
</template>
