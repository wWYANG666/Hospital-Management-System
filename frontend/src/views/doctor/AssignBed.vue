<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import { money, statusText } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, type DoctorData } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const selected = ref<DoctorData | null>(null);
const { data, loading, saving, error, notice, loadFailed, load, action } = useDoctorPage(
  () => `/doctor/hospitalizations/${route.params.id}/assign-bed`,
  () => { selected.value = null; },
);
const departments = computed(
  () =>
    Object.entries(data.value.bedsByDepartment || {}) as [
      string,
      DoctorData[],
    ][],
);
async function assign() {
  if (!selected.value || saving.value) return;
  const bed = selected.value;
  if (!await confirmAction(
    `${data.value.patientUser?.realName || "患者"} 将分配到 ${bed.bedNumber} 床，床位费 ¥${money(bed.pricePerDay)} / 天。确认后完成住院登记并占用床位。`,
    "确认床位分配",
  )) return;
  await action(
    `/doctor/hospitalizations/${route.params.id}/assign-bed`,
    { bedId: bed.id },
    "/app/doctor/hospitalizations",
  );
}
</script>
<template>
  <div class="doctor-view">
    <PageHeader title="床位分配" description="为患者选择所属科室的可用床位。"
      ><RouterLink
        to="/app/doctor/hospitalizations"
        class="button button-secondary"
        >返回住院患者</RouterLink
      ></PageHeader
    ><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      @retry="load"
    />
    <section v-if="data.hospitalization" class="panel doctor-patient-banner">
      <span class="doctor-patient-avatar"
        ><UiIcon name="user" :size="26"
      /></span>
      <div class="doctor-patient-banner-main">
        <h2>{{ data.patientUser?.realName || "患者" }}</h2>
        <p>
          住院 #{{ data.hospitalization.id }} · 预计
          {{ data.hospitalization.expectedDays || 1 }} 天
        </p>
      </div>
      <div class="doctor-chief-complaint">
        <span>住院原因</span>
        <p>
          {{
            data.hospitalization.admissionReason ||
            data.hospitalization.diagnosis ||
            "未填写"
          }}
        </p>
      </div>
    </section>
    <section v-if="!loading && !loadFailed" class="panel">
      <div class="doctor-panel-heading">
        <div>
          <h2>可分配床位</h2>
          <p>
            {{
              data.currentDoctor?.department
                ? `${data.currentDoctor.department} · 仅显示可分配空床`
                : "按科室查看可分配空床"
            }}
          </p>
        </div>
        <span class="doctor-tag doctor-tag-success"
          ><UiIcon name="bed" :size="15" /> 空闲床位</span
        >
      </div>
      <EmptyState
        v-if="!loading && !departments.length"
        title="当前暂无可分配床位"
        description="请联系管理员维护床位或等待其他患者出院。"
      />
      <section
        v-for="[name, beds] in departments"
        :key="name"
        class="doctor-bed-department"
      >
        <h3>
          {{ name }} <small>{{ beds.length }} 张空床</small>
        </h3>
        <div class="doctor-bed-grid">
          <button
            v-for="bed in beds"
            :key="bed.id"
            type="button"
            class="doctor-bed-card"
            :class="{ selected: selected?.id === bed.id }"
            :aria-pressed="selected?.id === bed.id"
            :disabled="saving"
            @click="selected = bed"
          >
            <span class="doctor-bed-card-top"
              ><UiIcon name="bed" :size="25" /><span class="doctor-bed-check"
                ><UiIcon
                  v-if="selected?.id === bed.id"
                  name="check"
                  :size="15" /></span></span
            ><strong>{{ bed.bedNumber }}</strong
            ><span
              >{{ bed.ward || "病区未设置" }} ·
              {{ bed.roomNumber || "房间未设置" }}</span
            >
            <div>
              <span class="doctor-tag">{{ statusText(bed.bedType) }}</span
              ><b>¥{{ money(bed.pricePerDay) }}<small>/天</small></b>
            </div>
          </button>
        </div>
      </section>
    </section>
    <footer v-if="!loading && !loadFailed && departments.length" class="doctor-sticky-actions">
      <span>{{
        selected
          ? `已选择 ${selected.bedNumber} · ${selected.ward || "所属病区未设置"}`
          : "请选择一个可用床位"
      }}</span
      ><button
        class="button button-primary"
        :disabled="!selected || saving"
        @click="assign"
      >
        {{ saving ? "分配中…" : "确认分配床位" }}
      </button>
    </footer>
  </div>
</template>
