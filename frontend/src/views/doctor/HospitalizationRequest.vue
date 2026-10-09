<script setup lang="ts">
import { reactive } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, list } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const form = reactive({
  admissionReason: "",
  expectedDays: 1,
  requestDepartmentId: "",
});
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => `/doctor/hospitalizations/request/${route.params.appointmentId}`,
  (value) =>
    (form.requestDepartmentId = value.requestDepartmentId
      ? String(value.requestDepartmentId)
      : ""),
  () => ({ ...form }),
);
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="开立住院申请"
      description="根据本次门诊诊断，填写住院原因并选择收治科室。"
      ><RouterLink
        :to="`/app/doctor/diagnose/${route.params.appointmentId}`"
        class="button button-secondary"
        >返回诊疗记录</RouterLink
      ></PageHeader
    ><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.appointment"
      @retry="load"
    /><template v-if="data.appointment"
      ><section class="panel doctor-patient-banner">
        <span class="doctor-patient-avatar"
          ><UiIcon name="bed" :size="26"
        /></span>
        <div class="doctor-patient-banner-main">
          <h2>{{ data.patientUser?.realName || "患者" }}</h2>
          <p>关联挂号 #{{ data.appointment.id }}</p>
        </div>
        <div class="doctor-chief-complaint">
          <span>门诊诊断</span>
          <p>
            {{
              data.appointment.diagnosis ||
              data.appointment.symptoms ||
              "暂无诊断记录"
            }}
          </p>
        </div>
      </section>
      <form
        class="panel doctor-form-panel"
        @submit.prevent="
          action(
            '/doctor/hospitalizations/request',
            { ...form, appointmentId: route.params.appointmentId },
            '/app/doctor/hospitalizations',
          )
        "
      >
        <fieldset class="doctor-form-lock" :disabled="saving">
        <div class="doctor-panel-heading">
          <div>
            <h2>住院申请信息</h2>
            <p>提交后请继续分配床位，完成患者住院登记</p>
          </div>
        </div>
        <label class="doctor-form-field"
          ><span>住院原因 <b>*</b></span
          ><textarea
            v-model="form.admissionReason"
            required
            rows="5"
            placeholder="请记录收治住院的临床原因及治疗目的"
          ></textarea>
        </label>
        <div v-if="data.commonReasons?.length" class="doctor-reason-presets">
          <span>常用原因</span
          ><button
            v-for="reason in data.commonReasons"
            :key="reason"
            type="button"
            class="doctor-reason-chip"
            @click="form.admissionReason = reason"
          >
            {{ reason }}
          </button>
        </div>
        <div class="form-grid">
          <label class="doctor-form-field"
            ><span>预计住院天数 <b>*</b></span
            ><input
              v-model.number="form.expectedDays"
              type="number"
              min="1"
              max="365"
              step="1"
              required
            /><small>按病情预估，出院结算以实际住院天数计算。</small></label
          ><label class="doctor-form-field"
            ><span>收治科室 <b>*</b></span
            ><select v-model="form.requestDepartmentId" required>
              <option value="">请选择科室</option>
              <option
                v-for="dept in list(data, 'departments')"
                :key="dept.id"
                :value="String(dept.id)"
              >
                {{ dept.name }}
              </option></select
            ><small
              >当前医生所属科室：{{
                data.doctor?.department || "未设置"
              }}</small
            ></label
          >
        </div>
        <div class="doctor-form-actions">
          <RouterLink
            to="/app/doctor/hospitalizations"
            class="button button-secondary"
            >取消</RouterLink
          ><button class="button button-primary" :disabled="saving">
            {{ saving ? "提交中…" : "提交住院申请" }}
          </button>
        </div>
        </fieldset>
      </form></template
    >
  </div>
</template>
