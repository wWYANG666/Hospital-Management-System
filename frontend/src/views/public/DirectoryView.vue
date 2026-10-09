<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { loadPage } from "@/lib/api";
import { rows, text } from "@/lib/format";
import UiIcon from "@/components/UiIcon.vue";
import PageHeader from "@/components/PageHeader.vue";
import EmptyState from "@/components/EmptyState.vue";
import ErrorState from "@/components/ErrorState.vue";
import LoadingState from "@/components/LoadingState.vue";
import type { PageData } from "@/types";
const route = useRoute();
const data = ref<PageData>({});
const error = ref("");
const loading = ref(true);
const keyword = ref(String(route.query.q || ""));
const selected = ref("");
const isDepartments = computed(() => route.path.endsWith("/departments"));
const query = computed(() => keyword.value.trim().toLocaleLowerCase());
const departments = computed(() =>
  rows(data.value, "departments").filter((d) =>
    (text(d.name, '') + ' ' + text(d.description, '')).toLocaleLowerCase().includes(query.value),
  ),
);
const doctors = computed(() =>
  rows(data.value, "doctors").filter(
    (d) =>
      (!selected.value || d.department === selected.value) &&
      [d.name, d.department, d.specialty].some((v) =>
        text(v, "").toLocaleLowerCase().includes(query.value),
      ),
  ),
);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    data.value = await loadPage("/public/directory");
    error.value = "";
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
watch(() => route.query.q, value => { keyword.value = String(value || ''); });
function resetFilters() { keyword.value = ''; selected.value = ''; }
onMounted(load);
</script>
<template>
  <main class="public-inner">
    <PageHeader
      :title="isDepartments ? '科室导航' : '医生排班'"
      :description="
        isDepartments
          ? '选择就诊科室，查看对应医生与预约号源。'
          : '按姓名、科室或擅长方向查找医生，出诊信息以实时排班为准。'
      "
    />
    <div class="directory-tabs" aria-label="查询方式"><RouterLink to="/app/departments" :class="{ active: isDepartments }" :aria-current="isDepartments ? 'page' : undefined">按科室查询</RouterLink><RouterLink to="/app/doctors" :class="{ active: !isDepartments }" :aria-current="!isDepartments ? 'page' : undefined">按医生查询</RouterLink></div>
    <div class="panel directory-toolbar">
      <UiIcon name="search" /><input
        v-model="keyword"
        :placeholder="isDepartments ? '搜索科室名称' : '搜索医生、科室或专长'"
        aria-label="搜索"
      /><select v-if="!isDepartments" v-model="selected" aria-label="筛选科室">
        <option value="">全部科室</option>
        <option
          v-for="department in rows(data, 'departments')"
          :key="department.id"
          :value="department.name"
        >
          {{ department.name }}
        </option>
      </select>
      <button v-if="keyword || selected" class="button button-link" @click="resetFilters">清空筛选</button>
    </div>
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="load" />
    <div v-else-if="isDepartments" class="directory-department-grid">
      <RouterLink
        v-for="dept in departments"
        :key="dept.id"
        class="panel department-card"
        :to="{
          path: '/app/patient/appointments/new/step2',
          query: { departmentId: dept.id },
        }"
        ><UiIcon name="building" :size="26" />
        <h2>{{ dept.name }}</h2>
        <p>{{ text(dept.description, "查看该科室医生与出诊安排") }}</p>
        <span
          >查看号源 <UiIcon name="arrow-right" :size="16" /></span></RouterLink
      ><EmptyState
        v-if="!departments.length"
          :title="keyword ? '未找到匹配的科室' : '暂无开放科室'"
          :description="keyword ? '请尝试其他关键词。' : '新的科室信息会在发布后显示。'"
      />
    </div>
    <div v-else class="directory-doctor-list">
      <article
        v-for="doctor in doctors"
        :key="doctor.id"
        class="panel directory-doctor"
      >
        <span class="doctor-avatar"
          ><UiIcon name="stethoscope" :size="28"
        /></span>
        <div>
          <h2>
            {{ text(doctor.name)
            }}<small>{{ text(doctor.title, "医生") }}</small>
          </h2>
          <span class="department-label">{{ doctor.department }}</span>
          <p><strong>擅长：</strong>{{ text(doctor.specialty, "暂无介绍") }}</p>
          <p v-if="doctor.introduction">{{ doctor.introduction }}</p>
        </div>
        <div class="directory-doctor-action">
          <span
            class="availability-label"
            :class="{ available: rows(doctor, 'schedules').length }"
            >{{
              rows(doctor, "schedules").length ? "近期有排班" : "暂无近期排班"
            }}</span
          ><RouterLink
            class="button button-primary"
            :to="{
              path: '/app/patient/appointments/new/step3',
              query: { doctorId: doctor.id },
            }"
            >查看预约<UiIcon name="arrow-right" :size="16"
          /></RouterLink>
        </div>
      </article>
      <EmptyState
        v-if="!doctors.length"
        :title="keyword || selected ? '未找到匹配的医生' : '暂无医生信息'"
        :description="keyword || selected ? '请更换科室或搜索关键词。' : '医生信息发布后会在这里显示。'"
      />
    </div>
  </main>
</template>
