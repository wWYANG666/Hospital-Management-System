<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { loadPage } from "@/lib/api";
import UiIcon from "@/components/UiIcon.vue";

interface Department {
  id: number;
  name: string;
}
interface Doctor {
  id: number;
  name: string;
  department: string;
  title: string;
  specialty?: string;
  schedules: unknown[];
}
interface Directory {
  departments: Department[];
  doctors: Doctor[];
}

const router = useRouter();
const mode = ref<"department" | "doctor">("department");
const directory = ref<Directory>({ departments: [], doctors: [] });
const loading = ref(true);
const error = ref("");
const departmentId = ref("");
const departmentDoctorId = ref("");
const doctorId = ref("");
const keyword = ref("");
const navigating = ref(false);
const department = computed(() =>
  directory.value.departments.find((item) => String(item.id) === departmentId.value),
);
const departmentDoctors = computed(() =>
  department.value
    ? directory.value.doctors.filter((doctor) => doctor.department === department.value?.name)
    : [],
);
const matchingDoctors = computed(() => {
  const query = keyword.value.trim().toLocaleLowerCase();
  return directory.value.doctors.filter((doctor) =>
    [doctor.name, doctor.department, doctor.title, doctor.specialty]
      .some((value) => String(value || "").toLocaleLowerCase().includes(query)),
  );
});
const selectedDoctor = computed(() =>
  (mode.value === "department" ? departmentDoctors.value : matchingDoctors.value)
    .find((doctor) => String(doctor.id) === (mode.value === "department" ? departmentDoctorId.value : doctorId.value)),
);
const canContinue = computed(() =>
  !loading.value && !error.value && !navigating.value &&
  (!!selectedDoctor.value || (mode.value === "department" && !!department.value && !!departmentDoctors.value.length)),
);

watch(departmentId, () => { departmentDoctorId.value = ""; });
watch(matchingDoctors, (doctors) => {
  if (!doctors.some((doctor) => String(doctor.id) === doctorId.value)) doctorId.value = "";
});
async function load() {
  loading.value = true;
  error.value = "";
  try {
    directory.value = await loadPage<Directory>("/public/directory");
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "科室与医生信息加载失败，请重试。";
  } finally {
    loading.value = false;
  }
}
async function continueBooking() {
  if (!canContinue.value) return;
  navigating.value = true;
  try {
    await router.push(selectedDoctor.value
      ? {
          path: "/app/patient/appointments/new/step3",
          query: { doctorId: selectedDoctor.value.id },
        }
      : {
          path: "/app/patient/appointments/new/step2",
          query: { departmentId: department.value?.id },
        });
  } finally {
    navigating.value = false;
  }
}
function changeTab(event: KeyboardEvent) {
  if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
  event.preventDefault();
  mode.value = event.key === "Home" ? "department"
    : event.key === "End" ? "doctor"
      : mode.value === "department" ? "doctor" : "department";
  const tabs = (event.currentTarget as HTMLElement).querySelectorAll<HTMLButtonElement>("[role=tab]");
  tabs[mode.value === "department" ? 0 : 1]?.focus();
}
onMounted(load);
</script>

