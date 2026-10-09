<script setup lang="ts">
import { reactive } from "vue";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage } from "./useDoctorPage";
import "@/styles/doctor.css";
const form = reactive({ title: "", department: "", introduction: "" });
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => "/doctor/profile",
  (value) => {
    form.title = value.doctor?.title || "";
    form.department = value.doctor?.department || "";
    form.introduction = value.doctor?.introduction || "";
  },
  () => ({ ...form }),
);
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="执业档案"
      description="维护患者端展示的医生科室、职称与个人简介。"
    /><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.doctor"
      @retry="load"
    />
    <div v-if="data.doctor" class="doctor-profile-grid">
      <aside class="panel doctor-profile-card">
        <span class="doctor-profile-avatar">{{
          String(data.user?.realName || "医").slice(0, 1)
        }}</span>
        <h2>{{ data.user?.realName || data.user?.username }}</h2>
        <p>
          {{ data.doctor.department || "科室未设置" }} ·
          {{ data.doctor.title || "职称未设置" }}
        </p>
        <span
          class="doctor-tag"
          :class="data.onDutyToday ? 'doctor-tag-success' : ''"
          >{{ data.onDutyToday ? "今日在岗" : "今日无排班" }}</span
        >
        <dl>
          <div>
            <dt>医生工号</dt>
            <dd>{{ data.doctor.doctorCode || "未设置" }}</dd>
          </div>
          <div>
            <dt>专业特长</dt>
            <dd>{{ data.doctor.specialty || "未填写" }}</dd>
          </div>
          <div>
            <dt>登录账号</dt>
            <dd>{{ data.user?.username || "未维护" }}</dd>
          </div>
        </dl>
        <RouterLink
          to="/app/doctor/schedules"
          class="button button-secondary doctor-full-width"
          ><UiIcon name="calendar" :size="17" /> 查看我的排班</RouterLink
        >
      </aside>
      <section class="panel">
        <div class="doctor-panel-heading">
          <div>
            <h2>编辑执业信息</h2>
            <p>保存后同步更新医生展示资料</p>
          </div>
        </div>
        <form @submit.prevent="action('/doctor/profile', { ...form })">
          <fieldset class="doctor-form-lock" :disabled="saving">
          <div class="form-grid">
            <label class="doctor-form-field"
              ><span>所属科室</span
              ><input
                v-model="form.department"
                placeholder="如：心血管内科" /></label
            ><label class="doctor-form-field"
              ><span>职称</span
              ><input v-model="form.title" placeholder="如：主治医师"
            /></label>
          </div>
          <label class="doctor-form-field"
            ><span>个人简介</span
            ><textarea
              v-model="form.introduction"
              rows="8"
              placeholder="介绍临床经验、专业方向与擅长诊疗领域"
            ></textarea>
          </label>
          <div class="doctor-form-actions">
            <span class="doctor-muted">供院内档案及患者挂号选择时展示</span
            ><button class="button button-primary" :disabled="saving">
              {{ saving ? "保存中…" : "保存档案" }}
            </button>
          </div>
          </fieldset>
        </form>
      </section>
    </div>
    <section v-else-if="!loading && !error" class="panel doctor-unbound">
      <UiIcon name="shield" :size="34" />
      <h2>医生档案未关联</h2>
      <p>请联系管理员维护医生账号的执业档案。</p>
    </section>
  </div>
</template>