<template>
  <div class="appointment-finder">
    <div class="appointment-finder-tabs" role="tablist" aria-label="预约查询方式" @keydown="changeTab">
      <button id="finder-department-tab" type="button" role="tab" aria-controls="finder-panel"
        :aria-selected="mode === 'department'" :tabindex="mode === 'department' ? 0 : -1"
        :class="{ active: mode === 'department' }" @click="mode = 'department'">
        <UiIcon name="building" :size="18" />按科室
      </button>
      <button id="finder-doctor-tab" type="button" role="tab" aria-controls="finder-panel"
        :aria-selected="mode === 'doctor'" :tabindex="mode === 'doctor' ? 0 : -1"
        :class="{ active: mode === 'doctor' }" @click="mode = 'doctor'">
        <UiIcon name="stethoscope" :size="18" />按医生
      </button>
    </div>

    <div id="finder-panel" role="tabpanel" :aria-labelledby="mode === 'department' ? 'finder-department-tab' : 'finder-doctor-tab'" :aria-busy="loading">
      <p v-if="loading" class="appointment-finder-state" role="status">正在加载科室与医生信息…</p>
      <div v-else-if="error" class="appointment-finder-state appointment-finder-error" role="alert">
        <UiIcon name="alert" :size="20" /><p>{{ error }}</p>
        <button type="button" class="button button-secondary" @click="load">重新加载</button>
      </div>
      <form v-else class="appointment-finder-form" @submit.prevent="continueBooking">
        <div class="appointment-finder-fields">
          <template v-if="mode === 'department'">
            <label class="appointment-finder-field" for="finder-department">
              <span>就诊科室</span>
              <select id="finder-department" v-model="departmentId" :disabled="!directory.departments.length">
                <option value="">请选择科室</option>
                <option v-for="item in directory.departments" :key="item.id" :value="String(item.id)">{{ item.name }}</option>
              </select>
            </label>
            <label class="appointment-finder-field" for="finder-department-doctor">
              <span>出诊医生 <small>可稍后选择</small></span>
              <select id="finder-department-doctor" v-model="departmentDoctorId" :disabled="!department || !departmentDoctors.length">
                <option value="">{{ !department ? '请先选择科室' : departmentDoctors.length ? '查看该科室全部医生' : '该科室暂无医生信息' }}</option>
                <option v-for="doctor in departmentDoctors" :key="doctor.id" :value="String(doctor.id)">{{ doctor.name }} · {{ doctor.title || '医生' }}</option>
              </select>
            </label>
          </template>
          <template v-else>
            <label class="appointment-finder-field" for="finder-keyword">
              <span>查找医生</span>
              <div class="appointment-finder-search"><UiIcon name="search" :size="19" />
                <input id="finder-keyword" v-model="keyword" type="search" placeholder="姓名、科室或擅长方向" autocomplete="off" />
              </div>
            </label>
            <label class="appointment-finder-field" for="finder-doctor">
              <span>选择医生</span>
              <select id="finder-doctor" v-model="doctorId" :disabled="!matchingDoctors.length">
                <option value="">{{ matchingDoctors.length ? '请选择医生' : '暂无匹配医生' }}</option>
                <option v-for="doctor in matchingDoctors" :key="doctor.id" :value="String(doctor.id)">{{ doctor.name }} · {{ doctor.title || '医生' }} · {{ doctor.department }}</option>
              </select>
            </label>
          </template>
          <button type="submit" class="button button-primary appointment-finder-submit" :disabled="!canContinue">
            {{ navigating ? '正在前往…' : selectedDoctor || mode === 'doctor' ? '查看可预约时段' : '查看科室医生' }}
            <UiIcon name="arrow-right" :size="17" />
          </button>
        </div>
        <p v-if="!directory.departments.length && mode === 'department'" class="appointment-finder-note" role="status">暂无开放科室，新的科室信息发布后会在这里显示。</p>
        <p v-else-if="mode === 'doctor' && !matchingDoctors.length" class="appointment-finder-note" role="status">{{ keyword.trim() ? '未找到匹配医生，请尝试其他姓名、科室或关键词。' : '暂无医生信息，请稍后再查看。' }}</p>
        <p v-else-if="selectedDoctor" class="appointment-finder-note" role="status">
          {{ selectedDoctor.name }} · {{ selectedDoctor.department }}
          <span v-if="selectedDoctor.schedules?.length">，下一步查看出诊日期与剩余号源。</span>
          <span v-else>，目前暂无近期排班，可在下一步查看其他日期。</span>
        </p>
        <p v-else-if="department && !departmentDoctors.length" class="appointment-finder-note" role="status">该科室暂无医生信息，您可以选择其他科室。</p>
        <p v-else class="appointment-finder-note">{{ mode === 'department' ? '选择科室后，可直接查看科室医生，或指定医生查看时段。' : '搜索并选择医生，下一步查看出诊日期与剩余号源。' }}</p>
      </form>
    </div>
  </div>
</template>
